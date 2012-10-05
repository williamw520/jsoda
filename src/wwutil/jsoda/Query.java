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
import java.util.*;
import java.lang.reflect.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;




/**
 * Query object to capture common query properties for both SimpleDB and DynamoDB
 */
public class Query<T>
{
    private static Log  log = LogFactory.getLog(Query.class);

    /** Select types
     * 1. select all => select * from table
     * 2. select id => select itemName() from table
     * 3. select id and rangeKey => select itemName() from table
     * 4. select id and others => select others from table.  PK decoded from itemName in results.
     * 5. select id and rangeKey and others => select others from table.  PK decoded from itemName in results.
     * 6. select rangeKey => select id and rangeKey from table.  (always need id)
     * 7. select rangeKey and others => select id and rangeKey and others from table.  (always need id)
     * 8. select others => select others from table
     */
    static final int    SELECT_ALL = 1;
    static final int    SELECT_ID = 2;
    static final int    SELECT_ID_RANGE = 3;
    static final int    SELECT_ID_OTHERS = 4;
    static final int    SELECT_ID_RANGE_OTHERS = 5;
    static final int    SELECT_RANGE = 6;
    static final int    SELECT_RANGE_OTHERS = 7;
    static final int    SELECT_OTHERS = 8;


    
    Class<T>        modelClass;
    String          modelName;
    Jsoda           jsoda;
    List<String>    selectTerms = new ArrayList<String>();
    List<Filter>    filters = new ArrayList<Filter>();
    List<String>    orderbyFields = new ArrayList<String>();
    int             limit = 0;
    boolean         consistentRead = false;
    int             selectType = SELECT_ALL;
    boolean         beforeRun = true;
    Object          nextKey = null;
    private boolean queryParsed = false;


    /** Create a Query object to build query, to run on the Jsoda object. */
    public Query(Class<T> modelClass, Jsoda jsoda) {
        this.modelClass = modelClass;
        this.modelName = jsoda.getModelName(modelClass);
        this.jsoda = jsoda;
    }

    // Select

    /** Add a field to select from the query.
     * <pre>
     *  query.select("field1")
     * </pre>
     */
    public Query<T> select(String field) {
        jsoda.validateField(modelName, field);

        selectTerms.add(field);
        return this;
    }

    /** Add the list of fields to select from the query.
     * <pre>
     *  query.select("field1", "field2", "field3")
     *  query.select(new String[] {"field1", "field2", "field3"})
     * </pre>
     */
    public Query<T> select(String... fields) {
        for (String field : fields)
            select(field);
        return this;
    }

    // Filter conditions

    public Query<T> is_null(String field) {
        filters.add(new Filter(jsoda, modelName, field, Filter.NULL));
        return this;
    }

    public Query<T> is_not_null(String field) {
        filters.add(new Filter(jsoda, modelName, field, Filter.NOT_NULL));
        return this;
    }
    
    public Query<T> eq(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.EQ, operand));
        return this;
    }

    public Query<T> ne(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.NE, operand));
        return this;
    }

    public Query<T> le(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.LE, operand));
        return this;
    }

    public Query<T> lt(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.LT, operand));
        return this;
    }

    public Query<T> ge(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.GE, operand));
        return this;
    }

    public Query<T> gt(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.GT, operand));
        return this;
    }

    public Query<T> like(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.LIKE, operand));
        return this;
    }

    public Query<T> not_like(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.NOT_LIKE, operand));
        return this;
    }

    public Query<T> contains(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.CONTAINS, operand));
        return this;
    }

    public Query<T> not_contains(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.NOT_CONTAINS, operand));
        return this;
    }

    public Query<T> begins_with(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.BEGINS_WITH, operand));
        return this;
    }

    public Query<T> between(String field, Object startValue, Object endValue) {
        filters.add(new Filter(jsoda, modelName, field, Filter.BETWEEN, startValue, endValue));
        return this;
    }

    public Query<T> in(String field, Object... operands) {
        filters.add(new Filter(jsoda, modelName, field, Filter.IN, operands));
        return this;
    }

    // Order by
    
    public Query<T> order_by(String field) {
        if (jsoda.getField(modelName, field) == null)
            throw new IllegalArgumentException("Field " + field + " does not exist in " + modelName);

        orderbyFields.add("+" + field);
        return this;
    }

    public Query<T> order_by_desc(String field) {
        if (jsoda.getField(modelName, field) == null)
            throw new IllegalArgumentException("Field " + field + " does not exist in " + modelName);

        orderbyFields.add("-" + field);
        return this;
    }

    // Query configuration

    public Query<T> limit(int limit) {
        this.limit = limit;
        return this;
    }

    public Query<T> consistentRead(boolean consistentRead) {
        this.consistentRead = consistentRead;
        return this;
    }

    private void parseQuery() {

        if (queryParsed)
            return;
        queryParsed = true;

        selectType = determineSelectType();
    }

    private int determineSelectType() {

        if (selectTerms.size() == 0) {
            return SELECT_ALL;
        }

        // Determine select type of the query.
        boolean selectId = false;
        boolean selectRange = false;
        for (String term : selectTerms) {
            if (jsoda.isIdField(modelName, term))
                selectId = true;
            if (jsoda.isRangeField(modelName, term))
                selectRange = true;
        }

        if (!selectRange) {
            if (selectId) {
                if (selectTerms.size() == 1) {
                    // Only id field
                    return SELECT_ID;
                } else {
                    // Has id and other fields
                    return SELECT_ID_OTHERS;
                }
            } else {
                // No id nor range field, but selectTerms.size() > 0, just the other fields.
                return SELECT_OTHERS;
            }
        } else {
            if (selectId) {
                // Has id and range fields in select
                if (selectTerms.size() == 2) {
                    return SELECT_ID_RANGE;
                } else {
                    return SELECT_ID_RANGE_OTHERS;
                }
            } else {
                // Has range field but no id field in select
                if (selectTerms.size() == 1) {
                    return SELECT_RANGE;
                } else {
                    return SELECT_RANGE_OTHERS;
                }
            }
        }
    }


    /** Execute the query to return the count, not the items. */
    public long count()
        throws JsodaException
    {
        parseQuery();
        return jsoda.getDb(modelName).queryCount(modelClass, this);
    }

    /** Execute the query and start returning result items.  It might or might not return the entire result set.
     * Call run() again to get the next batch of items.  It will return an empty list when there's no more result.
     *
     * A typical loop to handle successive result batch.  See another style of simpler iteration in hasNext()'s doc.
     * <pre>
     *  List<T> items;
     *  while ((items = query.run()).size() != 0) {
     *      for (T item : items)
     *          dump(item);
     *  }
     * </pre>
     */
    public List<T> run()
        throws JsodaException
    {
        try {
            parseQuery();

            List<T> resultObjs = jsoda.getDb(modelName).queryRun(modelClass, this, !beforeRun);
            for (T obj : resultObjs) {
                jsoda.postLoadSteps(obj, toCache());  // do callPostLoad and caching.
            }
            beforeRun = false;
            return resultObjs;
        } catch(JsodaException je) {
            throw je;
        } catch(Exception e) {
            throw new JsodaException("Failed to run query", e);
        }
    }

    /** Quick check to see if there are more result to return.  Before run() is called, hasNext() always returns true.
     * This simplies the iteration loop.  The typical loop is:
     * <pre>
     *  while (query.hasNext()) {
     *      for (Model1 item : query.run()) {
     *      }
     *  }
     * </pre>
     */
    public boolean hasNext() {
        return beforeRun || jsoda.getDb(modelName).queryHasNext(this);
    }

    /** Reset the result set if there's any.  Restart the query if run() is called again. */
    public Query<T> reset() {
        beforeRun = true;
        nextKey = null;
        return this;
    }

    private boolean toCache() {
        // Besides select *, all other select types have partial fields.
        // Don't cache partial field object.
        return selectTerms.size() == 0;     // no term => select *
    }

}

