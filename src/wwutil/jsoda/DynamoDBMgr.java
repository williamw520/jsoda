
package wwutil.jsoda;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.persistence.PrePersist;
import javax.persistence.PostLoad;

import org.apache.commons.beanutils.ConvertUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.CreateTableRequest;
import com.amazonaws.services.dynamodb.model.DeleteTableRequest;
import com.amazonaws.services.dynamodb.model.ListTablesResult;
import com.amazonaws.services.dynamodb.model.KeySchema;
import com.amazonaws.services.dynamodb.model.KeySchemaElement;
import com.amazonaws.services.dynamodb.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.PutItemRequest;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ExpectedAttributeValue;
import com.amazonaws.services.dynamodb.model.GetItemRequest;
import com.amazonaws.services.dynamodb.model.GetItemResult;
import com.amazonaws.services.dynamodb.model.DeleteItemRequest;

import wwutil.model.MemCacheable;
import wwutil.model.annotation.AModel;
import wwutil.model.annotation.CachePolicy;
import wwutil.model.annotation.DefaultGUID;
import wwutil.model.annotation.DefaultComposite;
import wwutil.model.annotation.CacheByField;


/**
 */
class DynamoDBMgr implements DbService
{
    private Jsoda                   jsoda;
    private AmazonDynamoDBClient    ddbClient;
    

    // AWS Access Key ID and Secret Access Key
    public DynamoDBMgr(Jsoda jsoda, AWSCredentials cred) {
        this.jsoda = jsoda;
        this.ddbClient = new AmazonDynamoDBClient(cred);
    }

    public void shutdown() {
        ddbClient.shutdown();
    }

    public AModel.DbType getDbType() {
        return AModel.DbType.DynamoDB;
    }

    public void setDbEndpoint(String endpoint) {
        ddbClient.setEndpoint(endpoint);
    }


    // Delegated Dynamodb API

    public void createModelTable(String modelName) {
        Class       modelClass = jsoda.getModelClass(modelName);
        String      table = jsoda.getModelTable(modelName);
        Field       idField = jsoda.getIdField(modelName);
        String      idName = getFieldAttrName(modelName, idField.getName());
        Field       rangeField = jsoda.getRangeField(modelName);
        KeySchema   key = new KeySchema();
        Long        readTP  = ReflectUtil.getAnnotationValueEx(modelClass, AModel.class, "readThroughput", Long.class, new Long(10));
        Long        writeTP = ReflectUtil.getAnnotationValueEx(modelClass, AModel.class, "writeThroughput", Long.class, new Long(5));

        key.setHashKeyElement(makeKeySchemaElement(idField));
        if (rangeField != null)
            key.setRangeKeyElement(makeKeySchemaElement(rangeField));

        ddbClient.createTable(new CreateTableRequest(table, key)
                              .withProvisionedThroughput(new ProvisionedThroughput()
                                                         .withReadCapacityUnits(readTP)
                                                         .withWriteCapacityUnits(writeTP)));
    }

    private KeySchemaElement makeKeySchemaElement(Field field) {
        KeySchemaElement    elem = new KeySchemaElement();
        String              attrType;

        if (isN(field.getType()))
            attrType = "N";
        else
            attrType = "S";     // everything else has string attribute type.
        
        return elem.withAttributeName(field.getName()).withAttributeType(attrType);
    }

    public void deleteTable(String tableName) {
        ddbClient.deleteTable(new DeleteTableRequest(tableName));
    }

    public List<String> listTables() {
        ListTablesResult   list = ddbClient.listTables();
        return list.getTableNames();
    }

    public void putObj(String modelName, Object dataObj, String expectedField, Object expectedValue)
        throws Exception
    {
        String  table = jsoda.getModelTable(modelName);
        PutItemRequest  req = new PutItemRequest(table, objToAttrs(dataObj, modelName));

        if (expectedField != null)
            req.setExpected(makeExpectedMap(modelName, expectedField, expectedValue));

        ddbClient.putItem(req);
    }

    public void putObjs(String modelName, List dataObjs)
        throws Exception
    {
        // Dynamodb has no batch put support.  Emulate it.
        for (Object obj : dataObjs)
            putObj(modelName, obj, null, null);
    }

    public Object getObj(String modelName, Object id)
        throws Exception
    {
        return getObj(modelName, id, null);
    }

    public Object getObj(String modelName, Object id, Object rangeKey)
        throws Exception
    {
        String          table = jsoda.getModelTable(modelName);
        GetItemRequest  req = new GetItemRequest(table, makeKey(modelName, id, rangeKey));
        GetItemResult   result = ddbClient.getItem(req);

        if (result.getItem() == null || result.getItem().size() == 0)
            return null;        // not existed.

        return itemToObj(modelName, result.getItem());
    }

    public Object getObj(String modelName, String field1, Object key1, Object... fieldKeys)
        throws Exception
    {
        throw new UnsupportedOperationException("Unsupported method");
    }

    public void delete(String modelName, String id)
        throws Exception
    {
        delete(modelName, id, null);
    }

    public void delete(String modelName, String id, Object rangeKey)
        throws Exception
    {
        String  table = jsoda.getModelTable(modelName);
        ddbClient.deleteItem(new DeleteItemRequest(table, makeKey(modelName, id, rangeKey)));
    }

    public void batchDelete(String modelName, List<String> idList)
        throws Exception
    {
        for (String id : idList)
            delete(modelName, id, null);
    }

    
    // /** Get by a field beside the id */
    // public <T> T findBy(Class<T> modelClass, String field, Object fieldValue)
    //     throws Exception
    // {
    //     String  modelName = getModelName(modelClass);
    //     T       obj = (T)cacheGet(modelName, field, fieldValue);
    //     if (obj != null)
    //         return obj;

    //     List<T> items = query(modelClass).filter(field, "=", fieldValue).run();
    //     // runQuery() has already cached the object.  No need to cache it here.
    //     return items.size() == 0 ? null : items.get(0);
    // }

    // public <T> SdbQuery<T> query(Class<T> modelClass)
    //     throws Exception
    // {
    //     SdbQuery<T> query = new SdbQuery<T>(this, modelClass);
    //     return query;
    // }

    public <T> List<T> runQuery(Class<T> modelClass, String queryStr)
        throws JsodaException
    {
        // String          modelName = jsoda.getModelName(modelClass);
        // List<T>         resultObjs = new ArrayList<T>();
        // SelectRequest   request = new SelectRequest(queryStr);

        // try {
        //     for (Item item : ddbClient.select(request).getItems()) {
        //         T   obj = (T)itemToObj(modelName, item.getName(), item.getAttributes());
        //         resultObjs.add(obj);
        //     }
        //     return resultObjs;
        // } catch(Exception e) {
        //     throw new JsodaException("Query failed.  Query: " + request.getSelectExpression() + "  Error: " + e.getMessage(), e);
        // }
        return null;
    }

    public String getFieldAttrName(String modelName, String fieldName) {
        String  attrName = jsoda.getFieldAttrMap(modelName).get(fieldName);
        return attrName;
    }


    private boolean isN(Class valueType) {
        if (valueType == Integer.class || valueType == int.class)
            return true;
        if (valueType == Long.class || valueType == long.class)
            return true;
        if (valueType == Float.class || valueType == float.class)
            return true;
        if (valueType == Double.class || valueType == double.class)
            return true;

        return false;
    }

    private AttributeValue valueToAttr(Field field, Object value) {
        // TODO: handle NumberSet and StringSet
        if (isN(field.getType())) {
            return new AttributeValue().withN(value.toString());
        } else {
            return new AttributeValue().withS(DataUtil.toValueStr(value, field.getType()));
        }
    }

    private Object attrToValue(Field field, AttributeValue attr)
        throws Exception
    {
        // TODO: handle NumberSet and StringSet
        if (isN(field.getType())) {
            return ConvertUtils.convert(attr.getN(), field.getType());
        } else {
            return DataUtil.toValueObj(attr.getS(), field.getType());
        }
    }

    private Map<String, AttributeValue> objToAttrs(Object dataObj, String modelName)
        throws Exception
    {
        Map<String, AttributeValue> attrs = new HashMap<String, AttributeValue>();

        // TODO: test load object in DynamoDB
        for (Map.Entry<String, String> fieldAttr : jsoda.getFieldAttrMap(modelName).entrySet()) {
            String  fieldName = fieldAttr.getKey();
            String  attrName  = fieldAttr.getValue();
            Field   field = jsoda.getField(modelName, fieldName);
            Object  fieldValue = field.get(dataObj);
            attrs.put(attrName, valueToAttr(field, fieldValue));
        }

        // Field   idField = jsoda.getIdField(modelName);
        // String  attrName = idField.getName();           // TODO: The HashKey attribute name is same as the Id field for now.  See if a mapping is needed via annotation.
        // Object  idValue = idField.get(dataObj);
        // attrs.put(attrName, valueToAttr(idField, idValue));

        return attrs;
    }

    private Key makeKey(String modelName, Object id, Object rangeKey)
        throws Exception
    {
        Field       idField = jsoda.getIdField(modelName);
        Field       rangeField = jsoda.getRangeField(modelName);
        if (rangeField == null)
            return new Key(valueToAttr(idField, id));
        else {
            if (rangeKey == null)
                throw new IllegalArgumentException("Missing range key for the composite primary key (id,rangekey) of " + modelName);
            return new Key(valueToAttr(idField, id), valueToAttr(rangeField, rangeKey));
        }
    }

    private Map<String, ExpectedAttributeValue> makeExpectedMap(String modelName, String expectedField, Object expectedValue)
        throws Exception
    {
        String      attrName = jsoda.getFieldAttrMap(modelName).get(expectedField);
        Field       field = jsoda.getField(modelName, expectedField);
        Map<String, ExpectedAttributeValue> expectedMap = new HashMap<String, ExpectedAttributeValue>();
        expectedMap.put(attrName, new ExpectedAttributeValue(true).withValue(valueToAttr(field, expectedValue)));
        return expectedMap;
    }

    private Object itemToObj(String modelName, Map<String, AttributeValue> attrs)
        throws Exception
    {
        Class       modelClass = jsoda.getModelClass(modelName);
        Object      dataObj = modelClass.newInstance();

        // Set the attr field 
        for (String attrName : attrs.keySet()) {
            Field   field = jsoda.getFieldByAttr(modelName, attrName);

            if (field == null) {
                //throw new Exception("Attribute name " + attrName + " has no corresponding field in object " + modelClass);
                // TODO: Enable logger to log warning
                //logger.warn("Attribute name " + attrName + " has no corresponding field in object " + modelClass);
                continue;
            }

            AttributeValue  attr = attrs.get(attrName);
            Object          fieldValue = attrToValue(field, attr);
            field.set(dataObj, fieldValue);
        }

        return dataObj;
    }

}

