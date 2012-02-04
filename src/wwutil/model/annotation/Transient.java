
package wwutil.model.annotation;

import java.lang.annotation.*;


/** Mark a field in a model class as transient and not stored in the database.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Transient {
}
