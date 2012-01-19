
package wwutil.model.annotation;

import java.lang.annotation.*;


/** Model field annotation.  Gives a description of the field.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AFieldDesc{
    public String desc();
}
