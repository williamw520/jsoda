
package wwutil.jsoda;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.persistence.PrePersist;
import javax.persistence.PostLoad;

import org.apache.commons.beanutils.ConvertUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSCredentials;

import wwutil.model.MemCacheable;
import wwutil.model.annotation.AModel;
import wwutil.model.annotation.ARangeKey;
import wwutil.model.annotation.CachePolicy;
import wwutil.model.annotation.DefaultGUID;
import wwutil.model.annotation.DefaultComposite;
import wwutil.model.annotation.CacheByField;


/**
 * Simple object-db mapping service for AWS.  Make storing POJO in SimpleDB/DynamoDB easy.
 * Class is thread-safe.
 * 
 * See utest.JsodaTest for usage.
 */
public class Jsoda
{
    private AWSCredentials          credentials;
    private MemCacheable            memCacheable;
    private SimpleDBMgr             sdbMgr;
    private DynamoDBMgr             ddbMgr;

    Map<String, Class>              modelClasses = new ConcurrentHashMap<String, Class>();
    Map<String, DbService>          modelDb = new ConcurrentHashMap<String, DbService>();
    Map<String, String>             modelTables = new ConcurrentHashMap<String, String>();
    Map<String, Field>              modelIdFields = new ConcurrentHashMap<String, Field>();
    Map<String, Field>              modelRangeFields = new ConcurrentHashMap<String, Field>();
    Map<String, Field[]>            modelAllFields = new ConcurrentHashMap<String, Field[]>();
    Map<String, Field[]>            modelAttrFields = new ConcurrentHashMap<String, Field[]>();
    Map<String, Map<String, Field>> modelAllFieldMap = new ConcurrentHashMap<String, Map<String, Field>>();
    Map<String, Map<String, Field>> modelAttrFieldMap = new ConcurrentHashMap<String, Map<String, Field>>();
    Map<String, Map<String, String>>    modelFieldAttrMap = new ConcurrentHashMap<String, Map<String, String>>();
    Map<String, Set<String>>        modelCacheByFields = new ConcurrentHashMap<String, Set<String>>();
    Map<String, Method>             modelPrePersistMethod = new ConcurrentHashMap<String, Method>();
    Map<String, Method>             modelPostLoadMethod = new ConcurrentHashMap<String, Method>();


    // AWS Access Key ID and Secret Access Key
    public Jsoda(AWSCredentials cred)
        throws Exception
    {
        if (cred == null || cred.getAWSAccessKeyId() == null || cred.getAWSSecretKey() == null)
            throw new IllegalArgumentException("AWS credential is missing in parameter");
        this.credentials = cred;
        this.sdbMgr = new SimpleDBMgr(this, cred);
        this.ddbMgr = new DynamoDBMgr(this, cred);
    }

    public void shutdown() {
        sdbMgr.shutdown();
        ddbMgr.shutdown();
        modelClasses.clear();
        modelTables.clear();
    }

    /** Set a cache service for the Jsoda object.  All objects accessed via the Jsoda object will be cached according to their CachePolicy. */
    public Jsoda setMemCacheable(MemCacheable memCacheable) {
        this.memCacheable = memCacheable;
        return this;
    }

    /** Return the Cacheable service object.
     *  Should only use the returned MemCacheable for dumping caching statistics and nothing else.
     */
    public MemCacheable getMemCacheable() {
        return memCacheable;
    }

    /** Set the AWS service endpoint for the underlying dbtype.  Different AWS region might have different endpoint. */
    public Jsoda setDbEndpoint(AModel.DbType dbtype, String endpoint) {
        getDbService(dbtype).setDbEndpoint(endpoint);
        return this;
    }


    /** Register a POJO model class.  Calling again will re-register the mode class, replacing the old one.
     */
    public void registerModel(Class modelClass)
        throws JsodaException
    {
        try {
            String      modelName = getModelName(modelClass);
            Field       idField = ReflectUtil.findAnnotatedField(modelClass, Id.class);
            Field       rangeField = ReflectUtil.findAnnotatedField(modelClass, ARangeKey.class);
            Field[]     allFields = getAllFields(modelClass);
            Field[]     attrFields = getAttrFields(allFields);

            if (idField == null)
                throw new ValidationException("Missing annotated Id field in the model class.");
            if (ReflectUtil.hasAnnotation(idField, Transient.class))
                throw new ValidationException("The Id field cannot be Transient in the model class.");
            if (idField.getType() != String.class)
                throw new ValidationException("The Id field can only be String.");

            modelClasses.put(modelName, modelClass);
            modelDb.put(modelName, toDbService(modelClass));
            modelTables.put(modelName, toTableName(modelClass));
            modelIdFields.put(modelName, idField);
            if (rangeField != null)
                modelRangeFields.put(modelName, rangeField);
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

    /** Return the DbService for the registered model class. */
    public DbService getDb(String modelName) {
        validateRegisteredModel(modelName);
        return modelDb.get(modelName);
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

    /** Return the RangeKey field of a registered model class. */
    public Field getRangeField(String modelName) {
        validateRegisteredModel(modelName);
        return modelRangeFields.get(modelName);
    }

    /** Check to see if field is the Id field. */
    public boolean isIdField(String modelName, String fieldName) {
        validateRegisteredModel(modelName);
        Field   idField = modelIdFields.get(modelName);
        return idField.getName().equals(fieldName);
    }

    public Field getFieldByAttr(String modelName, String attrName) {
        validateRegisteredModel(modelName);
        Field   idField = modelIdFields.get(modelName);
        Field   field = modelAttrFieldMap.get(modelName).get(attrName);
        if (field == null) {
            if (idField.getName().equals(attrName))
                return idField;
            else
                return null;
        }
        return field;
    }


    // DB API

    public void createModelTable(Class modelClass) {
        String  modelName = getModelName(modelClass);
        getDb(modelName).createModelTable(modelName);
    }

    public void deleteModelTable(Class modelClass) {
        String  modelName = getModelName(modelClass);
        getDb(modelName).deleteModelTable(modelName);
    }

    public void createRegisteredTables() {
        for (Class modelClass : modelClasses.values()) {
            createModelTable(modelClass);
        }
    }

    /** Get the list of native table names from the underlying database.
     * @param dbtype  the dbtype, as defined in AModel.
     */
    public List<String> listTables(AModel.DbType dbtype) {
        return getDbService(dbtype).listTables();
    }

    /** Create a new Dao for a model class.  DAO has the db access methods to load and store the data objects.
     * Call this method or the Dao's constructor to create a dao for a model class.
     * <pre>
     * e.g.
     *   Dao&lt;Model1&gt; dao1 = jsoda.dao(Model1.class);
     *   Dao&lt;Model2&gt; dao2 = new Dao&lt;Model2&gt;(Model2.class, jsoda);
     * </pre>
     */
    public <T> Dao<T> dao(Class<T> modelClass) {
        // TODO: dynamically register the modelClass if it doesn't exist in jsoda.
        return new Dao<T>(modelClass, this);
    }

    /** Create a Query object for a model class.  Additional conditions can be specified on the query.
     * Call this method or the Dao's constructor to create a dao for a model class.
     * <pre>
     * e.g.
     *   Dao&lt;Model1&gt; query1 = jsoda.query(Model1.class);
     *   Dao&lt;Model2&gt; query2 = new Query&lt;Model2&gt;(Model2.class, jsoda);
     * </pre>
     */
    public <T> Query<T> query(Class<T> modelClass) {
        // TODO: dynamically register the modelClass if it doesn't exist in jsoda.
        return new Query<T>(modelClass, this);
    }


    // Package level methods

    void callPrePersist(String modelName, Object dataObj)
        throws JsodaException
    {
        try {
            Method  prePersistMethod = modelPrePersistMethod.get(modelName);
            if (prePersistMethod != null) {
                prePersistMethod.invoke(dataObj);
            }
        } catch(Exception e) {
            throw new JsodaException("callPrePersist", e);
        }
    }

    void callPostLoad(String modelName, Object dataObj)
        throws JsodaException
    {
        try {
            Method  postLoadMethod = modelPostLoadMethod.get(modelName);
            if (postLoadMethod != null) {
                postLoadMethod.invoke(dataObj);
            }
        } catch(Exception e) {
            throw new JsodaException("callPrePersist", e);
        }
    }

    // Cache service

    private String makeCachePkKey(String modelName, Object id, Object rangeKey) {
        // Note: the cache keys are in the native string format to ensure always having a string key.
        if (rangeKey == null)
            return modelName + ".pk." + DataUtil.toValueStr(id);
        else
            return modelName + ".pk." + DataUtil.toValueStr(id) + "/" + DataUtil.toValueStr(rangeKey);
    }

    private String makeCacheFieldKey(String modelName, String fieldName, Object fieldValue) {
        String  valueStr = DataUtil.toValueStr(fieldValue);
        return modelName + "." + fieldName + "." + valueStr;
    }

    private void cachePutObj(String key, Serializable dataObj) {
        if (memCacheable != null) {
            try {
                int expireInSeconds = ReflectUtil.getAnnotationValue(dataObj.getClass(), CachePolicy.class, "expireInSeconds", Integer.class, 0);
                memCacheable.put(key, expireInSeconds, dataObj);
            } catch(Exception ignored) {
            }
        }
    }

    void cachePut(String modelName, Serializable dataObj) {
        if (memCacheable == null)
            return;

        // Cache by the primary key (id or id/rangekey)
        try {
            Field   idField = getIdField(modelName);
            Object  idValue = idField.get(dataObj);
            Field   rangeField = getRangeField(modelName);
            Object  rangeValue = rangeField == null ? null : rangeField.get(dataObj);
            String  key = makeCachePkKey(modelName, idValue, rangeValue);
            cachePutObj(key, dataObj);
        } catch(Exception ignored) {
        }

        // Cache by the CacheByFields
        for (String fieldName : modelCacheByFields.get(modelName)) {
            try {
                Field   field = getField(modelName, fieldName);
                Object  fieldValue = field.get(dataObj);
                String  key = makeCacheFieldKey(modelName, field.getName(), fieldValue);
                cachePutObj(key, dataObj);
            } catch(Exception ignore) {
            }
        }
    }

    void cacheDelete(String modelName, Object idValue) {
        cacheDelete(modelName, idValue, null);
    }

    void cacheDelete(String modelName, Object idValue, Object rangeValue)
    {
        if (memCacheable == null)
            return;

        Object  dataObj = cacheGet(modelName, idValue, rangeValue);
        if (dataObj != null) {
            for (String fieldName : modelCacheByFields.get(modelName)) {
                try {
                    Field   field = getField(modelName, fieldName);
                    Object  fieldValue = field.get(dataObj);
                    String  key = makeCacheFieldKey(modelName, field.getName(), fieldValue);
                    memCacheable.delete(key);
                } catch(Exception ignored) {
                }
            }
        }

        String  key = makeCachePkKey(modelName, idValue, rangeValue);
        memCacheable.delete(key);
    }

    Serializable cacheGet(String modelName, Object idValue, Object rangeValue) {
        if (memCacheable == null)
            return null;

        // Cache by the primary key (id or id/rangekey)
        String  key = makeCachePkKey(modelName, idValue, rangeValue);
        return memCacheable.get(key);
    }

    Serializable cacheGetByField(String modelName, String fieldName, Object fieldValue) {
        String  key = makeCacheFieldKey(modelName, fieldName, fieldValue);
        return memCacheable.get(key);
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
                continue;       // Skip Id field.  SimpleDB maps Id to the Name key of the object.
            fields.add(field);
        }
        return fields.toArray(new Field[fields.size()]);
    }

    private DbService getDbService(AModel.DbType dbtype) {
        if (dbtype == null)
            throw new IllegalArgumentException("Missing 'dbtype' parameter");

        if (dbtype == AModel.DbType.SimpleDB)
            return sdbMgr;
        if (dbtype == AModel.DbType.DynamoDB)
            return ddbMgr;

        throw new IllegalArgumentException(dbtype + " is not a supported dbtype");
    }

    private DbService toDbService(Class modelClass) {
        AModel.DbType   dbtype = null;

        try {
            dbtype = ReflectUtil.getAnnotationValue(modelClass, AModel.class, "dbtype", AModel.DbType.class, null);
        } catch(Exception e) {
            throw new IllegalArgumentException("Error in getting dbtype from the AModel annotation for " + modelClass, e);
        }

        if (dbtype == null)
            throw new IllegalArgumentException("Missing 'dbtype' specification in the AModel annotation for " + modelClass);

        return getDbService(dbtype);
    }

    private String toTableName(Class modelClass)
        throws Exception
    {
        String  modelName = getModelName(modelClass);
        String  tableName = ReflectUtil.getAnnotationValue(modelClass, AModel.class, "table", modelName);   // default to modelName
        String  prefix = ReflectUtil.getAnnotationValue(modelClass, AModel.class, "prefix", "");
        return prefix + tableName;
    }


    private Map<String, Field> toFieldMap(Field[] fields)
        throws Exception
    {
        Map<String, Field>  map = new ConcurrentHashMap<String, Field>();
        for (Field field : fields) {
            map.put(field.getName(), field);
        }
        return map;
    }

    private Map<String, Field> toAttrFieldMap(Field[] fields)
        throws Exception
    {
        Map<String, Field>  map = new ConcurrentHashMap<String, Field>();
        for (Field field : fields) {
            String  attrName = ReflectUtil.getAnnotationValue(field, Column.class, "name", field.getName());
            map.put(attrName, field);
        }
        return map;
    }

    private Map<String, String> toFieldAttrMap(Field[] fields)
        throws Exception
    {
        Map<String, String>  map = new ConcurrentHashMap<String, String>();
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

