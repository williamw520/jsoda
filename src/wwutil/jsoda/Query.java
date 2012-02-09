
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

    
    Class<T>        modelClass;
    String          modelName;
    Jsoda           jsoda;
    List<String>    selectTerms = new ArrayList<String>();
    List<Filter>    filters = new ArrayList<Filter>();
    List<String>    orderbyFields = new ArrayList<String>();
    int             limit = 0;
    boolean         consistentRead = false;
    int             selectType;
    boolean         beforeRun = true;
    Object          nextKey = null;


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

    public Query<T> isNull(String field) {
        filters.add(new Filter(jsoda, modelName, field, Filter.NULL));
        return this;
    }

    public Query<T> notNull(String field) {
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

    public Query<T> notLike(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.NOT_LIKE, operand));
        return this;
    }

    public Query<T> contains(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.CONTAINS, operand));
        return this;
    }

    public Query<T> notContains(String field, Object operand) {
        filters.add(new Filter(jsoda, modelName, field, Filter.NOT_CONTAINS, operand));
        return this;
    }

    public Query<T> beginsWith(String field, Object operand) {
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
    
    public Query<T> orderby(String field) {
        if (jsoda.getField(modelName, field) == null)
            throw new IllegalArgumentException("Field " + field + " does not exist in " + modelName);

        orderbyFields.add("+" + field);
        return this;
    }

    public Query<T> orderbyDesc(String field) {
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

    /** 7 select types:
     * 1. select all => select * from table
     * 2. select id => select itemName() from table
     * 3. select id, rangeKey => select itemName() from table
     * 4. select id, others => select others from table.  PK decoded from itemName in results.
     * 5. select id, rangeKey, others => select others from table.  PK decoded from itemName in results.
     * 6. select id or rangeKey, others => select id or rangeKey, others from table.
     * 7. select others => select others from table
     */
    void setupSelectType() {

        if (selectTerms.size() == 0) {
            selectType = 1;
            return;
        }

        // select Id, select Id, RangeKey
        boolean selectId = false;
        boolean selectRange = false;
        for (String term : selectTerms) {
            if (jsoda.isIdField(modelName, term))
                selectId = true;
            if (jsoda.isRangeField(modelName, term))
                selectRange = true;
        }

        if (jsoda.getRangeField(modelName) == null) {
            if (selectId) {
                if (selectTerms.size() == 1) {
                    selectType = 2;
                } else {
                    selectType = 4;
                }
                return;
            }
        } else {
            if (selectId && selectRange) {
                if (selectTerms.size() == 2) {
                    selectType = 3;
                } else {
                    selectType = 5;
                }
                return;
            } else {
                if (selectId || selectRange) {
                    selectType = 6;
                    return;
                }
            }
        }

        selectType = 7;
    }


    /** Execute the query to return the count, not the items. */
    public long count()
        throws JsodaException
    {
        setupSelectType();
        return jsoda.getDb(modelName).queryCount(modelClass, this);
    }

    /** Execute the query and start returning result items.  It might or might not return the entire result set.
     * Call hasNext() to find out.  Call run() again to get the next batch of result.
     * The typical loop is:
     * <pre>
     *  while (query.hasNext()) {
     *      List items = query.run();
     *  }
     * </pre>
     */
    public List<T> run()
        throws JsodaException
    {
        setupSelectType();

        List<T> resultObjs = jsoda.getDb(modelName).queryRun(modelClass, this, !beforeRun);
        Dao<T>  dao = jsoda.dao(modelClass);
        for (T obj : resultObjs) {
            dao.postGet(obj);       // do callPostLoad and caching.
        }
        beforeRun = false;
        return resultObjs;
    }

    /** Check if there are more result to return. */
    public boolean hasNext() {
        return beforeRun || jsoda.getDb(modelName).queryHasNext(this);
    }

    /** Reset the result set if there's any.  Restart the query if run() is called again. */
    public Query<T> reset() {
        beforeRun = true;
        nextKey = null;
        return this;
    }

}

