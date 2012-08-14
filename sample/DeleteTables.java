

import java.io.Serializable;
import java.util.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

import wwutil.model.annotation.DbType;
import wwutil.jsoda.Jsoda;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;



/**
 * Sample to illustrate table deletion (for cleaning up the sample tables and testing tables).
 */
public class DeleteTables {

    // Get AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY from environment variables.
    // You can hardcode them here for testing but should remove them afterward.
    private static final String key = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String secret = System.getenv("AWS_SECRET_ACCESS_KEY");

    // HTPP version of the service url for DynamoDB
    private static final String dynUrl = "http://dynamodb.us-east-1.amazonaws.com";



    private static void deleteTable(Jsoda jsoda, DbType dbtype, String table) {
        try {
            jsoda.deleteNativeTable(dbtype, table);
        } catch(Exception e) {
            System.out.println(e);
        }
    }

    public static void main(String[] args)
        throws Exception
    {
        Jsoda       jsoda = new Jsoda(new BasicAWSCredentials(key, secret));

        // Set up the DynamoDB endpoint to use service in the AWS east region.
        // Use http endpoint to skip setting up https client certificate.
        jsoda.setDbEndpoint(DbType.DynamoDB, dynUrl);

        // Uncomment the following to run delete.

        // Sample tables.
        
        // deleteTable(jsoda, DbType.SimpleDB, "SampleUser");
        // deleteTable(jsoda, DbType.SimpleDB, "SampleProduct");
        // deleteTable(jsoda, DbType.SimpleDB, "SampleProduct2");

        // deleteTable(jsoda, DbType.DynamoDB, "SampleUser");
        // deleteTable(jsoda, DbType.DynamoDB, "SampleProduct");
        // deleteTable(jsoda, DbType.DynamoDB, "SampleProduct2");

        // Unit test tables.
        
        // deleteTable(jsoda, DbType.SimpleDB, "Model1");
        // deleteTable(jsoda, DbType.SimpleDB, "Model2");
        // deleteTable(jsoda, DbType.SimpleDB, "Model3");
        // deleteTable(jsoda, DbType.SimpleDB, "Model4");
        // deleteTable(jsoda, DbType.SimpleDB, "Model5");
        // deleteTable(jsoda, DbType.SimpleDB, "Model7");
        // deleteTable(jsoda, DbType.SimpleDB, "SdbModel1");
        // deleteTable(jsoda, DbType.SimpleDB, "TestModel2");
        // deleteTable(jsoda, DbType.SimpleDB, "domain1");
        // deleteTable(jsoda, DbType.SimpleDB, "model1");
        
        // deleteTable(jsoda, DbType.DynamoDB, "Model1");
        // deleteTable(jsoda, DbType.DynamoDB, "Model2");
        // deleteTable(jsoda, DbType.DynamoDB, "Model3");
        // deleteTable(jsoda, DbType.DynamoDB, "Model4");
        // deleteTable(jsoda, DbType.DynamoDB, "Model5");
        // deleteTable(jsoda, DbType.SimpleDB, "Model7");
        // deleteTable(jsoda, DbType.DynamoDB, "SdbModel1");
        // deleteTable(jsoda, DbType.DynamoDB, "TestModel2");
        // deleteTable(jsoda, DbType.DynamoDB, "domain1");
        // deleteTable(jsoda, DbType.DynamoDB, "model1");


        // Cleanup on my tables

        // deleteTable(jsoda, DbType.SimpleDB, "MyTable123");
        // deleteTable(jsoda, DbType.DynamoDB, "MyTable123");

    }

}

