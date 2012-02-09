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


/** Mark an int field in a model class for optimistic locking using version.
 * Optimistic locking works as follow:
 * <pre>
 *  - When an object is put, its &#064;VersionLocking field is incremented by 1 automatically.
 *  - The put turns into a conditional putIf with the value of the &#064;VersionLocking field.
 *  - If the version number is not 1+ of the existing one, put is aborted since the object is outdated.
 *  - Caller should re-get the object and retry putting.
 * </pre>
 *
 * If caller calls putIf with other expected value, the version number is not checked
 * but it is still incremented by 1 automatically.
 *
 * Note: VersionLocking has no effect when calling Dao.batchPut().
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface VersionLocking {
}
