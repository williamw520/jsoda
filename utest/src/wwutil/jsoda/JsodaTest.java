/******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.  If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for 
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is: Jsoda
 * The Initial Developer of the Original Code is: William Wong (williamw520@gmail.com)
 * Portions created by William Wong are Copyright (C) 2012 William Wong, All Rights Reserved.
 *
 ******************************************************************************/


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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;

import wwutil.sys.ReflectUtil;
import wwutil.sys.FnUtil;
import wwutil.model.MemCacheableSimple;
import wwutil.model.annotation.Key;
import wwutil.model.annotation.PrePersist;
import wwutil.model.annotation.PreValidation;
import wwutil.model.annotation.PostLoad;
import wwutil.model.annotation.DbType;
import wwutil.model.annotation.Model;
import wwutil.model.annotation.AttrName;
import wwutil.model.annotation.CacheByField;
import wwutil.model.annotation.DefaultGUID;
import wwutil.model.annotation.DefaultComposite;
import wwutil.model.annotation.VersionLocking;
import wwutil.model.annotation.ModifiedTime;
import wwutil.model.annotation.CachePolicy;
import wwutil.model.annotation.ToUpper;
import wwutil.model.annotation.ToLower;
import wwutil.model.annotation.Trim;
import wwutil.model.annotation.RemoveChar;
import wwutil.model.annotation.RemoveAlphaDigits;
import wwutil.model.annotation.MaxValue;
import wwutil.model.annotation.MinValue;
import wwutil.model.annotation.AbsValue;
import wwutil.model.annotation.CeilValue;
import wwutil.model.annotation.FloorValue;
import wwutil.model.annotation.MaxSize;
import wwutil.model.annotation.MinSize;
import wwutil.model.annotation.MaskMatch;
import wwutil.model.annotation.EmailMatch;
import wwutil.model.annotation.S3Field;
import wwutil.model.annotation.FormatMsg;

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
        jsodaSdb.registerModel(Model4.class, DbType.SimpleDB);
        jsodaSdb.registerModel(Model5.class, DbType.SimpleDB);
        jsodaSdb.registerModel(Model6.class, DbType.SimpleDB);
        jsodaSdb.registerModel(Model7.class, DbType.SimpleDB);

        // Set up a Jsoda for testing the same models in DynamoDB
        jsodaDyn = new Jsoda(new BasicAWSCredentials(key, secret))
            .setDbEndpoint(DbType.DynamoDB, awsUrl);
        jsodaDyn.registerModel(Model1.class, DbType.DynamoDB);
        jsodaDyn.registerModel(Model2.class, DbType.DynamoDB);
        jsodaDyn.registerModel(Model3.class, DbType.DynamoDB);
        jsodaDyn.registerModel(Model4.class, DbType.DynamoDB);
        jsodaDyn.registerModel(Model5.class, DbType.DynamoDB);
        jsodaDyn.registerModel(Model6.class, DbType.DynamoDB);
        jsodaDyn.registerModel(Model7.class, DbType.DynamoDB);

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

    public void xx_test_registration_inherited() throws Exception {
        System.out.println("test_registration_force_inherited");

        Jsoda   jsoda = new Jsoda(new BasicAWSCredentials(key, secret));
        String  modelName;

        // Register non-annotated model class as SimpleDB
        jsoda.registerModel(Model4.class, DbType.SimpleDB);
        modelName = jsoda.getModelName(Model4.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(DbType.SimpleDB) ));

        // Register non-annotated model class as DynamoDB
        jsoda.registerModel(Model4.class, DbType.DynamoDB);
        modelName = jsoda.getModelName(Model4.class);
        assertThat(jsoda.getDb(modelName).getDbType(),
                   allOf( notNullValue(), is(DbType.DynamoDB) ));

	}

    public void xx_test_registration_composite_key() throws Exception {
        System.out.println("test_registration_composite_key");

        Jsoda   jsoda = new Jsoda(new BasicAWSCredentials(key, secret));
        String  modelName;

        // Register SimpleDB model class with composite key
        jsoda.registerModel(Model3.class, DbType.SimpleDB);
        modelName = jsoda.getModelName(Model3.class);

        assertThat(jsoda.getIdField(modelName),
                   allOf( notNullValue(), instanceOf(Field.class) ));
        assertThat(jsoda.getIdField(modelName).getName(),
                   allOf( notNullValue(), is("id") ));
        assertThat(jsoda.getRangeField(modelName),
                   allOf( notNullValue() ));
        assertThat(jsoda.getRangeField(modelName).getName(),
                   allOf( notNullValue(), is("name") ));

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
            assertThat("Generic model without dbtype cannot be auto-registered", false, is(true));
        } catch(JsodaException expected) {
            System.out.println("Expected: " + expected);
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

    public void xx_test_registration_cachepolicy() throws Exception {
        System.out.println("test_registration_cachepolicy");
        Jsoda   jsoda = new Jsoda(new BasicAWSCredentials(key, secret));

        try {
            jsoda.registerModel(InvalidModel1.class, DbType.SimpleDB);
            assertThat(false, is(true));
        } catch(JsodaException expected) {
            System.out.println("Expected: " + expected);
        }

    }

    public void xx_test_setDbEndpoint() throws Exception {
        System.out.println("test_setDbEndpoint");

        Jsoda   jsoda = new Jsoda(new BasicAWSCredentials(key, secret));

        assertThat(jsoda.getDbEndpoint(DbType.DynamoDB),
                   nullValue());

        jsoda.setDbEndpoint(DbType.DynamoDB, "abc");
        assertThat(jsoda.getDbEndpoint(DbType.DynamoDB),
                   is("abc"));

        jsoda.setDbEndpoint(DbType.SimpleDB, "foobar");
        assertThat(jsoda.getDbEndpoint(DbType.SimpleDB),
                   is("foobar"));

    }

    public void xx_test_setGlobalPrefix() throws Exception {
        System.out.println("test_setGlobalPrefix");

        Jsoda   jsoda = new Jsoda(new BasicAWSCredentials(key, secret));

        assertThat(jsoda.getGlobalPrefix(),
                   nullValue());

        jsoda.setGlobalPrefix("foobar_");
        assertThat(jsoda.getGlobalPrefix(),
                   is("foobar_"));

        jsoda.registerModel(Model1.class, DbType.SimpleDB);
        jsoda.registerModel(Model2.class, DbType.DynamoDB);
        assertThat(jsoda.getModelTable(Model1.class),
                   is("foobar_Model1"));
        assertThat(jsoda.getModelTable(Model2.class),
                   is("foobar_TestModel2"));

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
        // jsodaSdb.deleteModelTable(Model4.class);
        // jsodaSdb.deleteModelTable(Model5.class);
        // jsodaSdb.deleteModelTable(Model6.class);
        // jsodaSdb.deleteModelTable(Model7.class);

        // jsodaDyn.deleteModelTable(Model1.class);
        // jsodaDyn.deleteModelTable(Model2.class);
        // jsodaDyn.deleteModelTable(Model3.class);
        // jsodaDyn.deleteModelTable(Model4.class);
        // jsodaDyn.deleteModelTable(Model5.class);
        // jsodaDyn.deleteModelTable(Model6.class);
        // jsodaDyn.deleteModelTable(Model7.class);

        // Tables take a while to be completely deleted.
        Thread.sleep(15000);

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

        // Comment and uncomment the following to create each individual table in the databases.
        
        jsoda.createModelTable(SdbModel1.class);
        jsoda.createModelTable(DynModel1.class);

        jsodaSdb.createModelTable(Model1.class);
        jsodaSdb.createModelTable(Model2.class);
        jsodaSdb.createModelTable(Model3.class);
        jsodaSdb.createModelTable(Model4.class);
        jsodaSdb.createModelTable(Model5.class);
        jsodaSdb.createModelTable(Model7.class);

        jsodaDyn.createModelTable(Model1.class);
        jsodaDyn.createModelTable(Model2.class);
        jsodaDyn.createModelTable(Model3.class);
        jsodaDyn.createModelTable(Model4.class);
        jsodaDyn.createModelTable(Model5.class);
        jsodaDyn.createModelTable(Model7.class);

        Thread.sleep(10000);
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
        dataObj2.day = Day.MONDAY;
        jsodaSdb.dao(Model2.class).put(dataObj2);
        jsodaDyn.dao(Model2.class).put(dataObj2);

        Model2  dataObj2b = new Model2(30, null, 20, 20.02);
        dataObj2b.day = Day.TUESDAY;
        jsodaSdb.dao(Model2.class).put(dataObj2b);
        jsodaDyn.dao(Model2.class).put(dataObj2b);

        Model3  dataObj3 = new Model3(31, "item31", 310,
                                      new HashSet<String>(Arrays.asList("sock1", "sock2", "sock3")),
                                      new HashSet<Long>(Arrays.asList(8L, 9L, 10L)));
        jsodaSdb.dao(Model3.class).put(dataObj3);
        jsodaDyn.dao(Model3.class).put(dataObj3);

        Model4  dataObj4 = new Model4("abc", 25, "111-25-1111");
        jsodaSdb.dao(Model4.class).put(dataObj4);
        jsodaDyn.dao(Model4.class).put(dataObj4);
        
        jsoda.dao(SdbModel1.class).put(new SdbModel1("abc", 25));
        jsoda.dao(DynModel1.class).put(new DynModel1("abc", 25));
	}

    public void xx_test_get() throws Exception {
        System.out.println("test_get");

        dump( jsodaSdb.dao(Model1.class).get("abc") );
        dump( jsodaDyn.dao(Model1.class).get("abc") );

        dump( jsodaSdb.dao(Model2.class).get(20) );
        dump( jsodaDyn.dao(Model2.class).get(20L) );

        dump( jsodaSdb.dao(Model2.class).get(30L) );
        dump( jsodaDyn.dao(Model2.class).get(30) );

        dump( jsodaSdb.dao(Model3.class).get(31, "item31") );
        dump( jsodaDyn.dao(Model3.class).get(31, "item31") );

        dump( jsodaSdb.dao(Model4.class).get("abc") );
        dump( jsodaDyn.dao(Model4.class).get("abc") );

        dump( jsoda.dao(SdbModel1.class).get("abc") );
        dump( jsoda.dao(DynModel1.class).get("abc") );

	}

    public void xx_test_getCompositePk() throws Exception {
        System.out.println("test_getCompositePk");

        dump( jsodaSdb.dao(Model3.class).get(31, "item31") );
        dump( jsodaDyn.dao(Model3.class).get(31, "item31") );

        try {
            dump( jsodaSdb.dao(Model3.class).get(31) );
            assertThat(false, is(true));
        } catch(JsodaException expected) {
            System.out.println("Expected: " + expected);
        }
        try {
            dump( jsodaDyn.dao(Model3.class).get(31) );
            assertThat(false, is(true));
        } catch(JsodaException expected) {
            System.out.println("Expected: " + expected);
        }
        
	}

    public void xx_test_batchPut() throws Exception {
        System.out.println("test_batchPut");

        Model1[]    objs1 = new Model1[] { new Model1("aa", 50), new Model1("bb", 51), new Model1("cc", 52) };
        jsodaSdb.dao(Model1.class).batchPut(objs1);
        jsodaDyn.dao(Model1.class).batchPut(objs1);

        jsodaSdb.dao(Model1.class).batchPut( new Model1("aa", 50), new Model1("bb", 51), new Model1("cc", 52) );
        jsodaDyn.dao(Model1.class).batchPut( new Model1("aa", 50), new Model1("bb", 51), new Model1("cc", 52) );
        
        Model2[]    objs2 = new Model2[] { new Model2(1, "p1", 11, 1.1), new Model2(2, "p2", 12, 1.2), new Model2(3, "p3", 13, 1.3) };
        jsodaSdb.dao(Model2.class).batchPut(Arrays.asList(objs2));
        jsodaDyn.dao(Model2.class).batchPut(Arrays.asList(objs2));

        Model3[]    objs3b = new Model3[] { new Model3(2, "item1", 1,
                                                       new HashSet<String>(Arrays.asList("item1sock1", "item1sock2")),
                                                       new HashSet<Long>(Arrays.asList(101L, 102L, 103L))),
                                            new Model3(2, "item2", 2,
                                                       new HashSet<String>(Arrays.asList("item2sock1", "item2sock2")),
                                                       new HashSet<Long>(Arrays.asList(201L, 202L, 203L))),
                                            new Model3(2, "item3", 3, null, null) };
        jsodaSdb.dao(Model3.class).batchPut(Arrays.asList(objs3b));
        jsodaDyn.dao(Model3.class).batchPut(Arrays.asList(objs3b));

        Model4[]    objs4 = new Model4[] { new Model4("aa", 50, "111-50-1111"), new Model4("bb", 54, "111-54-1111"), new Model4("cc", 52, "111-52-1111") };
        jsodaSdb.dao(Model4.class).batchPut(Arrays.asList(objs4));
        jsodaDyn.dao(Model4.class).batchPut(Arrays.asList(objs4));

        
        jsoda.dao(SdbModel1.class).batchPut(Arrays.asList(
            new SdbModel1[] { new SdbModel1("aa", 50), new SdbModel1("bb", 51), new SdbModel1("cc", 52) } ));

        jsoda.dao(DynModel1.class).batchPut(Arrays.asList(
            new DynModel1[] { new DynModel1("aa", 50), new DynModel1("bb", 51), new DynModel1("cc", 52) } ));
	}

    public void xx_test_batchPut_large_set() throws Exception {
        System.out.println("test_batchPut_large_set");

        List<Model1>    objs1 = new ArrayList<Model1>();

        for (int i = 0; i < 1000; i++) {
            objs1.add(new Model1("item_" + i, i));
        }
        
        jsodaSdb.dao(Model1.class).batchPut(objs1);
        jsodaDyn.dao(Model1.class).batchPut(objs1);
	}

    public void xx_test_cache1() throws Exception {
        System.out.println("test_cache1");

        jsoda = new Jsoda(new BasicAWSCredentials(key, secret), new MemCacheableSimple(1000))
            .setDbEndpoint(DbType.DynamoDB, awsUrl);

        System.out.println("Class has caching");
        jsoda.getMemCacheable().clearAll();
        jsoda.registerModel(Model1.class, DbType.SimpleDB);
        dump( jsoda.dao(Model1.class).get("abc") );
        dump( jsoda.dao(Model1.class).get("abc") );
        dump( jsoda.dao(Model1.class).get("abc") );
        dump( jsoda.dao(Model1.class).get("abc") );
        System.out.println(jsoda.getMemCacheable().dumpStats());
        assertThat( jsoda.getMemCacheable().getHits(),   is(3));
        assertThat( jsoda.getMemCacheable().getMisses(), is(1));

        System.out.println("Class has no caching");
        jsoda.getMemCacheable().clearAll();
        dump( jsoda.dao(SdbModel1.class).get("abc") );
        dump( jsoda.dao(SdbModel1.class).get("abc") );
        dump( jsoda.dao(SdbModel1.class).get("abc") );
        dump( jsoda.dao(SdbModel1.class).get("abc") );
        System.out.println(jsoda.getMemCacheable().dumpStats());
        assertThat( jsoda.getMemCacheable().getHits(),   is(0));
        assertThat( jsoda.getMemCacheable().getMisses(), is(4));

	}

    public void xx_test_cache2() throws Exception {
        System.out.println("test_cache2");

        jsoda = new Jsoda(new BasicAWSCredentials(key, secret), new MemCacheableSimple(1000))
            .setDbEndpoint(DbType.DynamoDB, awsUrl);

        jsoda.registerModel(Model3.class, DbType.SimpleDB);
        jsoda.getMemCacheable().clearAll();
        dump( jsoda.dao(Model3.class).get(31, "item31") );
        dump( jsoda.dao(Model3.class).get(31, "item31") );
        dump( jsoda.dao(Model3.class).get(31, "item31") );
        dump( jsoda.dao(Model3.class).get(31, "item31") );
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

        Model3  dataObj3 = new Model3(5531, "item31_delete", 310, null, null);
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

        try {
            jsodaSdb.dao(Model3.class).delete(5531);
            assertThat(false, is(true));
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }
        try {
            jsodaDyn.dao(Model3.class).delete(5531);
            assertThat(false, is(true));
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }

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
        jsodaSdb.dao(Model1.class).batchPut(objs1);
        jsodaDyn.dao(Model1.class).batchPut(objs1);

        Model2[]    objs2 = new Model2[] { new Model2(551, "p1", 11, 1.1), new Model2(552, "p2", 12, 1.2), new Model2(553, "p3", 13, 1.3) };
        jsodaSdb.dao(Model2.class).batchPut(Arrays.asList(objs2));
        jsodaDyn.dao(Model2.class).batchPut(Arrays.asList(objs2));

        Model3[]    objs3 = new Model3[] { new Model3(551, "item1", 1, null, null), new Model3(552, "item2", 2, null, null), new Model3(553, "item3", 3, null, null) };
        jsodaSdb.dao(Model3.class).batchPut(Arrays.asList(objs3));
        jsodaDyn.dao(Model3.class).batchPut(Arrays.asList(objs3));

        // Sleep a bit to wait for AWS db's eventual consistence to kick in.
        Thread.sleep(1000);

        jsodaSdb.dao(Model1.class).batchDelete("aa_delete", "bb_delete", "cc_delete");
        jsodaDyn.dao(Model1.class).batchDelete("aa_delete", "bb_delete", "cc_delete");

        jsodaSdb.dao(Model2.class).batchDelete(551, 552, 553);
        jsodaDyn.dao(Model2.class).batchDelete(551, 552, 553);

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

    public void xx_test_put_loop() throws Exception {
        System.out.println("test_put_loop");

        // This could take a while.
        int     n = 1000;
        for (int i = 0; i < n; i++) {
            Model1  dataObj1 = new Model1("loop" + i, 1000 + i);
            jsodaSdb.dao(Model1.class).put(dataObj1);
            jsodaDyn.dao(Model1.class).put(dataObj1);
        }
    }

    public void xx_test_select_loop() throws Exception {
        System.out.println("\n test_select_loop");

        Query<Model1>   q1;
        System.out.println("---- SimpleDB");
        q1 = jsodaSdb.query(Model1.class);
        while (q1.hasNext()) {
            System.out.println("---- SimpleDB batch");
            for (Model1 item : q1.run()) {
                dump(item);
            }
        }

        List<Model1>    items;
        q1.reset();
        while ((items = q1.run()).size() > 0) {
            for (Model1 item : items)
                dump(item);
        }
        
        System.out.println("---- DynamoDB");
        q1 = jsodaDyn.query(Model1.class);
        while (q1.hasNext()) {
            System.out.println("---- DynamoDB batch");
            for (Model1 item : q1.run()) {
                //dump(item);
            }
        }
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
        for (Model3 item : jsodaSdb.query(Model3.class).select("id", "name").run())
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

    public void xx_test_select_composite_id_part() throws Exception {
        System.out.println("\n test_select_composite_id_part");
        
        System.out.println("---- SimpleDB");
        for (Model3 item : jsodaSdb.query(Model3.class).select("id").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model3 item : jsodaDyn.query(Model3.class).select("id").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model3 item : jsodaSdb.query(Model3.class).select("name").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model3 item : jsodaDyn.query(Model3.class).select("name").run())
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
        for (Model3 item : jsodaSdb.query(Model3.class).select("id", "name", "age").run())
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

        System.out.println("---- SimpleDB eq 25");
        for (Model1 item : jsodaSdb.query(Model1.class).eq("age", 25).run())
            dump(item);
        System.out.println("---- DynamoDB eq");
        for (Model1 item : jsodaDyn.query(Model1.class).eq("age", 25).run())
            dump(item);

        System.out.println("---- SimpleDB ne 25");
        for (Model1 item : jsodaSdb.query(Model1.class).ne("age", 25).run())
            dump(item);
        System.out.println("---- DynamoDB ne");
        for (Model1 item : jsodaDyn.query(Model1.class).ne("age", 25).run())
            dump(item);

        System.out.println("---- SimpleDB le 25");
        for (Model1 item : jsodaSdb.query(Model1.class).le("age", 25).run())
            dump(item);
        System.out.println("---- DynamoDB le 25");
        for (Model1 item : jsodaDyn.query(Model1.class).le("age", 25).run())
            dump(item);

        System.out.println("---- SimpleDB lt 25");
        for (Model1 item : jsodaSdb.query(Model1.class).lt("age", 25).run())
            dump(item);
        System.out.println("---- DynamoDB lt 25");
        for (Model1 item : jsodaDyn.query(Model1.class).lt("age", 25).run())
            dump(item);

        System.out.println("---- SimpleDB ge 25");
        for (Model1 item : jsodaSdb.query(Model1.class).ge("age", 25).run())
            dump(item);
        System.out.println("---- DynamoDB ge 25");
        for (Model1 item : jsodaDyn.query(Model1.class).ge("age", 25).run())
            dump(item);

        System.out.println("---- SimpleDB gt 25");
        for (Model1 item : jsodaSdb.query(Model1.class).gt("age", 25).run())
            dump(item);
        System.out.println("---- DynamoDB gt 25");
        for (Model1 item : jsodaDyn.query(Model1.class).gt("age", 25).run())
            dump(item);


        System.out.println("---- SimpleDB like");
        for (Model2 item : jsodaSdb.query(Model2.class).like("name", "%item%").run())
            dump(item);
        System.out.println("---- DynamoDB like");
        try {
            jsodaDyn.query(Model2.class).like("name", "%item%").run();
            assertThat("Unsupported method returns", true, is(false));
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }

        System.out.println("---- SimpleDB not_like");
        for (Model2 item : jsodaSdb.query(Model2.class).not_like("name", "%item%").run())
            dump(item);
        System.out.println("---- DynamoDB not_like");
        try {
            jsodaDyn.query(Model2.class).not_like("name", "%item%").run();
            assertThat("Unsupported method returns", true, is(false));
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }

        System.out.println("---- SimpleDB contains");
        try {
            jsodaSdb.query(Model2.class).contains("name", "item").run();
            assertThat("Unsupported method returns", true, is(false));
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }
        System.out.println("---- DynamoDB contains");
        for (Model2 item : jsodaDyn.query(Model2.class).contains("name", "item").run())
            dump(item);

        System.out.println("---- SimpleDB not_contains");
        try {
            jsodaSdb.query(Model2.class).not_contains("name", "item").run();
            assertThat("Unsupported method returns", true, is(false));
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }
        for (Model2 item : jsodaDyn.query(Model2.class).not_contains("name", "item").run())
            dump(item);

        System.out.println("---- SimpleDB begins_with");
        try {
            jsodaSdb.query(Model2.class).begins_with("name", "p").run();
            assertThat("Unsupported method returns", true, is(false));
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }
        System.out.println("---- DynamoDB begins_with");
        for (Model2 item : jsodaDyn.query(Model2.class).begins_with("name", "p").run())
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

        System.out.println("---- SimpleDB composite:id");
        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 31).run())
            dump(item);
        System.out.println("---- DynamoDB composite:id");
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 31).run())
            dump(item);

        System.out.println("---- SimpleDB composite:name");
        for (Model3 item : jsodaSdb.query(Model3.class).eq("name", "item31").run())
            dump(item);
        System.out.println("---- DynamoDB composite:name");
        for (Model3 item : jsodaDyn.query(Model3.class).eq("name", "item31").run())
            dump(item);

        System.out.println("---- SimpleDB composite:id/name");
        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 31).eq("name", "item31").run())
            dump(item);
        System.out.println("---- DynamoDB composite:id/name");
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 31).eq("name", "item31").run())
            dump(item);

        System.out.println("---- SimpleDB composite:id multivalue");
        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 2).run())
            dump(item);
        System.out.println("---- DynamoDB composite:id multivalue");
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).run())
            dump(item);

        System.out.println("---- SimpleDB composite:name multivalue");
        for (Model3 item : jsodaSdb.query(Model3.class).eq("name", "item2").run())
            dump(item);
        System.out.println("---- DynamoDB composite:name multivalue");
        for (Model3 item : jsodaDyn.query(Model3.class).eq("name", "item2").run())
            dump(item);

        System.out.println("---- SimpleDB composite:id/name multivalue");
        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 2).eq("name", "item2").run())
            dump(item);
        System.out.println("---- DynamoDB composite:id/name multivalue");
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).eq("name", "item2").run())
            dump(item);

	}

    public void xx_test_filter_dynamodb_id_range_query() throws Exception {
        System.out.println("\n test_filter_dynamodb_id_range_query");

        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 2).eq("name", "item2").run())
            dump(item);
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).eq("name", "item2").run())
            dump(item);
        
        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 2).gt("name", "item2").run())
            dump(item);
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).gt("name", "item2").run())
            dump(item);
        
        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 2).ge("name", "item2").run())
            dump(item);
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).ge("name", "item2").run())
            dump(item);
        
        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 2).lt("name", "item2").run())
            dump(item);
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).lt("name", "item2").run())
            dump(item);
        
        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 2).le("name", "item2").run())
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
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }

	}

    public void xx_test_filter_in() throws Exception {
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
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }

	}

    public void xx_test_filter_null() throws Exception {
        System.out.println("\n test_filter_null");

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).is_null("name").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).is_null("name").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).is_not_null("name").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).is_not_null("name").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).is_null("nullLong").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).is_null("nullLong").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).is_null("nullInt").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).is_null("nullInt").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).is_null("nullFloat").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).is_null("nullFloat").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).is_null("nullDouble").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).is_null("nullDouble").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).is_null("nullStringSet").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).is_null("nullStringSet").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).is_null("nullIntSet").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model2 item : jsodaDyn.query(Model2.class).is_null("nullIntSet").run())
            dump(item);

	}

    public void xx_test_select_limit() throws Exception {
        System.out.println("\n test_select_limit");

        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).limit(2).run())
            dump(item);
        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).limit(3).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).limit(2).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).limit(3).run())
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

    public void xx_test_order_by() throws Exception {
        System.out.println("\n test_order_by");

        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).ne("age", 0).limit(10).order_by("age").run())
            dump(item);
        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).ne("age", 0).limit(10).order_by_desc("age").run())
            dump(item);
        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).is_not_null("name").order_by("name").run())
            dump(item);
        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).is_not_null("name").order_by_desc("name").run())
            dump(item);
        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).is_not_null("price").order_by("price").run())
            dump(item);
        System.out.println("---- SimpleDB");
        for (Model2 item : jsodaSdb.query(Model2.class).is_not_null("price").order_by_desc("price").run())
            dump(item);

        System.out.println("---- SimpleDB");
        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 2).ge("name", "item1").order_by("name").run())
            dump(item);
        System.out.println("---- SimpleDB");
        for (Model3 item : jsodaSdb.query(Model3.class).eq("id", 2).ge("name", "item1").order_by_desc("name").run())
            dump(item);
        
        System.out.println("---- DynamoDB");
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).ge("name", "item1").order_by("name").run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).ge("name", "item1").order_by_desc("name").run())
            dump(item);
        
        System.out.println("---- DynamoDB");
        try {
            for (Model1 item : jsodaDyn.query(Model1.class).order_by("age").run())
                dump(item);
            assertThat("Unsupported method returns", true, is(false));
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }

	}

    public void xx_test_consistent_read() throws Exception {
        System.out.println("\n test_consistent_read");

        // Monitor the AWS transmit log to check for the consistentRead flag
        
        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).consistentRead(true).run())
            dump(item);
        System.out.println("---- SimpleDB");
        for (Model1 item : jsodaSdb.query(Model1.class).consistentRead(false).run())
            dump(item);

        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).consistentRead(true).run())
            dump(item);
        System.out.println("---- DynamoDB");
        for (Model1 item : jsodaDyn.query(Model1.class).consistentRead(false).run())
            dump(item);

        System.out.println("---- DynamoDB");
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).ge("name", "item1").consistentRead(true).run())
            dump(item);
        for (Model3 item : jsodaDyn.query(Model3.class).eq("id", 2).ge("name", "item1").consistentRead(false).run())
            dump(item);
        
	}

    public void xx_test_version_locking() throws Exception {
        System.out.println("\n test_version_locking");

        Dao<Model5> daoSdb = jsodaSdb.dao(Model5.class);
        Dao<Model5> daoDyn = jsodaDyn.dao(Model5.class);
        Model5      dataObj5a;
        Model5      dataObj5b;


        System.out.println("Delete to reset to initial state.");
        daoSdb.delete("5a");
        daoDyn.delete("5b");
        Thread.sleep(500);

        System.out.println("Update non-existing version N before object exists.");
        dataObj5a = new Model5("5a");
        dataObj5a.myVersion = 500;
        dump(dataObj5a);
        try {
            daoSdb.put(dataObj5a);
            assertThat("Should not return", true, is(false));
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }
        dump(dataObj5a);

        dataObj5b = new Model5("5b");
        dataObj5b.myVersion = 500;
        dump(dataObj5b);
        try {
            daoDyn.put(dataObj5b);
            assertThat("Should not return", true, is(false));
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }
        dump(dataObj5b);


        System.out.println("Create version 1.");
        dataObj5a = new Model5("5a");
        dump(dataObj5a);
        daoSdb.put(dataObj5a);
        dump(dataObj5a);

        dataObj5b = new Model5("5b");
        dump(dataObj5b);
        daoDyn.put(dataObj5b);
        dump(dataObj5b);
        
        System.out.println("Load version 1.");
        Thread.sleep(500);
        dataObj5a = daoSdb.get("5a");
        dump(dataObj5a);
        dataObj5b = daoDyn.get("5b");
        dump(dataObj5b);


        System.out.println("Update version 2.");
        dataObj5a.age = 20;
        dump(dataObj5a);
        daoSdb.put(dataObj5a);
        dump(dataObj5a);

        dataObj5b.age = 20;
        dump(dataObj5b);
        daoDyn.put(dataObj5b);
        dump(dataObj5b);


        System.out.println("Load version 2.");
        Thread.sleep(500);
        dataObj5a = daoSdb.get("5a");
        dump(dataObj5a);
        dataObj5b = daoDyn.get("5b");
        dump(dataObj5b);


        System.out.println("Update old version 1.");
        dataObj5a = new Model5("5a");
        dataObj5a.myVersion = 1;
        dump(dataObj5a);
        try {
            daoSdb.put(dataObj5a);
            assertThat("Should not return", true, is(false));
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }
        dump(dataObj5a);

        dataObj5b = new Model5("5b");
        dataObj5b.myVersion = 1;
        dump(dataObj5b);
        try {
            daoDyn.put(dataObj5b);
            assertThat("Should not return", true, is(false));
        } catch(Exception expected) {
            System.out.println("Expected: " + expected);
        }
        dump(dataObj5b);
	}

    public void xx_test_dump() throws Exception {
        System.out.println(Jsoda.dump(new Model1("abc", 25)));
        System.out.println(Jsoda.dump(new Model2(123, "p123", 13, 1.3)));
        System.out.println(Jsoda.dump(new Model3(2, "item2", 2,
                                                 new HashSet<String>(Arrays.asList("item2sock1", "item2sock2")),
                                                 new HashSet<Long>(Arrays.asList(201L, 202L, 203L)))));
        System.out.println(Jsoda.dump(new Model4("cc", 52, "111-52-1111")));

        Model6  model6 = new Model6();
        model6.name = "model6name";
        model6.model3 = new Model3(2, "item2", 2,
                                   new HashSet<String>(Arrays.asList("item2sock1", "item2sock2")),
                                   new HashSet<Long>(Arrays.asList(201L, 202L, 203L)));
        System.out.println(Jsoda.dump(model6));

    }

    public void test_data_annotations() throws Exception {
        jsodaSdb.registerModel(Model6.class, DbType.SimpleDB);
        Model6  model6 = new Model6();
        model6.name = "model6name";
        model6.model3 = new Model3(2, "item2", 2,
                                   new HashSet<String>(Arrays.asList("item2sock1", "item2sock2")),
                                   new HashSet<Long>(Arrays.asList(201L, 202L, 203L)));
        jsodaSdb.preStoreSteps(model6);
        System.out.println(Jsoda.dump(model6));

        jsodaSdb.postLoadSteps(model6);
        System.out.println(Jsoda.dump(model6));

    }

    public void xx_test_s3fields() throws Exception {

        jsodaSdb.registerModel(Model7.class, DbType.SimpleDB);
        Model7  model7 = new Model7("name1", 30);
        model7.colors = new String[] {"Red", "Green", "Blue", "Red", "Blue", "Red", "Blue"};
        model7.books = FnUtil.asMap("book1", "Lord of the Rings", "book2", "Ender's Game");
        System.out.println(Jsoda.dump(model7));

        jsodaSdb.dao(Model7.class).put(model7);

        Model7  model7a = jsodaSdb.dao(Model7.class).get("name1");
        System.out.println(Jsoda.dump(model7a));

    }

    public void xx_test_s3fields_delete() throws Exception {

        jsodaSdb.registerModel(Model7.class, DbType.SimpleDB);
        jsodaSdb.dao(Model7.class).delete("name1");
    }

    public void xx_test_eutil() throws Exception {

        jsodaSdb.registerModel(Model7.class, DbType.SimpleDB);
        Model7  model7 = new Model7("name1", 30);
        model7.colors = new String[] {"Red", "Green", "Blue", "Red", "Blue", "Red", "Blue"};
        model7.books = FnUtil.asMap("book1", "Lord of the Rings", "book2", "Ender's Game");

        Map<String, Object> valueMap = jsodaSdb.eutil(Model7.class).asValueMap(model7);
        System.out.println(ReflectUtil.mapToStr(valueMap));

        System.out.println(jsodaSdb.eutil(Model7.class).getIdValue(model7));

        EUtil   eutil = jsodaSdb.eutil(Model7.class);

        System.out.println(eutil.getAnnotationValue("colors", S3Field.class, "s3Bucket", "none"));
        System.out.println(eutil.getAnnotationValue("colors", S3Field.class, "s3BucketNone", "none"));
        System.out.println(eutil.getAnnotationValue("colors", S3Field.class, "storeAs", 99));
        System.out.println(eutil.getAnnotationValue("colors", S3Field.class, "gzip", false));
        System.out.println(eutil.getAnnotationValue("books", S3Field.class, "gzip", false));

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
        @Key                            // Mark this field as the primary key.
        public String       name;       // String type PK.

        public int          age;

        public Model1() {}
        public Model1(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    public static enum Day {
        SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY;
    }

    /** Generic data model class to be stored in SimpleDB or DynamoDB.
     * Since no dbtype in the AModel annotation is specified, dbtype is required at model registration.
     * Model class is Serializable so that it can be stored in the cache service.
     * Use a different table name in the underlying DB, rather than using its class name as table name.
     */
    @Model(table = "TestModel2")        // Specify a table name for this model class.
    public static class Model2 implements Serializable {
        @Key                            // PK.  When cache service is enabled, objects are always cached by its PK.
        public long         id;         // Long type PK.

        @CacheByField                   // Additional field to cache the object.
        public String       name;       // Find-by-field Dao.findBy() will look up object by its field value in cache first.

        @AttrName("MaxCount")           // Specify the attribute name to use for this field in the underlying DB table.
        public int          count;

        public double       price;

        public String       note;

        public String       guid;

        public Day          day;        // test enum type

        @ModifiedTime                   // Auto-fill the Date field with current time when put.
        public Date         mdate;

        public Long         nullLong;
        public Integer      nullInt;
        public Float        nullFloat;
        public Double       nullDouble;
        public String       nullString;
        public Set<String>  nullStringSet;
        public Set<Integer> nullIntSet;
        
        public transient Date   currtime = new Date();    // Transient field is not stored in database.

        public transient double price3;


        public Model2() {}

        public Model2(long id, String name, int count, double price) {
            this.id = id;
            this.name = name;
            this.count = count;
            this.price = price;
        }

        @PrePersist
        public void myPrePersist() {
            System.out.println("myPrePersist id: " + id);
            note = "Auto fill this note with " + name + " has paid " + price;
        }

        @PreValidation
        public void myPreValidation() {
            System.out.println("myPreValidation id: " + id);
        }

        @PostLoad
        public void myPostLoad() {
            // System.out.println("myPostLoad id: " + id);
            // Triple the transient price3 in PostLoad
            price3 = price * 3;
        }

    }

    /** Model class for testing composite PK in DynamoDB */
    public static class Model3 implements Serializable {
        @Key(hashKey=true)          // Mark this field as the hashKey part of the composite primary key.
        public long         id;

        @Key(rangeKey=true)         // Mark this field as the rangeKey part of the composite primary key.
        public String       name;

        public int          age;

        public Set<String>  socks;  // Set is translated to Multi-values in DynamoDB and to JSON in SimpleDB.

        public Set<Long>    sizes;  // Set is translated to Multi-values in DynamoDB and to JSON in SimpleDB.

        public Set<Double>  sizes2; // Set is translated to Multi-values in DynamoDB and to JSON in SimpleDB.

        public Model3() {}
        public Model3(long id, String name, int age, Set<String> socks, Set<Long> sizes) {
            this.id = id;
            this.name = name;
            this.age = age;
            this.socks = socks;
            this.sizes = sizes;
        }

        @PrePersist
        public void generateFieldValues() {
            // Generate sizes2 from sizes;
            if (sizes != null) {
                sizes2 = new HashSet<Double>();
                for (Long i : sizes)
                    sizes2.add(new Double(i.longValue() * 2 + 0.1));
            }
        }
    }

    /** Dbtype annotation to use SimpleDB. */
    @Model(dbtype = DbType.SimpleDB)
    public static class SdbModel1 {     // no Serializable for no cache
        @Key
        public String   name;

        public int      age;

        public SdbModel1() {}
        public SdbModel1(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /** Dbtype annotation to use DynamoDB. */
    @Model(dbtype = DbType.DynamoDB)
    public static class DynModel1 {     // no Serializable for no cache
        @Key(id=true)
        public String   name;

        public int      age;

        public DynModel1() {}
        public DynModel1(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    /** inherits from Model1 */
    public static class Model4 extends Model1 {

        public String   ssn;

        @DefaultGUID                // Generate a GUID if field is not set.
        public String   moreId;

        //@DefaultComposite(fromFields = {"nameFooBar", "ssn", "moreId"}, separator = "/")      // test invalid fromFields
        @DefaultComposite(fromFields = {"name", "ssn", "moreId"}, separator = "/")
        public String   compositeName;


        public Model4() {}
        public Model4(String name, int age, String ssn) {
            super(name, age);
            this.ssn = ssn;
        }
    }

    /** VersionLocking test */
    public static class Model5 implements Serializable {

        @Key
        public String   name;

        public int      age;

        @VersionLocking
        public int      myVersion;
        

        public Model5() {}
        public Model5(String name) {
            this.name = name;
        }
    }

    @Model(dbtype = DbType.SimpleDB)
    public static class Model6 {
        @Key
        public String   name;
        public Model3   model3;

        @DefaultGUID
        @ToUpper
        public String   guidUpper;

        @ToUpper
        @DefaultGUID
        public String   guidReverse;

        @ModifiedTime
        public Date     mtime;

        @ToLower
        @Trim
        @RemoveChar('-')
        public String   lower = "  ABC-XYZ-678  ";

        @RemoveAlphaDigits
        public String   removeAlpha1 = "0abc123cdef7xy";
        
        @RemoveAlphaDigits(removeDigits = true)
        public String   removeAlpha2 = "0abc123cdef7xy";

        @MaxValue(10)
        public int      max10a = 17;
        
        @MaxValue(10)
        public int      max10b = 9;
        
        @MaxValue(10)
        public int      max10c = -50;
        
        @MinValue(10)
        public int      min10a = 17;
        
        @MinValue(10)
        public int      min10b = 9;
        
        @MinValue(10)
        public int      min10c = -50;
        
        @MinValue(10)
        @MaxValue(20)
        public int      minMax10_20a = 17;
        
        @MinValue(10)
        @MaxValue(20)
        public int      minMax10_20b = 9;
        
        @MinValue(10)
        @MaxValue(20)
        public int      minMax10_20c = -50;

        @AbsValue
        public int      abs1 = -50;
        
        @AbsValue
        public int      abs2 = 50;
        
        @CeilValue
        public float    ceil1 = 1.1f;
        
        @CeilValue
        public float    ceil2 = 1.4f;
        
        @CeilValue
        public float    ceil3 = 1.5f;
        
        @FloorValue
        public float    floor1 = 1.1f;
        
        @FloorValue
        public float    floor2 = 1.4f;
        
        @FloorValue
        public float    floor3 = 1.5f;

        @MaxSize(5)
        public int      maxSize1a = 0;
        
        @MaxSize(5)
        public float    maxSize1b = 0f;
        
        @MaxSize(5)
        public Double   maxSize1c = 0d;
        
        @MinSize(5)
        public int      minSize1a = 5;
        
        @MinSize(5)
        public float    minSize1b = 5f;
        
        @MinSize(5)
        public Double   minSize1c = 5d;
        
        @MinSize(10)
        @MaxSize(20)
        public int      minMaxSize1a = 17;
        
        @MinSize(10)
        @MaxSize(20)
        public long     minMaxSize1b = 17;
        
        @MinSize(10)
        @MaxSize(20)
        public double   minMaxSize1c = 17;

        @MaskMatch(pattern = "(###) ###-####")
        public String   mask1 = "(415) 555-1212";
        
        @MaskMatch(pattern = "800-@@@-####")
        public String   mask2 = "800-ABc-1212";

        @MaskMatch(pattern = "800-***-****")
        public String   mask3 = "800-A12-3[?D";
        
        @MaskMatch(pattern = "## $$/$$/$$$$ ##", digitMask = '$' )
        public String   mask4 = "## 07/20/1999 ##";

        @EmailMatch
        public String   email = "abcx.dsdfs@foo.com";

        @Trim                                   // trim spaces, then validate
        @MaskMatch(pattern = "800-***-****")
        public String   mask3b = " 800-A12-3[?D  ";

        @FormatMsg( format = "name is {0}, guidUpper is {1}, mtime is {2} ", paramFields = {"name", "guidUpper", "mtime"}, onSave = true, onLoad = false )
        public String   msg1;

        @FormatMsg( format = "name is {0}, email is {1} ", paramFields = {"name", "email"}, onSave = false, onLoad = true )
        public String   msg2;

    }

    /** Test S3 fields */
    public static class Model7 implements Serializable {
        @Key                            // Mark this field as the primary key.
        public String       name;       // String type PK.

        public int          age;

        @S3Field(s3Bucket = "jsoda", storeAs = S3Field.AS_JSON, gzip = true)
        public String[]     colors;

        @S3Field(s3Bucket = "jsoda")
        public Map          books;

        public Model7() {}
        public Model7(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }
    

    /** Invalid CachePolicy test.  CachePolicy by default turns on caching but class is not Serializable */
    @CachePolicy
    public static class InvalidModel1 {
        @Key
        public String   name;
    }

}

