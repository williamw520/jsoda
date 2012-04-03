

import java.io.Serializable;
import java.util.*;
import java.lang.annotation.*;
import java.lang.reflect.*;

import wwutil.model.annotation.DbType;
import wwutil.jsoda.Jsoda;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;



/**
 * Sample to illustrate native table listing and deletion
 */
public class Sample5 {

    // Get AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY from environment variables.
    // You can hardcode them here for testing but should remove them afterward.
    private static final String key = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String secret = System.getenv("AWS_SECRET_ACCESS_KEY");

    // HTPP version of the service url for DynamoDB
    private static final String dynUrl = "http://dynamodb.us-east-1.amazonaws.com";


    public static void main(String[] args)
        throws Exception
    {
        Jsoda       jsoda = new Jsoda(new BasicAWSCredentials(key, secret));

        // Set up the DynamoDB endpoint to use service in the AWS east region.
        // Use http endpoint to skip setting up https client certificate.
        jsoda.setDbEndpoint(DbType.DynamoDB, dynUrl);

        System.out.println("SimpleDB tables (domains):");
        for (String dbname : jsoda.listNativeTables(DbType.SimpleDB)) {
            System.out.println(dbname);
        }
        
        System.out.println("DynamoDB tables:");
        for (String dbname : jsoda.listNativeTables(DbType.DynamoDB)) {
            System.out.println(dbname);
        }

        // Uncomment the following to run delete.

        // Sample tables.
        
        // jsoda.deleteNativeTable(DbType.SimpleDB, "SampleUser");
        // jsoda.deleteNativeTable(DbType.SimpleDB, "SampleProduct");
        // jsoda.deleteNativeTable(DbType.SimpleDB, "SampleProduct2");

        // jsoda.deleteNativeTable(DbType.DynamoDB, "SampleUser");
        // jsoda.deleteNativeTable(DbType.DynamoDB, "SampleProduct");
        // jsoda.deleteNativeTable(DbType.DynamoDB, "SampleProduct2");

        // Unit test tables.
        
        // jsoda.deleteNativeTable(DbType.SimpleDB, "Model1");
        // jsoda.deleteNativeTable(DbType.SimpleDB, "Model2");
        // jsoda.deleteNativeTable(DbType.SimpleDB, "Model3");
        // jsoda.deleteNativeTable(DbType.SimpleDB, "Model4");
        // jsoda.deleteNativeTable(DbType.SimpleDB, "Model5");
        // jsoda.deleteNativeTable(DbType.SimpleDB, "SdbModel1");
        // jsoda.deleteNativeTable(DbType.SimpleDB, "TestModel2");
        // jsoda.deleteNativeTable(DbType.SimpleDB, "domain1");
        // jsoda.deleteNativeTable(DbType.SimpleDB, "model1");
        
        // jsoda.deleteNativeTable(DbType.DynamoDB, "Model1");
        // jsoda.deleteNativeTable(DbType.DynamoDB, "Model2");
        // jsoda.deleteNativeTable(DbType.DynamoDB, "Model3");
        // jsoda.deleteNativeTable(DbType.DynamoDB, "Model4");
        // jsoda.deleteNativeTable(DbType.DynamoDB, "Model5");
        // jsoda.deleteNativeTable(DbType.DynamoDB, "SdbModel1");
        // jsoda.deleteNativeTable(DbType.DynamoDB, "TestModel2");
        // jsoda.deleteNativeTable(DbType.DynamoDB, "domain1");
        // jsoda.deleteNativeTable(DbType.DynamoDB, "model1");
        
    }

}

