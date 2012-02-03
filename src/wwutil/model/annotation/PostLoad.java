
package wwutil.model.annotation;

import java.lang.annotation.*;


/** Make a method in a model class as PostLoad
 *
 * The PostLoad method will be called after a model object is loaded from database,
 * giving the app a chance to do post loading data setup, e.g. filling in the transient fields.
 * 
 * When a model class inherits from a base model class, its PostLoad method will be called
 * instead of the base class' one.  Call the base class' PostLoad method in the derived class one.
 * 
 * Note that cached object coming from cache won't call the PostLoad method.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PostLoad {
}
