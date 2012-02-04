
package wwutil.jsoda;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import wwutil.model.annotation.CachePolicy;
import wwutil.model.annotation.DefaultGUID;
import wwutil.model.annotation.DefaultComposite;
import wwutil.model.annotation.CacheByField;



@SuppressWarnings("unchecked")
public class Dao<T>
{
    private static Log  log = LogFactory.getLog(Dao.class);

    private Class<T>    modelClass;
    private String      modelName;
    private Jsoda       jsoda;


    public Dao(Class<T> modelClass, Jsoda jsoda) {
        this.modelClass = modelClass;
        this.modelName = jsoda.getModelName(modelClass);
        this.jsoda = jsoda;
    }

    public void put(T dataObj)
        throws JsodaException
    {
        putIf(dataObj, null, null);
    }    

    public void putIf(T dataObj, String expectedField, Object expectedValue)
        throws JsodaException
    {
        try {
            preStoreSteps(dataObj);
            jsoda.getDb(modelName).putObj(modelName, dataObj, expectedField, expectedValue);
            jsoda.getObjCacheMgr().cachePut(modelName, (Serializable)dataObj);
        } catch(Exception e) {
            throw new JsodaException("Failed to put object", e);
        }
    }

    public void batchPut(List<T> dataObjs)
        throws JsodaException
    {
        if (dataObjs.size() == 0)
            return;

        try {
            for (T dataObj : dataObjs) {
                preStoreSteps(dataObj);
            }
            jsoda.getDb(modelName).putObjs(modelName, dataObjs);
            for (T dataObj : dataObjs) {
                jsoda.getObjCacheMgr().cachePut(modelName, (Serializable)dataObj);
            }
        } catch(Exception e) {
            throw new JsodaException("Failed to batch put objects", e);
        }
    }

    private void preStoreSteps(T dataObj)
        throws Exception
    {
        callPrePersist(modelName, dataObj);
        fillDefaults(modelName, dataObj);
        fillCompositeDefaults(modelName, dataObj);
        callPreValidation(modelName, dataObj);
        validateFields(modelName, dataObj);
    }

    public T get(String id)
        throws JsodaException
    {
        return getObj(id, null);
    }

    public T get(String id, Object rangeKey)
        throws JsodaException
    {
        return getObj(id, rangeKey);
    }

    public T get(Long id)
        throws JsodaException
    {
        return getObj(id, null);
    }

    public T get(Long id, Object rangeKey)
        throws JsodaException
    {
        return getObj(id, rangeKey);
    }

    public T get(Integer id)
        throws JsodaException
    {
        return getObj(id, null);
    }

    public T get(Integer id, Object rangeKey)
        throws JsodaException
    {
        return getObj(id, rangeKey);
    }

    private T getObj(Object id, Object rangeKey)
        throws JsodaException
    {
        try {
            T   obj = (T)jsoda.getObjCacheMgr().cacheGet(modelName, id, rangeKey);
            if (obj != null)
                return obj;

            if (rangeKey == null)
                obj = (T)jsoda.getDb(modelName).getObj(modelName, id);
            else
                obj = (T)jsoda.getDb(modelName).getObj(modelName, id, rangeKey);

            postGet(obj);

            return obj;
        } catch(Exception e) {
            throw new JsodaException("Failed to get object", e);
        }
    }

    public void delete(String id)
        throws JsodaException
    {
        deleteObj(id, null);
    }

    public void delete(String id, Object rangeKey)
        throws JsodaException
    {
        deleteObj(id, rangeKey);
    }

    public void delete(Long id)
        throws JsodaException
    {
        deleteObj(id, null);
    }

    public void delete(Long id, Object rangeKey)
        throws JsodaException
    {
        deleteObj(id, rangeKey);
    }

    public void delete(Integer id)
        throws JsodaException
    {
        deleteObj(id, null);
    }

    public void delete(Integer id, Object rangeKey)
        throws JsodaException
    {
        deleteObj(id, rangeKey);
    }

    private void deleteObj(Object id, Object rangeKey)
        throws JsodaException
    {
        try {
            jsoda.getObjCacheMgr().cacheDelete(modelName, id, rangeKey);
            if (rangeKey == null) {
                jsoda.getDb(modelName).delete(modelName, id);
            } else {
                jsoda.getDb(modelName).delete(modelName, id, rangeKey);
            }
        } catch(Exception e) {
            throw new JsodaException("Failed to delete object " + id + "/" + rangeKey, e);
        }
    }

    public void batchDelete(List idList)
        throws JsodaException
    {
        try {
            for (Object id : idList) {
                jsoda.getObjCacheMgr().cacheDelete(modelName, id, null);
            }
            jsoda.getDb(modelName).batchDelete(modelName, idList);
        } catch(Exception e) {
            throw new JsodaException("Failed to batch delete objects", e);
        }
    }

    public void batchDelete(List idList, List rangeKeyList)
        throws JsodaException
    {
        try {
            for (int i = 0; i < idList.size(); i++) {
                jsoda.getObjCacheMgr().cacheDelete(modelName, idList.get(i), rangeKeyList.get(i));
            }
            jsoda.getDb(modelName).batchDelete(modelName, idList, rangeKeyList);
        } catch(Exception e) {
            throw new JsodaException("Failed to batch delete objects", e);
        }
    }

    /** Get an object by one of its field, beside the Id field. */
    public T findBy(String field, Object fieldValue)
        throws JsodaException
    {
        T       obj = (T)jsoda.getObjCacheMgr().cacheGetByField(modelName, field, fieldValue);
        if (obj != null)
            return obj;

        List<T> items = jsoda.query(modelClass).eq(field, fieldValue).run();
        // query.run() has already cached the object.  No need to cache it here.
        return items.size() == 0 ? null : items.get(0);
    }


    void postGet(Object obj)
        throws JsodaException
    {
        callPostLoad(modelName, obj);
        jsoda.getObjCacheMgr().cachePut(modelName, (Serializable)obj);
    }
    
    protected void validateFields(String modelName, Object dataObj)
        throws Exception
    {
        for (Field field : jsoda.getAllFields(modelName)) {

            // TODO: Use new annotation class for check null
            // Boolean isAttrNullable = ReflectUtil.getAnnotationValue(field, Column.class, "nullable", Boolean.class, Boolean.TRUE);
            // if (!isAttrNullable && field.get(dataObj) == null)
            //     throw new ValidationException("Field " + field.getName() + " cannot be null.");

        }
    }

    protected T fillDefaults(String modelName, T dataObj)
        throws Exception
    {
        for (Field field : jsoda.getAllFields(modelName)) {
            Object  value = field.get(dataObj);
            if (value == null || value.toString().length() == 0)
                continue;

            if (ReflectUtil.hasAnnotation(field, DefaultGUID.class)) {
                fillDefaultGUID(modelName, field, dataObj);
            }
        }
        return dataObj;
    }

    protected T fillCompositeDefaults(String modelName, T dataObj)
        throws Exception
    {
        for (Field field : jsoda.getAllFields(modelName)) {
            Object  value = field.get(dataObj);
            if (value == null || value.toString().length() == 0)
                continue;

            if (ReflectUtil.hasAnnotation(field, DefaultComposite.class)) {
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
            Field       subpartField = jsoda.getField(modelName, fromFields[i]);
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

    private void callPrePersist(String modelName, Object dataObj)
        throws JsodaException
    {
        try {
            Method  prePersistMethod = jsoda.getPrePersistMethod(modelName);
            if (prePersistMethod != null) {
                prePersistMethod.invoke(dataObj);
            }
        } catch(Exception e) {
            throw new JsodaException("callPrePersist", e);
        }
    }

    private void callPreValidation(String modelName, Object dataObj)
        throws JsodaException
    {
        try {
            Method  preValidationMethod = jsoda.getPreValidationMethod(modelName);
            if (preValidationMethod != null) {
                preValidationMethod.invoke(dataObj);
            }
        } catch(Exception e) {
            throw new JsodaException("callPreValidation", e);
        }
    }

    private void callPostLoad(String modelName, Object dataObj)
        throws JsodaException
    {
        try {
            Method  postLoadMethod = jsoda.getPostLoadMethod(modelName);
            if (postLoadMethod != null) {
                postLoadMethod.invoke(dataObj);
            }
        } catch(Exception e) {
            throw new JsodaException("callPostLoad", e);
        }
    }

}

