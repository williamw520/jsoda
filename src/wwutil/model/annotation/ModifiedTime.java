
package wwutil.model.annotation;

import java.lang.annotation.*;


/** Mark a java.util.Date field as ModifiedTime.  Automatically fill in the current time when put is called. */
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifiedTime {
}
