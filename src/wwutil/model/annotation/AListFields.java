
package wwutil.db.annotation;

import java.lang.annotation.*;


/** Model class annotation.  List the fields to be diplayed in the generated UI list view.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AListFields {
    public String[] fields() default {};
}
