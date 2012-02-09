
package wwutil.model.annotation;

import java.lang.annotation.*;


/** Mark a field in a model class as the primary key of the class.
 * There are two types of primary key: single Id or composite key with hashKey ane rangeKey.
 * For Id primary key, a model class can only have one field declared as the Id field.
 * <pre>
 *  class Model1 {
 *      @Key(id=true) String field1;
 * }
 * </pre>
 * For composite key, a model class must have two fields declared as the key, one for hashKey and one for rangeKey.
 * <pre>
 *  class Model2 {
 *      @Key(hashKey=true) String field1;
 *      @Key(rangeKey=true) String field2;
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Key {
    public boolean id() default true;
    public boolean hashKey() default false;
    public boolean rangeKey() default false;
}
