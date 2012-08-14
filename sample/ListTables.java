

import java.io.Serializable;

import wwutil.model.annotation.DbType;
import wwutil.model.annotation.Model;
import wwutil.model.annotation.Key;
import wwutil.jsoda.Jsoda;
import wwutil.jsoda.Dao;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;



/**
 * Sample to illustrate listing the table in SimpleDB and DynamoDB
 */
public class ListTables {

    // Get AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY from environment variables.
    // You can hardcode them here for testing but should remove them afterward.
    private static final String key = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String secret = System.getenv("AWS_SECRET_ACCESS_KEY");


    // Service url for DynamoDB
    private static final String dynUrl = "http://dynamodb.us-east-1.amazonaws.com";
    

    public static void main(String[] args)
        throws Exception
    {
        // Create a Jsoda object with AWS credentials.
        Jsoda       jsoda = new Jsoda(new BasicAWSCredentials(key, secret));

        // Set up the DynamoDB endpoint to use service in the AWS east region.
        // Use http endpoint to skip setting up https client certificate.
        jsoda.setDbEndpoint(DbType.DynamoDB, dynUrl);

        System.out.println("\nSimpleDB Tables:");
        for (String table : jsoda.listNativeTables(DbType.SimpleDB)) {
            System.out.println("    " + table);
        }

        System.out.println("\nDynamoDB Tables:");
        for (String table : jsoda.listNativeTables(DbType.DynamoDB)) {
            System.out.println("    " + table);
        }

    }

}

