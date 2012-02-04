
package wwutil.jsoda;


import java.util.*;
import java.lang.reflect.*;
import org.apache.commons.beanutils.ConvertUtils;


@SuppressWarnings("unchecked")
class ReflectUtil
{

	private ReflectUtil()
	{
		// disable
	}

    /** Return the list of fields declared at all level of class hierachy, include super private fields.
     */
    public static List<Field> getAllFields(Class clazz) {
        return getAllFields(clazz, new ArrayList<Field>());
    }

    private static List<Field> getAllFields(Class clazz, List<Field> list) {
        for (Field field : clazz.getDeclaredFields()) {
            // Filter out compiler synthesized fields and static fields.
            if (!field.isSynthetic() &&
                !Modifier.isStatic(field.getModifiers()))
                list.add(field);
        }
        Class   superClazz = clazz.getSuperclass();
        if (superClazz != null)
            getAllFields(superClazz, list);
        return list;
    }

    public static List<Method> getAllMethods(Class clazz) {
        return getAllMethods(clazz, new ArrayList<Method>());
    }

    public static List<Method> getAllMethods(Class clazz, List<Method> list) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.isSynthetic() &&
                !Modifier.isStatic(method.getModifiers()))
                list.add(method);
        }
        Class   superClazz = clazz.getSuperclass();
        if (superClazz != null) 
            getAllMethods(superClazz, list);
        return list;
    }

    public static List<Class> getAllInterfaces(Class clazz) {
        return getAllInterfaces(clazz, new ArrayList<Class>());
    }

    private static List<Class> getAllInterfaces(Class clazz, List<Class> list) {
        for (Class intf : clazz.getInterfaces()) {
            list.add(intf);
        }
        Class   superClazz = clazz.getSuperclass();
        if (superClazz != null)
            getAllInterfaces(superClazz, list);
        return list;
    }


    public static String[] getFieldNames(Class clazz) {
        List<Field> fields = getAllFields(clazz);
        String[]    names = new String[fields.size()];

        for (int i = 0; i < fields.size(); i++) {
            try {
                names[i] = fields.get(i).getName();
            } catch(Exception ignored) {}
        }
        return names;
    }

    public static Map<String, Field> getFieldMap(Class clazz) {
        List<Field>         fields = getAllFields(clazz);
        Map<String, Field>  map = new HashMap<String, Field>();

        for (Field field : fields) {
            try {
                map.put(field.getName(), field);
            } catch(Exception ignored) {}
        }
        return map;
    }

    /** Get the parameterized type of a generic type GenericType<ParameterizedType>
     * For method, call method.getGenericParameterTypes() or method.getGenericReturnType() for the parameterizedType.
     * For field, call field.getGenericType() for the parameterizedType.
     * e.g.  List<String> field1 ==> getGenericParamType1(field11.getGenericType()) => String
     * Return null for none found.
     */
    public static Class getGenericParamType1(Type parameterizedType) {
        if (parameterizedType instanceof ParameterizedType) {
            ParameterizedType   type = (ParameterizedType) parameterizedType;
            Type[]              typeArguments = type.getActualTypeArguments();
            if (typeArguments.length > 0)
                return (Class)typeArguments[0];
        }
        return null;
    }

    public static Map<String, Object> objToMap(Object obj) {
        Map<String, Object> map = new HashMap<String, Object>();

        for (Field field : getAllFields(obj.getClass())) {
            try {
                map.put(field.getName(), field.get(obj));
            } catch(Exception ignored) {}
        }
        return map;
    }

    public static String mapToStr(Map<String, ?> map)
    {
        StringBuilder   sb = new StringBuilder();
        boolean         isFirst = true;

        sb.append("{");

        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (isFirst)
                isFirst = false;
            else
                sb.append(",");
            String valueStr = "" + entry.getValue();
            sb.append(entry.getKey()).append("=").append(valueStr);
        }

        sb.append("}");
        return sb.toString();
    }

    public static String dumpToStr(Object obj) {
        if (obj == null)
            return "null";
        Map<String, Object> map = objToMap(obj);
        return mapToStr(map);
    }

	public static String dumpToStr(List list, String delimiter)
	{
        if (list == null)
            return "";
        
		StringBuilder    sb = new StringBuilder();

		for (Object obj : list)
		{
			if (sb.length() > 0)
				sb.append(delimiter);
			sb.append(obj);
		}
		return sb.toString();
	}

	public static Object run(Object obj, String methodName, Object[] params)
		throws Exception
	{
		Class[]	prototypeParams = null;

		if (params != null)
		{
			prototypeParams = new Class[params.length];
			for (int i = 0; i < params.length; i++)
			{
				prototypeParams[i] = params[i].getClass();
			}
		}

		Method	method = obj.getClass().getMethod(methodName, prototypeParams);
		if (method == null)
			return null;
		return method.invoke(obj, params);
	}

	public static Object run(Object obj, String methodName)
		throws Exception
	{
		return run(obj, methodName, null);
	}

	public static Object run(Object obj, String methodName, Object param1)
		throws Exception
	{
		return run(obj, methodName, new Object[]{param1});
	}

	public static Object run(Object obj, String methodName, Object param1, Object param2)
		throws Exception
	{
		return run(obj, methodName, new Object[]{param1, param2});
	}

	public static Object run(Object obj, String methodName, Object param1, Object param2, Object param3)
		throws Exception
	{
		return run(obj, methodName, new Object[]{param1, param2, param3});
	}

    public static Object getFieldValue(Object entity, String fieldName)
        throws Exception
    {
        Field   field = entity.getClass().getField(fieldName);
        if (field != null)
            return field.get(entity);
        return null;
    }

    /** Convert str value to Object */
    public static Object strToObj(String valueStr, Class valueType) {
        return ConvertUtils.convert(valueStr, valueType);
    }

    /** Initialize the fields of an object with the value object
     *  Use with MapUtil.toMap() for initializing an object.
     *  e.g. setFieldStrs(obj, MapUtil.toMap("field1", 123, "field2", 234, "myfield3", new Date() ));
     */
    public static <T> T initObj(T obj, Map<String, Object> fieldValues)
        throws Exception
    {
        if (obj != null && fieldValues != null) {
            for (Field field : getAllFields(obj.getClass())) {
                field.set(obj, fieldValues.get(field.getName()));
            }
        }
        return obj;
    }

    /** Initialize the fields of an object with the value object, converted from string.
     *  Use with MapUtil.toMap() for initializing an object.
     */
    public static <T> T initObjStrs(T obj, Map<String, String> fieldStrValues)
        throws Exception
    {
        if (obj != null && fieldStrValues != null) {
            for (Field field : getAllFields(obj.getClass())) {
                Object  value = ConvertUtils.convert(fieldStrValues.get(field.getName()), field.getType());
                field.set(obj, value);
            }
        }
        return obj;
    }
    
    public static boolean hasAnnotation(Class objClass, Class annClass) {
        return (objClass.getAnnotation(annClass) != null);
    }

    public static boolean hasAnnotation(Field field, Class annClass) {
        return (field.getAnnotation(annClass) != null);
    }

    public static boolean hasAnnotation(Method method, Class annClass) {
        return (method.getAnnotation(annClass) != null);
    }

    /** Get the value of a class annotation via its method.
     * e.g.   getAnnotationValue(clazz, EPlacemark.class, "latitude", String.class, (String)null);
     */
    public static <T> T getAnnotationValue(Class objClass, Class annClass, String valueMethod, Class<T> valueClass, T defaultValue)
        throws Exception
    {
        Object  annObj = objClass.getAnnotation(annClass);
        if (annObj != null)
            return (T) ReflectUtil.run(annObj, valueMethod);
        else
            return defaultValue;
    }

    /** Get the value of a class annotation via its method.  Use defaultValue for all exceptions.
     * e.g.   getAnnotationValueEx(clazz, EPlacemark.class, "latitude", Double.class, (Double)0.0);
     */
    public static <T> T getAnnotationValueEx(Class objClass, Class annClass, String valueMethod, Class<T> valueClass, T defaultValue) {
        try {
            Object  annObj = objClass.getAnnotation(annClass);
            if (annObj != null)
                return (T) ReflectUtil.run(annObj, valueMethod);
        } catch(Exception ignored) {
        }
        return defaultValue;
    }

    /** Get the String value of a class annotation via its method.  Return default if value is "" or null.
     * e.g.   getAnnotationValue(clazz, Table.class, "name", "abc");
     */
    public static String getAnnotationValue(Class objClass, Class annClass, String valueMethod, String defaultValue) {
        String  value = getAnnotationValueEx(objClass, annClass, valueMethod, String.class, defaultValue);
        return (value == null || value.length() == 0) ? defaultValue : value;
    }

    /** Get the value of a field annotation via its method.
     * e.g.   getAnnotationValue(field, Column.class, "name", String.class, (String)null);
     */
    public static <T> T getAnnotationValue(Field field, Class annClass, String valueMethod, Class<T> valueClass, T defaultValue)
        throws Exception
    {
        Object  annObj = field.getAnnotation(annClass);
        if (annObj != null)
            return (T) ReflectUtil.run(annObj, valueMethod);
        else
            return defaultValue;
    }

    /** Get the value of a field annotation via its method.  Use defaultValue for all exceptions.
     * e.g.   getAnnotationValue(field, Column.class, "name", String.class, (String)null);
     */
    public static <T> T getAnnotationValueEx(Field field, Class annClass, String valueMethod, Class<T> valueClass, T defaultValue) {
        try {
            Object  annObj = field.getAnnotation(annClass);
            if (annObj != null)
                return (T) ReflectUtil.run(annObj, valueMethod);
        } catch(Exception ignored) {
        }
        return defaultValue;
    }

    /** Get the String value of a field annotation via its method.  Return default if value is "" or null.
     * e.g.   getAnnotationValue(field, Column.class, "name", "abc");
     */
    public static String getAnnotationValue(Field field, Class annClass, String valueMethod, String defaultValue) {
        String  value = getAnnotationValueEx(field, annClass, valueMethod, String.class, defaultValue);
        return (value == null || value.length() == 0) ? defaultValue : value;
    }

    /** Find the field of the class having the annotation defined on it.
     */
    public static Field findAnnotatedField(Class clazz, Class fieldAnnClass) {
        for (Field field : clazz.getFields()) {
            Object  annObj = field.getAnnotation(fieldAnnClass);
            if (annObj != null)
                return field;
        }
        return null;
    }

}

