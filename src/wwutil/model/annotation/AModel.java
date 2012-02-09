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


@Retention(RetentionPolicy.RUNTIME)
public @interface AModel{

    /** Type of the underlying database: SimpleDB, DynamoDB */
    public DbType dbtype() default DbType.None;

    /** Optional name of the underlying table.  If omitted, the class name will be used as table name. */
    public String table() default "";

    /** Optional prefix added to the undertable name, for organizing tables.  E.g. The prefix Acct_ applied to the model name User will produce table name of Acct_User. */
    public String prefix() default "";

    /** Optional read ProvisionedThroughput.  App is strongly encouraged to set its own ProvisionedThroughput */
    public long readThroughput() default 10;

    /** Optional write ProvisionedThroughput.  App is strongly encouraged to set its own ProvisionedThroughput */
    public long writeThroughput() default 5;

}
