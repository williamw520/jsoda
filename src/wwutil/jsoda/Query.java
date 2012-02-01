
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
    List<SdbFilter> filters = new ArrayList<SdbFilter>();
    List<String>    orderbyFields = new ArrayList<String>();
    int             limit = 0;


    /** Create a Query object to build query, to run on the Jsoda object. */
    public Query(Class<T> modelClass, Jsoda jsoda) {
        this.modelClass = modelClass;
        this.modelName = jsoda.getModelName(modelClass);
        this.jsoda = jsoda;
    }

    public Query<T> select(String field) {

        if (jsoda.getField(modelName, field) == null)
            throw new IllegalArgumentException("Field " + field + " does not exist in " + modelName);

        if (selectId)
            throw new IllegalArgumentException("Selecting on the Id field doesn't allow selecting on other fields.");

        if (jsoda.isIdField(modelName, field)) {
            if (selectTerms.size() > 0)
                throw new IllegalArgumentException("Selecting on the Id field doesn't allow selecting on any other fields.");
            selectId = true;
        }
        selectTerms.add(field);
        return this;
    }

    public Query<T> select(String[] fields) {
        for (String field : fields)
            select(field);
        return this;
    }

    public Query<T> filter(String field, String binaryOperator, Object operand) {
        if (jsoda.getField(modelName, field) == null)
            throw new IllegalArgumentException("Field " + field + " does not exist in " + modelName);

        filters.add(new SdbFilter(jsoda, modelName, field, binaryOperator, operand));
        return this;
    }

    public Query<T> filterIsNull(String field) {
        if (jsoda.getField(modelName, field) == null)
            throw new IllegalArgumentException("Field " + field + " does not exist in " + modelName);

        filters.add(new SdbFilter(jsoda, modelName, field, "is null"));
        return this;
    }

    public Query<T> filterIsNotNull(String field)
        throws Exception
    {
        if (jsoda.getField(modelName, field) == null)
            throw new IllegalArgumentException("Field " + field + " does not exist in " + modelName);

        filters.add(new SdbFilter(jsoda, modelName, field, "is not null"));
        return this;
    }

    public Query<T> filterBetween(String field, Object operand1, Object operand2)
        throws Exception
    {
        if (jsoda.getField(modelName, field) == null)
            throw new IllegalArgumentException("Field " + field + " does not exist in " + modelName);

        filters.add(new SdbFilter(jsoda, modelName, field, "between", operand1, operand2));
        return this;
    }

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

    public Query<T> limit(int limit) {
        this.limit = limit;
        return this;
    }

    /** Execute the query.  Call callPostLoad() for each returned object. */
    public List<T> run()
        throws JsodaException
    {
        List<T> resultObjs = jsoda.getDb(modelName).runQuery(modelClass, this);
        Dao<T>  dao = jsoda.dao(modelClass);
        for (T obj : resultObjs) {
            dao.postGet(obj);
        }
        return resultObjs;
    }

}

