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

import wwutil.model.AnnotationRegistry;
import wwutil.model.AnnotationClassHandler;
import wwutil.model.AnnotationFieldHandler;
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
import wwutil.model.annotation.DefaultGUID;
import wwutil.model.annotation.DefaultComposite;
import wwutil.model.annotation.VersionLocking;
import wwutil.model.annotation.ModifiedTime;
import wwutil.model.annotation.ToUpper;
import wwutil.model.annotation.ToLower;
import wwutil.model.annotation.Trim;
import wwutil.model.annotation.RemoveChar;


/**
 * Built-in data generation and validation functions.
 */
class BuiltinFunc
{

    static void setupBuiltinData1Handlers(final Jsoda jsoda) {

        jsoda.registerData1Handler( DefaultGUID.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) {
                if (field.getType() != String.class)
                    throw new ValidationException("The @DefaultGUID field must have the String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) {
                try {
                    Object      value = field.get(object);
                    if (value == null || value.toString().length() == 0) {
                        boolean isShort = (Boolean)ReflectUtil.run(fieldAnnotation, "isShort");
                        String  uuidStr = isShort ? BaseXUtil.uuid8() : BaseXUtil.uuid16();
                        field.set(object, uuidStr);
                    }
                } catch (Exception e) {
                    throw new ValidationException("@DefaultGUID failed", e);
                }
            }
        });

        jsoda.registerData1Handler( ModifiedTime.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) {
                if (field.getType() != java.util.Date.class)
                    throw new ValidationException("The @ModifiedTime field must have the java.util.Date type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) {
                try {
                    field.set(object, new Date());
                } catch (Exception e) {
                    throw new ValidationException("@ModifiedTime failed", e);
                }
            }
        });

        jsoda.registerData1Handler( VersionLocking.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) {
                if (field.getType() != Integer.class && field.getType() != int.class)
                    throw new ValidationException("The @VersionLocking field must have int type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) {
                try {
                    DataUtil.incrementField(object, field, 1);
                } catch (Exception e) {
                    throw new ValidationException("@VersionLocking failed", e);
                }
            }
        });

        jsoda.registerData1Handler( ToUpper.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) {
                if (field.getType() != String.class)
                    throw new ValidationException("The @ToUpper field must have String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) {
                try {
                    String  value = (String)field.get(object);
                    if (value != null) {
                        field.set(object, value.toUpperCase());
                    }
                } catch (Exception e) {
                    throw new ValidationException("@ToUpper failed", e);
                }
            }
        });

        jsoda.registerData1Handler( ToLower.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) {
                if (field.getType() != String.class)
                    throw new ValidationException("The @ToLower field must have String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) {
                try {
                    String  value = (String)field.get(object);
                    if (value != null) {
                        field.set(object, value.toLowerCase());
                    }
                } catch (Exception e) {
                    throw new ValidationException("@ToLower failed", e);
                }
            }
        });

        jsoda.registerData1Handler( Trim.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) {
                if (field.getType() != String.class)
                    throw new ValidationException("The @Trim field must have String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) {
                try {
                    String  value = (String)field.get(object);
                    if (value != null) {
                        field.set(object, value.trim());
                    }
                } catch (Exception e) {
                    throw new ValidationException("@Trim failed", e);
                }
            }
        });

        jsoda.registerData1Handler( RemoveChar.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) {
                if (field.getType() != String.class)
                    throw new ValidationException("The @RemoveChar field must have String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) {
                try {
                    char    charToRemove = (Character)ReflectUtil.run(fieldAnnotation, "charToRemove");
                    String  value = (String)field.get(object);

                    if (value != null) {
                        field.set(object, StringUtils.remove(value, charToRemove));
                    }
                } catch (Exception e) {
                    throw new ValidationException("@RemoveChar failed", e);
                }
            }
        });

    }


    static void setupBuiltinData2Handlers(final Jsoda jsoda) {

        jsoda.registerData1Handler( DefaultComposite.class, new AnnotationFieldHandler() {
            public void checkModel(Annotation fieldAnnotation, Field field) {
                if (field.getType() != String.class)
                    throw new ValidationException("The @DefaultComposite field must have String type.  Field: " + field);
            }

            public void handle(Annotation fieldAnnotation, Object object, Field field) {
                try {
                    String  modelName = jsoda.getModelName(object.getClass());
                    fillDefaultComposite(jsoda, modelName, field, object);
                } catch (Exception e) {
                    throw new ValidationException("@DefaultComposite failed", e);
                }
            }
        });

    }


    static void setupBuiltinValidationHandlers(final Jsoda jsoda) {

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

