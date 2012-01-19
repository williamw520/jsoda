
package wwutil.db.annotation;

import java.lang.annotation.*;


/** Model field annotation.  List the valid values of a field.  Used for validation and UI generation.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AEnums {
    public String[] enums() default {};
}
