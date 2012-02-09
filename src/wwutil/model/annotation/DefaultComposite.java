/******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.  If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for 
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is: Jsoda
 * The Initial Developer of the Original Code is: William Wong (williamw520@gmail.com)
 * Portions created by William Wong are Copyright (C) 2012 William Wong, All Rights Reserved.
 *
 ******************************************************************************/


package wwutil.model.annotation;


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

