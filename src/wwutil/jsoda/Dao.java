
package wwutil.jsoda;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;

import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.persistence.PrePersist;
import javax.persistence.PostLoad;

import wwutil.model.annotation.CachePolicy;
import wwutil.model.annotation.DefaultGUID;
import wwutil.model.annotation.DefaultComposite;
import wwutil.model.annotation.CacheByField;



public class Dao<T>
{

    private Class<T>    modelClass;
    private String      modelName;
    private Jsoda       jsoda;


    public Dao(Class<T> modelClass, Jsoda jsoda) {
        jsoda.validateRegisteredModel(modelClass);
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
            fillDefaults(modelName, dataObj);
            callPrePersist(modelName, dataObj);
            validateFields(modelName, dataObj);

            jsoda.sdbMgr.putObj(modelName, dataObj, expectedField, expectedValue);
            jsoda.cachePutByFields(modelName, (Serializable)dataObj);
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
                fillDefaults(modelName, dataObj);
                callPrePersist(modelName, dataObj);
                validateFields(modelName, dataObj);
            }

            jsoda.sdbMgr.putObjs(modelName, dataObjs);

            for (T dataObj : dataObjs) {
                jsoda.cachePutByFields(modelName, (Serializable)dataObj);
            }
        } catch(Exception e) {
            throw new JsodaException("Failed to batch put objects", e);
        }
    }

    @SuppressWarnings("unchecked")
    public T get(String id)
        throws JsodaException
    {
        try {
            String  cacheKey = DataUtil.toValueStr(id);
            T       obj = (T)jsoda.cacheGet(modelName, cacheKey);
            if (obj != null)
                return obj;

            obj = (T)jsoda.sdbMgr.getObj(modelName, id);

            callPostLoad(modelName, obj);
            jsoda.cachePutByFields(modelName, (Serializable)obj);

            return obj;
        } catch(Exception e) {
            throw new JsodaException("Failed to get object", e);
        }
    }

    public void delete(String id)
        throws JsodaException
    {
        try {
            String  cacheKey = DataUtil.toValueStr(id);
            jsoda.cacheDeleteByFields(modelName, cacheKey);
            jsoda.sdbMgr.delete(modelName, id);
        } catch(Exception e) {
            throw new JsodaException("Failed to delete object " + id, e);
        }
    }

    public void batchDelete(List<String> idList)
        throws JsodaException
    {
        try {
            for (String id : idList) {
                String  cacheKey = DataUtil.toValueStr(id);
                jsoda.cacheDeleteByFields(modelName, cacheKey);
            }
            jsoda.sdbMgr.batchDelete(modelName, idList);
        } catch(Exception e) {
            throw new JsodaException("Failed to batch delete objects", e);
        }
    }
    
    

    protected void validateFields(String modelName, Object dataObj)
        throws Exception
    {
        for (Field field : jsoda.modelAllFields.get(modelName)) {
            Boolean isAttrNullable = ReflectUtil.getAnnotationValue(field, Column.class, "nullable", Boolean.class, Boolean.TRUE);
            if (!isAttrNullable && field.get(dataObj) == null)
                throw new ValidationException("Field " + field.getName() + " cannot be null.");
        }
    }

    protected T fillDefaults(String modelName, T dataObj)
        throws Exception
    {
        for (Field field : jsoda.modelAllFields.get(modelName)) {
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
            Field       subpartField = jsoda.modelAllFieldMap.get(modelName).get(fromFields[i]);
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

    protected void callPrePersist(String modelName, Object dataObj)
        throws Exception
    {
        Method  prePersistMethod = jsoda.modelPrePersistMethod.get(modelName);
        if (prePersistMethod != null) {
            prePersistMethod.invoke(dataObj);
        }
    }

    protected void callPostLoad(String modelName, Object dataObj)
        throws Exception
    {
        Method  postLoadMethod = jsoda.modelPostLoadMethod.get(modelName);
        if (postLoadMethod != null) {
            postLoadMethod.invoke(dataObj);
        }
    }

}

