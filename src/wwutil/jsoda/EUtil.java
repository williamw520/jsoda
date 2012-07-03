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

import java.util.*;
import java.io.*;
import java.lang.reflect.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import wwutil.sys.FnUtil;
import wwutil.sys.FnUtil.*;
import wwutil.sys.ReflectUtil;
import wwutil.model.ValidationException;
import wwutil.model.annotation.CachePolicy;
import wwutil.model.annotation.CacheByField;
import wwutil.model.annotation.DefaultGUID;
import wwutil.model.annotation.DefaultComposite;
import wwutil.model.annotation.VersionLocking;
import wwutil.model.annotation.S3Field;



/**
 * Entity util.  Helper util methods on the model object.
 */
@SuppressWarnings("unchecked")
public class EUtil<T>
{
    private static Log  log = LogFactory.getLog(EUtil.class);

    private Class<T>    modelClass;
    private String      modelName;
    private Jsoda       jsoda;


    public EUtil(Class<T> modelClass, Jsoda jsoda) {
        this.modelClass = modelClass;
        this.modelName = jsoda.getModelName(modelClass);
        this.jsoda = jsoda;
    }

    /** Create a new initialize object instance of the model class.
     * Initialize with default values using the field annotations.
     * The following preStore steps will be called: @PrePersist, data handlers, composite data handlers.
     * e.g. @DefaultGUID and @DefaultComposite will be run.
     * Note that validations will not be run.
     */
    public T newInitInstance()
        throws Exception
    {
        T   dataObj = modelClass.newInstance();
        jsoda.preStoreTransformSteps(dataObj);
        return dataObj;
    }

    /** Create a new initialize object instance of the model class.
     * Initialize the key field(s) with idKey and idRangeKey (optional).
     * Initialize with default values using the field annotations.
     */
    public T newInitInstance(Object idKey, Object idRangeKey)
        throws Exception
    {
        T   dataObj = modelClass.newInstance();
        setIdValue(dataObj, idKey);
        setRangeValue(dataObj, idRangeKey);
        jsoda.preStoreTransformSteps(dataObj);
        return dataObj;
    }


    /** Dump object's fields to string, for debugging. */
    public static String dump(Object obj) {
        return ReflectUtil.dumpObj(obj);
    }

    /** Return the Id field of the model */
    public Field getIdField() {
        return jsoda.getIdField(modelName);
    }

    /** Return the range field of the model.  Return null if range field not defined. */
    public Field getRangeField() {
        return jsoda.getRangeField(modelName);
    }

    public Field getField(String fieldName) {
        Field   f = jsoda.getField(modelName, fieldName);
        if (f == null)
            throw new IllegalArgumentException("Field " + fieldName + " doesn't exist on model class " + modelName);
        return f;
    }

    /** Check to see if field is the Id field. */
    public boolean isIdField(String fieldName) {
        return getIdField().getName().equals(fieldName);
    }

    /** Check to see if field is an RangeKey field. */
    public boolean isRangeField(String fieldName) {
        Field   rangeField = getRangeField();
        return rangeField != null && rangeField.getName().equals(fieldName);
    }

    /** Get the value of the Id field */
    public Object getIdValue(T obj)
        throws IllegalAccessException
    {
        return getIdField().get(obj);
    }

    /** Get the value of the Range field */
    public Object getRangeValue(T obj)
        throws IllegalAccessException
    {
        Field   f = getRangeField();
        return  f != null ? f.get(obj) : null;
    }

    /** Get the value of the field of fieldName */
    public Object getFieldValue(T obj, String fieldName)
        throws IllegalAccessException
    {
        return getField(fieldName).get(obj);
    }

    /** Set the value of the Id field */
    public T setIdValue(T obj, Object idValue)
        throws IllegalAccessException
    {
        getIdField().set(obj, idValue);
        return obj;
    }

    /** Set the value of the RangeKey field */
    public T setRangeValue(T obj, Object rangeKeyValue)
        throws IllegalAccessException
    {
        Field   rangeField = getRangeField();
        if (rangeField != null)
            rangeField.set(obj, rangeKeyValue);
        return obj;
    }

    /** Set the value of the field of fieldName */
    public T setFieldValue(T obj, String fieldName, Object fieldValue)
        throws IllegalAccessException
    {
        getField(fieldName).set(obj, fieldValue);
        return obj;
    }


    /** Transform the dataObj into a value map with the field names as keys. */
    public Map<String, Object> asValueMap(final T dataObj) {
        Map<String, Field>  allFields = jsoda.getAllFieldMap(modelName);
        return FnUtil.map(allFields, new TransformFn<Object, Field>() {
                public Object apply(Field f) {
                    try {
                        return f.get(dataObj);
                    } catch(IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
    }

}

