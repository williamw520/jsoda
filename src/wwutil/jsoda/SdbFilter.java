
package wwutil.jsoda;

import java.util.*;
import java.lang.reflect.*;

import com.amazonaws.services.simpledb.util.SimpleDBUtils;




/**
 * Filter condition for query.  Helper class used in SdbQuery.
 */
class SdbFilter
{
    private final static Set<String>    BINARY_OPERATORS = new HashSet<String>();
    private final static Set<String>    UNARY_OPERATORS = new HashSet<String>();
    private final static Set<String>    BETWEEN_OPERATORS = new HashSet<String>();


    static {
        BINARY_OPERATORS.add("=");
        BINARY_OPERATORS.add("!=");
        BINARY_OPERATORS.add(">");
        BINARY_OPERATORS.add(">=");
        BINARY_OPERATORS.add("<");
        BINARY_OPERATORS.add("<=");
        BINARY_OPERATORS.add("like");
        BINARY_OPERATORS.add("not like");

        UNARY_OPERATORS.add("is null");
        UNARY_OPERATORS.add("is not null");

        BETWEEN_OPERATORS.add("between");
        
        // Not supported
        // SDB_OPERATORS.add("in");
        // SDB_OPERATORS.add("every()");
        
    }

    Field           field;
    String          fieldName;
    String          attr;       // quoted
    boolean         isId;       // attr is a buildin function like 'itemName()'
    String          operator;
    Object          operand;
    Object          operand2;


    SdbFilter(Jsoda jsoda, String modelName, String fieldName, String operator, Object operand)
        throws Exception
    {
        setField(jsoda, modelName, fieldName);
        this.operator = operator;
        this.operand = operand;
    }

    SdbFilter(Jsoda jsoda, String modelName, String fieldName, String operator)
        throws Exception
    {
        setField(jsoda, modelName, fieldName);

        if (!UNARY_OPERATORS.contains(operator))
            throw new UnsupportedOperationException("Unsupported unary operator " + operator);
        this.operator = operator;
    }

    SdbFilter(Jsoda jsoda, String modelName, String fieldName, String operator, Object operand, Object operand2)
        throws Exception
    {
        setField(jsoda, modelName, fieldName);

        if (!BETWEEN_OPERATORS.contains(operator))
            throw new UnsupportedOperationException("Unsupported operator " + operator);

        this.operator = operator;
        this.operand = operand;
        this.operand2 = operand2;
    }


    void addFilterStr(StringBuilder sb)
        throws Exception
    {

        if (BINARY_OPERATORS.contains(operator)) {
            sb.append(attr);
            sb.append(" ").append(operator).append(" ");
            sb.append(SimpleDBUtils.quoteValue(DataUtil.toValueStr(operand)));
            return;
        }

        if (UNARY_OPERATORS.contains(operator)) {
            sb.append(attr);
            sb.append(" ").append(operator).append(" ");
            return;
        }

        if (BETWEEN_OPERATORS.contains(operator)) {
            sb.append(attr);
            sb.append(" between ");
            sb.append(SimpleDBUtils.quoteValue(DataUtil.toValueStr(operand)));
            sb.append(" and ");
            sb.append(SimpleDBUtils.quoteValue(DataUtil.toValueStr(operand2)));
            return;
        }

        throw new UnsupportedOperationException(operator);
    }

    private void setField(Jsoda jsoda, String modelName, String fieldName2)
        throws Exception
    {
        this.fieldName = fieldName2.trim();
        this.isId = jsoda.isIdField(modelName, this.fieldName);
        this.field = jsoda.getField(modelName, this.fieldName);
        this.attr = jsoda.getFieldAttrQuoted(modelName, this.fieldName);
        if (field == null || attr == null)
            throw new Exception("field " + this.fieldName + " is not defined in model " + modelName);
    }

}

