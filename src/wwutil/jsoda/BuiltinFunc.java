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
import java.util.regex.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.concurrent.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.beanutils.ConvertUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSCredentials;

import wwutil.model.AnnotationRegistry;
import wwutil.model.AnnotationClassHandler;
import wwutil.model.AnnotationFieldHandler;
import wwutil.model.ValidationException;
import wwutil.model.MaskMatcher;
import wwutil.model.annotation.*;


/**
 * Built-in data generation and validation functions.
 */
class BuiltinFunc
{
    private static Pattern  sEmailPattern = Pattern.compile(EmailMatch.regex);
    

    ////////////////////////////////////////////////////////////////////////////
    // Stage 1 data handlers
    ////////////////////////////////////////////////////////////////////////////

    static void setupBuiltinData1Handlers(final Jsoda jsoda) {

        jsoda.registerData1Handler( DefaultGUID.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @DefaultGUID field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                Object      value = field.get(object);
                if (value == null || value.toString().length() == 0) {
                    boolean isShort = ReflectUtil.getAnnoValue(fieldAnnotation, "isShort", false);
                    String  uuidStr = isShort ? BaseXUtil.uuid8() : BaseXUtil.uuid16();
                    field.set(object, uuidStr);
                }
            }
        });

        jsoda.registerData1Handler( ModifiedTime.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != java.util.Date.class)
                    throw new ValidationException("The @ModifiedTime field must be java.util.Date type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                field.set(object, new Date());
            }
        });

        jsoda.registerData1Handler( VersionLocking.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class)
                    throw new ValidationException("The @VersionLocking field must be int type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                DataUtil.incrementField(object, field, 1);
            }
        });

        jsoda.registerData1Handler( ToUpper.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @ToUpper field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                String  value = (String)field.get(object);
                if (value != null) {
                    field.set(object, value.toUpperCase());
                }
            }
        });

        jsoda.registerData1Handler( ToLower.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @ToLower field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                String  value = (String)field.get(object);
                if (value != null) {
                    field.set(object, value.toLowerCase());
                }
            }
        });

        jsoda.registerData1Handler( Trim.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @Trim field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                String  value = (String)field.get(object);
                if (value != null) {
                    field.set(object, value.trim());
                }
            }
        });

        jsoda.registerData1Handler( RemoveChar.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @RemoveChar field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                char    charToRemove = ReflectUtil.getAnnoValue(fieldAnnotation, "charToRemove", ' ');
                String  value = (String)field.get(object);
                if (value != null) {
                    field.set(object, StringUtils.remove(value, charToRemove));
                }
            }
        });

        jsoda.registerData1Handler( RemoveAlphaDigits.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @RemoveAlphaDigits field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
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

        jsoda.registerData1Handler( MaxValue.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class)
                    throw new ValidationException("The @MaxValue field must be number type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                Object  maxValueObj = ReflectUtil.getAnnoValue(fieldAnnotation, "value", (Object)null);
                double  maxValue = ((Double)ConvertUtils.convert(maxValueObj, Double.class)).doubleValue();
                Object  valueObj = field.get(object);
                double  value = ((Double)ConvertUtils.convert(valueObj, Double.class)).doubleValue();
                value = (value > maxValue ? maxValue : value);
                field.set(object, ConvertUtils.convert(value, field.getType()));
            }
        });

        jsoda.registerData1Handler( MinValue.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class)
                    throw new ValidationException("The @MinValue field must be number type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                Object  minValueObj = ReflectUtil.getAnnoValue(fieldAnnotation, "value", (Object)null);
                double  minValue = ((Double)ConvertUtils.convert(minValueObj, Double.class)).doubleValue();
                Object  valueObj = field.get(object);
                double  value = ((Double)ConvertUtils.convert(valueObj, Double.class)).doubleValue();
                value = (value < minValue ? minValue : value);
                field.set(object, ConvertUtils.convert(value, field.getType()));
            }
        });

        jsoda.registerData1Handler( AbsValue.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class)
                    throw new ValidationException("The @AbsValue field must be number type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                Object  valueObj = field.get(object);
                double  value = ((Double)ConvertUtils.convert(valueObj, Double.class)).doubleValue();
                value = Math.abs(value);
                field.set(object, ConvertUtils.convert(value, field.getType()));
            }
        });

        jsoda.registerData1Handler( CeilValue.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class)
                    throw new ValidationException("The @CeilValue field must be number type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                Object  valueObj = field.get(object);
                double  value = ((Double)ConvertUtils.convert(valueObj, Double.class)).doubleValue();
                value = Math.ceil(value);
                field.set(object, ConvertUtils.convert(value, field.getType()));
            }
        });

        jsoda.registerData1Handler( FloorValue.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class)
                    throw new ValidationException("The @FloorValue field must be number type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
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

    static void setupBuiltinData2Handlers(final Jsoda jsoda) {

        jsoda.registerData1Handler( DefaultComposite.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @DefaultComposite field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                String  modelName = jsoda.getModelName(object.getClass());
                fillDefaultComposite(jsoda, modelName, field, object);
            }
        });

    }


    ////////////////////////////////////////////////////////////////////////////
    // Validation handlers
    ////////////////////////////////////////////////////////////////////////////

    static void setupBuiltinValidationHandlers(final Jsoda jsoda) {

        jsoda.registerValidationHandler( Required.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                if (field.get(object) == null)
                    throw new ValidationException("@Required field cannot be null.  Field: " + field);
            }
        });

        jsoda.registerValidationHandler( MaxSize.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class)
                    throw new ValidationException("The @MaxSize field must be number type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                Object  annValueObj = ReflectUtil.getAnnoValue(fieldAnnotation, "value", (Object)null);
                double  annValue = ((Double)ConvertUtils.convert(annValueObj, Double.class)).doubleValue();
                if (annValue != 0) {
                    Object  valueObj = field.get(object);
                    double  value = ((Double)ConvertUtils.convert(valueObj, Double.class)).doubleValue();
                    if (value > annValue)
                        throw new ValidationException("Field value " + valueObj + " exceeds MaxSize " + annValueObj + ".  Field: " + field);
                }
            }
        });
        
        jsoda.registerValidationHandler( MinSize.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != Integer.class && field.getType() != int.class &&
                    field.getType() != Long.class && field.getType() != long.class &&
                    field.getType() != Short.class && field.getType() != short.class &&
                    field.getType() != Float.class && field.getType() != float.class &&
                    field.getType() != Double.class && field.getType() != double.class)
                    throw new ValidationException("The @MinSize field must be number type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                Object  annValueObj = ReflectUtil.getAnnoValue(fieldAnnotation, "value", (Object)null);
                double  annValue = ((Double)ConvertUtils.convert(annValueObj, Double.class)).doubleValue();
                if (annValue != 0) {
                    Object  valueObj = field.get(object);
                    double  value = ((Double)ConvertUtils.convert(valueObj, Double.class)).doubleValue();
                    if (value < annValue)
                        throw new ValidationException("Field value " + valueObj + " is less than MinSize " + annValueObj + ".  Field: " + field);
                }
            }
        });

        jsoda.registerValidationHandler( StartsWith.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @StartsWith field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                String  annValue = ReflectUtil.getAnnoValue(fieldAnnotation, "value", "");
                String  value = (String)field.get(object);
                if (value != null && !value.startsWith(annValue))
                    throw new ValidationException("Field value " + value + " does not start with " + annValue + ".  Field: " + field);
            }
        });

        jsoda.registerValidationHandler( EndsWith.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @EndsWith field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                String  annValue = ReflectUtil.getAnnoValue(fieldAnnotation, "value", "");
                String  value = (String)field.get(object);
                if (value != null && !value.endsWith(annValue))
                    throw new ValidationException("Field value " + value + " does not end with " + annValue + ".  Field: " + field);
            }
        });

        jsoda.registerValidationHandler( Contains.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @Contains field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                String  annValue = ReflectUtil.getAnnoValue(fieldAnnotation, "value", "");
                String  value = (String)field.get(object);
                if (value != null && !value.contains(annValue))
                    throw new ValidationException("Field value " + value + " does not contain " + annValue + ".  Field: " + field);
            }
        });

        jsoda.registerValidationHandler( NotContains.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @NotContains field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                String  annValue = ReflectUtil.getAnnoValue(fieldAnnotation, "value", "");
                String  value = (String)field.get(object);
                if (value != null && value.contains(annValue))
                    throw new ValidationException("Field value " + value + " contains " + annValue + ".  Field: " + field);
            }
        });

        jsoda.registerValidationHandler( RegexMatch.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @RegexMatch field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                String  annValue = ReflectUtil.getAnnoValue(fieldAnnotation, "value", "");
                String  value = (String)field.get(object);
                if (value != null && !Pattern.matches(annValue, value))
                    throw new ValidationException("Field value " + value + " does not match the regex " + annValue + ".  Field: " + field);
            }
        });

        jsoda.registerValidationHandler( EmailMatch.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @EmailMatch field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                String  value = (String)field.get(object);
                if (value != null) {
                    if (!sEmailPattern.matcher(value.toUpperCase()).matches())
                        throw new ValidationException("Field value " + value + " is not an email.  Field: " + field);
                }
            }
        });



        jsoda.registerValidationHandler( MaskMatch.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @MaskMatch field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                char    digitMask = ReflectUtil.getAnnoValue(fieldAnnotation, "digitMask", '#');
                char    letterMask = ReflectUtil.getAnnoValue(fieldAnnotation, "letterMask", '@');
                char    anyMask = ReflectUtil.getAnnoValue(fieldAnnotation, "anyMask", '*');
                String  pattern = ReflectUtil.getAnnoValue(fieldAnnotation, "pattern", "");
                String  value = (String)field.get(object);
                if (value != null) {
                    MaskMatcher matcher = new MaskMatcher(pattern, digitMask, letterMask, anyMask);
                    if (!matcher.matches(value))
                        throw new ValidationException("Field value " + value + " does not match the mask pattern " + pattern + ".  Field: " + field);
                }
            }
        });

        jsoda.registerValidationHandler( OneOf.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The @OneOf field must be String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) throws Exception {
                String[]    annValue = (String[])ReflectUtil.getAnnoValue(fieldAnnotation, "choices", new String[0]);
                String      value = (String)field.get(object);
                if (value != null) {
                    for (String choice : annValue) {
                        if (value.equals(choice))
                            return;
                    }
                    throw new ValidationException("Field value " + value + " is not one of the choices.  Field: " + field);
                }
            }
        });

    }


    private static void fillDefaultComposite(Jsoda jsoda, String modelName, Field field, Object dataObj)
        throws Exception
    {
        String[]        fromFields = ReflectUtil.getAnnotationValue(field, DefaultComposite.class, "fromFields", String[].class, new String[0]);
        int[]           substrLen = ReflectUtil.getAnnotationValue(field, DefaultComposite.class, "substrLen", int[].class, new int[0]);
        String          separator = ReflectUtil.getAnnotationValue(field, DefaultComposite.class, "separator", "-");
        StringBuilder   sb = new StringBuilder();

        for (int i = 0; i < fromFields.length; i++) {
            Field       subpartField = jsoda.getField(modelName, fromFields[i]);
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


}

