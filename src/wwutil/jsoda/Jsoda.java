
package wwutil.jsoda;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.persistence.PrePersist;
import javax.persistence.PostLoad;

import org.apache.commons.beanutils.ConvertUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSCredentials;

import wwutil.model.MemCacheable;
import wwutil.model.annotation.DbType;
import wwutil.model.annotation.AModel;
import wwutil.model.annotation.AttrName;
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
    // Services
    private AWSCredentials          credentials;
    private ObjCacheMgr             objCacheMgr;
    private SimpleDBService         sdbMgr;
    private DynamoDBService         ddbMgr;

    // Model registry
    private Map<String, Class>      modelClasses = new ConcurrentHashMap<String, Class>();
    private Map<String, DbService>  modelDb = new ConcurrentHashMap<String, DbService>();
    private Map<String, String>     modelTables = new ConcurrentHashMap<String, String>();
    private Map<String, Field>      modelIdFields = new ConcurrentHashMap<String, Field>();
    private Map<String, Field>      modelRangeFields = new ConcurrentHashMap<String, Field>();
    private Map<String, Field[]>    modelAllFields = new ConcurrentHashMap<String, Field[]>();
    private Map<String, Map<String, Field>>     modelAllFieldMap = new ConcurrentHashMap<String, Map<String, Field>>();
    private Map<String, Map<String, Field>>     modelAttrFieldMap = new ConcurrentHashMap<String, Map<String, Field>>();
    private Map<String, Map<String, String>>    modelFieldAttrMap = new ConcurrentHashMap<String, Map<String, String>>();
    private Map<String, Set<String>>            modelCacheByFields = new ConcurrentHashMap<String, Set<String>>();
    private Map<String, Method>     modelPrePersistMethod = new ConcurrentHashMap<String, Method>();
    private Map<String, Method>     modelPostLoadMethod = new ConcurrentHashMap<String, Method>();
    private Map<String, Dao>        modelDao = new ConcurrentHashMap<String, Dao>();


    /** Create a Jsoda object with the AWS Access Key ID and Secret Access Key
     * The tables in the database scope belonged to the AWS Access Key ID will be accessed.
     */
    public Jsoda(AWSCredentials cred)
        throws Exception
    {
        this(cred, null);
    }

    /** Set a cache service for the Jsoda object.  All objects accessed via the Jsoda object will be cached according to their CachePolicy. */
    public Jsoda(AWSCredentials cred, MemCacheable memCacheable)
        throws Exception
    {
        if (cred == null || cred.getAWSAccessKeyId() == null || cred.getAWSSecretKey() == null)
            throw new IllegalArgumentException("AWS credential is missing in parameter");
        this.credentials = cred;
        this.objCacheMgr = new ObjCacheMgr(this);
        this.objCacheMgr.setMemCacheable(memCacheable);
        this.sdbMgr = new SimpleDBService(this, cred);
        this.ddbMgr = new DynamoDBService(this, cred);
    }

    /** Return the cache service object.
     *  Should only use the returned MemCacheable for dumping caching statistics and nothing else.
     */
    public MemCacheable getMemCacheable() {
        return objCacheMgr.getMemCacheable();
    }

    /** Set the AWS service endpoint for the underlying dbtype.  Different AWS region might have different endpoint. */
    public Jsoda setDbEndpoint(DbType dbtype, String endpoint) {
        getDbService(dbtype).setDbEndpoint(endpoint);
        return this;
    }

    /** Shut down any underlying database services and free up resources */
    public void shutdown() {
        objCacheMgr.shutdown();
        sdbMgr.shutdown();
        ddbMgr.shutdown();
        modelClasses.clear();
        modelTables.clear();
        modelDb.clear();
        modelTables.clear();
        modelIdFields.clear();
        modelRangeFields.clear();
        modelAllFields.clear();
        modelAllFieldMap.clear();
        modelAttrFieldMap.clear();
        modelFieldAttrMap.clear();
        modelCacheByFields.clear();
        modelPrePersistMethod.clear();
        modelPostLoadMethod.clear();
        modelDao.clear();
    }


    /** Register a POJO model class.  Calling again will re-register the model class, replacing the old one. */
    public <T> void registerModel(Class<T> modelClass)
        throws JsodaException
    {
        registerModel(modelClass, null);
    }

    /** Register a POJO model class.  Register the model to use the dbtype, ignoring the model's dbtype annotation. */
    public <T> void registerModel(Class<T> modelClass, DbType dbtype)
        throws JsodaException
    {
        try {
            String      modelName = getModelName(modelClass);
            Field       idField = ReflectUtil.findAnnotatedField(modelClass, Id.class);
            Field       rangeField = ReflectUtil.findAnnotatedField(modelClass, ARangeKey.class);
            Field[]     allFields = getAllFields(modelClass);

            if (idField == null)
                throw new ValidationException("Missing annotated Id field in the model class.");
            if (ReflectUtil.hasAnnotation(idField, Transient.class))
                throw new ValidationException("The Id field cannot be Transient in the model class.");
            if (idField.getType() != String.class &&
                idField.getType() != Integer.class &&
                idField.getType() != int.class &&
                idField.getType() != Long.class &&
                idField.getType() != long.class)
                throw new ValidationException("The Id field can only be String, Integer, or Long.");

            modelClasses.put(modelName, modelClass);
            modelDb.put(modelName, toDbService(modelClass, dbtype));
            modelTables.put(modelName, toTableName(modelClass));
            modelIdFields.put(modelName, idField);
            if (rangeField != null && dbtype == DbType.DynamoDB)
                modelRangeFields.put(modelName, rangeField);
            modelAllFields.put(modelName, allFields);                       // Save all fields, including the Id field
            modelAllFieldMap.put(modelName, toFieldMap(allFields));
            modelAttrFieldMap.put(modelName, toAttrFieldMap(allFields));
            modelFieldAttrMap.put(modelName, toFieldAttrMap(allFields));
            modelCacheByFields.put(modelName, toCacheByFields(allFields));  // Build CacheByFields on all fields, including the Id field
            Method  prepersistMethod = toPrePersistMethod(modelClass);
            Method  postLoadMethod = toPostLoadMethod(modelClass);
            if (prepersistMethod != null)
                modelPrePersistMethod.put(modelName, prepersistMethod);
            if (postLoadMethod != null)
                modelPostLoadMethod.put(modelName, postLoadMethod);
            modelDao.put(modelName, new Dao<T>(modelClass, this));
        } catch(JsodaException je) {
            throw je;
        } catch(Exception e) {
            throw new JsodaException("Failed to register model class", e);
        }
    }

    public boolean isRegistered(Class modelClass) {
        return (modelClasses.get(getModelName(modelClass)) != null);
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

    void validateField(String modelName, String field) {
        if (getField(modelName, field) == null)
            throw new IllegalArgumentException("Field " + field + " does not exist in " + modelName);
    }

    /** Return the model name of a model class. */
    static String getModelName(Class modelClass) {
		int		index;
        String  name = modelClass.getName();

		index = name.lastIndexOf(".");
        name = (index == -1 ? name : name.substring(index + 1));        // Get classname in pkg.classname
        index = name.lastIndexOf("$");
        name = (index == -1 ? name : name.substring(index + 1));        // Get nested_classname in pkg.class1$nested_classname
        return name;
    }

    /** Return the registered model class by its name. */
    Class getModelClass(String modelName) {
        validateRegisteredModel(modelName);
        return modelClasses.get(modelName);
    }

    /** Return the DbService for the registered model class. */
    DbService getDb(String modelName) {
        validateRegisteredModel(modelName);
        return modelDb.get(modelName);
    }

    /** Return the table name of a registered model class. */
    String getModelTable(String modelName) {
        validateRegisteredModel(modelName);
        return modelTables.get(modelName);
    }

    /** Return a field of a registered model class by the field name. */
    Field getField(String modelName, String fieldName) {
        validateRegisteredModel(modelName);
        return modelAllFieldMap.get(modelName).get(fieldName);
    }

    /** Return the Id field of a registered model class. */
    Field getIdField(String modelName) {
        validateRegisteredModel(modelName);
        return modelIdFields.get(modelName);
    }

    /** Return the RangeKey field of a registered model class. */
    Field getRangeField(String modelName) {
        validateRegisteredModel(modelName);
        return modelRangeFields.get(modelName);
    }

    Field[] getAllFields(String modelName) {
        return modelAllFields.get(modelName);
    }

    /** Check to see if field is the Id field. */
    boolean isIdField(String modelName, String fieldName) {
        return getIdField(modelName).getName().equals(fieldName);
    }

    /** Check to see if field is an RangeKey field. */
    boolean isRangeField(String modelName, String fieldName) {
        Field   rangeField = getRangeField(modelName);
        return rangeField != null && rangeField.getName().equals(fieldName);
    }

    // DB Table API

    /** Create modelClass' table in the registered model's database */
    public void createModelTable(Class modelClass) {
        String  modelName = getModelName(modelClass);
        getDb(modelName).createModelTable(modelName);
    }

    /** Delete modelClass' table in the registered model's database */
    public void deleteModelTable(Class modelClass) {
        String  modelName = getModelName(modelClass);
        String  tableName = getModelTable(modelName);
        getDb(modelName).deleteTable(tableName);
    }

    /** Delete the table in the database, as named in tableName, in the dbtype */
    public void deleteNativeTable(DbType dbtype, String tableName) {
        getDbService(dbtype).deleteTable(tableName);
    }

    public void createRegisteredTables() {
        for (Class modelClass : modelClasses.values()) {
            createModelTable(modelClass);
        }
    }

    /** Get the list of native table names from the underlying database.
     * @param dbtype  the dbtype, as defined in AModel.
     */
    public List<String> listNativeTables(DbType dbtype) {
        return getDbService(dbtype).listTables();
    }

    /** Get the data access object for the model class.
     * DAO has methods to load or save the data object.
     * The modelClass is registered automatically if it has not been registered.
     * <pre>
     * e.g.
     *   Dao&lt;Model1&gt; dao1 = jsoda.dao(Model1.class);
     *   Dao&lt;Model2&gt; dao2 = jsoda.dao(Model2.class);
     *   dao1.put(model1One);
     *   dao1.put(model1Two);
     *   dao2.put(model2One);
     *   dao2.put(model2Two);
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public <T> Dao<T> dao(Class<T> modelClass)
        throws JsodaException
    {
        if (!isRegistered(modelClass))
            registerModel(modelClass);
        return (Dao<T>)modelDao.get(getModelName(modelClass));
    }

    /** Create a Query object for a model class.  Additional conditions can be specified on the query.
     * Call this method or the Dao's constructor to create a dao for a model class.
     * <pre>
     * e.g.
     *   Dao&lt;Model1&gt; query1 = jsoda.query(Model1.class);
     *   Dao&lt;Model2&gt; query2 = new Query&lt;Model2&gt;(Model2.class, jsoda);
     * </pre>
     */
    public <T> Query<T> query(Class<T> modelClass)
        throws JsodaException
    {
        if (!isRegistered(modelClass))
            registerModel(modelClass);
        return new Query<T>(modelClass, this);
    }



    // Package level methods

    ObjCacheMgr getObjCacheMgr() {
        return objCacheMgr;
    }

    Field getFieldByAttr(String modelName, String attrName) {
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

    Map<String, String> getFieldAttrMap(String modelName) {
        return modelFieldAttrMap.get(modelName);
    }

    Map<String, Field> getAttrFieldMap(String modelName) {
        return modelAttrFieldMap.get(modelName);
    }

    Set<String> getCacheByFields(String modelName) {
        return modelCacheByFields.get(modelName);
    }

    Method getPrePersistMethod(String modelName) {
        return modelPrePersistMethod.get(modelName);
    }

    Method getPostLoadMethod(String modelName) {
        return modelPostLoadMethod.get(modelName);
    }


    // Registration helper methods

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

    // TODO: delete
    private Field[] getAttrFields(Field[] allFields) {
        List<Field> fields = new ArrayList<Field>();

        for (Field field : allFields) {
            if (ReflectUtil.hasAnnotation(field, Id.class))
                continue;       // Skip Id field.  SimpleDB maps Id to the Name key of the object.
            fields.add(field);
        }
        return fields.toArray(new Field[fields.size()]);
    }

    private DbService getDbService(DbType dbtype) {
        if (dbtype == null)
            throw new IllegalArgumentException("Missing 'dbtype' parameter");

        if (dbtype == DbType.SimpleDB)
            return sdbMgr;
        if (dbtype == DbType.DynamoDB)
            return ddbMgr;

        throw new IllegalArgumentException(dbtype + " is not a supported dbtype");
    }

    private DbService toDbService(Class modelClass, DbType dbtype) {
        if (dbtype == null) {
            try {
                dbtype = ReflectUtil.getAnnotationValue(modelClass, AModel.class, "dbtype", DbType.class, null);
            } catch(Exception e) {
                throw new IllegalArgumentException("Error in getting dbtype from the AModel annotation for " + modelClass, e);
            }
            if (dbtype == null || dbtype == DbType.None)
                throw new IllegalArgumentException("No valid 'dbtype' specification in the AModel annotation for " + modelClass);
        }

        return getDbService(dbtype);
    }

    private String toTableName(Class modelClass) {
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
            String  attrName = ReflectUtil.getAnnotationValue(field, AttrName.class, "value", field.getName());
            map.put(attrName, field);
        }
        return map;
    }

    private Map<String, String> toFieldAttrMap(Field[] fields)
        throws Exception
    {
        Map<String, String>  map = new ConcurrentHashMap<String, String>();
        for (Field field : fields) {
            String  attrName = ReflectUtil.getAnnotationValue(field, AttrName.class, "value", field.getName());
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

