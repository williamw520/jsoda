
package wwutil.model.annotation;

import java.lang.annotation.*;


@Retention(RetentionPolicy.RUNTIME)
public @interface AModel{

    public enum DbType {
        SimpleDB, DynamoDB;
    }

    /** Type of the underlying database: SimpleDB, DynamoDB */
    public DbType dbtype();

    /** Optional name of the underlying table.  If omitted, the class name will be used as table name. */
    public String table() default "";

    /** Optional prefix added to the undertable name, for organizing tables.  E.g. The prefix Acct_ applied to the model name User will produce table name of Acct_User. */
    public String prefix() default "";

    /** Optional read ProvisionedThroughput.  App is strongly encouraged to set its own ProvisionedThroughput */
    public long readThroughput() default 10;

    /** Optional write ProvisionedThroughput.  App is strongly encouraged to set its own ProvisionedThroughput */
    public long writeThroughput() default 5;

}
