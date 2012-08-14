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


package wwutil.model;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.text.MessageFormat;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.beanutils.ConvertUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSCredentials;

import wwutil.sys.BaseXUtil;
import wwutil.sys.ReflectUtil;
import wwutil.model.AnnotationRegistry;
import wwutil.model.AnnotationClassHandler;
import wwutil.model.AnnotationFieldHandler;
import wwutil.model.ValidationException;
import wwutil.model.MaskMatcher;
import wwutil.model.annotation.*;


/**
 * Built-in data generation and validation functions.
 */
public class BuiltinFunc
{
    private static Pattern  sEmailPattern = Pattern.compile(EmailMatch.regex);

    private static AnnotationRegistry   sPreStore1Registry = new AnnotationRegistry();
    private static AnnotationRegistry   sPreStore2Registry = new AnnotationRegistry();
    private static AnnotationRegistry   sValidationRegistry = new AnnotationRegistry();
    private static AnnotationRegistry   sPostLoadRegistry = new AnnotationRegistry();


    static {
        try {
            setupBuiltinPreStore1Handlers(sPreStore1Registry);
            setupBuiltinPreStore2Handlers(sPreStore2Registry);
            setupBuiltinValidationHandlers(sValidationRegistry);
            setupBuiltinPostLoadHandlers(sPostLoadRegistry);
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }

    public static AnnotationRegistry clonePreStore1Registry() {
        return sPreStore1Registry.cloneRegistry();
    }

    public static AnnotationRegistry clonePreStore2Registry() {
        return sPreStore2Registry.cloneRegistry();
    }

    public static AnnotationRegistry cloneValidationRegistry() {
        return sValidationRegistry.cloneRegistry();
    }

    public static AnnotationRegistry clonePostLoadRegistry() {
        return sPostLoadRegistry.cloneRegistry();
    }
    

    ////////////////////////////////////////////////////////////////////////////
    // Stage 1 data handlers
    ////////////////////////////////////////////////////////////////////////////

    private static void setupBuiltinPreStore1Handlers(AnnotationRegistry registry) {

        registry.register( DefaultGUID.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @DefaultGUID field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                Object      value = field.get(object);
                if (value == null || value.toString().length() == 0) {
                    boolean isShort = ReflectUtil.getAnnoValue(fieldAnnotation, "isShort", false);
                    String  uuidStr = isShort ? BaseXUtil.uuid8() : BaseXUtil.uuid16();
                    field.set(object, uuidStr);
                }
            }
        });

        registry.register( ModifiedTime.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != java.util.Date.class)
                    throw new ValidationException("The @ModifiedTime field must be java.util.Date type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                field.set(object, new Date());
            }
        });

        registry.register( VersionLocking.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class)
                    throw new ValidationException("The @VersionLocking field must be int type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                ReflectUtil.incrementField(object, field, 1);
            }
        });

        registry.register( ToUpper.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @ToUpper field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                String  value = (String)field.get(object);
                if (value != null) {
                    field.set(object, value.toUpperCase());
                }
            }
        });

        registry.register( ToLower.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @ToLower field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                String  value = (String)field.get(object);
                if (value != null) {
                    field.set(object, value.toLowerCase());
                }
            }
        });

        registry.register( Trim.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @Trim field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                String  value = (String)field.get(object);
                if (value != null) {
                    field.set(object, value.trim());
                }
            }
        });

        registry.register( RemoveChar.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @RemoveChar field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                char    charToRemove = ReflectUtil.getAnnoValue(fieldAnnotation, "charToRemove", ' ');
                String  value = (String)field.get(object);
                if (value != null) {
                    field.set(object, StringUtils.remove(value, charToRemove));
                }
            }
        });

        registry.register( RemoveAlphaDigits.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @RemoveAlphaDigits field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                boolean removeDigits = ReflectUtil.getAnnoValue(fieldAnnotation, "removeDigits", false);
                String  value = (String)field.get(object);
                if (value != null) {
                    if (removeDigits)
                        field.set(object, value.replaceAll("[\\d]", ""));   // remove all digits
                    else
                        field.set(object, value.replaceAll("[^\\d]", ""));  // remove all alphas (non-digits)
                }
            }
        });

        registry.register( MaxValue.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class)
                    throw new ValidationException("The @MaxValue field must be number type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                Object  maxValueObj = ReflectUtil.getAnnoValue(fieldAnnotation, "value", (Object)null);
                double  maxValue = ((Double)ConvertUtils.convert(maxValueObj, Double.class)).doubleValue();
                Object  valueObj = field.get(object);
                double  value = ((Double)ConvertUtils.convert(valueObj, Double.class)).doubleValue();
                value = (value > maxValue ? maxValue : value);
                field.set(object, ConvertUtils.convert(value, field.getType()));
            }
        });

        registry.register( MinValue.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class)
                    throw new ValidationException("The @MinValue field must be number type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                Object  minValueObj = ReflectUtil.getAnnoValue(fieldAnnotation, "value", (Object)null);
                double  minValue = ((Double)ConvertUtils.convert(minValueObj, Double.class)).doubleValue();
                Object  valueObj = field.get(object);
                double  value = ((Double)ConvertUtils.convert(valueObj, Double.class)).doubleValue();
                value = (value < minValue ? minValue : value);
                field.set(object, ConvertUtils.convert(value, field.getType()));
            }
        });

        registry.register( AbsValue.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class)
                    throw new ValidationException("The @AbsValue field must be number type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                Object  valueObj = field.get(object);
                double  value = ((Double)ConvertUtils.convert(valueObj, Double.class)).doubleValue();
                value = Math.abs(value);
                field.set(object, ConvertUtils.convert(value, field.getType()));
            }
        });

        registry.register( CeilValue.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class)
                    throw new ValidationException("The @CeilValue field must be number type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                Object  valueObj = field.get(object);
                double  value = ((Double)ConvertUtils.convert(valueObj, Double.class)).doubleValue();
                value = Math.ceil(value);
                field.set(object, ConvertUtils.convert(value, field.getType()));
            }
        });

        registry.register( FloorValue.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class)
                    throw new ValidationException("The @FloorValue field must be number type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                Object  valueObj = field.get(object);
                double  value = ((Double)ConvertUtils.convert(valueObj, Double.class)).doubleValue();
                value = Math.floor(value);
                field.set(object, ConvertUtils.convert(value, field.getType()));
            }
        });

    }


    ////////////////////////////////////////////////////////////////////////////
    // Stage 2 data handlers
    ////////////////////////////////////////////////////////////////////////////

    private static void setupBuiltinPreStore2Handlers(AnnotationRegistry registry) {

        registry.register( DefaultComposite.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @DefaultComposite field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                fillDefaultComposite(field, object, allFieldMap);
            }
        });

        registry.register( FormatMsg.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @FormatMsg field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                FormatMsg       formatMsg = (FormatMsg)fieldAnnotation;
                if (formatMsg.onSave()) {
                    fillFormatMsg(formatMsg, field, object, allFieldMap);
                }
            }
        });

    }


    ////////////////////////////////////////////////////////////////////////////
    // Validation handlers
    ////////////////////////////////////////////////////////////////////////////

    private static void setupBuiltinValidationHandlers(AnnotationRegistry registry) {

        registry.register( Required.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                if (field.get(object) == null)
                    throw new ValidationException("@Required field cannot be null.  Field: " + field.getName());
            }
        });

        registry.register( MaxSize.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class &&
                    field.getType() != String.class)
                    throw new ValidationException("The @MaxSize field must be number type or String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                Object  annValueObj = ReflectUtil.getAnnoValue(fieldAnnotation, "value", (Object)null);
                double  annValue = ((Double)ConvertUtils.convert(annValueObj, Double.class)).doubleValue();
                if (annValue != 0) {
                    Object  valueObj = field.get(object);
                    double  value;
                    if (valueObj instanceof String)
                        value = ((String)valueObj).length();
                    else
                        value = ((Double)ConvertUtils.convert(valueObj, Double.class)).doubleValue();
                    if (value > annValue)
                        throw new ValidationException("Field value " + valueObj + " exceeds MaxSize " + annValueObj + ".  Field: " + field.getName());
                }
            }
        });
        
        registry.register( MinSize.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class &&
                    field.getType() != String.class)
                    throw new ValidationException("The @MinSize field must be number type or String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                Object  annValueObj = ReflectUtil.getAnnoValue(fieldAnnotation, "value", (Object)null);
                double  annValue = ((Double)ConvertUtils.convert(annValueObj, Double.class)).doubleValue();
                if (annValue != 0) {
                    Object  valueObj = field.get(object);
                    double  value;
                    if (valueObj instanceof String)
                        value = ((String)valueObj).length();
                    else
                        value = ((Double)ConvertUtils.convert(valueObj, Double.class)).doubleValue();
                    if (value < annValue)
                        throw new ValidationException("Field value " + valueObj + " is less than MinSize " + annValueObj + ".  Field: " + field.getName());
                }
            }
        });

        registry.register( StartsWith.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @StartsWith field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                String  annValue = ReflectUtil.getAnnoValue(fieldAnnotation, "value", "");
                String  value = (String)field.get(object);
                if (value != null && !value.startsWith(annValue))
                    throw new ValidationException("Field value " + value + " does not start with " + annValue + ".  Field: " + field.getName());
            }
        });

        registry.register( EndsWith.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @EndsWith field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                String  annValue = ReflectUtil.getAnnoValue(fieldAnnotation, "value", "");
                String  value = (String)field.get(object);
                if (value != null && !value.endsWith(annValue))
                    throw new ValidationException("Field value " + value + " does not end with " + annValue + ".  Field: " + field.getName());
            }
        });

        registry.register( Contains.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @Contains field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                String  annValue = ReflectUtil.getAnnoValue(fieldAnnotation, "value", "");
                String  value = (String)field.get(object);
                if (value != null && !value.contains(annValue))
                    throw new ValidationException("Field value " + value + " does not contain " + annValue + ".  Field: " + field.getName());
            }
        });

        registry.register( NotContains.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @NotContains field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                String  annValue = ReflectUtil.getAnnoValue(fieldAnnotation, "value", "");
                String  value = (String)field.get(object);
                if (value != null && value.contains(annValue))
                    throw new ValidationException("Field value " + value + " contains " + annValue + ".  Field: " + field.getName());
            }
        });

        registry.register( RegexMatch.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @RegexMatch field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                String  annValue = ReflectUtil.getAnnoValue(fieldAnnotation, "value", "");
                String  value = (String)field.get(object);
                if (value != null && !Pattern.matches(annValue, value))
                    throw new ValidationException("Field value " + value + " does not match the regex " + annValue + ".  Field: " + field.getName());
            }
        });

        registry.register( EmailMatch.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @EmailMatch field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                String  value = (String)field.get(object);
                if (value != null) {
                    if (!sEmailPattern.matcher(value.toUpperCase()).matches())
                        throw new ValidationException("Field value " + value + " is not an email.  Field: " + field.getName());
                }
            }
        });



        registry.register( MaskMatch.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @MaskMatch field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                char    digitMask = ReflectUtil.getAnnoValue(fieldAnnotation, "digitMask", '#');
                char    letterMask = ReflectUtil.getAnnoValue(fieldAnnotation, "letterMask", '@');
                char    anyMask = ReflectUtil.getAnnoValue(fieldAnnotation, "anyMask", '*');
                String  pattern = ReflectUtil.getAnnoValue(fieldAnnotation, "pattern", "");
                String  value = (String)field.get(object);
                if (value != null) {
                    MaskMatcher matcher = new MaskMatcher(pattern, digitMask, letterMask, anyMask);
                    if (!matcher.matches(value))
                        throw new ValidationException("Field value " + value + " does not match the mask pattern " + pattern + ".  Field: " + field.getName());
                }
            }
        });

        registry.register( OneOf.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @OneOf field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                String[]    annValue = (String[])ReflectUtil.getAnnoValue(fieldAnnotation, "choices", new String[0]);
                String      value = (String)field.get(object);
                if (value != null) {
                    for (String choice : annValue) {
                        if (value.equals(choice))
                            return;
                    }
                    throw new ValidationException("Field value " + value + " is not one of the choices.  Field: " + field.getName());
                }
            }
        });

    }


    ////////////////////////////////////////////////////////////////////////////
    // Stage 2 data handlers
    ////////////////////////////////////////////////////////////////////////////

    private static void setupBuiltinPostLoadHandlers(AnnotationRegistry registry) {

        registry.register( FormatMsg.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @FormatMsg field must be String type.  Field: " + field.getName());
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                FormatMsg       formatMsg = (FormatMsg)fieldAnnotation;
                if (formatMsg.onLoad()) {
                    fillFormatMsg(formatMsg, field, object, allFieldMap);
                }
            }
        });

    }

    

    private static void fillDefaultComposite(Field field, Object dataObj, Map<String, Field> allFieldMap)
        throws Exception
    {
        String[]        fromFields = ReflectUtil.getAnnotationValue(field, DefaultComposite.class, "fromFields", String[].class, new String[0]);
        int[]           substrLen = ReflectUtil.getAnnotationValue(field, DefaultComposite.class, "substrLen", int[].class, new int[0]);
        String          separator = ReflectUtil.getAnnotationValue(field, DefaultComposite.class, "separator", "-");
        StringBuilder   sb = new StringBuilder();

        for (int i = 0; i < fromFields.length; i++) {
            Field       subpartField = allFieldMap.get(fromFields[i]);
            if (subpartField == null)
                throw new IllegalArgumentException(fromFields[i] + " specified in the fromFields parameter of the @DefaultComposite field " +
                                                   field.getName() + " doesn't exist.");
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

    private static void fillFormatMsg(FormatMsg formatMsg, Field field, Object dataObj, Map<String, Field> allFieldMap)
        throws Exception
    {
        Object[]    paramObjs = new Object[formatMsg.paramFields().length];
        for (int i = 0; i < formatMsg.paramFields().length; i++) {
            Field   paramField = allFieldMap.get(formatMsg.paramFields()[i]);
            if (paramField == null)
                throw new IllegalArgumentException(formatMsg.paramFields()[i] + " specified in the paramFields parameter of the @FormatMsg field " +
                                                   field.getName() + " doesn't exist.");
            paramObjs[i] = paramField.get(dataObj);
        }
        String      msg = MessageFormat.format(formatMsg.format(), paramObjs);
        field.set(dataObj, msg);
    }

}

