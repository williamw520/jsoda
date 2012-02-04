
package wwutil.jsoda;

import java.util.*;
import java.lang.reflect.*;

import org.apache.commons.beanutils.ConvertUtils;
import org.codehaus.jackson.map.ObjectMapper;
import com.amazonaws.services.simpledb.util.SimpleDBUtils;

import wwutil.sys.TlsMap;


class DataUtil
{
    public static final String  NULL_STR = "_NULL_";            // null encoding

    private static TlsMap.Factory<ObjectMapper> sTlsObjectMapper = new TlsMap.Factory<ObjectMapper>() {
        public ObjectMapper create(Object key) {
            return new ObjectMapper();
        }
    };


    static String getFieldValueStr(Object dataObj, Field field)
        throws Exception
    {
        Object  value = field.get(dataObj);
        return encodeValueToAttrStr(value, field.getType());
    }

    static void setFieldValueStr(Object dataObj, Field field, String attrStr)
        throws Exception
    {
        Object  value = decodeAttrStrToValue(attrStr, field.getType());
        field.set(dataObj, value);
    }


    /** Caller should handle custom valueType first before calling this.
     * E.g. DynamoDB's Set<String> and Set<long> fields are encoded as Multi-Value AttributeValue.
     */
    @SuppressWarnings("unchecked")
    static String encodeValueToAttrStr(Object value, Class valueType) {
        if (value == null)
            return null;                // Caller needs to handle null correctly, e.g. skip storing AttributeValue.

        if (valueType == String.class)
            return value.toString();

        // Stringify basic type and encode them for sorting.
        if (valueType == Byte.class || valueType == byte.class) {
            Byte casted = (Byte)ConvertUtils.convert(value, Byte.class);
            return SimpleDBUtils.encodeZeroPadding(casted.intValue(), 3);           // 0-Padded for sorting
        } else if (valueType == Short.class || valueType == short.class) {
            Short casted = (Short)ConvertUtils.convert(value, Short.class);
            return SimpleDBUtils.encodeZeroPadding(casted.intValue(), 5);           // 0-Padded for sorting
        } else if (valueType == Integer.class || valueType == int.class) {
            Integer casted = (Integer)ConvertUtils.convert(value, Integer.class);
            return SimpleDBUtils.encodeZeroPadding(casted.intValue(), 10);          // 0-Padded for sorting
        } else if (valueType == Long.class || valueType == long.class) {
            Long    casted = (Long)ConvertUtils.convert(value, Long.class);
            return SimpleDBUtils.encodeZeroPadding(casted.longValue(), 19);         // 0-Padded for sorting
        } else if (valueType == Float.class || valueType == float.class) {
            Float   casted = (Float)ConvertUtils.convert(value, Float.class);
            return SimpleDBUtils.encodeZeroPadding(casted.floatValue(), 16);        // 0-Padded for sorting
        } else if (valueType == Double.class || valueType == double.class) {
            // SimpleDBUtils has no padding for double.  Just convert it to String.
            return value.toString();
        } else if (valueType == Boolean.class || valueType == boolean.class) {
            return value.toString();
        } else if (valueType == Character.class || valueType == char.class) {
            return value.toString();
        } else if (valueType == Date.class) {
            return SimpleDBUtils.encodeDate((Date)value);
        }

        // JSONify the rest.
        try {
            return TlsMap.get("jsoda_om", sTlsObjectMapper).writeValueAsString(value);
        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    /** Caller should handle custom valueType first before calling this. */
    @SuppressWarnings("unchecked")
    static Object decodeAttrStrToValue(String attrStr, Class valueType)
        throws Exception
    {
        // Set null if input is null.
        if (attrStr == null || attrStr.equals(DataUtil.NULL_STR))
            return null;

        if (valueType == String.class)
            return attrStr;                 // Return string type as it is.

        // non-String field having "" is treated as null.
        if (attrStr.equals(""))
            return null;

        if (valueType == Byte.class || valueType == byte.class) {
            return new Byte((byte)SimpleDBUtils.decodeZeroPaddingInt(attrStr));
        } else if (valueType == Short.class || valueType == short.class) {
            return new Short((short)SimpleDBUtils.decodeZeroPaddingInt(attrStr));
        } else if (valueType == Integer.class || valueType == int.class) {
            return new Integer(SimpleDBUtils.decodeZeroPaddingInt(attrStr));
        } else if (valueType == Long.class || valueType == long.class) {
            return new Long(SimpleDBUtils.decodeZeroPaddingLong(attrStr));
        } else if (valueType == Float.class || valueType == float.class) {
            return new Float(SimpleDBUtils.decodeZeroPaddingFloat(attrStr));
        } else if (valueType == Double.class || valueType == double.class) {
            return new Double(attrStr);
        } else if (valueType == Boolean.class || valueType == boolean.class) {
            return new Boolean(attrStr);
        } else if (valueType == Character.class || valueType == char.class) {
            return attrStr.charAt(0);
        } else if (valueType == Date.class) {
            return SimpleDBUtils.decodeDate(attrStr);
        }

        // de-JSONify the rest.
        return TlsMap.get("jsoda_om", sTlsObjectMapper).readValue(attrStr, valueType);
    }

    /** Check whether a value/valueType can be encoded, so that it can be used as condition value in query. */
    static boolean canBeEncoded(Object value, Class valueType) {
        if (value == null)
            return true;

        if (valueType == String.class)
            return true;

        // Stringify basic type and encode them for sorting.
        if (valueType == Byte.class || valueType == byte.class) {
            return true;
        } else if (valueType == Short.class || valueType == short.class) {
            return true;
        } else if (valueType == Integer.class || valueType == int.class) {
            return true;
        } else if (valueType == Long.class || valueType == long.class) {
            return true;
        } else if (valueType == Float.class || valueType == float.class) {
            return true;
        } else if (valueType == Double.class || valueType == double.class) {
            return true;
        } else if (valueType == Boolean.class || valueType == boolean.class) {
            return true;
        } else if (valueType == Character.class || valueType == char.class) {
            return true;
        } else if (valueType == Date.class) {
            return true;
        }

        // JSON string value should not be used in query condition.
        return false;
    }


    /** Convert Set<paramType> to Set<String> */
    static Set<String> toStringSet(Set valueSet, Class paramType) {
        Set<String> strSet = new HashSet<String>();
        for (Object value : valueSet) {
            strSet.add(encodeValueToAttrStr(value, paramType));
        }
        return strSet;
    }

    /** Convert List<String> to Set<paramType> */
    static Set<Object> toObjectSet(List<String> strs, Class paramType)
        throws Exception
    {
        Set<Object> set = new HashSet<Object>();
        for (String str : strs) {
            set.add(decodeAttrStrToValue(str, paramType));
        }
        return set;
    }
    
}
