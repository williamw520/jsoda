
package wwutil.model.annotation;

import java.lang.annotation.*;


/** Model field annotation.  Set whether the field is editable.  Used in generic generated UI.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AEditable {
    public boolean isEditable() default true;
}

