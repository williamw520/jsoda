
package wwutil.model.annotation;

import java.lang.annotation.*;


/** Model class annotation.  List the fields to be diplayed in the generated UI form view.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AFormFields {
    public String[] fields() default {};
}
