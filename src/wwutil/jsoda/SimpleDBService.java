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
import wwutil.sys.ReflectUtil;
import wwutil.model.MemCacheable;
import wwutil.model.annotation.DbType;
import wwutil.model.annotation.Model;
import wwutil.model.annotation.CachePolicy;
import wwutil.model.annotation.DefaultGUID;
import wwutil.model.annotation.DefaultComposite;
import wwutil.model.annotation.CacheByField;


/**
 * SimpleDB specific functions
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
    public static final int         MAX_PUT_ITEMS = 25;             // SimpleDB has a limit of 25 items per batch.

    private Jsoda                   jsoda;
    private AmazonSimpleDBClient    sdbClient;
    private String                  endPoint;


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
        this.endPoint = endpoint;
        sdbClient.setEndpoint(endpoint);
    }

    public String getDbEndpoint() {
        return this.endPoint;
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

    public <T> void putObj(Class<T> modelClass, T dataObj, String expectedField, Object expectedValue, boolean expectedExists)
        throws Exception
    {
        String  modelName = jsoda.getModelName(modelClass);
        String  table = jsoda.getModelTable(modelName);
        String  idValue = makeIdValue(modelName, dataObj);
        PutAttributesRequest    req =
            expectedField == null ?
                new PutAttributesRequest(table, idValue, buildAttrs(dataObj, modelName)) :
                new PutAttributesRequest(table, idValue, buildAttrs(dataObj, modelName),
                                         buildExpectedValue(modelName, expectedField, expectedValue, expectedExists));
        sdbClient.putAttributes(req);
    }

    public <T> void putObjs(Class<T> modelClass, List<T> dataObjs)
        throws Exception
    {
        String  modelName = jsoda.getModelName(modelClass);
        int     offset = 0;
        String  table = jsoda.getModelTable(modelName);

        while (offset < dataObjs.size()) {
            List<ReplaceableItem>   items = buildPutItems(dataObjs, modelName, offset);
            offset += items.size();
            sdbClient.batchPutAttributes(new BatchPutAttributesRequest(table, items));
        }
    }

    public <T> T getObj(Class<T> modelClass, Object id, Object rangeKey)
        throws Exception
    {
        if (id == null)
            throw new IllegalArgumentException("Id cannot be null.");

        String              modelName = jsoda.getModelName(modelClass);
        String              table = jsoda.getModelTable(modelName);
        String              idValue = makeIdValue(modelName, id, rangeKey);
        GetAttributesResult result = sdbClient.getAttributes(new GetAttributesRequest(table, idValue));
        if (result.getAttributes().size() == 0)
            return null;        // not existed.
        return buildLoadObj(modelClass, modelName, idValue, result.getAttributes(), null);
        
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
                String      idValue = item.getName();   // get the id from the item's name()
                T           obj = buildLoadObj(modelClass, query.modelName, idValue, item.getAttributes(), query);
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

    private List<ReplaceableItem> buildPutItems(List dataObjs, String modelName, int offset)
        throws Exception
    {
        List<ReplaceableItem>   items = new ArrayList<ReplaceableItem>();

        for (int i = offset; i < dataObjs.size() && items.size() < MAX_PUT_ITEMS; i++) {
            Object  dataObj = dataObjs.get(i);
            String  idValue = makeIdValue(modelName, dataObj);
            items.add(new ReplaceableItem(idValue, buildAttrs(dataObj, modelName)));
        }
        return items;
    }

    private <T> T buildLoadObj(Class<T> modelClass, String modelName, String idValue, List<Attribute> attrs, Query query)
        throws Exception
    {
        T                   obj = modelClass.newInstance();
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
            backfillIdAndRange(modelClass, modelName, obj, idValue);
        } else {
            // Any select type involving the id or the range key need them to be backfilled.
            switch (query.selectType) {
            case Query.SELECT_ALL:
            case Query.SELECT_ID:
            case Query.SELECT_ID_RANGE:
            case Query.SELECT_ID_OTHERS:
            case Query.SELECT_ID_RANGE_OTHERS:
            case Query.SELECT_RANGE:
            case Query.SELECT_RANGE_OTHERS:
                backfillIdAndRange(modelClass, modelName, obj, idValue);
                break;
            case Query.SELECT_OTHERS:
                break;
            }
        }

        return obj;
    }

    private <T> void backfillIdAndRange(Class<T> modelClass, String modelName, T obj, String idValue)
        throws Exception
    {
        Field   idField = jsoda.getIdField(modelName);
        Field   rangeField = jsoda.getRangeField(modelName);

        if (jsoda.getRangeField(modelName) == null) {
            // Backfill idField with the the item's name as the idValue.
            DataUtil.setFieldValueStr(obj, idField, idValue);
        } else {
            // Decode the idField and rangeField from the idValue
            String[]    pair = parseCompositePk(modelName, idValue);
            idField.set(obj, DataUtil.decodeAttrStrToValue(pair[0], idField.getType()));
            rangeField.set(obj, DataUtil.decodeAttrStrToValue(pair[1], rangeField.getType()));
        }
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
        case Query.SELECT_ALL:
        {
            sb.append("select * ");
            return;
        }
        case Query.SELECT_ID:
        {
            // Select just the id field (the ITEM_NAME() term).
            sb.append("select ").append(ITEM_NAME);
            return;
        }
        case Query.SELECT_ID_RANGE:
        case Query.SELECT_ID_OTHERS:
        case Query.SELECT_ID_RANGE_OTHERS:
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
        case Query.SELECT_RANGE:
        case Query.SELECT_RANGE_OTHERS:
        case Query.SELECT_OTHERS:
        {
            // Id field is not needed.
            // Id field is always back-fill during post query processing from the item name so it will be in the result.
            int     index = 0;
            for (String term : query.selectTerms) {
                sb.append(index++ == 0 ? "select " : ", ");
                sb.append(getFieldAttrName(query.modelName, term));
            }
            return;
        }
        }
    }

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

