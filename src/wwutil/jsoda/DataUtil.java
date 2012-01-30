
package wwutil.jsoda;

import java.util.*;
import java.lang.reflect.*;

import org.apache.commons.beanutils.ConvertUtils;
import com.amazonaws.services.simpledb.util.SimpleDBUtils;
import com.amazonaws.services.dynamodb.model.AttributeValue;


class DataUtil
{
    public static final String  NULL_STR = "_NULL_";            // null encoding


    static String getFieldValueStr(Object dataObj, Field field)
        throws Exception
    {
        Object  value = field.get(dataObj);
        return toValueStr(value, field.getType());
    }

    static void setFieldValueStr(Object dataObj, Field field, String valueStr)
        throws Exception
    {
        Object  value = toValueObj(valueStr, field.getType());
        field.set(dataObj, value);
    }

    // valueType comes from Field.getType()
    static String toValueStr(Object value, Class valueType) {
        if (value == null)
            return NULL_STR;

        if (valueType == Integer.class || valueType == int.class) {
            Integer casted = (Integer)ConvertUtils.convert(value, Integer.class);
            return SimpleDBUtils.encodeZeroPadding(casted.intValue(), 10);
        } else if (valueType == Long.class || valueType == long.class) {
            Long    casted = (Long)ConvertUtils.convert(value, Long.class);
            return SimpleDBUtils.encodeZeroPadding(casted.longValue(), 19);
        } else if (valueType == Float.class || valueType == float.class) {
            Float   casted = (Float)ConvertUtils.convert(value, Float.class);
            return SimpleDBUtils.encodeZeroPadding(casted.floatValue(), 16);
        } else if (valueType == Double.class || valueType == double.class) {
            // Padding of double not support.  Just use default to convert it to String.
        } else if (valueType == Date.class) {
            return SimpleDBUtils.encodeDate((Date)value);
        }

        return value.toString();
    }

    /**
     * valueType = Field.getType()
     */
    static Object toValueObj(String valueStr, Class valueType)
        throws Exception
    {
        // Set null if input is null, or non-String field having "" or "null" input.
        if (valueStr == null ||
            valueStr.equals(NULL_STR) ||
            (valueType != String.class && valueStr.equals(""))) {
            return null;
        }

        if (valueType == Integer.class || valueType == int.class) {
            return SimpleDBUtils.decodeZeroPaddingInt(valueStr);
        } else if (valueType == Long.class || valueType == long.class) {
            return SimpleDBUtils.decodeZeroPaddingLong(valueStr);
        } else if (valueType == Float.class || valueType == float.class) {
            return SimpleDBUtils.decodeZeroPaddingFloat(valueStr);
        } else if (valueType == Double.class || valueType == double.class) {
            // Padding of double not support.  Just use default to convert it to String.
        } else if (valueType == Date.class) {
            return SimpleDBUtils.decodeDate(valueStr);
        }

        return ConvertUtils.convert(valueStr, valueType);
    }

}
