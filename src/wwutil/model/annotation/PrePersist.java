
package wwutil.model.annotation;

import java.lang.annotation.*;


/** Make a method in a model class as PrePersist
 * 
 * The PrePersist method will be called when a model object is about to be stored in the database.
 * Here are the call order of the operations when an object is stored:
 * 1. PrePersist is called, giving you the chance to construct any fields
 * 2. Annotated default-value makers on fields are called.
 * 3. Annotated composite default-value makers on fields are called.
 * 4. PreValidation is called, giving you the 2nd chance to construct any fields based on previous steps and do custom validation.
 * 5. Annotated validation on fields are called.
 * 
 * When a model class inherits from a base model class, its PostLoad method will be called
 * instead of the base class' one.  Call the base class' PostLoad method in the derived class one.
 * 
 * Note that cached object coming from cache won't call the PostLoad method.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PrePersist {
}
