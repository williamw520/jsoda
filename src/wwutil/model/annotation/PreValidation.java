
package wwutil.model.annotation;

import java.lang.annotation.*;


/** Mark a method in a model class as PreValidation
 * 
 * The PreValidation method will be called when a model object is about to be stored in the database.
 * The app has a chance to perform data validation on the fields.  Throws ValidationException if the
 * app wants the data store to be stopped due to validation error.
 * 
 * Here are the call order of the operations when an object is stored:
 * 1. PrePersist is called, giving you the chance to construct any fields
 * 2. Annotated default-value makers on fields are called.
 * 3. Annotated composite default-value makers on fields are called.
 * 4. PreValidation is called, giving you the 2nd chance to construct any fields based on previous steps and do custom validation.
 * 5. Annotated validation on fields are called.
 * 
 * When a model class inherits from a base model class, its PreValidation method will be called
 * instead of the base class' one.  Call the base class' PreValidation method in the derived class one.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PreValidation {
}
