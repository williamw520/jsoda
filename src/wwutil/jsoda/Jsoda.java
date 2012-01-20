
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
    SimpleDBMgr                     sdbMgr;

    Map<String, Class>              modelClasses = new HashMap<String, Class>();
    Map<String, String>             modelTables = new HashMap<String, String>();
    Map<String, Field>              modelIdFields = new HashMap<String, Field>();
    Map<String, Field[]>            modelAllFields = new HashMap<String, Field[]>();
    Map<String, Field[]>            modelAttrFields = new HashMap<String, Field[]>();
    Map<String, Map<String, Field>> modelAllFieldMap = new HashMap<String, Map<String, Field>>();
    Map<String, Map<String, Field>> modelAttrFieldMap = new HashMap<String, Map<String, Field>>();
    Map<String, Map<String, String>>    modelFieldAttrMap = new HashMap<String, Map<String, String>>();
    Map<String, Set<String>>        modelCacheByFields = new HashMap<String, Set<String>>();
    Map<String, Method>             modelPrePersistMethod = new HashMap<String, Method>();
    Map<String, Method>             modelPostLoadMethod = new HashMap<String, Method>();


    // AWS Access Key ID and Secret Access Key
    public Jsoda(AWSCredentials cred)
        throws Exception
    {
        this(cred, null);
    }

    public Jsoda(AWSCredentials cred, MemCacheable memCacheable)
        throws Exception
    {
        this.memCacheable = memCacheable;
        this.sdbMgr = new SimpleDBMgr(this, cred);
    }

    public void shutdown() {
        modelClasses.clear();
        modelTables.clear();
        //sdbClient.shutdown();
    }

    /** Register a POJO model class.  Calling again will re-register the mode class, replacing the old one.
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

    void validateRegisteredModel(Class modelClass)
        throws IllegalArgumentException
    {
        if (!modelClasses.containsKey(getModelName(modelClass)))
            throw new IllegalArgumentException("Model class " + modelClass + " has not been registered.");
    }

    void validateRegisteredModel(String modelName)
        throws IllegalArgumentException
    {
        if (!modelClasses.containsKey(modelName))
            throw new IllegalArgumentException("Model class " + modelName + " has not been registered.");
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

    /** Return all registered model classes as a map. */
    public Map<String, Class> getModelClasses() {
        return modelClasses;
    }

    /** Return the registered model class by its name. */
    public Class getModelClass(String modelName) {
        validateRegisteredModel(modelName);
        return modelClasses.get(modelName);
    }

    /** Return the table name of a registered model class. */
    public String getModelTable(String modelName) {
        validateRegisteredModel(modelName);
        return modelTables.get(modelName);
    }

    /** Return a field of a registered model class by the field name. */
    public Field getField(String modelName, String fieldName) {
        validateRegisteredModel(modelName);
        return modelAllFieldMap.get(modelName).get(fieldName);
    }

    /** Return the Id field of a registered model class. */
    public Field getIdField(String modelName) {
        validateRegisteredModel(modelName);
        return modelIdFields.get(modelName);
    }

    /** Check to see if field is the Id field. */
    public boolean isIdField(String modelName, String fieldName) {
        validateRegisteredModel(modelName);
        Field   idField = modelIdFields.get(modelName);
        return idField.getName().equals(fieldName);
    }

    /**  Return the Cacheable service object.
     * Should only use the return MemCacheable for dumping caching statistics. */
    public MemCacheable getMemCacheable() {
        return memCacheable;
    }

    // DB API

    public void createTable(Class modelClass) {
        sdbMgr.createTable(getModelTable(getModelName(modelClass)));
    }

    public void deleteTable(Class modelClass) {
        sdbMgr.deleteTable(getModelTable(getModelName(modelClass)));
    }

    public void createRegisteredTables() {
        for (Class modelClass : modelClasses.values()) {
            createTable(modelClass);
        }
    }

    public List<String> listTables() {
        return sdbMgr.listTables();
    }

    /** Helper method to create a new Dao for a model class. */
    public <T> Dao<T> newDao(Class<T> modelClass) {
        return new Dao<T>(modelClass, this);
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

    String getFieldAttr(String modelName, String fieldName) {
        Field   idField = modelIdFields.get(modelName);
        if (idField.getName().equals(fieldName))
            return ITEM_NAME;
        return modelFieldAttrMap.get(modelName).get(fieldName);
    }

    String getFieldAttrQuoted(String modelName, String fieldName) {
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


    // Cache service
    
    void cachePut(String key, Serializable dataObj) {
        if (memCacheable != null) {
            try {
                int     expireInSeconds = ReflectUtil.getAnnotationValue(dataObj.getClass(), CachePolicy.class, "expireInSeconds", Integer.class, 0);
                memCacheable.put(key, expireInSeconds, dataObj);
            } catch(Exception ignored) {
            }
        }
    }

    void cachePutByFields(String modelName, Serializable dataObj)
        throws Exception
    {
        for (String fieldName : modelCacheByFields.get(modelName)) {
            Field   field = modelAllFieldMap.get(modelName).get(fieldName);
            // Note: the cache keys are in the native string format to ensure always having a string key.
            String  valueStr = DataUtil.getFieldValueStr(dataObj, field);
            String  key = modelName + "." + fieldName + "." + valueStr;
            cachePut(key, dataObj);
        }
    }

    Serializable cacheGet(String key) {
        if (memCacheable != null) {
            return memCacheable.get(key);
        }
        return null;
    }

    Serializable cacheGet(String modelName, String idValue) {
        Field   idField = modelIdFields.get(modelName);
        String  key = modelName + "." + idField.getName() + "." + idValue;
        return cacheGet(key);
    }

    Serializable cacheGet(String modelName, String fieldName, Object fieldValue)
        throws Exception
    {
        String  valueStr = DataUtil.toValueStr(fieldValue);
        String  key = modelName + "." + fieldName + "." + valueStr;
        return cacheGet(key);
    }

    void cacheDelete(String key) {
        if (memCacheable != null) {
            memCacheable.delete(key);
        }
    }

    void cacheDeleteByFields(String modelName, String idValue)
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

}

