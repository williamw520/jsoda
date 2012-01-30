
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
    private final static Set<String>    BUILDIN_TERMS = new HashSet<String>();

    private Class<T>        modelClass;
    private String          modelName;
    private Jsoda           jsoda;
    private List<String>    selectTerms = new ArrayList<String>();
    private boolean         selectId = false;
    private List<SdbFilter> filters = new ArrayList<SdbFilter>();
    private List<String>    orderbyFields = new ArrayList<String>();
    private int             limit = 0;


    static {
        BUILDIN_TERMS.add("itemName()");
    }


    Query(Class<T> modelClass, Jsoda jsoda) {
        jsoda.validateRegisteredModel(modelClass);
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

    public String toQueryStr() {
        StringBuilder   sb = new StringBuilder();
        addSelectStr(sb);
        addFromStr(sb);
        addFilterStr(sb);
        addOrderbyStr(sb);
        addLimitStr(sb);
        return sb.toString();
    }

    private void addSelectStr(StringBuilder sb) {
        sb.append("select");

        if (selectTerms.size() == 0) {
            sb.append(" * ");
            return;
        }

        for (int i = 0; i < selectTerms.size(); i++) {
            if (i > 0)
                sb.append(", ");
            else
                sb.append(" ");
            String  term = selectTerms.get(i);
            sb.append(jsoda.getDb(modelName).getFieldAttrName(modelName, term));
        }
    }

    private void addFromStr(StringBuilder sb) {
        sb.append(" from ").append(SimpleDBUtils.quoteName(jsoda.getModelTable(modelName)));
    }

    private void addFilterStr(StringBuilder sb) {
        if (filters.size() == 0) {
            return;
        }

        sb.append(" where ");

        for (int i = 0; i < filters.size(); i++) {
            if (i > 0)
                sb.append(" and ");

            filters.get(i).addFilterStr(sb);
        }
    }

    private void addOrderbyStr(StringBuilder sb) {
        if (orderbyFields.size() == 0)
            return;

        sb.append(" order by");

        for (int i = 0; i < orderbyFields.size(); i++) {
            if (i > 0)
                sb.append(", ");
            else
                sb.append(" ");
            String  orderby = orderbyFields.get(i);
            String  ascDesc = orderby.charAt(0) == '+' ? " asc" : " desc";
            String  term = orderby.substring(1);
            sb.append(jsoda.getDb(modelName).getFieldAttrName(modelName, term));
            sb.append(ascDesc);
        }
    }

    private void addLimitStr(StringBuilder sb) {
        if (limit > 0)
            sb.append(" limit ").append(limit);
    }

    /** Execute the query.  Call callPostLoad() for each returned object. */
    public List<T> run()
        throws JsodaException
    {
        List<T> resultObjs = (List<T>)jsoda.getDb(modelName).runQuery(modelClass, toQueryStr());
        for (T obj : resultObjs) {
            jsoda.callPostLoad(modelName, obj);
            jsoda.cachePut(modelName, (Serializable)obj);
        }
        return resultObjs;
    }

}

