/******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.  If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for 
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is: Jsoda
 * The Initial Developer of the Original Code is: William Wong (williamw520@gmail.com)
 * Portions created by William Wong are Copyright (C) 2012 William Wong, All Rights Reserved.
 *
 ******************************************************************************/


package wwutil.jsoda;

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;

import wwutil.sys.FnUtil;
import wwutil.sys.FnUtil.*;
import wwutil.sys.ReflectUtil;
import wwutil.model.MemCacheable;
import wwutil.model.MemCacheableSimple;
import wwutil.model.AnnotationRegistry;
import wwutil.model.AnnotationClassHandler;
import wwutil.model.AnnotationFieldHandler;
import wwutil.model.ValidationException;
import wwutil.model.BuiltinFunc;
import wwutil.model.annotation.Key;
import wwutil.model.annotation.Transient;
import wwutil.model.annotation.PrePersist;
import wwutil.model.annotation.PreValidation;
import wwutil.model.annotation.PostLoad;
import wwutil.model.annotation.DbType;
import wwutil.model.annotation.Model;
import wwutil.model.annotation.AttrName;
import wwutil.model.annotation.CachePolicy;
import wwutil.model.annotation.CacheByField;
import wwutil.model.annotation.VersionLocking;
import wwutil.model.annotation.S3Field;


/**
 * Simple object-db mapping service for AWS.  Make storing POJO in SimpleDB/DynamoDB easy.
 * Class is thread-safe.
 * 
 * See utest.JsodaTest for usage.
 */
public class Jsoda
{
    private static Log  log = LogFactory.getLog(Jsoda.class);

    // Services
    private AWSCredentials          credentials;
    private ObjCacheMgr             objCacheMgr;
    private SimpleDBService         sdbMgr;
    private DynamoDBService         ddbMgr;
    private AmazonS3Client          s3Client;
    private AnnotationRegistry      data1Registry;
    private AnnotationRegistry      data2Registry;
    private AnnotationRegistry      validationRegistry;

    private String                  globalPrefix;
    private String                  defaultS3Bucket = "";
    private String                  s3KeyPrefix = "";

    // Model registry
    private Map<String, Class>      modelClasses = new ConcurrentHashMap<String, Class>();
    private Map<String, DbService>  modelDb = new ConcurrentHashMap<String, DbService>();
    private Map<String, String>     modelTables = new ConcurrentHashMap<String, String>();
    private Map<String, Field>      modelIdFields = new ConcurrentHashMap<String, Field>();
    private Map<String, Field>      modelRangeFields = new ConcurrentHashMap<String, Field>();
    private Map<String, Field>      modelVersionFields = new ConcurrentHashMap<String, Field>();
    private Map<String, Integer>    modelCachePolicy = new ConcurrentHashMap<String, Integer>();    // -1 for non-cacheable
    private Map<String, Map<String, Field>>     modelAllFieldMap = new ConcurrentHashMap<String, Map<String, Field>>();
    private Map<String, Map<String, Field>>     modelAttrFieldMap = new ConcurrentHashMap<String, Map<String, Field>>();
    private Map<String, Map<String, String>>    modelFieldAttrMap = new ConcurrentHashMap<String, Map<String, String>>();
    private Map<String, Map<String, Field>>     modelS3FieldMap = new ConcurrentHashMap<String, Map<String, Field>>();
    private Map<String, Set<String>>            modelCacheByFields = new ConcurrentHashMap<String, Set<String>>();
    private Map<String, Method>     modelPrePersistMethod = new ConcurrentHashMap<String, Method>();
    private Map<String, Method>     modelPreValidationMethod = new ConcurrentHashMap<String, Method>();
    private Map<String, Method>     modelPostLoadMethod = new ConcurrentHashMap<String, Method>();
    private Map<String, Dao>        modelDao = new ConcurrentHashMap<String, Dao>();
    private Map<String, S3Dao>      modelS3Dao = new ConcurrentHashMap<String, S3Dao>();



    /** Create a Jsoda object with the AWS Access Key ID and Secret Access Key
     * The tables in the database scope belonged to the AWS Access Key ID will be accessed.
     */
    public Jsoda(AWSCredentials cred)
        throws Exception
    {
        this(cred, new MemCacheableSimple(10000));
    }

    /** Set a cache service for the Jsoda object.  All objects accessed via the Jsoda object will be cached according to their CachePolicy. */
    public Jsoda(AWSCredentials cred, MemCacheable memCacheable)
        throws Exception
    {
        if (cred == null || cred.getAWSAccessKeyId() == null || cred.getAWSSecretKey() == null)
            throw new IllegalArgumentException("AWS credentials are needed.");

        this.credentials = cred;
        this.objCacheMgr = new ObjCacheMgr(this, memCacheable);
        this.sdbMgr = new SimpleDBService(this, cred);
        this.ddbMgr = new DynamoDBService(this, cred);
        this.s3Client = new AmazonS3Client(cred);
        this.data1Registry = BuiltinFunc.cloneData1Registry();
        this.data2Registry = BuiltinFunc.cloneData2Registry();
        this.validationRegistry = BuiltinFunc.cloneValidationRegistry();
    }

    /** Set a new cache service for this Jsoda object.  All old cached content are gone. */
    public void setMemCacheable(MemCacheable memCacheable) {
        objCacheMgr.setMemCacheable(memCacheable);
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

    /** Get the AWS service endpoint for the underlying dbtype.  Return null for using AWS default. */
    public String getDbEndpoint(DbType dbtype) {
        return getDbService(dbtype).getDbEndpoint();
    }

    /** Set the global table prefix to add a prefix to all tables managed by the Jsoda object.
     * The global prefix is added in front of any other per-model-class prefix defined in @Model.prefix.
     * Global prefix is useful to scope all the tables for different purposes, e.g. creating test tables.
     */
    public Jsoda setGlobalPrefix(String globalPrefix) {
        this.globalPrefix = globalPrefix;
        return this;
    }

    /** Return the global table prefix to add a prefix to all tables managed by the Jsoda object. */
    public String getGlobalPrefix() {
        return globalPrefix;
    }

    /** Set the global S3 bucket and key base path for all S3 keys managed by the Jsoda object.
     * The global is added in front of any other per-model-class prefix defined in @Model.S3KeyPrefix.
     * Global prefix is useful to scope all the S3 keys for different purposes, e.g. creating test S3 objects.
     */
    public Jsoda setDefaultS3Bucket(String defaultS3Bucket) {
        this.defaultS3Bucket = defaultS3Bucket;
        return this;
    }

    public String getDefaultS3Bucket() {
        return this.defaultS3Bucket;
    }

    /** Set the global s3 key prefix to add a prefix to all S3 key managed by the Jsoda object.
     * The global prefix is added in front of any other per-model-field prefix defined in @S3Field.
     * Global prefix is useful to scope all the S3 objects for different purposes, e.g. creating test objects.
     */
    public Jsoda setS3KeyPrefix(String s3KeyPrefix) {
        this.s3KeyPrefix = s3KeyPrefix;
        return this;
    }

    public String getS3KeyPrefix() {
        return this.s3KeyPrefix;
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
        modelVersionFields.clear();
        modelCachePolicy.clear();
        modelAllFieldMap.clear();
        modelAttrFieldMap.clear();
        modelFieldAttrMap.clear();
        modelS3FieldMap.clear();
        modelCacheByFields.clear();
        modelPrePersistMethod.clear();
        modelPreValidationMethod.clear();
        modelPostLoadMethod.clear();
        modelDao.clear();
        modelS3Dao.clear();
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
            Field[]     allFields = getAllFields(modelClass);
            Field       idField = toKeyField(modelClass, allFields, false);
            Field       rangeField = toKeyField(modelClass, allFields, true);

            validateClass(modelClass);
            validateFields(modelClass, idField, rangeField, allFields);

            modelClasses.put(modelName, modelClass);
            modelDb.put(modelName, toDbService(modelClass, dbtype));
            modelTables.put(modelName, toTableName(modelClass));
            modelIdFields.put(modelName, idField);
            if (rangeField != null)
                modelRangeFields.put(modelName, rangeField);
            Field       versionField = ReflectUtil.findAnnotatedField(modelClass, VersionLocking.class);
            if (versionField != null)
                modelVersionFields.put(modelName, versionField);
            toCachePolicy(modelName, modelClass);
            modelAllFieldMap.put(modelName, toFieldMap(allFields));
            modelAttrFieldMap.put(modelName, toAttrFieldMap(allFields));
            modelFieldAttrMap.put(modelName, toFieldAttrMap(allFields));
            modelS3FieldMap.put(modelName, toS3FieldMap(modelClass));
            modelCacheByFields.put(modelName, toCacheByFields(allFields));  // Build CacheByFields on all fields, including the Id field
            toAnnotatedMethods(modelName, modelClass);
            modelDao.put(modelName, new Dao<T>(modelClass, this));
            modelS3Dao.put(modelName, new S3Dao<T>(modelClass, this));

            validateS3Fields(modelClass, idField, rangeField, modelS3FieldMap.get(modelName));
            data1Registry.checkModelOnFields(modelAllFieldMap.get(modelName));
            data2Registry.checkModelOnFields(modelAllFieldMap.get(modelName));
            validationRegistry.checkModelOnFields(modelAllFieldMap.get(modelName));

        } catch(JsodaException je) {
            throw je;
        } catch(Exception e) {
            throw new JsodaException("Failed to register model class", e);
        }
    }

    public boolean isRegistered(Class modelClass) {
        return (modelClasses.get(getModelName(modelClass)) != null);
    }

    public void registerData1Handler(Class annotationClass, AnnotationFieldHandler handler) {
        data1Registry.register(annotationClass, handler);
    }

    public void registerData2Handler(Class annotationClass, AnnotationFieldHandler handler) {
        data2Registry.register(annotationClass, handler);
    }

    public void registerValidationHandler(Class annotationClass, AnnotationFieldHandler handler) {
        validationRegistry.register(annotationClass, handler);
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

    public AmazonS3Client getS3Client() {
        return s3Client;
    }

    /** Return the table name of a registered model class. */
    String getModelTable(String modelName) {
        validateRegisteredModel(modelName);
        return modelTables.get(modelName);
    }

    /** Return the table name of a registered model class. */
    public String getModelTable(Class modelClass) {
        return getModelTable((String)getModelName(modelClass));
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

    /** Return the VersionLocking field of a registered model class. */
    Field getVersionField(String modelName) {
        validateRegisteredModel(modelName);
        return modelVersionFields.get(modelName);
    }

    /** Return a map of all the fields. */
    Map<String, Field> getAllFieldMap(String modelName) {
        validateRegisteredModel(modelName);
        return modelAllFieldMap.get(modelName);
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
    public <T> void createModelTable(Class<T> modelClass)
        throws JsodaException
    {
        if (!isRegistered(modelClass))
            registerModel(modelClass);
        String  modelName = getModelName(modelClass);
        getDb(modelName).createModelTable(modelName);
    }

    /** Delete modelClass' table in the registered model's database */
    public <T> void deleteModelTable(Class<T> modelClass)
        throws JsodaException
    {
        if (!isRegistered(modelClass))
            registerModel(modelClass);
        String  modelName = getModelName(modelClass);
        String  tableName = getModelTable(modelName);
        getDb(modelName).deleteTable(tableName);
    }

    /** Delete the table in the database, as named in tableName, in the dbtype */
    public void deleteNativeTable(DbType dbtype, String tableName) {
        getDbService(dbtype).deleteTable(tableName);
    }

    /** Create tables for all registered models. */
    @SuppressWarnings("unchecked")
    public void createRegisteredTables()
        throws JsodaException
    {
        for (Class modelClass : modelClasses.values()) {
            createModelTable(modelClass);
        }
    }

    /** Get the list of native table names from the underlying database.
     * @param dbtype  the dbtype, as defined in Model.
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

    @SuppressWarnings("unchecked")
    public <T> S3Dao<T> s3dao(Class<T> modelClass)
        throws JsodaException
    {
        if (!isRegistered(modelClass))
            registerModel(modelClass);
        return (S3Dao<T>)modelS3Dao.get(getModelName(modelClass));
    }

    /** Dump object's fields to string, for debugging. */
    public static String dump(Object obj) {
        return ReflectUtil.dumpObj(obj);
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

    Map<String, Field> getS3Fields(String modelName) {
        return modelS3FieldMap.get(modelName);
    }

    Set<String> getCacheByFields(String modelName) {
        return modelCacheByFields.get(modelName);
    }

    Integer getCachePolicy(String modelName) {
        return modelCachePolicy.get(modelName);
    }

    Method getPrePersistMethod(String modelName) {
        return modelPrePersistMethod.get(modelName);
    }

    Method getPreValidationMethod(String modelName) {
        return modelPreValidationMethod.get(modelName);
    }

    Method getPostLoadMethod(String modelName) {
        return modelPostLoadMethod.get(modelName);
    }

    String makePkKey(String modelName, Object dataObj)
        throws java.lang.IllegalAccessException
    {
        Field   idField = getIdField(modelName);
        Object  idKey = idField.get(dataObj);
        Field   rangeField = getRangeField(modelName);
        Object  rangeKey = rangeField == null ? null : rangeField.get(dataObj);
        return makePkKey(modelName, idKey, rangeKey);
    }

    String makePkKey(String modelName, Object idKey, Object rangeKey) {
        String  dbId = getDb(modelName).getDbTypeId();
        String  idStr = DataUtil.encodeValueToAttrStr(idKey, getIdField(modelName).getType());
        Field   rangeField = getRangeField(modelName);
        return rangeField == null ? idStr : idStr + "/" + DataUtil.encodeValueToAttrStr(rangeKey, rangeField.getType());
    }


    // Registration helper methods

    /** Get all fields to store in DB, including the Id field.  Skip the Transient fields and S3 fields. */
    private Field[] getAllFields(Class modelClass) {
        List<Field> fields = new ArrayList<Field>();

        for (Field field : ReflectUtil.getAllFields(modelClass)) {
            if (Modifier.isTransient(field.getModifiers()) ||
                ReflectUtil.hasAnnotation(field, Transient.class) ||
                ReflectUtil.hasAnnotation(field, S3Field.class) )
                continue;       // Skip
            fields.add(field);
        }
        return fields.toArray(new Field[fields.size()]);
    }

    private void validateClass(Class modelClass) {
        
    }

    private void validateFields(Class modelClass, Field idField, Field rangeField, Field[] allFields) {
        if (idField == null)
            throw new ValidationException("Missing the annotated @Id field in the model class.");
        if (Modifier.isTransient(idField.getModifiers()) || ReflectUtil.hasAnnotation(idField, Transient.class))
            throw new ValidationException("The @Key field cannot be transient or annotated with Transient in the model class.");
        if (rangeField != null && (Modifier.isTransient(idField.getModifiers()) || Modifier.isTransient(rangeField.getModifiers())))
            throw new ValidationException("The @Key field cannot be transient in the model class.");
        if (idField.getType() != String.class &&
            idField.getType() != Integer.class &&
            idField.getType() != int.class &&
            idField.getType() != Long.class &&
            idField.getType() != long.class)
            throw new ValidationException("The @Id field can only be String, Integer, or Long.");
    }

    private void validateS3Fields(Class modelClass, Field idField, Field rangeField, Map<String, Field> s3fields) {
        for (Field f : s3fields.values()) {
            if (f == idField || f == rangeField)
                throw new ValidationException("The @S3Field field cannot be a @Key field");
            if (Modifier.isTransient(f.getModifiers()) || ReflectUtil.hasAnnotation(f, Transient.class))
                throw new ValidationException("The @S3Field field cannot be transient or annotated with Transient in the model class.");
        }
    }

    private Field toKeyField(Class modelClass, Field[] allFields, boolean returnRangeKey) {
        Field   idField = null;
        Field   hashKeyField = null;
        Field   rangeKeyField = null;

        for (Field field : allFields) {
            if (!ReflectUtil.hasAnnotation(field, Key.class))
                continue;
            
            if (ReflectUtil.getAnnotationValueEx(field, Key.class, "hashKey", Boolean.class, Boolean.FALSE)) {
                if (hashKeyField == null) {
                    hashKeyField = field;
                } else {
                    throw new IllegalArgumentException("Only one field can be annotated as haskKey with @Key(hashKey=true).");
                }
            } else if (ReflectUtil.getAnnotationValueEx(field, Key.class, "rangeKey", Boolean.class, Boolean.FALSE)) {
                if (rangeKeyField == null) {
                    rangeKeyField = field;
                } else {
                    throw new IllegalArgumentException("Only one field can be annotated as rangeKey with @Key(rangeKey=true).");
                }
            } else if (ReflectUtil.getAnnotationValueEx(field, Key.class, "id", Boolean.class, Boolean.FALSE)) {
                if (idField == null) {
                    idField = field;
                } else {
                    throw new IllegalArgumentException("Only one field can be annotated as primary key with @Key(id=true).");
                }
            }
        }

        if (idField != null && (hashKeyField != null || rangeKeyField != null))
            throw new IllegalArgumentException("@Key(id=true) and @Key(hashKey=ture) cannot be specified in the same class.");

        if (returnRangeKey) {
            return rangeKeyField;
        }

        if (idField != null)
            return idField;
        else
            return hashKeyField;
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
                dbtype = ReflectUtil.getAnnotationValue(modelClass, Model.class, "dbtype", DbType.class, null);
            } catch(Exception e) {
                throw new IllegalArgumentException("Error in getting dbtype from the Model annotation for " + modelClass, e);
            }
            if (dbtype == null || dbtype == DbType.None)
                throw new IllegalArgumentException("No valid 'dbtype' specification in the Model annotation for " + modelClass);
        }

        return getDbService(dbtype);
    }

    private String toTableName(Class modelClass) {
        String  modelName = getModelName(modelClass);
        String  tableName = ReflectUtil.getAnnotationValue(modelClass, Model.class, "table", modelName);   // default to modelName
        String  prefix = ReflectUtil.getAnnotationValue(modelClass, Model.class, "prefix", "");
        if (!StringUtils.isEmpty(globalPrefix))
            prefix = globalPrefix + prefix;
        return prefix + tableName;
    }

    private void toCachePolicy(String modelName, Class modelClass)
        throws Exception
    {
        // See if class has implemented Serializable
        List<Class> allInterfaces = ReflectUtil.getAllInterfaces(modelClass);
        boolean     hasSerializable = FnUtil.fold(false, allInterfaces, new FoldFn<Boolean, Class>() {
                public Boolean apply(Boolean hasSerializable, Class intf) {
                    return hasSerializable || (intf == Serializable.class);
                }
            });

        // Decide CachePolicy
        boolean     cacheable = ReflectUtil.getAnnotationValue(modelClass, CachePolicy.class, "cacheable", Boolean.class, Boolean.TRUE); // default is true

        if (hasSerializable) {
            if (cacheable) {
                // Serializable and Cacheable.  Can cache.
                int expireInSeconds = ReflectUtil.getAnnotationValue(modelClass, CachePolicy.class, "expireInSeconds", Integer.class, 0);
                modelCachePolicy.put(modelName, new Integer(expireInSeconds));
                return;
            }
        } else {
            if (ReflectUtil.hasAnnotation(modelClass, CachePolicy.class)) {
                // Not Serializable but CachePolicy specified.  Specification conflict.
                throw new IllegalArgumentException("Model class " + modelClass.getName() + " must implement the java.io.Serializable when specifying CachePolicy for caching.");
            }
        }

        // Don't cache objects of the model.
        modelCachePolicy.put(modelName, new Integer(-1));
    }


    @SuppressWarnings("unchecked")
    private Map<String, Field> toFieldMap(Field[] fields)
        throws Exception
    {
        return FnUtil.fold(new ConcurrentHashMap(), 
                           fields,
                           new FoldFn<Map, Field>() {
                               public Map apply(Map map, Field field) {
                                   map.put(field.getName(), field);
                                   return map;
                               }
                           });
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

    private Map<String, Field> toS3FieldMap(Class modelClass) {
        Map<String, Field>  map = new HashMap<String, Field>();

        for (Field field : ReflectUtil.getAllFields(modelClass)) {
            if (ReflectUtil.hasAnnotation(field, S3Field.class))
                map.put(field.getName(), field);
        }
        return map;
    }


    private void toAnnotatedMethods(String modelName, Class modelClass)
        throws Exception
    {
        for (Method method : ReflectUtil.getAllMethods(modelClass)) {
            if (ReflectUtil.hasAnnotation(method, PrePersist.class))
                modelPrePersistMethod.put(modelName, method);
            if (ReflectUtil.hasAnnotation(method, PreValidation.class))
                modelPreValidationMethod.put(modelName, method);
            if (ReflectUtil.hasAnnotation(method, PostLoad.class))
                modelPostLoadMethod.put(modelName, method);
        }
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



    /**
     * Perform the pre-store steps to call the data generators, conversion, and validation on the object.
     * The object is not stored yet; only its fields are transformed.
     * Call this directly for debugging data conversion and validation.
     */
    public void preStoreSteps(Object dataObj)
        throws Exception
    {
        if (dataObj == null)
            return;

        String  modelName = getModelName(dataObj.getClass());
        validateRegisteredModel(modelName);

        if (getPrePersistMethod(modelName) != null)
            getPrePersistMethod(modelName).invoke(dataObj);

        data1Registry.applyFieldHandlers(dataObj, getAllFieldMap(modelName));

        data2Registry.applyFieldHandlers(dataObj, getAllFieldMap(modelName));

        if (getPreValidationMethod(modelName) != null)
            getPreValidationMethod(modelName).invoke(dataObj);
        
        validationRegistry.applyFieldHandlers(dataObj, getAllFieldMap(modelName));
    }

    public void postGetSteps(Object dataObj)
        throws Exception
    {
        if (dataObj == null)
            return;

        String  modelName = getModelName(dataObj.getClass());
        validateRegisteredModel(modelName);

        if (getPostLoadMethod(modelName) != null)
            getPostLoadMethod(modelName).invoke(dataObj);

        getObjCacheMgr().cachePut(modelName, dataObj);
    }

}

