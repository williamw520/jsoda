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


package wwutil.jsoda;

import java.util.*;
import wwutil.model.annotation.DbType;


interface DbService {
    public DbType getDbType();
    public String getDbTypeId();
    public void setDbEndpoint(String endpoint);

    public void createModelTable(String modelName);
    public void deleteTable(String tableName);
    public List<String> listTables();

    public void putObj(String modelName, Object dataObj, String expectedField, Object expectedValue, boolean expectedExists) throws Exception;
    public void putObjs(String modelName, List dataObjs) throws Exception;
    public Object getObj(String modelName, Object id, Object rangeKey) throws Exception;
    public void delete(String modelName, Object id, Object rangeKey) throws Exception;
    public void batchDelete(String modelName, List idList, List rangeKeyList) throws Exception;
    public <T> long queryCount(Class<T> modelClass, Query<T> query) throws JsodaException;
    public <T> List<T> queryRun(Class<T> modelClass, Query<T> query, boolean continueFromLastRun) throws JsodaException;
    public <T> boolean queryHasNext(Query<T> query);
    public String getFieldAttrName(String modelName, String fieldName);

    public void validateFilterOperator(String operator);
}
