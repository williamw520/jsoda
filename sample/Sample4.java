

import java.io.Serializable;
import java.util.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

import wwutil.model.AnnotationFieldHandler;
import wwutil.model.ValidationException;
import wwutil.model.annotation.DbType;
import wwutil.model.annotation.Model;
import wwutil.model.annotation.Key;
import wwutil.jsoda.Jsoda;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;



/**
 * Sample to illustrate adding custom annotation for data handlers and validations
 */
public class Sample4 {

    // Get AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY from environment variables.
    // You can hardcode them here for testing but should remove them afterward.
    private static final String key = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String secret = System.getenv("AWS_SECRET_ACCESS_KEY");


    @Retention(RetentionPolicy.RUNTIME)
    public static @interface MyDataConverter {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface MyValidator {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface MyDataJoiner {
        public String field1();
        public String field2();
        public String field3();
    }

    @Model
    public static class SampleCustom implements Serializable {
        @Key
        public int      id;

        @MyDataConverter            // apply a custom data converter
        public String   name;

        @MyValidator                // apply a custom validator
        public String   desc;

        @MyDataJoiner(field1="id", field2="name", field3="desc")    // apply a custom stage 2 data converter
        public String   upperText;


        public SampleCustom() {}

        public SampleCustom(int id, String name, String desc) {
            this.id = id;
            this.name = name;
            this.desc = desc;
        }

        public String toString() {
            return "   Product [" + id + ", " + name + ", " + desc + ", " + upperText + "]";
        }

    }


    public static void main(String[] args)
        throws Exception
    {
        Jsoda       jsoda = new Jsoda(new BasicAWSCredentials(key, secret));

        // Register a custom AnnotationFieldHandler that implements the functionality of MyDataConverter.
        jsoda.registerData1Handler(MyDataConverter.class, new AnnotationFieldHandler() {
            // checkModel() is called when a model class is registered to see if the annotated fields confirm to this annotation's requirement.
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The field must be String type.  Field: " + field.getName());
            }

            // handle() is called when a model object is stored
            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                String      value = (String)field.get(object);
                if (value != null) {
                    String  trimValue = (value.length() > 4 ? value.substring(0, 4) : value);
                    field.set(object, trimValue);
                }
            }
        });

        // Register a custom AnnotationFieldHandler that implements the functionality of MyValidator
        jsoda.registerValidationHandler(MyValidator.class, new AnnotationFieldHandler() {
            // checkModel() is called when a model class is registered to see if the annotated fields confirm to this annotation's requirement.
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The field must be String type.  Field: " + field.getName());
            }

            // handle() is called when a model object is stored
            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                String      value = (String)field.get(object);
                if (value != null) {
                    if (value.startsWith("foobar"))
                        throw new ValidationException("Field cannot start with foobar.  Field: " + field.getName());
                }
            }
        });

        // Register a custom AnnotationFieldHandler that implements the functionality of MyDataJoiner
        // Note it's registered as stage 2 handler, to run after the stage 1 handlers.
        jsoda.registerData2Handler(MyDataJoiner.class, new AnnotationFieldHandler() {
            // checkModel() is called when a model class is registered to see if the annotated fields confirm to this annotation's requirement.
            public void checkModel(Annotation fieldAnnotation, Field field, Map<String, Field> allFieldMap) throws ValidationException {
                if (field.getType() != String.class)
                    throw new ValidationException("The field must be String type.  Field: " + field.getName());
            }

            // handle() is called when a model object is stored
            public void handle(Annotation fieldAnnotation, Object object, Field field, Map<String, Field> allFieldMap) throws Exception {
                // Join the values from field1, field2, and field3, and convert it to upper case.
                MyDataJoiner    ann = (MyDataJoiner)fieldAnnotation;
                Field           field1 = allFieldMap.get(ann.field1());
                Field           field2 = allFieldMap.get(ann.field2());
                Field           field3 = allFieldMap.get(ann.field3());
                String          value1 = field1.get(object).toString();
                String          value2 = field2.get(object).toString();
                String          value3 = field3.get(object).toString();
                String          result = (value1 + ": " + value2 + "/" + value3).toUpperCase();
                field.set(object, result);
            }
        });


        jsoda.registerModel(SampleCustom.class, DbType.SimpleDB);

        // Sample object to show how the fields are transformed via the custom annotations
        try {
            SampleCustom    obj = new SampleCustom(101, "this-long-long-name", "custom object");
            jsoda.preStoreSteps(obj);
            System.out.println(obj);
        } catch(Exception e) {
            System.out.println(e);
        }

        // Sample object to show how the custom validation is triggered.
        try {
            SampleCustom    obj = new SampleCustom(101, "this-long-long-name", "foobar custom object");
            jsoda.preStoreSteps(obj);
            System.out.println(obj);
        } catch(Exception e) {
            System.out.println(e);
        }

    }

}

