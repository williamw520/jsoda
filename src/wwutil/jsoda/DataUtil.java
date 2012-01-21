
package wwutil.jsoda;

import java.util.*;
import java.lang.reflect.*;

import org.apache.commons.beanutils.ConvertUtils;
import com.amazonaws.services.simpledb.util.SimpleDBUtils;



class DataUtil
{
    public static final String  NULL_STR = "_null_";            // null encoding


    static String getFieldValueStr(Object dataObj, Field field)
        throws Exception
    {
        Object  value = field.get(dataObj);
        return toValueStr(value);
    }

    static void setFieldValueStr(Object dataObj, Field field, String valueStr)
        throws Exception
    {
        Object  value = toValueObj(field.getType(), valueStr);
        field.set(dataObj, value);
    }

    static String toValueStr(Object value) {
        if (value == null)
            return NULL_STR;

        if (value instanceof Integer)
            return SimpleDBUtils.encodeZeroPadding(((Integer)value).intValue(), 10);
        if (value instanceof Long)
            return SimpleDBUtils.encodeZeroPadding(((Long)value).longValue(), 19);
        if (value instanceof Float)
            return SimpleDBUtils.encodeZeroPadding(((Float)value).floatValue(), 16);
        if (value instanceof Date)
            return SimpleDBUtils.encodeDate((Date)value);

        return value.toString();
    }

    /**
     * valueType = Field.getType()
     */
    static Object toValueObj(Class valueType, String valueStr)
        throws Exception
    {
        // Set null if input is null, or non-String field having "" or "null" input.
        if (valueStr == null ||
            valueStr.equals(NULL_STR) ||
            (valueType != String.class && valueStr.equals(""))) {
            return null;
        }

        if (valueType == Integer.class)
            return SimpleDBUtils.decodeZeroPaddingInt(valueStr);
        if (valueType == Long.class)
            return SimpleDBUtils.decodeZeroPaddingLong(valueStr);
        if (valueType == Float.class)
            return SimpleDBUtils.decodeZeroPaddingFloat(valueStr);
        if (valueType == Double.class)
            return SimpleDBUtils.decodeZeroPaddingFloat(valueStr);
        if (valueType == Date.class)
            return SimpleDBUtils.decodeDate(valueStr);

        return ConvertUtils.convert(valueStr, valueType);
    }

}
