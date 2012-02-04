
package wwutil.jsoda;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
import com.amazonaws.services.dynamodb.model.ComparisonOperator;
import com.amazonaws.services.dynamodb.model.QueryRequest;
import com.amazonaws.services.dynamodb.model.QueryResult;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;
import com.amazonaws.services.dynamodb.model.Condition;

import wwutil.model.MemCacheable;
import wwutil.model.annotation.DbType;
import wwutil.model.annotation.AModel;
import wwutil.model.annotation.CachePolicy;
import wwutil.model.annotation.DefaultGUID;
import wwutil.model.annotation.DefaultComposite;
import wwutil.model.annotation.CacheByField;


/**
 */
class DynamoDBService implements DbService
{
    private static Log  log = LogFactory.getLog(DynamoDBService.class);

    
    static final Map<String, ComparisonOperator>    sOperatorMap = new HashMap<String, ComparisonOperator>(){{
            put(Filter.NULL,        ComparisonOperator.NULL);
            put(Filter.NOT_NULL,    ComparisonOperator.NOT_NULL);
            put(Filter.EQ,          ComparisonOperator.EQ);
            put(Filter.NE,          ComparisonOperator.NE);
            put(Filter.LE,          ComparisonOperator.LE);
            put(Filter.LT,          ComparisonOperator.LT);
            put(Filter.GE,          ComparisonOperator.GE);
            put(Filter.GT,          ComparisonOperator.GT);
            put(Filter.CONTAINS,    ComparisonOperator.CONTAINS);
            put(Filter.NOT_CONTAINS, ComparisonOperator.NOT_CONTAINS);
            put(Filter.BEGINS_WITH, ComparisonOperator.BEGINS_WITH);
            put(Filter.BETWEEN,     ComparisonOperator.BETWEEN);
            put(Filter.IN,          ComparisonOperator.IN);
        }};


    private Jsoda                   jsoda;
    private AmazonDynamoDBClient    ddbClient;
    

    // AWS Access Key ID and Secret Access Key
    public DynamoDBService(Jsoda jsoda, AWSCredentials cred) {
        this.jsoda = jsoda;
        this.ddbClient = new AmazonDynamoDBClient(cred);
    }

    public void shutdown() {
        ddbClient.shutdown();
    }

    public DbType getDbType() {
        return DbType.DynamoDB;
    }

    public String getDbTypeId() {
        return "DYN";
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

    public void putObj(String modelName, Object dataObj, String expectedField, Object expectedValue, boolean expectedExists)
        throws Exception
    {
        String  table = jsoda.getModelTable(modelName);
        PutItemRequest  req = new PutItemRequest(table, objToAttrs(dataObj, modelName));

        if (expectedField != null)
            req.setExpected(makeExpectedMap(modelName, expectedField, expectedValue, expectedExists));

        ddbClient.putItem(req);
    }

    public void putObjs(String modelName, List dataObjs)
        throws Exception
    {
        // Dynamodb has no batch put support.  Emulate it.
        for (Object obj : dataObjs)
            putObj(modelName, obj, null, null, false);
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
        // TODO: implements batchGet
        throw new UnsupportedOperationException("Unsupported method");
    }

    public void delete(String modelName, Object id)
        throws Exception
    {
        delete(modelName, id, null);
    }

    public void delete(String modelName, Object id, Object rangeKey)
        throws Exception
    {
        String  table = jsoda.getModelTable(modelName);
        ddbClient.deleteItem(new DeleteItemRequest(table, makeKey(modelName, id, rangeKey)));
    }

    public void batchDelete(String modelName, List idList)
        throws Exception
    {
        for (Object id : idList)
            delete(modelName, id, null);
    }

    public void batchDelete(String modelName, List idList, List rangeKeyList)
        throws Exception
    {
        for (int i = 0; i < idList.size(); i++) {
            delete(modelName, idList.get(i), rangeKeyList.get(i));
        }
    }

    public void validateFilterOperator(String operator) {
        if (sOperatorMap.get(operator) == null)
            throw new UnsupportedOperationException("Unsupported operator " + operator);
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

    @SuppressWarnings("unchecked")
    public <T> long queryCount(Class<T> modelClass, Query<T> query)
        throws JsodaException
    {
        String          modelName = jsoda.getModelName(modelClass);
        QueryRequest    queryReq = new QueryRequest();
        ScanRequest     scanReq = new ScanRequest();

        try {
            if (toRequest(query, queryReq, scanReq)) {
                return ddbClient.query(queryReq).getCount().intValue();
            } else {
                return ddbClient.scan(scanReq).getCount().intValue();
            }
        } catch(Exception e) {
            throw new JsodaException("Query failed.  Error: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> queryRun(Class<T> modelClass, Query<T> query, boolean continueFromLastRun)
        throws JsodaException
    {
        List<T>         resultObjs = new ArrayList<T>();

        if (continueFromLastRun && !queryHasNext(query))
            return resultObjs;

        QueryRequest    queryReq = new QueryRequest();
        ScanRequest     scanReq = new ScanRequest();
        List<Map<String,AttributeValue>>    items;

        try {
            if (toRequest(query, queryReq, scanReq)) {
                if (continueFromLastRun)
                    queryReq.setExclusiveStartKey((Key)query.nextKey);
                QueryResult result = ddbClient.query(queryReq);
                query.nextKey = result.getLastEvaluatedKey();
                items = result.getItems();
            } else {
                if (continueFromLastRun)
                    queryReq.setExclusiveStartKey((Key)query.nextKey);
                ScanResult  result = ddbClient.scan(scanReq);
                query.nextKey = result.getLastEvaluatedKey();
                items = result.getItems();
            }
            for (Map<String, AttributeValue> item : items) {
                T   obj = (T)itemToObj(query.modelName, item);
                resultObjs.add(obj);
            }
            return resultObjs;
        } catch(Exception e) {
            throw new JsodaException("Query failed.  Error: " + e.getMessage(), e);
        }
    }

    public <T> boolean queryHasNext(Query<T> query) {
        return query.nextKey != null;
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

    private boolean isMultiValuetype(Class valueType) {
        if (valueType != null &&
            (valueType == Integer.class || 
             valueType == Long.class || 
             valueType == Float.class || 
             valueType == Double.class || 
             valueType == String.class))
            return true;
        return false;
    }

    private AttributeValue valueToAttr(Field field, Object value) {
        // Don't set the AttributeValue for null value
        if (value == null)
            return null;

        // Handle Set<String>, Set<Long>, or Set<Integer> field.
        if (Set.class.isAssignableFrom(field.getType())) {
            Class   paramType = ReflectUtil.getGenericParamType1(field.getGenericType());
            if (isMultiValuetype(paramType)) {
                if (isN(paramType)) {
                    return new AttributeValue().withNS(DataUtil.toStringSet((Set)value, paramType));
                } else {
                    return new AttributeValue().withSS(DataUtil.toStringSet((Set)value, paramType));
                }
            }
        }

        // Handle number types
        if (isN(field.getType())) {
            return new AttributeValue().withN(value.toString());
        }

        // Delegate to DataUtil to encode the rest.
        return new AttributeValue().withS(DataUtil.encodeValueToAttrStr(value, field.getType()));
    }

    private Object attrToValue(Field field, AttributeValue attr)
        throws Exception
    {
        // Handle Set<String>, Set<Long>, or Set<Integer> field.
        if (Set.class.isAssignableFrom(field.getType())) {
            Class   paramType = ReflectUtil.getGenericParamType1(field.getGenericType());
            if (isMultiValuetype(paramType)) {
                if (isN(paramType))
                    return DataUtil.toObjectSet(attr.getNS(), paramType);
                else
                    return DataUtil.toObjectSet(attr.getSS(), paramType);
            }
        }

        // Handle number types
        if (isN(field.getType())) {
            return ConvertUtils.convert(attr.getN(), field.getType());
        }
        
        // Delegate to DataUtil to decode the rest.
        return DataUtil.decodeAttrStrToValue(attr.getS(), field.getType());
    }

    private Map<String, AttributeValue> objToAttrs(Object dataObj, String modelName)
        throws Exception
    {
        Map<String, AttributeValue> attrs = new HashMap<String, AttributeValue>();

        for (Map.Entry<String, String> fieldAttr : jsoda.getFieldAttrMap(modelName).entrySet()) {
            String  fieldName = fieldAttr.getKey();
            String  attrName  = fieldAttr.getValue();
            Field   field = jsoda.getField(modelName, fieldName);
            Object  fieldValue = field.get(dataObj);
            AttributeValue  attr = valueToAttr(field, fieldValue);
            if (attr != null)
                attrs.put(attrName, attr);
            // Skip setting attribute if it's null.
        }

        return attrs;
    }

    private Key makeKey(String modelName, Object id, Object rangeKey)
        throws Exception
    {
        if (id == null)
            throw new IllegalArgumentException("Id cannot be null.");

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

    private Map<String, ExpectedAttributeValue> makeExpectedMap(String modelName, String expectedField, Object expectedValue, boolean expectedExists)
        throws Exception
    {
        if (expectedValue == null)
            throw new IllegalArgumentException("ExpectedValue cannot be null.");

        String      attrName = jsoda.getFieldAttrMap(modelName).get(expectedField);
        Field       field = jsoda.getField(modelName, expectedField);
        ExpectedAttributeValue  cond;

        if (expectedExists) {
            cond = new ExpectedAttributeValue(expectedExists).withValue(valueToAttr(field, expectedValue));
        } else {
            cond = new ExpectedAttributeValue(expectedExists);
        }

        Map<String, ExpectedAttributeValue> expectedMap = new HashMap<String, ExpectedAttributeValue>();
        expectedMap.put(attrName, cond);
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
                //throw new Exception("Attribute " + attrName + " from db has no corresponding field in object " + modelClass);
                log.warn("Attribute " + attrName + " from db has no corresponding field in model class " + modelClass);
                continue;
            }

            AttributeValue  attr = attrs.get(attrName);
            Object          fieldValue = attrToValue(field, attr);
            field.set(dataObj, fieldValue);
        }

        return dataObj;
    }

    private <T> boolean toRequest(Query<T> query, QueryRequest queryReq, ScanRequest scanReq) {
        addSelect(query, queryReq, scanReq);
        addFrom(query, queryReq, scanReq);
        boolean doQuery = addFilter(query, queryReq, scanReq);
        addOrderby(query, queryReq, scanReq, doQuery);
        addLimit(query, queryReq, scanReq, doQuery);
        queryReq.setConsistentRead(query.consistentRead);
        return doQuery;
    }

    private <T> void addSelect(Query<T> query, QueryRequest queryReq, ScanRequest scanReq) {
        if (query.selectTerms.size() == 0)
            return;

        boolean             hasId = false;
        boolean             hasRangeKey = false;
        Collection<String>  attributesToGet = new ArrayList<String>();
        for (String fieldName : query.selectTerms) {
            attributesToGet.add(getFieldAttrName(query.modelName, fieldName));
            if (jsoda.isIdField(query.modelName, fieldName))
                hasId = true;
            if (jsoda.isRangeField(query.modelName, fieldName))
                hasRangeKey = true;
        }

        // Always add the Id field (and rangeKey) for the result attributes.
        if (!hasId)
            attributesToGet.add(getFieldAttrName(query.modelName, jsoda.getIdField(query.modelName).getName()));
        Field   rangeField = jsoda.getRangeField(query.modelName);
        if (!hasRangeKey && rangeField != null)
            attributesToGet.add(getFieldAttrName(query.modelName, rangeField.getName()));

        queryReq.setAttributesToGet(attributesToGet);
        scanReq.setAttributesToGet(attributesToGet);
    }

    private <T> void addFrom(Query<T> query, QueryRequest queryReq, ScanRequest scanReq) {
        queryReq.setTableName(jsoda.getModelTable(query.modelName));
        scanReq.setTableName(jsoda.getModelTable(query.modelName));
    }

    private <T> boolean addFilter(Query<T> query, QueryRequest queryReq, ScanRequest scanReq) {
        boolean         hasIdEq = false;
        boolean         hasRange = false;
        boolean         doQuery;

        // Find out if query has an Id EQ filter and a Range filter.
        for (Filter filter : query.filters) {
            if (jsoda.isIdField(query.modelName, filter.fieldName)) {
                if (filter.operator.equals(Filter.EQ))
                    hasIdEq = true;
            } else if (jsoda.isRangeField(query.modelName, filter.fieldName)) {
                hasRange = true;
            }
        }

        doQuery = (hasIdEq && hasRange);

        if (doQuery) {
            log.info("Query results in a DynamoDB query.");
        } else {
            log.info("Query results in a DynamoDB scan.");
            addScanFilter(query, scanReq);
        }
        return doQuery;
    }

    private <T> void addQueryFilter(Query<T> query, QueryRequest queryReq) {
        AttributeValue  hashKeyValue = null;
        Condition       rangeCondition = null;

        for (Filter filter : query.filters) {
            if (jsoda.isIdField(query.modelName, filter.fieldName)) {
                if (filter.operator.equals(Filter.EQ))
                    if (filter.operand == null)
                        throw new IllegalArgumentException("Operand of EQ cannot be null.");
                    else
                        hashKeyValue = valueToAttr(jsoda.getIdField(query.modelName), filter.operand);
                else
                    throw new IllegalArgumentException("Only EQ condition is allowed on the Id field for DynamoDB.");
            } else if (jsoda.isRangeField(query.modelName, filter.fieldName)) {
                rangeCondition = toCondition(filter);
            } else {
                throw new IllegalArgumentException("Condition on " + filter.fieldName +
                                                   " is not supported.  DynamoDB only supports condition on the Id field and the RangeKey field.");
            }
        }

        if (hashKeyValue != null)
            queryReq.setHashKeyValue(hashKeyValue);
        else
            throw new IllegalArgumentException("Missing an EQ condition for the Id field.  DynamoDB query requires the HashKey value from the Id field.");
        if (rangeCondition != null)
            queryReq.setRangeKeyCondition(rangeCondition);
    }

    private <T> void addScanFilter(Query<T> query, ScanRequest scanReq) {
        Map<String,Condition>   conditions = new HashMap<String,Condition>();

        for (Filter filter : query.filters) {
            String  attrName = jsoda.getFieldAttrMap(query.modelName).get(filter.fieldName);
            conditions.put(attrName, toCondition(filter));
        }
        scanReq.setScanFilter(conditions);
    }

    private Condition toCondition(Filter filter) {

        if (Filter.BINARY_OPERATORS.contains(filter.operator)) {
            if (filter.operand == null)
                throw new IllegalArgumentException("Operand of a condition cannot be null.");
            return new Condition()
                .withComparisonOperator(sOperatorMap.get(filter.operator))
                .withAttributeValueList(valueToAttr(filter.field, filter.operand));
        }

        if (Filter.UNARY_OPERATORS.contains(filter.operator)) {
            return new Condition()
                .withComparisonOperator(sOperatorMap.get(filter.operator));
        }

        if (Filter.TRINARY_OPERATORS.contains(filter.operator)) {
            if (filter.operand == null || filter.operand2 == null)
                throw new IllegalArgumentException("Operand of a condition cannot be null.");
            return new Condition()
                .withComparisonOperator(sOperatorMap.get(filter.operator))
                .withAttributeValueList(valueToAttr(filter.field, filter.operand),
                                        valueToAttr(filter.field, filter.operand2));
        }

        if (Filter.LIST_OPERATORS.contains(filter.operator)) {
            Condition   cond = new Condition()
                .withComparisonOperator(sOperatorMap.get(filter.operator));
            List<AttributeValue>    attrs = new ArrayList<AttributeValue>();
            for (Object valueObj : filter.operands) {
                attrs.add(valueToAttr(filter.field, valueObj));
            }
            cond.setAttributeValueList(attrs);
            return cond;
        }

        throw new UnsupportedOperationException("Condition operator " + filter.operator + " not supported.");
    }

    private <T> void addOrderby(Query<T> query, QueryRequest queryReq, ScanRequest scanReq, boolean doQuery) {
        for (String orderby : query.orderbyFields) {
            String  fieldName = orderby.substring(1);
            boolean forward = orderby.charAt(0) == '+';

            if (!jsoda.isRangeField(query.modelName, fieldName))
                throw new IllegalArgumentException("Field " + fieldName + " is not the Range Key field.  DynamoDB only supports order by on the Range Key field.");

            if (doQuery)
                queryReq.setScanIndexForward(forward);
            else
                throw new IllegalArgumentException("DynamoDB doesn't support order by on scanning.  Use Id and Range Key conditions to form a query for order by.");
        }
    }

    private <T> void addLimit(Query<T> query, QueryRequest queryReq, ScanRequest scanReq, boolean doQuery) {
        if (query.limit > 0) {
            if (doQuery)
                queryReq.setLimit(query.limit);
            else
                scanReq.setLimit(query.limit);
        }
    }

}

