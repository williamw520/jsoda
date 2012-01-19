
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
 * Simple object-db mapping service for AWS.  Make storing POJO simple in SimpleDB.
 * See utests for usage.
 */
public class Jsoda
{
    public static final String      ITEM_NAME = "itemName()";


    private MemCacheable            memCacheable;

    private Map<String, Class>      modelClasses = new HashMap<String, Class>();
    private Map<String, String>     modelTables = new HashMap<String, String>();
    private Map<String, Field>      modelIdFields = new HashMap<String, Field>();
    private Map<String, Field[]>    modelAllFields = new HashMap<String, Field[]>();
    private Map<String, Field[]>    modelAttrFields = new HashMap<String, Field[]>();
    private Map<String, Map<String, Field>>     modelAllFieldMap = new HashMap<String, Map<String, Field>>();
    private Map<String, Map<String, Field>>     modelAttrFieldMap = new HashMap<String, Map<String, Field>>();
    private Map<String, Map<String, String>>    modelFieldAttrMap = new HashMap<String, Map<String, String>>();
    private Map<String, Set<String>>            modelCacheByFields = new HashMap<String, Set<String>>();
    private Map<String, Method>     modelPrePersistMethod = new HashMap<String, Method>();
    private Map<String, Method>     modelPostLoadMethod = new HashMap<String, Method>();


    // AWS Access Key ID and Secret Access Key
    public Jsoda(AWSCredentials cred)
        throws Exception
    {
        this(cred, null);
    }

    public Jsoda(AWSCredentials cred, MemCacheable memCacheable)
        throws Exception
    {
        //this.sdbClient = new AmazonSimpleDBClient(cred);
        this.memCacheable = memCacheable;
    }

    public void shutdown() {
        modelClasses.clear();
        modelTables.clear();
        //sdbClient.shutdown();
    }

    /** Register a POJO model class.  Calling again will re-register the mode class.
     */
    public void registerModel(Class modelClass)
        throws JsodaException
    {
        try {
            String      modelName = getModelName(modelClass);
            String      tableName = ReflectUtil.getAnnotationValue(modelClass, Table.class, "name", modelName);
            Field       idField = ReflectUtil.findAnnotatedField(modelClass, Id.class);
            Field[]     allFields = getAllFields(modelClass);
            Field[]     attrFields = getAttrFields(allFields);

            if (idField == null)
                throw new ValidationException("Missing annotated Id field in the model class.");
            if (ReflectUtil.hasAnnotation(idField, Transient.class))
                throw new ValidationException("The Id field cannot be Transient in the model class.");
            if (idField.getType() != String.class)
                throw new ValidationException("The Id field can only be String.");

            modelClasses.put(modelName, modelClass);
            modelTables.put(modelName, tableName);
            modelIdFields.put(modelName, idField);
            modelAllFields.put(modelName, allFields);                       // Save all fields, including the Id field
            modelAllFieldMap.put(modelName, toFieldMap(allFields));
            modelAttrFields.put(modelName, attrFields);
            modelAttrFieldMap.put(modelName, toAttrFieldMap(attrFields));
            modelFieldAttrMap.put(modelName, toFieldAttrMap(attrFields));
            modelCacheByFields.put(modelName, toCacheByFields(allFields));  // Build CacheByFields on all fields, including the Id field
            Method  prepersistMethod = toPrePersistMethod(modelClass);
            Method  postLoadMethod = toPostLoadMethod(modelClass);
            if (prepersistMethod != null)
                modelPrePersistMethod.put(modelName, prepersistMethod);
            if (postLoadMethod != null)
                modelPostLoadMethod.put(modelName, postLoadMethod);
        } catch(JsodaException je) {
            throw je;
        } catch(Exception e) {
            throw new JsodaException("Failed to register model class", e);
        }
    }

    /** Return all registered model classes as a map. */
    public Map<String, Class> getModelClasses() {
        return modelClasses;
    }

    /** Return the model name of a model class. */
    public static String getModelName(Class modelClass) {
		int		index;
        String  name = modelClass.getName();

		index = name.lastIndexOf(".");
        name = (index == -1 ? name : name.substring(index + 1));        // Get classname in pkg.classname
        index = name.lastIndexOf("$");
        name = (index == -1 ? name : name.substring(index + 1));        // Get nested_classname in pkg.class1$nested_classname
        return name;
    }

    /** Return the registered model class by its name. */
    public Class getModelClass(String modelName) {
        return modelClasses.get(modelName);
    }

    /** Return the table name of a registered model class. */
    public String getModelTable(Class modelClass) {
        return modelTables.get(getModelName(modelClass));
    }

    /** Return a field of a registered model class by the field name. */
    public Field getField(Class modelClass, String fieldName) {
        return modelAllFieldMap.get(getModelName(modelClass)).get(fieldName);
    }

    /** Return the Id field of a registered model class. */
    public Field getIdField(Class modelClass) {
        return modelIdFields.get(getModelName(modelClass));
    }

    /** Check to see if field is the Id field. */
    public boolean isIdField(Class modelClass, String fieldName) {
        Field   idField = modelIdFields.get(getModelName(modelClass));
        return idField.getName().equals(fieldName);
    }

    /**  Return the Cacheable service object.
     * Should only use the return MemCacheable for dumping caching statistics. */
    public MemCacheable getMemCacheable() {
        return memCacheable;
    }



    // Delegated SimpleDB API

    // public void createTable(Class modelClass)
    //     throws JsodaException
    // {
    //     String  table = getModelTable(modelClass);
    //     if (table == null)
    //         throw new JsodaException("Model class " + modelClass + " has not been registered.");
    //     sdbClient.createTable(new CreateTableRequest(table));
    // }

    // public void deleteTable(Class modelClass)
    //     throws Exception
    // {
    //     String      modelName = getModelName(modelClass);
    //     String      tableName = ReflectUtil.getAnnotationValue(modelClass, Table.class, "name", modelName);
    //     sdbClient.deleteTable(new DeleteTableRequest(tableName));
    // }

    // public void createRegisteredTables()
    //     throws JsodaException
    // {
    //     for (Class modelClass : modelClasses.values()) {
    //         createTable(modelClass);
    //     }
    // }

    // public List<String> listTables()
    //     throws Exception
    // {
    //     ListTablesResult   list = sdbClient.listTables();
    //     return list.getTableNames();
    // }

    // public void put(Object dataObj)
    //     throws Exception
    // {
    //     put(dataObj, null, null);
    // }    

    // public void put(Object dataObj, String expectedField, Object expectedValue)
    //     throws Exception
    // {
    //     Class   modelClass = dataObj.getClass();
    //     String  modelName = getModelName(modelClass);
    //     String  table = modelTables.get(modelName);
    //     if (table == null)
    //         throw new Exception("Model class " + dataObj.getClass() + " has not been registered.");

    //     fillDefaults(modelClass, dataObj);
    //     handlePrePersist(dataObj);
    //     validateFields(dataObj);

    //     String  idValue = DataUtil.getFieldValueStr(dataObj, modelIdFields.get(modelName));
    //     PutAttributesRequest    req =
    //         expectedField == null ?
    //             new PutAttributesRequest(table, idValue, buildAttrs(dataObj, modelName)) :
    //             new PutAttributesRequest(table, idValue, buildAttrs(dataObj, modelName), buildExpectedValue(modelName, expectedField, expectedValue));
    //     sdbClient.putAttributes(req);
    //     cachePutByFields(modelName, dataObj);
    // }

    // public void batchPut(Object[] dataObjs)
    //     throws Exception
    // {
    //     batchPut(Arrays.asList(dataObjs));
    // }

    // public void batchPut(List dataObjs)
    //     throws Exception
    // {
    //     if (dataObjs.size() == 0)
    //         return;

    //     Class   modelClass = dataObjs.get(0).getClass();
    //     String  modelName = getModelName(modelClass);
    //     String  table = modelTables.get(modelName);
    //     if (table == null)
    //         throw new Exception("Model class " + dataObjs.get(0).getClass() + " has not been registered.");

    //     for (Object dataObj : dataObjs) {
    //         fillDefaults(modelClass, dataObj);
    //         handlePrePersist(dataObj);
    //         validateFields(dataObj);
    //     }

    //     sdbClient.batchPutAttributes(new BatchPutAttributesRequest(table, buildPutItems(dataObjs, modelName)));
    // }

    // public <T> T get(Class<T> modelClass, String id)
    //     throws Exception
    // {
    //     String  modelName = getModelName(modelClass);
    //     String  table = modelTables.get(modelName);
    //     if (table == null)
    //         throw new Exception("Model class " + modelClass + " has not been registered.");

    //     String  idValue = DataUtil.toValueStr(id);
    //     T       obj = (T)cacheGet(modelName, idValue);
    //     if (obj != null)
    //         return obj;

    //     GetAttributesResult result = sdbClient.getAttributes(new GetAttributesRequest(table, idValue));
    //     if (result.getAttributes().size() == 0)
    //         return null;        // not existed.

    //     return buildLoadObj(modelClass, modelName, id, result.getAttributes());
    // }

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

    // public void delete(Class modelClass, String id)
    //     throws Exception
    // {
    //     String  modelName = getModelName(modelClass);
    //     String  table = modelTables.get(modelName);
    //     if (table == null)
    //         throw new Exception("Model class " + modelClass + " has not been registered.");

    //     cacheDeleteByFields(modelName, id);

    //     String  idValue = DataUtil.toValueStr(id);
    //     sdbClient.deleteAttributes(new DeleteAttributesRequest(table, idValue));
    // }

    // public void batchDelete(Class modelClass, List<String> idList)
    //     throws Exception
    // {
    //     String  modelName = getModelName(modelClass);
    //     String  table = modelTables.get(modelName);
    //     if (table == null)
    //         throw new Exception("Model class " + modelClass + " has not been registered.");

    //     List<DeletableItem> items = new ArrayList<DeletableItem>();
    //     for (String id : idList) {
    //         String  idValue = DataUtil.toValueStr(id);
    //         items.add(new DeletableItem().withName(idValue));
    //         cacheDelete(modelName + ".id." + idValue);
    //     }
    //     sdbClient.batchDeleteAttributes(new BatchDeleteAttributesRequest(table, items));
    // }

    // public <T> SdbQuery<T> query(Class<T> modelClass)
    //     throws Exception
    // {
    //     SdbQuery<T> query = new SdbQuery<T>(this, modelClass);
    //     return query;
    // }


    // Package level methods

    String getFieldAttr(Class modelClass, String fieldName) {
        String  modelName = getModelName(modelClass);
        Field   idField = modelIdFields.get(modelName);
        if (idField.getName().equals(fieldName))
            return ITEM_NAME;
        return modelFieldAttrMap.get(modelName).get(fieldName);
    }

    String getFieldAttrQuoted(Class modelClass, String fieldName) {
        String  modelName = getModelName(modelClass);
        Field   idField = modelIdFields.get(modelName);
        if (idField.getName().equals(fieldName))
            return ITEM_NAME;
        String attr = modelFieldAttrMap.get(modelName).get(fieldName);
        return attr != null ? SimpleDBUtils.quoteName(attr) : null;
    }

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

    private void cachePut(String key, Serializable dataObj) {
        if (memCacheable != null) {
            try {
                int     expireInSeconds = ReflectUtil.getAnnotationValue(dataObj.getClass(), CachePolicy.class, "expireInSeconds", Integer.class, 0);
                memCacheable.put(key, expireInSeconds, dataObj);
            } catch(Exception ignored) {
            }
        }
    }

    private void cachePutByFields(String modelName, Serializable dataObj)
        throws Exception
    {
        for (String fieldName : modelCacheByFields.get(modelName)) {
            Field   field = modelAllFieldMap.get(modelName).get(fieldName);
            String  valueStr = DataUtil.getFieldValueStr(dataObj, field);
            String  key = modelName + "." + fieldName + "." + valueStr;
            cachePut(key, dataObj);
        }
    }

    private Serializable cacheGet(String key) {
        if (memCacheable != null) {
            return memCacheable.get(key);
        }
        return null;
    }

    private Serializable cacheGet(String modelName, String idValue) {
        Field   idField = modelIdFields.get(modelName);
        String  key = modelName + "." + idField.getName() + "." + idValue;
        return cacheGet(key);
    }

    private Serializable cacheGet(String modelName, String fieldName, Object fieldValue)
        throws Exception
    {
        String  valueStr = DataUtil.toValueStr(fieldValue);
        String  key = modelName + "." + fieldName + "." + valueStr;
        return cacheGet(key);
    }

    private void cacheDelete(String key) {
        if (memCacheable != null) {
            memCacheable.delete(key);
        }
    }

    private void cacheDeleteByFields(String modelName, String idValue)
        throws Exception
    {
        Object  obj = cacheGet(modelName, idValue);
        if (obj != null) {
            for (String fieldName : modelCacheByFields.get(modelName)) {
                Field   field = modelAllFieldMap.get(modelName).get(fieldName);
                String  valueStr = DataUtil.getFieldValueStr(obj, field);
                cacheDelete(modelName + "." + fieldName + "." + valueStr);
            }
        }
    }


    // Private implementation.

    /** Get all fields, including the Id field.  Skip the Transient field. */
    private Field[] getAllFields(Class modelClass) {
        List<Field> fields = new ArrayList<Field>();

        for (Field field : ReflectUtil.getAllFields(modelClass)) {
            if (ReflectUtil.hasAnnotation(field, Transient.class))
                continue;       // Skip
            fields.add(field);
        }
        return fields.toArray(new Field[fields.size()]);
    }

    private Field[] getAttrFields(Field[] allFields) {
        List<Field> fields = new ArrayList<Field>();

        for (Field field : allFields) {
            if (ReflectUtil.hasAnnotation(field, Id.class))
                continue;       // Skip Id field.
            fields.add(field);
        }
        return fields.toArray(new Field[fields.size()]);
    }

    private Map<String, Field> toFieldMap(Field[] fields)
        throws Exception
    {
        Map<String, Field>  map = new HashMap<String, Field>();
        for (Field field : fields) {
            map.put(field.getName(), field);
        }
        return map;
    }

    private Map<String, Field> toAttrFieldMap(Field[] fields)
        throws Exception
    {
        Map<String, Field>  map = new HashMap<String, Field>();
        for (Field field : fields) {
            String  attrName = ReflectUtil.getAnnotationValue(field, Column.class, "name", field.getName());
            map.put(attrName, field);
        }
        return map;
    }

    private Map<String, String> toFieldAttrMap(Field[] fields)
        throws Exception
    {
        Map<String, String>  map = new HashMap<String, String>();
        for (Field field : fields) {
            String  attrName = ReflectUtil.getAnnotationValue(field, Column.class, "name", field.getName());
            map.put(field.getName(), attrName);
        }
        return map;
    }

    private Method toPrePersistMethod(Class modelClass)
        throws Exception
    {
        for (Method method : ReflectUtil.getAllMethods(modelClass)) {
            if (ReflectUtil.hasAnnotation(method, PrePersist.class))
                return method;
        }
        return null;
    }

    private Method toPostLoadMethod(Class modelClass)
        throws Exception
    {
        for (Method method : ReflectUtil.getAllMethods(modelClass)) {
            if (ReflectUtil.hasAnnotation(method, PostLoad.class))
                return method;
        }
        return null;
    }

    private Set<String> toCacheByFields(Field[] fields)
        throws Exception
    {
        Set<String> set = new HashSet<String>();
        for (Field field : fields) {
            if (ReflectUtil.hasAnnotation(field, CacheByField.class))
                set.add(field.getName());
        }
        return set;
    }

    private List<ReplaceableAttribute> buildAttrs(Object dataObj, String modelName)
        throws Exception
    {
        Field[]                     fields = modelAttrFields.get(modelName);
        Map<String, String>         fieldAttrMap = modelFieldAttrMap.get(modelName);
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
        Map<String, Field>  attrFieldMap = modelAttrFieldMap.get(modelName);
        Map<String, String> fieldAttrMap = modelFieldAttrMap.get(modelName);
        Field               field = attrFieldMap.get(expectedField);
        String              attrName = fieldAttrMap.get(field.getName());
        String              fieldValue = DataUtil.toValueStr(expectedValue);
        return new UpdateCondition(attrName, fieldValue, true);
    }

    private List<ReplaceableItem> buildPutItems(List dataObjs, String modelName)
        throws Exception
    {
        String                  table = modelTables.get(modelName);
        Field[]                 fields = modelAttrFields.get(modelName);
        List<ReplaceableItem>   items = new ArrayList<ReplaceableItem>();
        String                  idValue;

        for (Object dataObj : dataObjs) {
            idValue = DataUtil.getFieldValueStr(dataObj, modelIdFields.get(modelName));
            items.add(new ReplaceableItem(idValue, buildAttrs(dataObj, modelName)));
            cachePutByFields(modelName, (Serializable)dataObj);
        }

        return items;
    }

    private <T> T buildLoadObj(Class<T> modelClass, String modelName, String id, List<Attribute> attrs)
        throws Exception
    {
        T                   obj = modelClass.newInstance();
        Map<String, Field>  attrFieldMap = modelAttrFieldMap.get(modelName);

        // Set the attr field 
        for (Attribute attr : attrs) {
            String  attrName  = attr.getName();
            String  fieldValue = attr.getValue();
            Field   field = attrFieldMap.get(attrName);

            if (field == null) {
                //throw new Exception("Attribute name " + attrName + " has no corresponding field in object " + modelClass);
                //logger.severe("Attribute name " + attrName + " has no corresponding field in object " + modelClass);
                continue;
            }

            DataUtil.setFieldValueStr(obj, field, fieldValue);
        }

        // Set the Id field
        DataUtil.setFieldValueStr(obj, modelIdFields.get(modelName), id);

        // Do post load processing
        handlePostLoad(obj);
        cachePutByFields(modelName, (Serializable)obj);

        return obj;
    }

    private void validateFields(Object dataObj)
        throws Exception
    {
        String  modelName = getModelName(dataObj.getClass());
        for (Field field : modelAllFields.get(modelName)) {
            Boolean isAttrNullable = ReflectUtil.getAnnotationValue(field, Column.class, "nullable", Boolean.class, Boolean.TRUE);
            if (!isAttrNullable && field.get(dataObj) == null)
                throw new ValidationException("Field " + field.getName() + " cannot be null.");
        }
    }
    
    private void handlePrePersist(Object dataObj)
        throws Exception
    {
        String  modelName = getModelName(dataObj.getClass());
        Method  prePersistMethod = modelPrePersistMethod.get(modelName);

        if (prePersistMethod != null) {
            prePersistMethod.invoke(dataObj);
        }
    }
    
    private void handlePostLoad(Object dataObj)
        throws Exception
    {
        String  modelName = getModelName(dataObj.getClass());
        Method  postLoadMethod = modelPostLoadMethod.get(modelName);

        if (postLoadMethod != null) {
            postLoadMethod.invoke(dataObj);
        }
    }

    public <T> T fillDefaults(Class<T> modelClass, T dataObj)
        throws Exception
    {
        String  modelName = getModelName(modelClass);
        for (Field field : modelAllFields.get(modelName)) {
            Object  value = field.get(dataObj);
            if (value == null || value.toString().length() == 0)
                continue;

            if (ReflectUtil.hasAnnotation(field, DefaultGUID.class)) {
                fillDefaultGUID(modelName, field, dataObj);
            } else if (ReflectUtil.hasAnnotation(field, DefaultComposite.class)) {
                fillDefaultComposite(modelName, field, dataObj);
            }
        }
        return dataObj;
    }

    private void fillDefaultGUID(String modelName, Field field, Object dataObj)
        throws Exception
    {
        boolean isShort = ReflectUtil.getAnnotationValue(field, DefaultGUID.class, "isShort", Boolean.class, Boolean.FALSE);
        String  uuidStr = isShort ? BaseXUtil.uuid8() : BaseXUtil.uuid16();
        field.set(dataObj, uuidStr);
    }

    private void fillDefaultComposite(String modelName, Field field, Object dataObj)
        throws Exception
    {
        String[]        fromFields = ReflectUtil.getAnnotationValue(field, DefaultComposite.class, "fromFields", String[].class, new String[0]);
        int[]           substrLen = ReflectUtil.getAnnotationValue(field, DefaultComposite.class, "substrLen", int[].class, new int[0]);
        String          separator = ReflectUtil.getAnnotationValue(field, DefaultComposite.class, "separator", "-");
        StringBuilder   sb = new StringBuilder();

        for (int i = 0; i < fromFields.length; i++) {
            Field       subpartField = modelAllFieldMap.get(modelName).get(fromFields[i]);
            Object      subpartValue = subpartField.get(dataObj);
            String      subpartStr = subpartValue == null ? "" : subpartValue.toString();

            subpartStr = getSubpartMax(subpartStr, i, substrLen);

            if (subpartStr.length() > 0) {
                if (sb.length() > 0)
                    sb.append(separator);
                sb.append(subpartStr);
            }
        }

        field.set(dataObj, sb.toString());
    }

    private static String getSubpartMax(String fieldStr, int fieldPos, int[] substrLen) {
        if (substrLen == null || fieldPos >= substrLen.length || substrLen[fieldPos] == 0)
            return fieldStr;
        int len = substrLen[fieldPos] > fieldStr.length() ? fieldStr.length() : substrLen[fieldPos];
        return fieldStr.substring(0, len);
    }

}

