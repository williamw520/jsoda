
package wwutil.jsoda;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import javax.persistence.Entity;
import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.apache.commons.beanutils.ConvertUtils;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
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
import com.amazonaws.services.simpledb.util.SimpleDBUtils;



/**
 * SimpleDB Query.
 */
public class Query<T>
{
    Class<T>        modelClass;
    String          modelName;
    Jsoda           jsoda;
    List<String>    selectTerms = new ArrayList<String>();
    boolean         selectId = false;
    List<Filter>    filters = new ArrayList<Filter>();
    List<String>    orderbyFields = new ArrayList<String>();
    int             limit = 0;
    boolean         consistentRead = false;


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

        if (jsoda.isIdField(modelName, field)) {
            if (selectTerms.size() > 0)
                throw new IllegalArgumentException("Selecting on the Id field doesn't allow selecting other fields.");
            selectId = true;
        } else {
            if (selectId)
                throw new IllegalArgumentException("Selecting on the Id field doesn't allow selecting other fields.");
        }

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
    

    /** Execute the query to return the items. */
    public List<T> run()
        throws JsodaException
    {
        List<T> resultObjs = jsoda.getDb(modelName).runQuery(modelClass, this);
        Dao<T>  dao = jsoda.dao(modelClass);
        for (T obj : resultObjs) {
            dao.postGet(obj);       // do callPostLoad and caching.
        }
        return resultObjs;
    }

    /** Execute the query to return the count, not the items. */
    public long count()
        throws JsodaException
    {
        return jsoda.getDb(modelName).countQuery(modelClass, this);
    }


}

