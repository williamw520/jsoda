
package wwutil.db.annotation;

import java.lang.annotation.*;


/** Model field annotation.  Set the field with a composite of other fields.
 * <pre>
 * e.g.
 * &#064;DefaultComposite(fromFields = {"FirstName", "MiddleName", "LastName"})
 *    generates "first-middle-last"
 * &#064;DefaultComposite(fromFields = {"FirstName", "MiddleName", "LastName"}, separator = ".")
 *    generates "first.middle.last"
 * &#064;DefaultComposite(fromFields = {"FirstName", "MiddleName", "LastName"}, separator = ".", substrLen = {0,1,0})
 *    generates "first.m.last"
 *    takes one left of initial from MiddleName in this example
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultComposite {
    /** The fields to form the subparts of the key. */
    public String[] fromFields() default {};
    /** The max prefix length to keep for each field in the subpart.  0 or null=use all, no max */
    public int[] substrLen() default {};
    /** The separator joining the subparts, e.g. part1-part2-part3 */
    public String separator() default "-";
}

