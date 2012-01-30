
package wwutil.model.annotation;

import java.lang.annotation.*;


/** Model field annotation.  Map a model field to an attribute name in the underlying DB table.
 * If a model field is not annotated with this, its field name is used as the attribute name.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AttrName{
    public String value();
}
