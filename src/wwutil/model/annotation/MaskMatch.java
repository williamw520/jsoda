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


/** Validate a String field matches the mask expression.
 * digitMask is the mask character for matching any digit.
 * letterMask is the mask character for matching any alphabet.
 * anyMask is the mask character for matching any character.
 * pattern is a the string expression contains the literal characters and the mask characters.
 *
 *<pre> 
 * e.g. @MaskMatch( pattern = "(###) ###-####" )
 * will match "(415) 555-1212" or "(408) 121-5555"
 *
 * e.g. @MaskMatch( pattern = "800-@@@-####" )
 * will match "800-ABC-1212" or "800-EFG-4567"
 *
 * e.g. @MaskMatch( pattern = "800-***-****" )
 * will match "800-A12-3[C?D"
 *
 * e.g. @MaskMatch( digitMask = '$', pattern = "## $$/$$/$$$$ ##" )
 * redefine the digitMask to $ and will match "## 07/20/1999 ##"
 *</pre> 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MaskMatch {
    public char digitMask()  default '#';
    public char letterMask() default '@';
    public char anyMask()    default '*';
    public String pattern();
}

