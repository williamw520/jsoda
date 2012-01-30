

package utest;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;

import static org.junit.Assert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.AnyOf.anyOf;
import junit.framework.*;

import javax.persistence.Id;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;

import wwutil.jsoda.*;
import wwutil.model.MemCacheableSimple;
import wwutil.model.annotation.AModel;
import wwutil.model.annotation.AttrName;
import wwutil.model.annotation.ARangeKey;
import wwutil.model.annotation.CacheByField;




//
// Note: Some tests are disabled with the xx_ prefix on the methods.
// Remove the xx_ prefix to run them.
//

public class JsodaTest extends TestCase
{
    // Get AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY from environment variables.
    // You can hardcode them here for testing but should remove them afterward.
    private static final String key = System.getenv("AWS_ACCESS_KEY_ID");
    private static final String secret = System.getenv("AWS_SECRET_ACCESS_KEY");

    // Service url for DynamoDB
    private static final String awsUrl = "http://dynamodb.us-east-1.amazonaws.com";

    private Jsoda  jsodaDyn;
    private Jsoda  jsodaSdb;
    private Jsoda  jsoda;


    protected void setUp() throws Exception {

        // Set up a Jsoda object for both SimpleDB and DynamoDB model classes.
        // Set up the DynamoDB endpoint to use service in the AWS east region.
        // Use http endpoint to skip setting up https client certificate.
        jsoda = new Jsoda(new BasicAWSCredentials(key, secret))
            .setDbEndpoint(AModel.DbType.DynamoDB, awsUrl);
        jsoda.registerModel(SdbModel1.class);
        jsoda.registerModel(DynModel1.class);

        // Set up a Jsoda for testing the same models in SimpleDB
        jsodaSdb = new Jsoda(new BasicAWSCredentials(key, secret));
        jsodaSdb.registerModel(Model1.class, AModel.DbType.SimpleDB);
        jsodaSdb.registerModel(Model2.class, AModel.DbType.SimpleDB);

        // Set up a Jsoda for testing the same models in DynamoDB
        jsodaDyn = new Jsoda(new BasicAWSCredentials(key, secret))
            .setDbEndpoint(AModel.DbType.DynamoDB, awsUrl);
        jsodaDyn.registerModel(Model1.class, AModel.DbType.DynamoDB);
        jsodaDyn.registerModel(Model2.class, AModel.DbType.DynamoDB);

    }

    protected void tearDown() {
    }


    public void test_registration_dbtype_annotated() throws Exception {
        System.out.println("test_registration_dbtype_annotated");
        Jsoda   jsoda = new Jsoda(new BasicAWSCredentials(key, secret));
        String  modelName;

        // Register SimpleDB model class
        jsoda.registerModel(SdbModel1.class);
        modelName = jsoda.getModelName(SdbModel1.class);

        assertThat(modelName,
                   allOf( notNullValue(), not(is("")), is("SdbModel1") ));
        assertThat(jsoda.getModelTable(modelName),
                   allOf( notNullValue(), not(is("")), is("SdbModel1") ));
        assertThat(jsoda.getModelClass(modelName),
                   allOf( notNullValue(), equalTo(SdbModel1.class) ));
        assertThat(jsoda.getDb(modelName),
                   notNullValue());
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(AModel.DbType.SimpleDB), not(AModel.DbType.DynamoDB) ));
        assertThat(jsoda.getField(modelName, "nonExistingField"),
                   nullValue());
        assertThat(jsoda.getField(modelName, "name"),
                   allOf( notNullValue(), instanceOf(Field.class) ));
        assertThat(jsoda.getField(modelName, "name").getName(),
                   allOf( notNullValue(), is("name") ));
        assertThat(jsoda.getIdField(modelName),
                   allOf( notNullValue(), instanceOf(Field.class) ));
        assertThat(jsoda.getIdField(modelName).getName(),
                   allOf( notNullValue(), is("name") ));
        assertThat(jsoda.getRangeField(modelName),
                   allOf( nullValue() ));
        assertThat(jsoda.isIdField(modelName, "name"),
                   is(true));
        assertThat(jsoda.isIdField(modelName, "age"),
                   is(false));
        assertThat(jsoda.isIdField(modelName, "nonExistingField"),
                   is(false));

        System.out.println("model: " + jsoda.getModelName(SdbModel1.class));
        System.out.println("table: " + jsoda.getModelTable(jsoda.getModelName(SdbModel1.class)));
        System.out.println("class: " + jsoda.getModelClass(jsoda.getModelName(SdbModel1.class)));

        // Register DynamoDB model class
        jsoda.registerModel(DynModel1.class);
        modelName = jsoda.getModelName(DynModel1.class);

        assertThat(modelName,
                   allOf( notNullValue(), not(is("")), is("DynModel1") ));
        assertThat(jsoda.getModelTable(modelName),
                   allOf( notNullValue(), not(is("")), is("DynModel1") ));
        assertThat(jsoda.getModelClass(modelName),
                   allOf( notNullValue(), equalTo(DynModel1.class) ));
        assertThat(jsoda.getDb(modelName),
                   notNullValue());
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), not(AModel.DbType.SimpleDB), is(AModel.DbType.DynamoDB) ));
        assertThat(jsoda.getField(modelName, "nonExistingField"),
                   nullValue());
        assertThat(jsoda.getField(modelName, "name"),
                   allOf( notNullValue(), instanceOf(Field.class) ));
        assertThat(jsoda.getField(modelName, "name").getName(),
                   allOf( notNullValue(), is("name") ));
        assertThat(jsoda.getIdField(modelName),
                   allOf( notNullValue(), instanceOf(Field.class) ));
        assertThat(jsoda.getIdField(modelName).getName(),
                   allOf( notNullValue(), is("name") ));
        assertThat(jsoda.getRangeField(modelName),
                   allOf( nullValue() ));
        assertThat(jsoda.isIdField(modelName, "name"),
                   is(true));
        assertThat(jsoda.isIdField(modelName, "age"),
                   is(false));
        assertThat(jsoda.isIdField(modelName, "nonExistingField"),
                   is(false));

        System.out.println("model: " + jsoda.getModelName(DynModel1.class));
        System.out.println("table: " + jsoda.getModelTable(jsoda.getModelName(DynModel1.class)));
        System.out.println("class: " + jsoda.getModelClass(jsoda.getModelName(DynModel1.class)));

	}

    public void test_registration_force_dbtype() throws Exception {
        System.out.println("test_registration1_force_dbtype");

        System.out.println("test_registration_dbtype_annotated");
        Jsoda   jsoda = new Jsoda(new BasicAWSCredentials(key, secret));
        String  modelName;

        // Register non-annotated model class as SimpleDB
        jsoda.registerModel(Model1.class, AModel.DbType.SimpleDB);
        modelName = jsoda.getModelName(Model1.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(AModel.DbType.SimpleDB) ));

        jsoda.registerModel(Model2.class, AModel.DbType.SimpleDB);
        modelName = jsoda.getModelName(Model2.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(AModel.DbType.SimpleDB) ));
        assertThat(jsoda.getModelTable(modelName),
                   allOf( notNullValue(), not(is("")), is("TestModel2") ));     // Model2 has mapped its table name to TestModel2

        // Register non-annotated model class as DynamoDB
        jsoda.registerModel(Model1.class, AModel.DbType.DynamoDB);
        modelName = jsoda.getModelName(Model1.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(AModel.DbType.DynamoDB) ));

        jsoda.registerModel(Model2.class, AModel.DbType.DynamoDB);
        modelName = jsoda.getModelName(Model2.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(AModel.DbType.DynamoDB) ));
        assertThat(jsoda.getModelTable(modelName),
                   allOf( notNullValue(), not(is("")), is("TestModel2") ));     // Model2 has mapped its table name to TestModel2

        // Register annotated DynamoDB model class as SimpleDB
        jsoda.registerModel(DynModel1.class, AModel.DbType.SimpleDB);
        modelName = jsoda.getModelName(DynModel1.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(AModel.DbType.SimpleDB) ));

        // Register annotated SimpleDB model class as DynamoDB
        jsoda.registerModel(SdbModel1.class, AModel.DbType.DynamoDB);
        modelName = jsoda.getModelName(SdbModel1.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(AModel.DbType.DynamoDB) ));

	}

    public void xx_test_createTable() throws Exception {
        System.out.println("test_createTable");

        jsoda.createModelTable(SdbModel1.class);
        jsoda.createModelTable(DynModel1.class);

        jsodaSdb.createModelTable(Model1.class);
        jsodaSdb.createModelTable(Model2.class);

        jsodaDyn.createModelTable(Model1.class);
        jsodaDyn.createModelTable(Model2.class);
	}

    public void xx_test_listSdbTables() throws Exception {
        List<String>    tables = jsoda.listTables(AModel.DbType.SimpleDB);
        System.out.println("SimpleDB tables: " + ReflectUtil.dumpToStr(tables, ", "));
	}

    public void xx_test_listDynTables() throws Exception {
        List<String>    tables = jsoda.listTables(AModel.DbType.DynamoDB);
        System.out.println("DynamoDB tables: " + ReflectUtil.dumpToStr(tables, ", "));
	}

    public void xx_test_deleteTables() throws Exception {
        System.out.println("test_deleteTables");

        // BE CAREFUL RUNNING THIS TEST.  Make sure not tables with same name are in the databases.
        // It will delete them.  Uncomment below if you are sure.

        // jsoda.deleteModelTable(SdbModel1.class);
        // jsoda.deleteModelTable(DynModel1.class);

        // jsodaSdb.deleteModelTable(Model1.class);
        // jsodaSdb.deleteModelTable(Model2.class);

        // jsodaDyn.deleteModelTable(Model1.class);
        // jsodaDyn.deleteModelTable(Model2.class);
	}

    public void xx_test_deleteTablesDirect() throws Exception {
        System.out.println("test_deleteTablesDirect");

        // BE CAREFUL RUNNING THIS TEST.  Make sure not tables with same name are in the databases.
        // It will delete them.  Uncomment below if you are sure.

        // jsoda.deleteTable(AModel.DbType.SimpleDB, "TestModel1");
        // jsoda.deleteTable(AModel.DbType.SimpleDB, "test_Model2");
        // jsoda.deleteTable(AModel.DbType.SimpleDB, "Model2");

        // jsoda.deleteTable(AModel.DbType.DynamoDB, "TestModel1");
        // jsoda.deleteTable(AModel.DbType.DynamoDB, "test_Model2");
    }    

    public void xx_test_put() throws Exception {
        System.out.println("test_put");
        Model1  dataObj1 = new Model1("abc", 25);
        jsodaSdb.dao(Model1.class).put(dataObj1);
        jsodaDyn.dao(Model1.class).put(dataObj1);

        Model2  dataObj2 = new Model2(20, "item20", 20, 20.02);
        jsodaSdb.dao(Model2.class).put(dataObj2);
        jsodaDyn.dao(Model2.class).put(dataObj2);

        jsoda.dao(SdbModel1.class).put(new SdbModel1("abc", 25));
        jsoda.dao(DynModel1.class).put(new DynModel1("abc", 25));
	}

    public void xx_test_batchPut() throws Exception {
        System.out.println("test_batchPut");

        Model1[]    objs1 = new Model1[] { new Model1("aa", 50), new Model1("bb", 51), new Model1("cc", 52) };
        jsodaSdb.dao(Model1.class).batchPut(Arrays.asList(objs1));

        Model2[]    objs2 = new Model2[] { new Model2(1, "p1", 11, 1.1), new Model2(2, "p2", 12, 1.2), new Model2(3, "p3", 13, 1.3) };
        jsodaSdb.dao(Model2.class).batchPut(Arrays.asList(objs2));

        jsoda.dao(SdbModel1.class).batchPut(Arrays.asList(
            new SdbModel1[] { new SdbModel1("aa", 50), new SdbModel1("bb", 51), new SdbModel1("cc", 52) } ));

        jsoda.dao(DynModel1.class).batchPut(Arrays.asList(
            new DynModel1[] { new DynModel1("aa", 50), new DynModel1("bb", 51), new DynModel1("cc", 52) } ));
	}

    public void test_get1() throws Exception {
        System.out.println("test_get1");

        dump( jsodaSdb.dao(Model1.class).get("abc") );
        dump( jsodaDyn.dao(Model1.class).get("abc") );

        dump( jsodaSdb.dao(Model2.class).get(20) );
        dump( jsodaDyn.dao(Model2.class).get(20L) );

        dump( jsoda.dao(SdbModel1.class).get("abc") );
        dump( jsoda.dao(DynModel1.class).get("abc") );
	}

    public void xx_test_getCompositePk() throws Exception {
        System.out.println("test_getCompositePk");

        
        
	}

    public void xx_test_cache1() throws Exception {
        System.out.println("test_cache1");

        jsoda = new Jsoda(new BasicAWSCredentials(key, secret))
            .setMemCacheable(new MemCacheableSimple(1000))
            .setDbEndpoint(AModel.DbType.DynamoDB, awsUrl);

        jsoda.registerModel(Model1.class);
        jsoda.dao(Model1.class).get("abc");
        jsoda.dao(Model1.class).get("abc");
        jsoda.dao(Model1.class).get("abc");
        jsoda.dao(Model1.class).get("abc");
        System.out.println(jsoda.getMemCacheable().dumpStats());
	}

    public void xx_test_cache2() throws Exception {
        System.out.println("test_cache2");

        jsoda = new Jsoda(new BasicAWSCredentials(key, secret))
            .setMemCacheable(new MemCacheableSimple(1000))
            .setDbEndpoint(AModel.DbType.DynamoDB, awsUrl);

        jsoda.registerModel(Model2.class);
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
        dao.put(new Model1("delete123", 123));

        // Sleep a bit to wait for SimpleDB's eventual consistence to kick in.
        Thread.sleep(1000);
        dao.delete("delete123", 12345);
        Thread.sleep(1000);

        System.out.println("test_delete");
        Model1  dataObj2 = dao.get("delete123");
        if (dataObj2 == null)
            System.out.println("Obj deleted.");
        else
            System.out.println("Deleted   " + ReflectUtil.dumpToStr(dataObj2));
	}

    public void xx_test_delete2() throws Exception {
        System.out.println("test_delete2");
        Dao<Model2> dao = jsoda.dao(Model2.class);
        dao.put(new Model2(123, "delete123", 99, 55.5));

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

    public void xx_test_batchDelete() throws Exception {
        System.out.println("test_batchDelete");
        Dao<Model2> dao = jsoda.dao(Model2.class);
        dao.batchPut(Arrays.asList( new Model2[] { new Model2(101, "delete1", 1, 3.14), new Model2(102, "delete2", 2, 3.141), new Model2(103, "delete3", 3, 3.1415) } ));

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


    /** Generic data model class to be stored in SimpleDB or DynamoDB.
     * Since no dbtype in the AModel annotation is specified, dbtype is required at model registration.
     * Model class is Serializable so that it can be stored in the cache service.
     * Use the model class name as the table name in the underlying DB.
     */
    public static class Model1 implements Serializable {
        @Id                         // Mark this field as the primary key.
        public String   name;       // String type PK.

        public int      age;

        public Model1() {}
        public Model1(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /** Generic data model class to be stored in SimpleDB or DynamoDB.
     * Since no dbtype in the AModel annotation is specified, dbtype is required at model registration.
     * Model class is Serializable so that it can be stored in the cache service.
     * Use a different table name in the underlying DB, rather than using its class name as table name.
     */
    @AModel(table = "TestModel2")   // Specify a table name for this model class.
    public static class Model2 implements Serializable {
        @Id                         // PK.  When cache service is enabled, objects are always cached by its PK.
        public long     id;         // Long type PK.

        @CacheByField               // Additional field to cache the object.
        public String   name;       // Find-by-field Dao.findBy() will look up object by its field value in cache first.

        @AttrName("MaxCount")       // Specify the attribute name to use for this field in the underlying DB table.
        public int      count;

        public double   price;

        public Date     mdate = new Date();

        public Model2() {}

        public Model2(long id, String name, int count, double price) {
            this.id = id;
            this.name = name;
            this.count = count;
            this.price = price;
        }

    }

    /** Dbtype annotation to use SimpleDB. */
    @AModel(dbtype = AModel.DbType.SimpleDB)
    public static class SdbModel1 implements Serializable {
        @Id
        public String   name;

        public int      age;

        public SdbModel1() {}
        public SdbModel1(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /** Dbtype annotation to use DynamoDB. */
    @AModel(dbtype = AModel.DbType.DynamoDB)
    public static class DynModel1 implements Serializable {
        @Id
        public String   name;

        public int      age;

        public DynModel1() {}
        public DynModel1(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    //@AModel(dbtype = AModel.DbType.SimpleDB, prefix = "test_")

    public static class Model3 {
        @Id
        public Float   name;
        public Integer  age;

        public Model3() {}
    }

}


