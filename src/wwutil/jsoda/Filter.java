
package wwutil.jsoda;

import java.util.*;
import java.lang.reflect.*;

import com.amazonaws.services.simpledb.util.SimpleDBUtils;




/**
 * Filter condition for query.  Helper class used in SdbQuery.
 */
class Filter
{
    final static Set<String>    UNARY_OPERATORS = new HashSet<String>();
    final static Set<String>    BINARY_OPERATORS = new HashSet<String>();
    final static Set<String>    TRINARY_OPERATORS = new HashSet<String>();
    final static Set<String>    LIST_OPERATORS = new HashSet<String>();

    public final static String  NULL = "is null";
    public final static String  NOT_NULL = "is not null";

    public final static String  EQ = "=";
    public final static String  NE = "!=";
    public final static String  LE = "<=";
    public final static String  LT = "<";
    public final static String  GE = ">=";
    public final static String  GT = ">";
    public final static String  LIKE = "like";
    public final static String  NOT_LIKE = "not like";
    public final static String  CONTAINS = "contains";
    public final static String  NOT_CONTAINS = "not contains";
    public final static String  BEGINS_WITH = "begins with";
    public final static String  EVERY = "every";

    public final static String  BETWEEN = "between";
    public final static String  IN = "in";


    static {
        UNARY_OPERATORS.add(NULL);
        UNARY_OPERATORS.add(NOT_NULL);

        BINARY_OPERATORS.add(EQ);
        BINARY_OPERATORS.add(NE);
        BINARY_OPERATORS.add(LE);
        BINARY_OPERATORS.add(LT);
        BINARY_OPERATORS.add(GE);
        BINARY_OPERATORS.add(GT);
        BINARY_OPERATORS.add(LIKE);
        BINARY_OPERATORS.add(NOT_LIKE);
        BINARY_OPERATORS.add(CONTAINS);
        BINARY_OPERATORS.add(NOT_CONTAINS);
        BINARY_OPERATORS.add(BEGINS_WITH);

        TRINARY_OPERATORS.add(BETWEEN);

        LIST_OPERATORS.add(IN);

    }

    Field           field;
    String          fieldName;
    String          attr;       // quoted
    boolean         isId;       // attr is a buildin function like 'itemName()'
    String          operator;
    Object          operand;
    Object          operand2;
    List            operands;


    Filter(Jsoda jsoda, String modelName, String fieldName, String operator) {
        jsoda.validateField(modelName, fieldName);
        jsoda.getDb(modelName).validateFilterOperator(operator);
        setField(jsoda, modelName, fieldName);
        this.operator = operator;
    }

    Filter(Jsoda jsoda, String modelName, String fieldName, String operator, Object operand) {
        jsoda.validateField(modelName, fieldName);
        jsoda.getDb(modelName).validateFilterOperator(operator);
        setField(jsoda, modelName, fieldName);
        this.operator = operator;
        this.operand = operand;
    }

    Filter(Jsoda jsoda, String modelName, String fieldName, String operator, Object operand, Object operand2) {
        jsoda.validateField(modelName, fieldName);
        jsoda.getDb(modelName).validateFilterOperator(operator);
        setField(jsoda, modelName, fieldName);
        this.operator = operator;
        this.operand = operand;
        this.operand2 = operand2;
    }

    Filter(Jsoda jsoda, String modelName, String fieldName, String operator, Object... operands) {
        jsoda.validateField(modelName, fieldName);
        jsoda.getDb(modelName).validateFilterOperator(operator);
        setField(jsoda, modelName, fieldName);
        this.operator = operator;
        this.operands = Arrays.asList(operands);
    }

    private void setField(Jsoda jsoda, String modelName, String fieldName2) {
        this.fieldName = fieldName2.trim();
        this.isId = jsoda.isIdField(modelName, this.fieldName);
        this.field = jsoda.getField(modelName, this.fieldName);
        this.attr = jsoda.getDb(modelName).getFieldAttrName(modelName, this.fieldName);
    }


    void toSimpleDBConditionStr(StringBuilder sb) {

        if (BINARY_OPERATORS.contains(operator)) {
            sb.append(attr);
            sb.append(" ").append(operator).append(" ");
            sb.append(SimpleDBUtils.quoteValue(DataUtil.toValueStr(operand, field.getType())));
            return;
        }

        if (UNARY_OPERATORS.contains(operator)) {
            sb.append(attr);
            sb.append(" ").append(operator).append(" ");
            return;
        }

        if (TRINARY_OPERATORS.contains(operator)) {
            sb.append(attr);
            sb.append(" between ");
            sb.append(SimpleDBUtils.quoteValue(DataUtil.toValueStr(operand, field.getType())));
            sb.append(" and ");
            sb.append(SimpleDBUtils.quoteValue(DataUtil.toValueStr(operand2, field.getType())));
            return;
        }

        throw new UnsupportedOperationException(operator);
    }

}

