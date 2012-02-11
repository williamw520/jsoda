

import java.io.Serializable;
import java.util.Date;

import wwutil.model.annotation.DbType;
import wwutil.model.annotation.Model;
import wwutil.model.annotation.Key;
import wwutil.model.annotation.ModifiedTime;
import wwutil.model.annotation.PrePersist;
import wwutil.jsoda.Jsoda;
import wwutil.jsoda.Dao;
import wwutil.jsoda.Query;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;



public class Sample2 {

    // Get AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY from environment variables.
    // You can hardcode them here for testing but should remove them afterward.
    private static final String key = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String secret = System.getenv("AWS_SECRET_ACCESS_KEY");



    // A sample model class for illustrating saving and loading from the SimpleDB.
    // Implements Serializable for automatic caching support.
    @Model
    public static class SampleProduct implements Serializable {
        @Key
        public String   productId;
        public String   name;
        public String   desc;
        public Float    price;

        @ModifiedTime                   // Auto fill in the time at object saving.
        public Date     updateTime;


        public SampleProduct() {}

        public SampleProduct(String productId, String name, String desc, Float price) {
            this.productId = productId;
            this.name = name;
            this.desc = desc;
            this.price = price;
        }

        public String toString() {
            return "   Product [" + productId + ", " + name + ", " + desc + ", " + price + ", " + updateTime + "]";
        }

        // This method will be called when the object is being saved.
        @PrePersist
        public void myPrePersist() {
            System.out.println("myPrePersist called on " + toString());
            // Fill in the desc field if it's not set.
            if (desc == null)
                desc = "Product " + name;
        }

    }


    public static void main(String[] args)
        throws Exception
    {
        Jsoda       jsoda = new Jsoda(new BasicAWSCredentials(key, secret));

        // Create the table corresponding to the model class.  Only need to do this once.
        jsoda.createModelTable(SampleProduct.class);


        // Save some objects
        Dao<SampleProduct>      dao = jsoda.dao(SampleProduct.class);
        dao.put(new SampleProduct("item1", "Red Shirt", "Premium red shirt", 29.95f));
        dao.put(new SampleProduct("item2", "Tophat", "Tophat for the cat", 90f));
        dao.put(new SampleProduct("item3", "Socks", null, 2.95f));
        dao.put(new SampleProduct("item4", "Steak", "Sizzling steak", 12.95f));
        dao.put(new SampleProduct("item5", null, "product with null name", 0.0f));


        // Create a query object specific to the SampleProduct model class.
        // No additional filtering condition means to get all the items.
        Query<SampleProduct>    query = jsoda.query(SampleProduct.class);

        // Run the count query to get back the count of the query.
        System.out.println("Number of objects: " + query.count());

        // Run the query to get all the items.
        for (SampleProduct product : query.run()) {
            System.out.println(product);
        }

        // Run a query to get all products whose price > 10
        Query<SampleProduct>    query2 = jsoda.query(SampleProduct.class).gt("price", 10);
        for (SampleProduct product : query2.run()) {
            System.out.println(product);
        }

        // Run a query to get the null name product.  Chaining style method calls.
        for (SampleProduct product : jsoda.query(SampleProduct.class)
                 .is_null("name")
                 .run()) {
            System.out.println(product);
        }

        // Run a query to get all products with name not null and price >= 29.95
        for (SampleProduct product : jsoda.query(SampleProduct.class)
                 .is_not_null("name")
                 .ge("price", 29.95f)
                 .run()) {
            System.out.println(product);
        }

        // Run a query to get all products whose price > 10 and order by price descending.
        for (SampleProduct product : jsoda.query(SampleProduct.class)
                 .gt("price", 10)
                 .order_by_desc("price")
                 .run()) {
            System.out.println(product);
        }

    }

}

