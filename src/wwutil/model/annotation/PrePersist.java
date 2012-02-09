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


/** Mark a method in a model class as PrePersist
 * 
 * The PrePersist method will be called when a model object is about to be stored in the database.
 * Here are the call order of the operations when an object is stored:
 * 1. PrePersist is called, giving you the chance to construct any fields
 * 2. Annotated default-value makers on fields are called.
 * 3. Annotated composite default-value makers on fields are called.
 * 4. PreValidation is called, giving you the 2nd chance to construct any fields based on previous steps and do custom validation.
 * 5. Annotated validation on fields are called.
 * 
 * When a model class inherits from a base model class, its PostLoad method will be called
 * instead of the base class' one.  Call the base class' PostLoad method in the derived class one.
 * 
 * Note that cached object coming from cache won't call the PostLoad method.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface PrePersist {
}
