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


/** Model field annotation.
 * Marks a field to be cached by its value when data object is loaded.
 * Multiple fields can be marked as CacheByField so that an object can be looked up from the cache by all these field values.
 * Id field should be marked if cache by id is desired.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheByField {
}
