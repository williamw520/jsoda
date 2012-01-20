
package wwutil.jsoda;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
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
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.GetAttributesResult;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.BatchDeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.DeletableItem;
import com.amazonaws.services.simpledb.model.UpdateCondition;
import com.amazonaws.services.simpledb.util.SimpleDBUtils;

import wwutil.model.MemCacheable;
import wwutil.model.annotation.CachePolicy;
import wwutil.model.annotation.DefaultGUID;
import wwutil.model.annotation.DefaultComposite;
import wwutil.model.annotation.CacheByField;


/**
 */
class SimpleDBMgr implements DbService
{
    public static final String      ITEM_NAME = "itemName()";


    private Jsoda                   jsoda;
    private AmazonSimpleDBClient    sdbClient;
    

    // AWS Access Key ID and Secret Access Key
    public SimpleDBMgr(Jsoda jsoda, AWSCredentials cred)
        throws Exception
    {
        this.jsoda = jsoda;
        this.sdbClient = new AmazonSimpleDBClient(cred);
    }

    public void shutdown() {
        sdbClient.shutdown();
    }

    // Delegated SimpleDB API

    public void createTable(String table) {
        sdbClient.createDomain(new CreateDomainRequest(table));
    }

    public void deleteTable(String table) {
        sdbClient.deleteDomain(new DeleteDomainRequest(table));
    }

    public List<String> listTables() {
        ListDomainsResult   list = sdbClient.listDomains();
        return list.getDomainNames();
    }

    public void putObj(String modelName, Object dataObj, String expectedField, Object expectedValue)
        throws Exception
    {
        String  table = jsoda.getModelTable(modelName);
        String  idValue = DataUtil.getFieldValueStr(dataObj, jsoda.getIdField(modelName));
        PutAttributesRequest    req =
            expectedField == null ?
                new PutAttributesRequest(table, idValue, buildAttrs(dataObj, modelName)) :
                new PutAttributesRequest(table, idValue, buildAttrs(dataObj, modelName),
                                         buildExpectedValue(modelName, expectedField, expectedValue));
        sdbClient.putAttributes(req);
    }

    public void putObjs(String modelName, List dataObjs)
        throws Exception
    {
        String  table = jsoda.getModelTable(modelName);
        sdbClient.batchPutAttributes(new BatchPutAttributesRequest(table, buildPutItems(dataObjs, modelName)));
    }

    public Object getObj(String modelName, String id)
        throws Exception
    {
        String              table = jsoda.getModelTable(modelName);
        String              idValue = DataUtil.toValueStr(id);
        GetAttributesResult result = sdbClient.getAttributes(new GetAttributesRequest(table, idValue));
        if (result.getAttributes().size() == 0)
            return null;        // not existed.

        return buildLoadObj(modelName, id, result.getAttributes());
    }

    public void delete(String modelName, String id)
        throws Exception
    {
        String  table = jsoda.getModelTable(modelName);
        String  idValue = DataUtil.toValueStr(id);
        sdbClient.deleteAttributes(new DeleteAttributesRequest(table, idValue));
    }

    public void batchDelete(String modelName, List<String> idList)
        throws Exception
    {
        String  table = jsoda.getModelTable(modelName);
        List<DeletableItem> items = new ArrayList<DeletableItem>();
        for (String id : idList) {
            String  idValue = DataUtil.toValueStr(id);
            items.add(new DeletableItem().withName(idValue));
        }
        sdbClient.batchDeleteAttributes(new BatchDeleteAttributesRequest(table, items));
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

    // <T> List<T> runQuery(Class<T> modelClass, SelectRequest request)
    //     throws Exception
    // {
    //     String  modelName = getModelName(modelClass);
    //     List<T> resultObjs = new ArrayList<T>();

    //     try {
    //         for (Item item : sdbClient.select(request).getItems()) {
    //             T   obj = buildLoadObj(modelClass, modelName, item.getName(), item.getAttributes());
    //             resultObjs.add(obj);
    //         }
    //         // TODO: add caching list
    //         return resultObjs;
    //     } catch(Exception e) {
    //         throw new Exception("Select failed.  Query: " + request.getSelectExpression() + "  Error: " + e.getMessage(), e);
    //     }
    // }

    private List<ReplaceableAttribute> buildAttrs(Object dataObj, String modelName)
        throws Exception
    {
        Field[]                     fields = jsoda.modelAttrFields.get(modelName);
        Map<String, String>         fieldAttrMap = jsoda.modelFieldAttrMap.get(modelName);
        List<ReplaceableAttribute>  attrs = new ArrayList<ReplaceableAttribute>();

        for (Field field : fields) {
            String  attrName = fieldAttrMap.get(field.getName());
            String  fieldValue = DataUtil.getFieldValueStr(dataObj, field);
            attrs.add(new ReplaceableAttribute(attrName, fieldValue, true));
        }

        return attrs;
    }

    private UpdateCondition buildExpectedValue(String modelName, String expectedField, Object expectedValue)
        throws Exception
    {
        Map<String, Field>  attrFieldMap = jsoda.modelAttrFieldMap.get(modelName);
        Map<String, String> fieldAttrMap = jsoda.modelFieldAttrMap.get(modelName);
        Field               field = attrFieldMap.get(expectedField);
        String              attrName = fieldAttrMap.get(field.getName());
        String              fieldValue = DataUtil.toValueStr(expectedValue);
        return new UpdateCondition(attrName, fieldValue, true);
    }

    private List<ReplaceableItem> buildPutItems(List dataObjs, String modelName)
        throws Exception
    {
        String                  table = jsoda.modelTables.get(modelName);
        Field[]                 fields = jsoda.modelAttrFields.get(modelName);
        List<ReplaceableItem>   items = new ArrayList<ReplaceableItem>();
        String                  idValue;

        for (Object dataObj : dataObjs) {
            idValue = DataUtil.getFieldValueStr(dataObj, jsoda.getIdField(modelName));
            items.add(new ReplaceableItem(idValue, buildAttrs(dataObj, modelName)));
        }
        return items;
    }

    private Object buildLoadObj(String modelName, String id, List<Attribute> attrs)
        throws Exception
    {
        Class               modelClass = jsoda.getModelClass(modelName);
        Object              obj = modelClass.newInstance();
        Map<String, Field>  attrFieldMap = jsoda.modelAttrFieldMap.get(modelName);

        // Set the attr field 
        for (Attribute attr : attrs) {
            String  attrName  = attr.getName();
            String  fieldValue = attr.getValue();
            Field   field = attrFieldMap.get(attrName);

            if (field == null) {
                // TODO: log warning
                //throw new Exception("Attribute name " + attrName + " has no corresponding field in object " + modelClass);
                //logger.severe("Attribute name " + attrName + " has no corresponding field in object " + modelClass);
                continue;
            }

            DataUtil.setFieldValueStr(obj, field, fieldValue);
        }

        // Set the Id field
        DataUtil.setFieldValueStr(obj, jsoda.getIdField(modelName), id);

        return obj;
    }

}

