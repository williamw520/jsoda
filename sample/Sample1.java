

import java.io.Serializable;

import wwutil.model.annotation.DbType;
import wwutil.model.annotation.Model;
import wwutil.model.annotation.Key;
import wwutil.jsoda.Jsoda;
import wwutil.jsoda.Dao;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;



public class Sample1 {

    // Get AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY from environment variables.
    // You can hardcode them here for testing but should remove them afterward.
    private static final String key = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String secret = System.getenv("AWS_SECRET_ACCESS_KEY");



    // A sample model class for illustrating saving and loading from the SimpleDB
    @Model
    public static class SampleUser implements Serializable {
        @Key
        public int      id;
        public String   name;
        public long     visits;

        public SampleUser() {}

        public SampleUser(int id, String name, long visits) {
            this.id = id;
            this.name = name;
            this.visits = visits;
        }

        public String toString() {
            return id + " " + name + " " + visits;
        }
    }


    public static void main(String[] args)
        throws Exception
    {
        Jsoda           jsoda = new Jsoda(new BasicAWSCredentials(key, secret));
        Dao<SampleUser> dao = jsoda.dao(SampleUser.class);

        // Create the table corresponding to the model class.  Only need to do this once.
        jsoda.createModelTable(SampleUser.class);

        // Save some objects
        dao.put(new SampleUser(101, "Jack", 1));
        dao.put(new SampleUser(102, "Jill", 1));
        dao.put(new SampleUser(103, "Joe", 1));
        dao.put(new SampleUser(104, "Jae", 1));

        // Load the objects by key
        SampleUser      obj1 = dao.get(101);
        SampleUser      obj2 = dao.get(102);
        SampleUser      obj3 = dao.get(103);
        SampleUser      obj4 = dao.get(104);

        System.out.println(obj1);
        System.out.println(obj2);
        System.out.println(obj3);
        System.out.println(obj4);

        // Modify an object and save it
        obj1.visits++;
        dao.put(obj1);
        System.out.println(obj1);

        Thread.sleep(1000);

        // Reload the modified object
        System.out.println(dao.get(101));

    }

}

