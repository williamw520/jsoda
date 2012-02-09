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
