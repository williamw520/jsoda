
package wwutil.jsoda;

import java.util.*;
import wwutil.model.annotation.DbType;
import wwutil.model.annotation.AModel;


interface DbService {
    public DbType getDbType();
    public String getDbTypeId();
    public void setDbEndpoint(String endpoint);

    public void createModelTable(String modelName);
    public void deleteTable(String tableName);
    public List<String> listTables();

    public void putObj(String modelName, Object dataObj, String expectedField, Object expectedValue, boolean expectedExists) throws Exception;
    public void putObjs(String modelName, List dataObjs) throws Exception;
    public Object getObj(String modelName, Object id) throws Exception;
    public Object getObj(String modelName, Object id, Object rangeKey) throws Exception;
    public Object getObj(String modelName, String field1, Object key1, Object... fieldKeys) throws Exception;
    public void delete(String modelName, Object id) throws Exception;
    public void delete(String modelName, Object id, Object rangeKey) throws Exception;
    public void batchDelete(String modelName, List idList) throws Exception;
    public void batchDelete(String modelName, List idList, List rangeKeyList) throws Exception;
    public <T> long queryCount(Class<T> modelClass, Query<T> query) throws JsodaException;
    public <T> List<T> queryRun(Class<T> modelClass, Query<T> query, boolean continueFromLastRun) throws JsodaException;
    public <T> boolean queryHasNext(Query<T> query);
    public String getFieldAttrName(String modelName, String fieldName);

    public void validateFilterOperator(String operator);
}
