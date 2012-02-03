
package wwutil.model.annotation;

import java.lang.annotation.*;


/** Model field annotation.  Mark a field as the range key in a composite primary key of a DynamoDB table.
 * A DynamoDB composite primary key consists of the hash key and the range key.  Use @Id for the hash key.
 * This has no effect on non-DynamoDB table.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RangeKey {
}
