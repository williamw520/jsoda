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


/** Model field annotation for post load data handler.  Format the String field with other fields as parameters.
 * Use java.text.MessageFormat for formatting syntax.
 * <pre>
 * e.g.
 * &#064;FormatMsg(format = "The user name is {0} {2} with middle name {1}.", paramFields = {"FirstName", "MiddleName", "LastName"})
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface FormatMsg {
    
    /** The format string.  See java.text.MessageFormat for format syntax */
    public String format();
    
    /** The fields to form the subparts of the key. */
    public String[] paramFields() default {};

    /** Run this handler on load */
    public boolean onLoad() default true;

    /** Run this handler on save */
    public boolean onSave() default false;

}

