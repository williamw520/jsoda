

package utest;

import java.io.*;
import java.util.*;
import junit.framework.*;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import javax.persistence.Table;
import javax.persistence.Id;
import javax.persistence.Column;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;

import wwutil.jsoda.*;
import wwutil.model.MemCacheableSimple;
import wwutil.model.annotation.AModel;
import wwutil.model.annotation.ARangeKey;
import wwutil.model.annotation.CacheByField;




public class JsodaTest extends TestCase
{
    private static final String awsUrl = "http://dynamodb.us-east-1.amazonaws.com";
    private static final String key = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String secret = System.getenv("AWS_SECRET_ACCESS_KEY");

    private Jsoda  jsoda;


    protected void setUp() throws Exception {
        jsoda = new Jsoda(new BasicAWSCredentials(key, secret));
        jsoda.registerModel(Model1.class);
        jsoda.registerModel(Model2.class);

        // Set up the DynamoDB endpoint to use service in the AWS east region.
        // Use http endpoint to skip setting up https client certificate.
        jsoda.setDbEndpoint(AModel.DbType.DynamoDB, awsUrl);
    }

    protected void tearDown() {
    }

    //
    // Note: remove the xx_ prefix on the test methods to enable them.
    //

    public void xx_test_registration1() throws Exception {
        Jsoda   jsoda1 = new Jsoda(new BasicAWSCredentials(key, secret));
        jsoda1.registerModel(Model1.class);
        System.out.println("test_registration1");
        System.out.println("model: " + jsoda1.getModelName(Model1.class));
        System.out.println("table: " + jsoda1.getModelTable(jsoda1.getModelName(Model1.class)));
        System.out.println("class: " + jsoda1.getModelClass(jsoda1.getModelName(Model1.class)));
	}

    public void xx_test_registration2() throws Exception {
        Jsoda   jsoda1 = new Jsoda(new BasicAWSCredentials(key, secret));
        jsoda1.registerModel(Model1.class);
        jsoda1.registerModel(Model2.class);

        System.out.println("test_registration2");
        System.out.println("model: " + jsoda1.getModelName(Model1.class));
        System.out.println("table: " + jsoda1.getModelTable(jsoda1.getModelName(Model1.class)));
        System.out.println("class: " + jsoda1.getModelClass(jsoda1.getModelName(Model1.class)));

        System.out.println("model: " + jsoda1.getModelName(Model2.class));
        System.out.println("table: " + jsoda1.getModelTable(jsoda1.getModelName(Model2.class)));
        System.out.println("class: " + jsoda1.getModelClass(jsoda1.getModelName(Model2.class)));

	}

    public void xx_test_createTable() throws Exception {
        jsoda.createModelTable(Model1.class);
        jsoda.createModelTable(Model2.class);
	}

    public void xx_test_listTables1() throws Exception {
        List<String>    tables = jsoda.listTables(AModel.DbType.SimpleDB);
        System.out.println("SimpleDB tables: " + ReflectUtil.dumpToStr(tables, ", "));
	}

    public void xx_test_listTables2() throws Exception {
        List<String>    tables = jsoda.listTables(AModel.DbType.DynamoDB);
        System.out.println("DynamoDB tables: " + ReflectUtil.dumpToStr(tables, ", "));
	}

    public void xx_test_deleteTable() throws Exception {
        jsoda.deleteModelTable(Model1.class);
        jsoda.deleteModelTable(Model2.class);
	}

    public void xx_test_put() throws Exception {
        Dao<Model1> dao = jsoda.dao(Model1.class);
        Model1      dataObj1 = new Model1("abc", 25);
        dao.put(dataObj1);
	}

    public void xx_test_put2() throws Exception {
        Dao<Model1> dao = new Dao<Model1>(Model1.class, jsoda);
        Model1      dataObj1 = new Model1("abc2", 26);
        dao.put(dataObj1);
	}

    public void xx_test_batchPut() throws Exception {
        Dao<Model1> dao = jsoda.dao(Model1.class);
        dao.batchPut(Arrays.asList( new Model1[] { new Model1("bb", 26), new Model1("cc", 27), new Model1("dd", 20) } ));
	}

    public void xx_test_batchPut2() throws Exception {
        Dao<Model2> dao = jsoda.dao(Model2.class);
        dao.batchPut(Arrays.asList( new Model2[] { new Model2("p1", 10, 1.2), new Model2("p2", 20, 3), new Model2("p3", 9, 0.7) } ));
	}

    public void xx_test_get() throws Exception {
        System.out.println("test_get");
        Model1  dataObj1 = jsoda.dao(Model1.class).get("abc", new Integer(25));
        dump(dataObj1);
	}

    public void xx_test_get2() throws Exception {
        System.out.println("test_get2");
        Model2  dataObj1 = jsoda.dao(Model2.class).get("p2");
        dump(dataObj1);
	}

    public void xx_test_cache1() throws Exception {
        System.out.println("test_cache1");

        jsoda = new Jsoda(new BasicAWSCredentials(key, secret)).setMemCacheable(new MemCacheableSimple(1000));
        jsoda.registerModel(Model1.class);
        jsoda.setDbEndpoint(AModel.DbType.DynamoDB, awsUrl);

        jsoda.dao(Model1.class).get("abc", new Integer(25));
        jsoda.dao(Model1.class).get("abc", new Integer(25));
        jsoda.dao(Model1.class).get("abc", new Integer(25));
        jsoda.dao(Model1.class).get("abc", new Integer(25));
        System.out.println(jsoda.getMemCacheable().dumpStats());
	}

    public void xx_test_cache2() throws Exception {
        System.out.println("test_cache2");

        jsoda = new Jsoda(new BasicAWSCredentials(key, secret)).setMemCacheable(new MemCacheableSimple(1000));
        jsoda.registerModel(Model2.class);
        jsoda.setDbEndpoint(AModel.DbType.DynamoDB, awsUrl);

        jsoda.dao(Model2.class).get("p2");
        jsoda.dao(Model2.class).get("p2");
        jsoda.dao(Model2.class).get("p2");
        jsoda.dao(Model2.class).get("p2");
        System.out.println(jsoda.getMemCacheable().dumpStats());
	}

    public void xx_test_getNonExist() throws Exception {
        System.out.println("test_getNonExist");
        Model2  dataObj1 = jsoda.dao(Model2.class).get("_not_exist_");
        assertEquals(dataObj1, null);
	}

    public void xx_test_delete1() throws Exception {
        System.out.println("test_delete1");
        Dao<Model1> dao = jsoda.dao(Model1.class);
        dao.put(new Model1("delete123", 12345));

        // Sleep a bit to wait for SimpleDB's eventual consistence to kick in.
        Thread.sleep(1000);
        dao.delete("delete123", 12345);
        Thread.sleep(1000);

        System.out.println("test_delete");
        Model1  dataObj2 = dao.get("delete123", 12345);
        if (dataObj2 == null)
            System.out.println("Obj deleted.");
        else
            System.out.println("Deleted   " + ReflectUtil.dumpToStr(dataObj2));
	}

    public void xx_test_delete2() throws Exception {
        System.out.println("test_delete2");
        Dao<Model2> dao = jsoda.dao(Model2.class);
        dao.put(new Model2("delete123", 99, 55.5));

        // Sleep a bit to wait for SimpleDB's eventual consistence to kick in.
        Thread.sleep(1000);
        dao.delete("delete123");
        Thread.sleep(1000);

        System.out.println("test_delete");
        Model2  dataObj2 = dao.get("delete123");
        if (dataObj2 == null)
            System.out.println("Obj deleted.");
        else
            System.out.println("Deleted   " + ReflectUtil.dumpToStr(dataObj2));
	}

    public void test_batchDelete() throws Exception {
        System.out.println("test_batchDelete");
        Dao<Model2> dao = jsoda.dao(Model2.class);
        dao.batchPut(Arrays.asList( new Model2[] { new Model2("delete1", 1, 3.14), new Model2("delete2", 2, 3.141), new Model2("delete3", 3, 3.1415) } ));

        // Sleep a bit to wait for SimpleDB's eventual consistence to kick in.
        Thread.sleep(1000);
        dao.batchDelete(Arrays.asList("delete1", "delete2", "delete3"));
        Thread.sleep(1000);

        if (dao.get("delete1") == null)
            System.out.println("delete1 deleted.");
        else
            System.out.println("delete1 still exists.");

        if (dao.get("delete2") == null)
            System.out.println("delete2 deleted.");
        else
            System.out.println("delete2 still exists.");
        
        if (dao.get("delete3") == null)
            System.out.println("delete3 deleted.");
        else
            System.out.println("delete3 still exists.");
	}

    public void xx_test_select_all() throws Exception {
        System.out.println("test_select_all");
        List<Model1>    items = jsoda.query(Model1.class).run();
        for (Model1 item : items) {
            dump(item);
        }
	}

    public void xx_test_select_field() throws Exception {
        System.out.println("test_select_field");
        for (Model1 item :
                 jsoda.query(Model1.class)
                 .select("age")
                 .run() ) {
            dump(item);
        }
	}

    public void xx_test_select_id() throws Exception {
        System.out.println("test_select_id");
        for (Model1 item :
                 jsoda.query(Model1.class)
                 .select("name")
                 .run() ) {
            dump(item);
        }
	}

    public void xx_test_filter_age() throws Exception {
        System.out.println("test_filter_age");
        for (Model1 item :
                 jsoda.query(Model1.class)
                 .filter("age", ">", 25)
                 .run() ) {
            dump(item);
        }
	}

    public void xx_test_filter_between() throws Exception {
        System.out.println("test_filter_between");
        for (Model1 item :
                 jsoda.query(Model1.class)
                 .filterBetween("age", 25, 26)
                 .run() ) {
            dump(item);
        }
	}

    public void xx_test_filter_id() throws Exception {
        System.out.println("test_filter_id");
        for (Model1 item :
                 jsoda.query(Model1.class)
                 .filter("name", "=", "abc")
                 .run() ) {
            dump(item);
        }
	}

    public void xx_test_filter_id_and_age() throws Exception {
        System.out.println("test_filter_id_and_age");
        for (Model1 item :
                 jsoda.query(Model1.class)
                 .filter("name", "=", "abc")
                 .filter("age", "=", 25)
                 .run() ) {
            dump(item);
        }
	}

    public void xx_test_select_limit() throws Exception {
        System.out.println("test_select_limit");
        for (Model1 item :
                 jsoda.query(Model1.class)
                 .limit(2)
                 .run() ) {
            dump(item);
        }
	}

    public void xx_test_select_all_2() throws Exception {
        System.out.println("test_select_all_2");
        for (Model2 item :
                 jsoda.query(Model2.class)
                 .run() ) {
            dump(item);
        }
	}

    public void xx_test_select_field_2() throws Exception {
        System.out.println("test_select_field_2");
        for (Model2 item :
                 jsoda.query(Model2.class)
                 .select("count")
                 .run() ) {
            dump(item);
        }
	}

    public void xx_test_filter_count() throws Exception {
        System.out.println("test_filter_count");
        for (Model2 item :
                 jsoda.query(Model2.class)
                 .filter("count", ">", 10)
                 .run() ) {
            dump(item);
        }
	}

    public void xx_test_filter_id_and_count() throws Exception {
        System.out.println("test_filter_id_and_count");
        for (Model2 item :
                 jsoda.query(Model2.class)
                 .filter("name", "=", "p2")
                 .filter("count", "=", 20)
                 .run() ) {
            dump(item);
        }
	}

    public void xx_test_findby_count() throws Exception {
        System.out.println("test_findby_count");
        Model2 item = jsoda.dao(Model2.class).findBy("count", 20);
        dump(item);
	}


    public void xx_test_dummy()
    {
		assertTrue(true);
	}

    private static void dump(Object obj) {
        System.out.println(ReflectUtil.dumpToStr(obj));
    }
	
    // Main
    public static void main(String[] argv)
    {
		TestUtil.runTests(JsodaTest.class);
    }


    // xx_test data model classes to be stored in SimpleDB.

    @AModel(dbtype = AModel.DbType.DynamoDB, table = "TestModel1")
    //@AModel(dbtype = AModel.DbType.SimpleDB, table = "TestModel1")
    public static class Model1 implements Serializable {
        @Id
        public String   name;
        @ARangeKey
        public Integer  age;

        public Model1() {}

        public Model1(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    @AModel(dbtype = AModel.DbType.DynamoDB, prefix = "test_")
    //@AModel(dbtype = AModel.DbType.SimpleDB, prefix = "test_")
    public static class Model2 implements Serializable {
        @Id
        public String   name;
        @Column(name = "MaxCount")
        @CacheByField
        public int      count;
        public double   price;
        public Date     mdate = new Date();

        public Model2() {}

        public Model2(String name, int count, double price) {
            this.name = name;
            this.count = count;
            this.price = price;
        }

    }

    public static class Model3 {
        @Id
        public Float   name;
        public Integer  age;

        public Model3() {}
    }

}


