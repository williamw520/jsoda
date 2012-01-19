
package wwutil.model.annotation;


import java.lang.annotation.*;


/** Model field annotation.  Set the field with a generated GUID if it's not set.  Used for Id generation.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultGUID {
    /** Use the short form of ID.  Default is the long form. */
    public boolean isShort() default false;
}

