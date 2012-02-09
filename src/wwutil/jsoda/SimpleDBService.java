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

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.DeleteDomainRequest;
import com.amazonaws.services.simpledb.model.ListDomainsResult;
import com.amazonaws.services.simpledb.model.PutAttributesRequest;
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableItem;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.GetAttributesRequest;
import com.amazonaws.services.simpledb.model.GetAttributesResult;
import com.amazonaws.services.simpledb.model.Attribute;
import com.amazonaws.services.simpledb.model.SelectRequest;
import com.amazonaws.services.simpledb.model.SelectResult;
import com.amazonaws.services.simpledb.model.Item;
import com.amazonaws.services.simpledb.model.DeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.BatchDeleteAttributesRequest;
import com.amazonaws.services.simpledb.model.DeletableItem;
import com.amazonaws.services.simpledb.model.UpdateCondition;
import com.amazonaws.services.simpledb.util.SimpleDBUtils;

import wwutil.sys.TlsMap;
import wwutil.model.MemCacheable;
import wwutil.model.annotation.DbType;
import wwutil.model.annotation.Model;
import wwutil.model.annotation.CachePolicy;
import wwutil.model.annotation.DefaultGUID;
import wwutil.model.annotation.DefaultComposite;
import wwutil.model.annotation.CacheByField;


/**
 */
class SimpleDBService implements DbService
{
    private static Log  log = LogFactory.getLog(SimpleDBService.class);

    static final Set<String>    sOperatorMap = new HashSet<String>(){{
            add(Filter.NULL);
            add(Filter.NOT_NULL);
            add(Filter.EQ);
            add(Filter.NE);
            add(Filter.LE);
            add(Filter.LT);
            add(Filter.GE);
            add(Filter.GT);
            add(Filter.LIKE);
            add(Filter.NOT_LIKE);
            add(Filter.BETWEEN);
            add(Filter.IN);
        }};

    public static final String      ITEM_NAME = "itemName()";

    private Jsoda                   jsoda;
    private AmazonSimpleDBClient    sdbClient;


    // AWS Access Key ID and Secret Access Key
    public SimpleDBService(Jsoda jsoda, AWSCredentials cred)
        throws Exception
    {
        this.jsoda = jsoda;
        this.sdbClient = new AmazonSimpleDBClient(cred);
    }

    public void shutdown() {
        sdbClient.shutdown();
    }

    public DbType getDbType() {
        return DbType.SimpleDB;
    }
    
    public String getDbTypeId() {
        return "SDB";
    }

    public void setDbEndpoint(String endpoint) {
        sdbClient.setEndpoint(endpoint);
    }


    // Delegated SimpleDB API

    public void createModelTable(String modelName) {
        sdbClient.createDomain(new CreateDomainRequest(jsoda.getModelTable(modelName)));
    }

    public void deleteTable(String tableName) {
        sdbClient.deleteDomain(new DeleteDomainRequest(tableName));
    }

    public List<String> listTables() {
        ListDomainsResult   list = sdbClient.listDomains();
        return list.getDomainNames();
    }

    private String makeCompositePk(String modelName, Object id, Object rangeKey)
        throws Exception
    {
        String  idStr = DataUtil.encodeValueToAttrStr(id, jsoda.getIdField(modelName).getType());
        String  rangeStr = DataUtil.encodeValueToAttrStr(rangeKey, jsoda.getRangeField(modelName).getType());
        String  pk = idStr.length() + ":" + idStr + "/" + rangeStr;
        return pk;
    }

    private String[] parseCompositePk(String modelName, String compositePk) {
        int     index = compositePk.indexOf(":");
        String  lenStr = compositePk.substring(0, index);
        int     len = Integer.parseInt(lenStr);
        String  idStr = compositePk.substring(index + 1, index + 1 + len);
        String  rangeStr = compositePk.substring(index + 1 + len + 1);
        return new String[] {idStr, rangeStr};
    }

    private String makeIdValue(String modelName, Object id, Object rangeKey)
        throws Exception
    {
        String  idStr = DataUtil.encodeValueToAttrStr(id, jsoda.getIdField(modelName).getType());
        Field   rangeField = jsoda.getRangeField(modelName);
        String  pk = rangeField == null ? idStr : makeCompositePk(modelName, id, rangeKey);
        return pk;
    }

    private String makeIdValue(String modelName, Object dataObj)
        throws Exception
    {
        Field   idField = jsoda.getIdField(modelName);
        Field   rangeField = jsoda.getRangeField(modelName);
        Object  id = idField.get(dataObj);
        Object  rangeKey = rangeField == null ? null : rangeField.get(dataObj);
        return makeIdValue(modelName, id, rangeKey);
    }

    public void putObj(String modelName, Object dataObj, String expectedField, Object expectedValue, boolean expectedExists)
        throws Exception
    {
        String  table = jsoda.getModelTable(modelName);
        String  idValue = makeIdValue(modelName, dataObj);
        PutAttributesRequest    req =
            expectedField == null ?
                new PutAttributesRequest(table, idValue, buildAttrs(dataObj, modelName)) :
                new PutAttributesRequest(table, idValue, buildAttrs(dataObj, modelName),
                                         buildExpectedValue(modelName, expectedField, expectedValue, expectedExists));
        sdbClient.putAttributes(req);
    }

    public void putObjs(String modelName, List dataObjs)
        throws Exception
    {
        String  table = jsoda.getModelTable(modelName);
        sdbClient.batchPutAttributes(new BatchPutAttributesRequest(table, buildPutItems(dataObjs, modelName)));
    }

    public Object getObj(String modelName, Object id, Object rangeKey)
        throws Exception
    {
        if (id == null)
            throw new IllegalArgumentException("Id cannot be null.");

        String              table = jsoda.getModelTable(modelName);
        String              idValue = makeIdValue(modelName, id, rangeKey);
        GetAttributesResult result = sdbClient.getAttributes(new GetAttributesRequest(table, idValue));
        if (result.getAttributes().size() == 0)
            return null;        // not existed.
        return buildLoadObj(modelName, idValue, result.getAttributes(), null);
        
    }

    public void delete(String modelName, Object id, Object rangeKey)
        throws Exception
    {
        if (id == null)
            throw new IllegalArgumentException("Id cannot be null.");

        String  table = jsoda.getModelTable(modelName);
        String  idValue = makeIdValue(modelName, id, rangeKey);
        sdbClient.deleteAttributes(new DeleteAttributesRequest(table, idValue));
    }

    public void batchDelete(String modelName, List idList, List rangeKeyList)
        throws Exception
    {
        String  table = jsoda.getModelTable(modelName);
        List<DeletableItem> items = new ArrayList<DeletableItem>();
        for (int i = 0; i < idList.size(); i++) {
            String  idValue = makeIdValue(modelName, idList.get(i), rangeKeyList == null ? null : rangeKeyList.get(i));
            items.add(new DeletableItem().withName(idValue));
        }
        sdbClient.batchDeleteAttributes(new BatchDeleteAttributesRequest(table, items));
    }

    public void validateFilterOperator(String operator) {
        if (!sOperatorMap.contains(operator))
            throw new UnsupportedOperationException("Unsupported operator: " + operator);
    }

    // /** Get by a field beside the id */
    // public <T> T findBy(Class<T> modelClass, String field, Object fieldValue)
    //     throws Exception
    // {
    //     String  modelName = getModelName(modelClass);
    //     T       obj = (T)cacheGet(modelName, field, fieldValue);
    //     if (obj != null)
    //         return obj;

    //     List<T> items = query(modelClass).filter(field, "=", fieldValue).run();
    //     // runQuery() has already cached the object.  No need to cache it here.
    //     return items.size() == 0 ? null : items.get(0);
    // }


    @SuppressWarnings("unchecked")
    public <T> long queryCount(Class<T> modelClass, Query<T> query)
        throws JsodaException
    {
        String          modelName = jsoda.getModelName(modelClass);
        String          queryStr = toQueryStr(query, true);
        SelectRequest   request = new SelectRequest(queryStr, query.consistentRead);

        try {
            for (Item item : sdbClient.select(request).getItems()) {
                for (Attribute attr : item.getAttributes()) {
                    String  attrName  = attr.getName();
                    String  fieldValue = attr.getValue();
                    long    count = Long.parseLong(fieldValue);
                    return count;
                }
            }
        } catch(Exception e) {
            throw new JsodaException("Query failed.  Query: " + request.getSelectExpression() + "  Error: " + e.getMessage(), e);
        }
        throw new JsodaException("Query failed.  Not result for count query.");
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> queryRun(Class<T> modelClass, Query<T> query, boolean continueFromLastRun)
        throws JsodaException
    {
        List<T>         resultObjs = new ArrayList<T>();

        if (continueFromLastRun && !queryHasNext(query))
            return resultObjs;

        String          queryStr = toQueryStr(query, false);
        log.info("Query: " + queryStr);
        SelectRequest   request = new SelectRequest(queryStr, query.consistentRead);

        if (continueFromLastRun)
            request.setNextToken((String)query.nextKey);

        try {
            SelectResult    result = sdbClient.select(request);
            query.nextKey = request.getNextToken();
            for (Item item : result.getItems()) {
                T   obj = (T)buildLoadObj(query.modelName, item.getName(), item.getAttributes(), query);
                resultObjs.add(obj);
            }
            return resultObjs;
        } catch(Exception e) {
            throw new JsodaException("Query failed.  Query: " + request.getSelectExpression() + "  Error: " + e.getMessage(), e);
        }
    }

    public <T> boolean queryHasNext(Query<T> query) {
        return query.nextKey != null;
    }


    public String getFieldAttrName(String modelName, String fieldName) {
        // SimpleDB's attribute name for single Id always maps to "itemName()"
        if (jsoda.getRangeField(modelName) == null && jsoda.isIdField(modelName, fieldName))
            return ITEM_NAME;

        String  attrName = jsoda.getFieldAttrMap(modelName).get(fieldName);
        return attrName != null ? SimpleDBUtils.quoteName(attrName) : null;
    }


    private List<ReplaceableAttribute> buildAttrs(Object dataObj, String modelName)
        throws Exception
    {
        List<ReplaceableAttribute>  attrs = new ArrayList<ReplaceableAttribute>();
        for (Map.Entry<String, String> fieldAttr : jsoda.getFieldAttrMap(modelName).entrySet()) {
            String  fieldName = fieldAttr.getKey();
            String  attrName  = fieldAttr.getValue();
            Field   field = jsoda.getField(modelName, fieldName);
            Object  value = field.get(dataObj);
            String  fieldValueStr = DataUtil.encodeValueToAttrStr(value, field.getType());

            // Skip null value field.  No attribute stored at db.
            if (fieldValueStr == null)
                continue;

            // Add attr:fieldValueStr to list.  Skip the single Id field.  Treats single Id field as the itemName key in SimpleDB.
            if (!(jsoda.getRangeField(modelName) == null && jsoda.isIdField(modelName, fieldName)))
                attrs.add(new ReplaceableAttribute(attrName, fieldValueStr, true));
        }

        return attrs;
    }

    private UpdateCondition buildExpectedValue(String modelName, String expectedField, Object expectedValue, boolean expectedExists)
        throws Exception
    {
        if (expectedValue == null)
            throw new IllegalArgumentException("ExpectedValue cannot be null.");

        String          attrName = jsoda.getFieldAttrMap(modelName).get(expectedField);
        String          fieldValue = DataUtil.encodeValueToAttrStr(expectedValue, jsoda.getField(modelName, expectedField).getType());
        UpdateCondition cond = new UpdateCondition();

        cond.setExists(expectedExists);
        cond.setName(attrName);
        if (expectedExists) {
            cond.setValue(fieldValue);
        }
        return cond;
    }

    private List<ReplaceableItem> buildPutItems(List dataObjs, String modelName)
        throws Exception
    {
        String                  table = jsoda.getModelTable(modelName);
        List<ReplaceableItem>   items = new ArrayList<ReplaceableItem>();
        String                  idValue;

        for (Object dataObj : dataObjs) {
//          idValue = DataUtil.getFieldValueStr(dataObj, jsoda.getIdField(modelName));
            idValue = makeIdValue(modelName, dataObj);
            items.add(new ReplaceableItem(idValue, buildAttrs(dataObj, modelName)));
        }
        return items;
    }

    private Object buildLoadObj(String modelName, String idValue, List<Attribute> attrs, Query query)
        throws Exception
    {
        Class               modelClass = jsoda.getModelClass(modelName);
        Object              obj = modelClass.newInstance();
        Map<String, Field>  attrFieldMap = jsoda.getAttrFieldMap(modelName);

        // Set the attr field 
        for (Attribute attr : attrs) {
            String  attrName  = attr.getName();
            String  attrStr = attr.getValue();
            Field   field = attrFieldMap.get(attrName);

            //log.debug("attrName " + attrName + " attrStr: " + attrStr);

            if (field == null) {
                log.warn("Attribute " + attrName + " from db has no corresponding field in model class " + modelClass);
                continue;
            }

            DataUtil.setFieldValueStr(obj, field, attrStr);
        }

        if (query == null) {
            if (jsoda.getRangeField(modelName) == null) {
                DataUtil.setFieldValueStr(obj, jsoda.getIdField(modelName), idValue);
            } else {
                String[]    pair = parseCompositePk(modelName, idValue);
                Field       idField = jsoda.getIdField(modelName);
                Field       rangeField = jsoda.getRangeField(modelName);
                idField.set(obj, DataUtil.decodeAttrStrToValue(pair[0], idField.getType()));
                rangeField.set(obj, DataUtil.decodeAttrStrToValue(pair[1], rangeField.getType()));
            }
        } else {
            switch (query.selectType) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                if (jsoda.getRangeField(modelName) == null) {
                    DataUtil.setFieldValueStr(obj, jsoda.getIdField(modelName), idValue);
                } else {
                    String[]    pair = parseCompositePk(modelName, idValue);
                    Field       idField = jsoda.getIdField(modelName);
                    Field       rangeField = jsoda.getRangeField(modelName);
                    idField.set(obj, DataUtil.decodeAttrStrToValue(pair[0], idField.getType()));
                    rangeField.set(obj, DataUtil.decodeAttrStrToValue(pair[1], rangeField.getType()));
                }
            }
        }

        return obj;
    }

    private <T> String toQueryStr(Query<T> query, boolean selectCount) {
        StringBuilder   sb = new StringBuilder();
        addSelectStr(query, selectCount, sb);
        addFromStr(query, sb);
        addFilterStr(query, sb);
        addOrderbyStr(query, sb);
        addLimitStr(query, sb);
        return sb.toString();
    }

    private <T> void addSelectStr(Query<T> query, boolean selectCount, StringBuilder sb) {
        if (selectCount) {
            sb.append("select count(*) ");
            return;
        }

        switch (query.selectType) {
        case 1:
        {
            sb.append("select * ");
            return;
        }
        case 2:
        case 3:
        {
            sb.append("select ").append(ITEM_NAME);
            return;
        }
        case 4:
        case 5:
        case 7:
        {
            int     index = 0;
            for (String term : query.selectTerms) {
                // Skip the Id term as SimpleDB doesn't allow mixing of Select itemName(), other1, other2.
                // Id field is always back-fill during post query processing from the item name so it will be in the result.
                if (jsoda.isIdField(query.modelName, term))
                    continue;
                sb.append(index++ == 0 ? "select " : ", ");
                sb.append(getFieldAttrName(query.modelName, term));
            }
            return;
        }
        case 6:
        {
            int     index = 0;
            for (String term : query.selectTerms) {
                sb.append(index++ == 0 ? "select " : ", ");
                sb.append(getFieldAttrName(query.modelName, term));
            }
            return;
        }
        }
    }

    // private <T> void addSelectStr(Query<T> query, boolean selectCount, StringBuilder sb) {
    //     if (selectCount) {
    //         sb.append("select count(*) ");
    //         return;
    //     }

    //     if (query.selectTerms.size() == 0) {
    //         sb.append("select * ");
    //         return;
    //     }

    //     // select Id, select Id, RangeKey
    //     boolean selectId = false;
    //     boolean selectRange = false;
    //     for (String term : query.selectTerms) {
    //         if (jsoda.isIdField(query.modelName, term))
    //             selectId = true;
    //         if (jsoda.isRangeField(query.modelName, term))
    //             selectRange = true;
    //     }
    //     if (jsoda.getRangeField(query.modelName) == null) {
    //         if (selectId && query.selectTerms.size() == 1) {
    //             // Select itemName() from ...
    //             sb.append("select ").append(ITEM_NAME);
    //             return;
    //         }
    //     } else {
    //         if (selectId && selectRange && query.selectTerms.size() == 2) {
    //             // Select itemName() from ...
    //             sb.append("select ").append(ITEM_NAME);
    //             return;
    //         }
    //     }

    //     int     index = 0;
    //     for (String term : query.selectTerms) {
    //         if (jsoda.getRangeField(query.modelName) == null) {
    //             // Skip the Id term as SimpleDB doesn't allow mixing of Select itemName(), other1, other2.
    //             // Id field is always back-fill during post query processing from the item name so it will be in the result.
    //             if (jsoda.isIdField(query.modelName, term))
    //                 continue;
    //         } else {
    //             // Allow selecting single Id or RangeKey for a composite PK model.
    //         }

    //         sb.append(index++ == 0 ? "select " : ", ");
    //         sb.append(getFieldAttrName(query.modelName, term));
    //     }
    // }
    
    private <T> void addFromStr(Query<T> query, StringBuilder sb) {
        sb.append(" from ").append(SimpleDBUtils.quoteName(jsoda.getModelTable(query.modelName)));
    }

    private <T> void addFilterStr(Query<T> query, StringBuilder sb) {
        int index = 0;
        for (Filter filter : query.filters) {
            sb.append(index++ == 0 ? " where " : " and ");
            filter.toSimpleDBConditionStr(sb);
        }
    }

    private <T> void addOrderbyStr(Query<T> query, StringBuilder sb) {
        int index = 0;
        for (String orderby : query.orderbyFields) {
            sb.append(index++ == 0 ? " order by " : ", ");
            String  term = orderby.substring(1);
            String  ascDesc = orderby.charAt(0) == '+' ? " asc" : " desc";
            sb.append(getFieldAttrName(query.modelName, term));
            sb.append(ascDesc);
        }
    }

    private <T> void addLimitStr(Query<T> query, StringBuilder sb) {
        if (query.limit > 0)
            sb.append(" limit ").append(query.limit);
    }

}

