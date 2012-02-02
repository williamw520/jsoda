

package wwutil.jsoda;

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
import javax.persistence.Transient;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;

import wwutil.model.MemCacheableSimple;
import wwutil.model.annotation.DbType;
import wwutil.model.annotation.AModel;
import wwutil.model.annotation.AttrName;
import wwutil.model.annotation.ARangeKey;
import wwutil.model.annotation.CacheByField;

import static wwutil.jsoda.Query.*;




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
            .setDbEndpoint(DbType.DynamoDB, awsUrl);
        jsoda.registerModel(SdbModel1.class);
        jsoda.registerModel(DynModel1.class);

        // Set up a Jsoda for testing the same models in SimpleDB
        jsodaSdb = new Jsoda(new BasicAWSCredentials(key, secret));
        jsodaSdb.registerModel(Model1.class, DbType.SimpleDB);
        jsodaSdb.registerModel(Model2.class, DbType.SimpleDB);
        jsodaSdb.registerModel(Model3.class, DbType.SimpleDB);

        // Set up a Jsoda for testing the same models in DynamoDB
        jsodaDyn = new Jsoda(new BasicAWSCredentials(key, secret))
            .setDbEndpoint(DbType.DynamoDB, awsUrl);
        jsodaDyn.registerModel(Model1.class, DbType.DynamoDB);
        jsodaDyn.registerModel(Model2.class, DbType.DynamoDB);
        jsodaDyn.registerModel(Model3.class, DbType.DynamoDB);

    }

    protected void tearDown() {
    }


    public void xx_test_registration_dbtype_annotated() throws Exception {
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
                   allOf( notNullValue(), is(DbType.SimpleDB), not(DbType.DynamoDB) ));
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
                   allOf( notNullValue(), not(DbType.SimpleDB), is(DbType.DynamoDB) ));
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

    public void xx_test_registration_force_dbtype() throws Exception {
        System.out.println("test_registration_force_dbtype");

        Jsoda   jsoda = new Jsoda(new BasicAWSCredentials(key, secret));
        String  modelName;

        // Register non-annotated model class as SimpleDB
        jsoda.registerModel(Model1.class, DbType.SimpleDB);
        modelName = jsoda.getModelName(Model1.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(DbType.SimpleDB) ));

        jsoda.registerModel(Model2.class, DbType.SimpleDB);
        modelName = jsoda.getModelName(Model2.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(DbType.SimpleDB) ));
        assertThat(jsoda.getModelTable(modelName),
                   allOf( notNullValue(), not(is("")), is("TestModel2") ));     // Model2 has mapped its table name to TestModel2

        // Register non-annotated model class as DynamoDB
        jsoda.registerModel(Model1.class, DbType.DynamoDB);
        modelName = jsoda.getModelName(Model1.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(DbType.DynamoDB) ));

        jsoda.registerModel(Model2.class, DbType.DynamoDB);
        modelName = jsoda.getModelName(Model2.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(DbType.DynamoDB) ));
        assertThat(jsoda.getModelTable(modelName),
                   allOf( notNullValue(), not(is("")), is("TestModel2") ));     // Model2 has mapped its table name to TestModel2

        // Register annotated DynamoDB model class as SimpleDB
        jsoda.registerModel(DynModel1.class, DbType.SimpleDB);
        modelName = jsoda.getModelName(DynModel1.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(DbType.SimpleDB) ));

        // Register annotated SimpleDB model class as DynamoDB
        jsoda.registerModel(SdbModel1.class, DbType.DynamoDB);
        modelName = jsoda.getModelName(SdbModel1.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(DbType.DynamoDB) ));

	}

    public void xx_test_registration_composite_key() throws Exception {
        System.out.println("test_registration_composite_key");

        Jsoda   jsoda = new Jsoda(new BasicAWSCredentials(key, secret));
        String  modelName;

        // Register DynamoDB model class with composite key
        jsoda.registerModel(Model3.class, DbType.DynamoDB);
        modelName = jsoda.getModelName(Model3.class);

        assertThat(jsoda.getIdField(modelName),
                   allOf( notNullValue(), instanceOf(Field.class) ));
        assertThat(jsoda.getIdField(modelName).getName(),
                   allOf( notNullValue(), is("id") ));
        assertThat(jsoda.getRangeField(modelName),
                   allOf( notNullValue() ));
        assertThat(jsoda.getRangeField(modelName).getName(),
                   allOf( notNullValue(), is("name") ));
	}

    public void xx_test_registration_auto() throws Exception {
        System.out.println("test_registration_auto");
        Jsoda   jsoda = new Jsoda(new BasicAWSCredentials(key, secret));

        // Register model class automatically in dao creation.
        jsoda.dao(SdbModel1.class);
        assertThat(jsoda.isRegistered(SdbModel1.class),
                   is(true));

        jsoda.dao(DynModel1.class);
        assertThat(jsoda.isRegistered(DynModel1.class),
                   is(true));

        try {
            jsoda.dao(Model1.class);
            assertThat("Generic model without dbtype cannot be auto-registered", false,
                       is(true));
        } catch(JsodaException expected) {
            //expected.printStackTrace();
        }
    }
    
    public void xx_test_registration_transient() throws Exception {
        System.out.println("test_registration_transient");
        Jsoda   jsoda = new Jsoda(new BasicAWSCredentials(key, secret));

        jsoda.registerModel(Model2.class, DbType.SimpleDB);
        assertThat(jsoda.getField(jsoda.getModelName(Model2.class), "currtime"),
                   is(nullValue()));

        jsoda.registerModel(Model2.class, DbType.DynamoDB);
        assertThat(jsoda.getField(jsoda.getModelName(Model2.class), "currtime"),
                   is(nullValue()));

    }

    public void xx_test_deleteTables() throws Exception {
        System.out.println("test_deleteTables");

        // BE CAREFUL RUNNING THIS TEST.  Make sure not tables with same name are in the databases.
        // It will delete them.  Uncomment below if you are sure.

        // jsoda.deleteModelTable(SdbModel1.class);
        // jsoda.deleteModelTable(DynModel1.class);

        // jsodaSdb.deleteModelTable(Model1.class);
        // jsodaSdb.deleteModelTable(Model2.class);
        // jsodaSdb.deleteModelTable(Model3.class);

        // jsodaDyn.deleteModelTable(Model1.class);
        // jsodaDyn.deleteModelTable(Model2.class);
        // jsodaDyn.deleteModelTable(Model3.class);

	}

    public void xx_test_deleteTablesDirect() throws Exception {
        System.out.println("test_deleteTablesDirect");

        // BE CAREFUL RUNNING THIS TEST.  Make sure not tables with same name are in the databases.
        // It will delete them.  Uncomment below if you are sure.

        // jsoda.deleteTable(DbType.SimpleDB, "TestModel1");
        // jsoda.deleteTable(DbType.SimpleDB, "test_Model2");
        // jsoda.deleteTable(DbType.SimpleDB, "Model2");

        // jsoda.deleteTable(DbType.DynamoDB, "TestModel1");
        // jsoda.deleteTable(DbType.DynamoDB, "test_Model2");
    }    

    // Note: DynamoDB tables cannot be created while they exist.  Run this only once and comment out the DynamoDB table creation afterward.
    public void xx_test_createTable() throws Exception {
        System.out.println("test_createTable");

        jsoda.createModelTable(SdbModel1.class);
        jsoda.createModelTable(DynModel1.class);

        jsodaSdb.createModelTable(Model1.class);
        jsodaSdb.createModelTable(Model2.class);
        jsodaSdb.createModelTable(Model3.class);

        jsodaDyn.createModelTable(Model1.class);
        jsodaDyn.createModelTable(Model2.class);
        jsodaDyn.createModelTable(Model3.class);
	}

    public void xx_test_createRegisteredTables() throws Exception {
        System.out.println("test_createRegisteredTables");

        jsoda.createRegisteredTables();

        jsodaSdb.createRegisteredTables();

        jsodaDyn.createRegisteredTables();
	}

    public void xx_test_listSdbTables() throws Exception {
        List<String>    tables = jsoda.listNativeTables(DbType.SimpleDB);
        System.out.println("SimpleDB tables: " + ReflectUtil.dumpToStr(tables, ", "));
	}

    public void xx_test_listDynTables() throws Exception {
        List<String>    tables = jsoda.listNativeTables(DbType.DynamoDB);
        System.out.println("DynamoDB tables: " + ReflectUtil.dumpToStr(tables, ", "));
	}

    public void xx_test_put() throws Exception {
        System.out.println("test_put");

        Model1  dataObj1 = new Model1("abc", 25);
        jsodaSdb.dao(Model1.class).put(dataObj1);
        jsodaDyn.dao(Model1.class).put(dataObj1);

        Model2  dataObj2 = new Model2(20, "item20", 20, 20.02);
        jsodaSdb.dao(Model2.class).put(dataObj2);
        jsodaDyn.dao(Model2.class).put(dataObj2);

        Model3  dataObj3 = new Model3(31, "item31", 310);
        jsodaSdb.dao(Model3.class).put(dataObj3);
        jsodaDyn.dao(Model3.class).put(dataObj3);

        jsoda.dao(SdbModel1.class).put(new SdbModel1("abc", 25));
        jsoda.dao(DynModel1.class).put(new DynModel1("abc", 25));
	}

    public void xx_test_batchPut() throws Exception {
        System.out.println("test_batchPut");

        Model1[]    objs1 = new Model1[] { new Model1("aa", 50), new Model1("bb", 51), new Model1("cc", 52) };
        jsodaSdb.dao(Model1.class).batchPut(Arrays.asList(objs1));
        jsodaDyn.dao(Model1.class).batchPut(Arrays.asList(objs1));

        Model2[]    objs2 = new Model2[] { new Model2(1, "p1", 11, 1.1), new Model2(2, "p2", 12, 1.2), new Model2(3, "p3", 13, 1.3) };
        jsodaSdb.dao(Model2.class).batchPut(Arrays.asList(objs2));
        jsodaDyn.dao(Model2.class).batchPut(Arrays.asList(objs2));

        Model3[]    objs3a = new Model3[] { new Model3(1, "item1", 1), new Model3(2, "item2", 2), new Model3(3, "item3", 3) };
        Model3[]    objs3b = new Model3[] { new Model3(2, "item1", 1), new Model3(2, "item2", 2), new Model3(2, "item3", 3) };
        jsodaSdb.dao(Model3.class).batchPut(Arrays.asList(objs3a));
        jsodaDyn.dao(Model3.class).batchPut(Arrays.asList(objs3b));

        jsoda.dao(SdbModel1.class).batchPut(Arrays.asList(
            new SdbModel1[] { new SdbModel1("aa", 50), new SdbModel1("bb", 51), new SdbModel1("cc", 52) } ));

        jsoda.dao(DynModel1.class).batchPut(Arrays.asList(
            new DynModel1[] { new DynModel1("aa", 50), new DynModel1("bb", 51), new DynModel1("cc", 52) } ));
	}

    public void xx_test_get1() throws Exception {
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

        dump( jsodaSdb.dao(Model3.class).get(31) );
        try {
            dump( jsodaSdb.dao(Model3.class).get(31, "item31") );
        } catch(JsodaException expected) {
            // SimpleDB doesn't support composite key retrieval.
        }

        dump( jsodaDyn.dao(Model3.class).get(31, "item31") );
	}

    public void xx_test_cache1() throws Exception {
        System.out.println("test_cache1");

        jsoda = new Jsoda(new BasicAWSCredentials(key, secret), new MemCacheableSimple(1000))
            .setDbEndpoint(DbType.DynamoDB, awsUrl);

        jsoda.getMemCacheable().clearAll();
        dump( jsoda.dao(SdbModel1.class).get("abc") );
        dump( jsoda.dao(SdbModel1.class).get("abc") );
        dump( jsoda.dao(SdbModel1.class).get("abc") );
        dump( jsoda.dao(SdbModel1.class).get("abc") );
        System.out.println(jsoda.getMemCacheable().dumpStats());
        assertThat( jsoda.getMemCacheable().getHits(),   is(3));
        assertThat( jsoda.getMemCacheable().getMisses(), is(1));
	}

    public void xx_test_cache2() throws Exception {
        System.out.println("test_cache2");

        jsoda = new Jsoda(new BasicAWSCredentials(key, secret), new MemCacheableSimple(1000))
            .setDbEndpoint(DbType.DynamoDB, awsUrl);

        jsoda.registerModel(Model3.class, DbType.SimpleDB);
        jsoda.getMemCacheable().clearAll();
        dump( jsoda.dao(Model3.class).get(31) );
        dump( jsoda.dao(Model3.class).get(31) );
        dump( jsoda.dao(Model3.class).get(31) );
        dump( jsoda.dao(Model3.class).get(31) );
        System.out.println(jsoda.getMemCacheable().dumpStats());
        assertThat("SimpleDB cache hit",  jsoda.getMemCacheable().getHits(),   is(3));
        assertThat("SimpleDB cache miss", jsoda.getMemCacheable().getMisses(), is(1));

        jsoda.registerModel(Model3.class, DbType.DynamoDB);
        jsoda.getMemCacheable().clearAll();
        dump( jsoda.dao(Model3.class).get(31, "item31") );
        dump( jsoda.dao(Model3.class).get(31, "item31") );
        dump( jsoda.dao(Model3.class).get(31, "item31") );
        dump( jsoda.dao(Model3.class).get(31, "item31") );
        System.out.println(jsoda.getMemCacheable().dumpStats());
        assertThat("DynamoDB cache hit",  jsoda.getMemCacheable().getHits(),   is(3));
        assertThat("DynamoDB cache miss", jsoda.getMemCacheable().getMisses(), is(1));
	}

    public void xx_test_getNonExist() throws Exception {
        System.out.println("test_getNonExist");

        assertThat( jsodaSdb.dao(Model1.class).get("abc_non_existed"), is(nullValue()) );
        assertThat( jsodaDyn.dao(Model1.class).get("abc_non_existed"), is(nullValue()) );

        assertThat( jsodaSdb.dao(Model2.class).get(20 * -99), is(nullValue()) );
        assertThat( jsodaDyn.dao(Model2.class).get(20L * -99), is(nullValue()) );

        assertThat( jsoda.dao(SdbModel1.class).get("abc_non_existed"), is(nullValue()) );
        assertThat( jsoda.dao(DynModel1.class).get("abc_non_existed"), is(nullValue()) );
        
	}

    public void xx_test_delete() throws Exception {
        System.out.println("test_delete");

        // Create objects to delete
        Model1  dataObj1 = new Model1("abc_delete", 25);
        jsodaSdb.dao(Model1.class).put(dataObj1);
        jsodaDyn.dao(Model1.class).put(dataObj1);

        Model2  dataObj2 = new Model2(5520, "item20_delete", 20, 20.02);
        jsodaSdb.dao(Model2.class).put(dataObj2);
        jsodaDyn.dao(Model2.class).put(dataObj2);

        Model3  dataObj3 = new Model3(5531, "item31_delete", 310);
        jsodaSdb.dao(Model3.class).put(dataObj3);
        jsodaDyn.dao(Model3.class).put(dataObj3);

        jsoda.dao(SdbModel1.class).put(new SdbModel1("abc_delete", 25));
        jsoda.dao(DynModel1.class).put(new DynModel1("abc_delete", 25));

        // Sleep a bit to wait for AWS db's eventual consistence to kick in.
        Thread.sleep(1000);

        jsodaSdb.dao(Model1.class).delete("abc_delete");
        jsodaDyn.dao(Model1.class).delete("abc_delete");

        jsodaSdb.dao(Model2.class).delete(5520);
        jsodaDyn.dao(Model2.class).delete(5520);

        jsodaSdb.dao(Model3.class).delete(5531, "item31_delete");
        jsodaDyn.dao(Model3.class).delete(5531, "item31_delete");

        jsoda.dao(SdbModel1.class).delete("abc_delete", 25);
        jsoda.dao(DynModel1.class).delete("abc_delete", 25);

        // Sleep a bit to wait for AWS db's eventual consistence to kick in.
        Thread.sleep(1000);

        assertThat( jsodaSdb.dao(Model1.class).get("abc_delete"), is(nullValue()) );
        assertThat( jsodaDyn.dao(Model1.class).get("abc_delete"), is(nullValue()) );

        assertThat( jsodaSdb.dao(Model2.class).get(5520), is(nullValue()) );
        assertThat( jsodaDyn.dao(Model2.class).get(5520), is(nullValue()) );

        assertThat( jsodaSdb.dao(Model3.class).get(5531, "item31_delete"), is(nullValue()) );
        assertThat( jsodaDyn.dao(Model3.class).get(5531, "item31_delete"), is(nullValue()) );

        assertThat( jsoda.dao(SdbModel1.class).get("abc_delete"), is(nullValue()) );
        assertThat( jsoda.dao(DynModel1.class).get("abc_delete"), is(nullValue()) );

	}

    public void xx_test_batchDelete() throws Exception {
        System.out.println("test_batchDelete");

        Model1[]    objs1 = new Model1[] { new Model1("aa_delete", 50), new Model1("bb_delete", 51), new Model1("cc_delete", 52) };
        jsodaSdb.dao(Model1.class).batchPut(Arrays.asList(objs1));
        jsodaDyn.dao(Model1.class).batchPut(Arrays.asList(objs1));

        Model2[]    objs2 = new Model2[] { new Model2(551, "p1", 11, 1.1), new Model2(552, "p2", 12, 1.2), new Model2(553, "p3", 13, 1.3) };
        jsodaSdb.dao(Model2.class).batchPut(Arrays.asList(objs2));
        jsodaDyn.dao(Model2.class).batchPut(Arrays.asList(objs2));

        Model3[]    objs3 = new Model3[] { new Model3(551, "item1", 1), new Model3(552, "item2", 2), new Model3(553, "item3", 3) };
        jsodaSdb.dao(Model3.class).batchPut(Arrays.asList(objs3));
        jsodaDyn.dao(Model3.class).batchPut(Arrays.asList(objs3));

        // Sleep a bit to wait for AWS db's eventual consistence to kick in.
        Thread.sleep(1000);

        jsodaSdb.dao(Model1.class).batchDelete(Arrays.asList("aa_delete", "bb_delete", "cc_delete"));
        jsodaDyn.dao(Model1.class).batchDelete(Arrays.asList("aa_delete", "bb_delete", "cc_delete"));

        jsodaSdb.dao(Model2.class).batchDelete(Arrays.asList(551, 552, 553));
        jsodaDyn.dao(Model2.class).batchDelete(Arrays.asList(551, 552, 553));

        jsodaSdb.dao(Model3.class).batchDelete(Arrays.asList(551, 552, 553), Arrays.asList("item1", "item2", "item3"));
        jsodaDyn.dao(Model3.class).batchDelete(Arrays.asList(551, 552, 553), Arrays.asList("item1", "item2", "item3"));

        // Sleep a bit to wait for AWS db's eventual consistence to kick in.
        Thread.sleep(1000);

        assertThat( jsodaSdb.dao(Model1.class).get("aa_delete"), is(nullValue()) );
        assertThat( jsodaSdb.dao(Model1.class).get("bb_delete"), is(nullValue()) );
        assertThat( jsodaSdb.dao(Model1.class).get("cc_delete"), is(nullValue()) );
        assertThat( jsodaDyn.dao(Model1.class).get("aa_delete"), is(nullValue()) );
        assertThat( jsodaDyn.dao(Model1.class).get("bb_delete"), is(nullValue()) );
        assertThat( jsodaDyn.dao(Model1.class).get("cc_delete"), is(nullValue()) );

        assertThat( jsodaSdb.dao(Model2.class).get(551), is(nullValue()) );
        assertThat( jsodaSdb.dao(Model2.class).get(552), is(nullValue()) );
        assertThat( jsodaSdb.dao(Model2.class).get(553), is(nullValue()) );
        assertThat( jsodaDyn.dao(Model2.class).get(551), is(nullValue()) );
        assertThat( jsodaDyn.dao(Model2.class).get(552), is(nullValue()) );
        assertThat( jsodaDyn.dao(Model2.class).get(553), is(nullValue()) );

        assertThat( jsodaSdb.dao(Model3.class).get(551, "item1"), is(nullValue()) );
        assertThat( jsodaSdb.dao(Model3.class).get(552, "item2"), is(nullValue()) );
        assertThat( jsodaSdb.dao(Model3.class).get(553, "item3"), is(nullValue()) );
        
        assertThat( jsodaDyn.dao(Model3.class).get(551, "item1"), is(nullValue()) );
        assertThat( jsodaDyn.dao(Model3.class).get(552, "item2"), is(nullValue()) );
        assertThat( jsodaDyn.dao(Model3.class).get(553, "item3"), is(nullValue()) );

    }

    public void xx_test_select_all() throws Exception {
        System.out.println("\n test_select_all");

        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model3 item : jsodaSdb.query(Model3.class).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model3 item : jsodaDyn.query(Model3.class).run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (SdbModel1 item : jsoda.query(SdbModel1.class).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (DynModel1 item : jsoda.query(DynModel1.class).run())
            dump(item);
	}

    public void xx_test_select_field() throws Exception {
        System.out.println("\n test_select_field");

        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).select("age").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).select("age").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).select("price", "count").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).select("price", "count").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model3 item : jsodaSdb.query(Model3.class).select("age").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model3 item : jsodaDyn.query(Model3.class).select("age").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (SdbModel1 item : jsoda.query(SdbModel1.class).select("age").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (DynModel1 item : jsoda.query(DynModel1.class).select("age").run())
            dump(item);
        
	}

    public void xx_test_select_id() throws Exception {
        System.out.println("\n test_select_id");
        
        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).select("name").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).select("name").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).select("id").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).select("id").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model3 item : jsodaSdb.query(Model3.class).select("id").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model3 item : jsodaDyn.query(Model3.class).select("id", "name").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (SdbModel1 item : jsoda.query(SdbModel1.class).select("name").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (DynModel1 item : jsoda.query(DynModel1.class).select("name").run())
            dump(item);
	}

    public void xx_test_select_id_others() throws Exception {
        System.out.println("\n test_select_id_others");
        
        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).select("name", "age").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).select("name", "age").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).select("id", "count").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).select("id", "count").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model3 item : jsodaSdb.query(Model3.class).select("id", "age").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model3 item : jsodaDyn.query(Model3.class).select("id", "name", "age").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (SdbModel1 item : jsoda.query(SdbModel1.class).select("name", "age").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (DynModel1 item : jsoda.query(DynModel1.class).select("name", "age").run())
            dump(item);
	}

    public void xx_test_filter_comparison() throws Exception {
        System.out.println("\n test_filter_comparison");

        System.out.println("---- SimpleDB eq");
        for (Model1 item : jsodaSdb.query(Model1.class).eq("age", 25).run())
            dump(item);
        System.out.println("---- DynamoDB eq");
        for (Model1 item : jsodaDyn.query(Model1.class).eq("age", 25).run())
            dump(item);

        System.out.println("---- SimpleDB ne");
        for (Model1 item : jsodaSdb.query(Model1.class).ne("age", 25).run())
            dump(item);
        System.out.println("---- DynamoDB ne");
        for (Model1 item : jsodaDyn.query(Model1.class).ne("age", 25).run())
            dump(item);

        System.out.println("---- SimpleDB le");
        for (Model1 item : jsodaSdb.query(Model1.class).le("age", 25).run())
            dump(item);
        System.out.println("---- DynamoDB le");
        for (Model1 item : jsodaDyn.query(Model1.class).le("age", 25).run())
            dump(item);

        System.out.println("---- SimpleDB lt");
        for (Model1 item : jsodaSdb.query(Model1.class).lt("age", 25).run())
            dump(item);
        System.out.println("---- DynamoDB lt");
        for (Model1 item : jsodaDyn.query(Model1.class).lt("age", 25).run())
            dump(item);

        System.out.println("---- SimpleDB ge");
        for (Model1 item : jsodaSdb.query(Model1.class).ge("age", 25).run())
            dump(item);
        System.out.println("---- DynamoDB ge");
        for (Model1 item : jsodaDyn.query(Model1.class).ge("age", 25).run())
            dump(item);

        System.out.println("---- SimpleDB gt");
        for (Model1 item : jsodaSdb.query(Model1.class).gt("age", 25).run())
            dump(item);
        System.out.println("---- DynamoDB gt");
        for (Model1 item : jsodaDyn.query(Model1.class).gt("age", 25).run())
            dump(item);


        System.out.println("---- SimpleDB like");
        for (Model2 item : jsodaSdb.query(Model2.class).like("name", "%item%").run())
            dump(item);
        System.out.println("---- DynamoDB like");
        try {
            jsodaDyn.query(Model2.class).like("name", "%item%").run();
            assertThat("Unsupported method returns", true, is(false));
        } catch(Exception expected) {}

        System.out.println("---- SimpleDB notLike");
        for (Model2 item : jsodaSdb.query(Model2.class).notLike("name", "%item%").run())
            dump(item);
        System.out.println("---- DynamoDB notLike");
        try {
            jsodaDyn.query(Model2.class).notLike("name", "%item%").run();
            assertThat("Unsupported method returns", true, is(false));
        } catch(Exception expected) {}

        System.out.println("---- SimpleDB contains");
        try {
            jsodaSdb.query(Model2.class).contains("name", "item").run();
            assertThat("Unsupported method returns", true, is(false));
        } catch(Exception expected) {}
        System.out.println("---- DynamoDB contains");
        for (Model2 item : jsodaDyn.query(Model2.class).contains("name", "item").run())
            dump(item);

        System.out.println("---- SimpleDB notContains");
        try {
            jsodaSdb.query(Model2.class).notContains("name", "item").run();
            assertThat("Unsupported method returns", true, is(false));
        } catch(Exception expected) {}
        for (Model2 item : jsodaDyn.query(Model2.class).notContains("name", "item").run())
            dump(item);

        System.out.println("---- SimpleDB beginsWith");
        try {
            jsodaSdb.query(Model2.class).beginsWith("name", "p").run();
            assertThat("Unsupported method returns", true, is(false));
        } catch(Exception expected) {}
        System.out.println("---- DynamoDB beginsWith");
        for (Model2 item : jsodaDyn.query(Model2.class).beginsWith("name", "p").run())
            dump(item);


        System.out.println("---- SimpleDB ge");
        for (Model2 item : jsodaSdb.query(Model2.class).ge("price", 1.3).run())
            dump(item);
        System.out.println("---- DynamoDB ge");
        for (Model2 item : jsodaDyn.query(Model2.class).ge("price", 1.3).run())
            dump(item);

        System.out.println("---- SimpleDB eq");
        for (Model3 item : jsodaSdb.query(Model3.class).eq("age", 3).run())
            dump(item);
        System.out.println("---- DynamoDB eq");
        for (Model3 item : jsodaDyn.query(Model3.class).eq("age", 3).run())
            dump(item);

	}

    public void xx_test_filter_between() throws Exception {
        System.out.println("\n test_filter_between");

        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).between("age", 50, 51).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).between("age", 50, 51).run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).between("price", 1.2, 1.3).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).between("price", 1.2, 1.3).run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model3 item : jsodaSdb.query(Model3.class).between("age", 3, 3).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model3 item : jsodaDyn.query(Model3.class).between("age", 3, 3).run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (SdbModel1 item : jsoda.query(SdbModel1.class).between("age", 25, 26).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (DynModel1 item : jsoda.query(DynModel1.class).between("age", 25, 26).run())
            dump(item);
	}

    public void xx_test_filter_id() throws Exception {
        System.out.println("\n test_filter_id");

        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).eq("name", "abc").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).eq("name", "abc").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).eq("id", 20).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).eq("id", 20).run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 31).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 31).run())
            dump(item);
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 31).eq("name", "item31").run())
            dump(item);

	}

    public void xx_test_filter_dynamodb_id_range_query() throws Exception {
        System.out.println("\n test_filter_dynamodb_id_range_query");

        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).eq("name", "item2").run())
            dump(item);
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).gt("name", "item2").run())
            dump(item);
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).ge("name", "item2").run())
            dump(item);
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).lt("name", "item2").run())
            dump(item);
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).le("name", "item2").run())
            dump(item);

	}

    public void xx_test_filter_id_and_others() throws Exception {
        System.out.println("\n test_filter_id_and_others");

        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).eq("name", "abc").eq("age", 25).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).eq("name", "abc").eq("age", 25).run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).eq("id", 3).gt("count", 11).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).eq("id", 3).gt("count", 11).run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 31).eq("name", "item31").eq("age", 310).run())
            dump(item);
        System.out.println("---- DynamoDB");
        try {
            for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 31).eq("name", "item31").eq("age", 310).run())
                dump(item);
            assertThat("Unsupported method returns", true, is(false));
        } catch(Exception expected) {}
	}

    public void test_filter_in() throws Exception {
        System.out.println("\n test_filter_in");

        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).in("name", "bb", "cc").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).in("name", "bb", "cc").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).eq("id", 3).in("count", 11, 12, 13).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).eq("id", 3).in("count", 11, 12, 13).run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 31).in("name", "item31").run())
            dump(item);
        System.out.println("---- DynamoDB");
        try {
            for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 31).in("name", "item31").run())
                dump(item);
            assertThat("Unsupported method returns", true, is(false));
        } catch(Exception expected) {}
	}

    public void xx_test_select_limit() throws Exception {
        System.out.println("\n test_select_limit");

        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).run())
            dump(item);
        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).limit(2).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).limit(2).run())
            dump(item);
        
	}

    public void xx_test_select_count() throws Exception {
        System.out.println("\n test_select_count");

        System.out.println("---- SimpleDB");
        System.out.println(jsodaSdb.query(Model1.class).count());
        System.out.println(jsodaSdb.query(Model1.class).limit(2).count());
        System.out.println("---- DynamoDB");
        System.out.println(jsodaDyn.query(Model1.class).count());
        System.out.println(jsodaDyn.query(Model1.class).limit(2).count());

        System.out.println("---- DynamoDB");
        System.out.println(jsodaDyn.query(Model3.class).eq("id", 2).eq("name", "item2").count());
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).eq("name", "item2").run())
            dump(item);
        System.out.println(jsodaDyn.query(Model3.class).eq("id", 2).gt("name", "item2").count());
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).gt("name", "item2").run())
            dump(item);
        System.out.println(jsodaDyn.query(Model3.class).eq("id", 2).ge("name", "item2").count());
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).ge("name", "item2").run())
            dump(item);
        System.out.println(jsodaDyn.query(Model3.class).eq("id", 2).lt("name", "item2").count());
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).lt("name", "item2").run())
            dump(item);
        System.out.println(jsodaDyn.query(Model3.class).eq("id", 2).le("name", "item2").count());
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).le("name", "item2").run())
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
		utest.TestUtil.runTests(JsodaTest.class);
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

        @Transient                  // Transient field is not stored in database.
        public Date     currtime = new Date();

        public Model2() {}

        public Model2(long id, String name, int count, double price) {
            this.id = id;
            this.name = name;
            this.count = count;
            this.price = price;
        }

    }

    /** Model class for testing composite PK in DynamoDB */
    public static class Model3 implements Serializable {
        @Id                         // Mark this field as the primary key.
        public long     id;

        @ARangeKey                  // Mark this field as the range key for DynamoDB.  No effect on SimpleDB.
        public String   name;

        public int      age;

        public Model3() {}
        public Model3(long id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
    }

    /** Dbtype annotation to use SimpleDB. */
    @AModel(dbtype = DbType.SimpleDB)
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
    @AModel(dbtype = DbType.DynamoDB)
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


}


