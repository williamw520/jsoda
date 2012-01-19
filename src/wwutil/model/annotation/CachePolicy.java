
package wwutil.model.annotation;

import java.lang.annotation.*;


/**
 * Model class annotation specifying the cache policy.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface CachePolicy {
    /** The cached object will expire in the number of seconds.  The default value (0), which means no expiration. */
    int expireInSeconds() default 0;
}
