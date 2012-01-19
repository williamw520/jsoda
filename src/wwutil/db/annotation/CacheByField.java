
package wwutil.db.annotation;

import java.lang.annotation.*;


/** Model field annotation.
 * Marks a field to be cached by its value when data object is loaded.
 * Multiple fields can be marked as CacheByField so that an object can be looked up from the cache by all these field values.
 * Id field should be marked if cache by id is desired.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheByField {
}
