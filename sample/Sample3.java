

import java.io.Serializable;
import java.util.Date;

import wwutil.model.annotation.DefaultGUID;
import wwutil.model.annotation.DbType;
import wwutil.model.annotation.Model;
import wwutil.model.annotation.Key;
import wwutil.model.annotation.ModifiedTime;
import wwutil.model.annotation.PrePersist;
import wwutil.model.annotation.MinSize;
import wwutil.model.annotation.MaxSize;
import wwutil.model.annotation.MinValue;
import wwutil.model.annotation.MaxValue;
import wwutil.model.annotation.Required;
import wwutil.model.annotation.ToUpper;
import wwutil.model.annotation.MaskMatch;
import wwutil.model.annotation.OneOf;
import wwutil.model.annotation.NotContains;
import wwutil.jsoda.Jsoda;
import wwutil.jsoda.Dao;
import wwutil.jsoda.Query;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;



/**
 * Sample to illustrate field data conversion and field validation.
 *
 * When an object is stored, it goes through a series of steps to
 * call the data conversion annotations and validation annotations.
 *<pre>
 * Pre-Storing Steps:
 *  1. The @PrePersist method in the model class is called if one is
 *     annotated, giving you the chance to modify any data field.
 *  2. The data generators annotated on the fields are called to fill in the
 *     field value.  E.g. @DefaultGUID or @ModifiedTime.
 *  3. The composite data generatorson the fields are called to fill in the field
 *     value.  E.g. @DefaultComposite.
 *  4. The @PreValidation method in the model class is called if one
 *     is annotated, giving you the chance to modify the field after the data
 *     generators run and do any custom validation before the built-in ones run.
 *  5. Built-in validations annotated on the fields are called.
 *</pre>
 */
public class Sample3 {

    // Get AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY from environment variables.
    // You can hardcode them here for testing but should remove them afterward.
    private static final String key = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String secret = System.getenv("AWS_SECRET_ACCESS_KEY");



    // A sample model class for illustrating saving and loading from the SimpleDB.
    // Implements Serializable for automatic caching support.
    @Model
    public static class SampleProduct2 implements Serializable {
        @Key
        @DefaultGUID                // generate a GUID for the field if it's not set.
        public String   productId;

        @MinSize(3)                 // validation: field has minimum size of 3
        @MaxSize(10)                // validation: field has maximum size of 10
        @Required                   // validation: field cannot be null
        @OneOf(choices = {"Red Shirt", "Tophat", "Socks", "Steak"}) // validation: must be one of the choices.
        public String   name;

        @NotContains("blue sky")    // validation: field cannot contain "blue sky"
        @ToUpper                    // conversion: convert field to upper case
        public String   desc;

        @MaskMatch( pattern = "(###) ###-####" )    // validation: must be phone pattern
        public String   phone;

        @MaskMatch(pattern = "800-###-****")        // validation: phone pattern starts with 800-, 3 digits, and then anything
        public String   phone800;

        @MinValue(10.5)             // conversion: set minimum value at field
        @MaxValue(20.5)             // conversion: set maximum value at field
        public Float    price;


        public SampleProduct2() {}

        public SampleProduct2(String productId, String name, String desc, String phone, String phone800, Float price) {
            this.productId = productId;
            this.name = name;
            this.desc = desc;
            this.phone = phone;
            this.phone800 = phone800;
            this.price = price;
        }

        public String toString() {
            return "   Product [" + productId + ", " + name + ", " + desc + ", " + phone + ", " + phone800 + ", " + price + "]";
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
        jsoda.createModelTable(SampleProduct2.class);


        // Save some objects
        Dao<SampleProduct2>      dao = jsoda.dao(SampleProduct2.class);
        dao.put(new SampleProduct2("item1", "Red Shirt", "Premium red shirt", "(415) 555-1212", "800-123-4567", 29.95f));
        dao.put(new SampleProduct2("item2", "Tophat", "Tophat for the cat", "(415) 555-1212", "800-789-ABCD", 90f));
        dao.put(new SampleProduct2("item3", "Socks", null, "(415) 555-1212", "800-555-1212", 2.95f));
        dao.put(new SampleProduct2("item4", "Steak", "Sizzling steak", "(415) 555-1212", "800-555-ab$d", 12.95f));
        dao.put(new SampleProduct2("item5", "Tophat", "product with null name", "(415) 555-1212", "800-123-4567", 0.0f));


        // Create a query object specific to the SampleProduct2 model class.
        // No additional filtering condition means to get all the items.
        Query<SampleProduct2>    query = jsoda.query(SampleProduct2.class);

        // Run the count query to get back the count of the query.
        System.out.println("Number of objects: " + query.count());

        // Run the query to get all the items.
        for (SampleProduct2 product : query.run()) {
            System.out.println(product);
        }

        // Run a query to get all products whose price > 10
        Query<SampleProduct2>    query2 = jsoda.query(SampleProduct2.class).gt("price", 10);
        for (SampleProduct2 product : query2.run()) {
            System.out.println(product);
        }

        // Run a query to get the null name product.  Chaining style method calls.
        for (SampleProduct2 product : jsoda.query(SampleProduct2.class)
                 .is_null("name")
                 .run()) {
            System.out.println(product);
        }

        // Run a query to get all products with name not null and price >= 29.95
        for (SampleProduct2 product : jsoda.query(SampleProduct2.class)
                 .is_not_null("name")
                 .ge("price", 29.95f)
                 .run()) {
            System.out.println(product);
        }

        // Run a query to get all products whose price > 10 and order by price descending.
        for (SampleProduct2 product : jsoda.query(SampleProduct2.class)
                 .gt("price", 10)
                 .order_by_desc("price")
                 .run()) {
            System.out.println(product);
        }

    }

}

