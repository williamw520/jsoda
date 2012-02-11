
# Introduction

Jsoda is a Java library providing a simple object layer over AWS API to
simplify the storing of Java objects in the Amazon's SimpleDB and DynamoDB
databases.  Java classes are used as table model to create the database
tables.  Ordinary object instances (POJO) are stored as records in the
tables.  Java primitive data types are automatically encoded in the database
type to ensure correct querying and sorting.  DSL-style query methods make
querying simple and easy.

## A Hello World Sample

Here's a quick example to illustrate the usage of Jsoda.

Annotate the Java class as a model class with the <kbd>@Model</kbd>
annotation.  Annotate the _id_ field as the primary key with
<kbd>@Key</kbd>.

    @Model
    public class Hello {
        @Key
        public int      id;
        public String   message;
    }

That's it.  The class is ready to be stored in the AWS database.

Create a Jsoda object with your AWS credentials to use the Jsoda API.

    Jsoda jsoda = new Jsoda(new BasicAWSCredentials(awsKey, awsSecret));

Create the corresponding table in the AWS database.

    jsoda.createModelTable(Hello.class);

To store an object, call the Dao.put() method.

    jsoda.dao(Hello.class).put(new Hello(101, "Hello world"));

To load an object, call the Dao.get() method.

    Hello obj1 = jsoda.dao(Hello.class).get(101);

To load all the objects, run a query without any condition.

    List<Hello> objs = jsoda.query(Hello.class).run();

Build query with conditions using chainable DSL-style methods.

    List<Hello> objs = jsoda.query(Hello.class)
                        .eq("id", 101)
                        .run();

    List<Hello> objs = jsoda.query(Hello.class)
                        .between("id", 100, 500)
                        .like("message", "Hello%")
                        .run();

To count the possible return objects, call the Query.count() method.

    int objCount = jsoda.query(Hello.class)
                        .like("message", "Hello%")
                        .count();

By default <kbd>@Model</kbd> stores a class in SimpleDB.  To switch a class
to store in DynamoDB, change its <kbd>dbtype</kbd>:

    @Model(dbtype = DbType.DynamoDB)
    public class Hello {
        ...
    }

That's it.  All the other API calls above stay the same.  Simple and easy.

(See the sample files in the _sample_ directory or the unit tests for more
examples.)

## Quick Rundown on Features

- One unified API and modeling for both SimpleDB and DynamoDB
- Model table with Java class
- Store records with plain Java objects
- Encode primitive data type and JSON-ify complex type on object fields
- Simple get/put/delete operations.  Batch operations.
- Conditional update
- Chainable DSL-style methods for query
- Pagination for iterating query result
- Consistent read or Eventual Consistent read supported
- Object versioning for optimistic locking
- Pluggable object caching service with multi-attribute caching support
- Automatic field data generation
- Rich data validation rules
- Aspect style @PrePersist, @PreValidation, and @PostLoad methods on object
  loading and saving

## Simplicity

Jsoda adds just a thin layer over the AWS API to make it easy to work in
objects.  It doesn't try to implement the JPA, JDO, or EJB features on top
of AWS databases.  The AWS database concepts, modeling, operations, and
query feature are still there.  It's just a convenient object layer over the
bare metal in AWS.

## Unified API and Modeling

Jsoda aims to provide one unified API and modeling mechanism to both
SimpleDB and DynamoDB.  Switching between SimpleDB and DynamoDB is a matter
of changing the <kbd>@Model</kbd> annotation or one model registration API.
Storing the same model class in both SimpleDB and DynamoDB are supported as
well.


# Quick Start

## Setup

Place the <kbd>jsoda-*version*.jar</kbd> file in your classpath.  The jar
can be downloaded or found in the *dist* directory of the
jsoda-src-*version*.zip file.

## Dependency

Jsoda has a few dependent 3rd party jar libraries, but most of them are
needed for the AWS Java SDK.  Put them in your classpath as needed.

* Latest version of AWS Java SDK (aws-java-sdk-1.3.0.jar or higher) that supports DynamoDB
* Apache Commons BeanUtils (commons-beanutils-1.8.3.jar or higher)
* Apache Commons Lang (commons-lang-2.4.jar or higher)
* Apache Commons Logging (commons-logging-1.1.1.jar or higher)
* Apache Http Client (httpclient-4.1.2.jar or higher)
* Apache Http Core (httpcore-4.1.3.jar or higher)
* Jackson JSON Processor (jackson-all-1.8.4.jar or higher)
* Apache Xerces2 (xercesImpl.jar).  AWS SDK might or might not need this, depending on usage

The files lib/readme and utest/lib/readme list the dependent libraries for
building and running unit tests.



# Development Guide

## Jsoda API Object Model

There are only a few simple objects in Jsoda to access the API: Jsoda, Dao, and Query.

#### Jsoda API Object

The main factory is the <kbd>Jsoda</kbd> object, which has your AWS
credentials defining the scope of the database operations, i.e. the
operations initiated from the Jsoda object can access only the databases
managed under those AWS credentials.  <kbd>Jsoda</kbd> is the main entry to
access the other Jsoda API objects.  It also maintains the registration of
model classes.  Multiple Jsoda objects can be created in one JVM.  Each with
its own AWS credentials and registry of model classes.

<kbd>Jsoda</kbd> is thread-safe and can be shared globally.  One usage
pattern is to create one Jsoda object in your app-wide singleton object and
use it for the whole app.

##### Setting Database Endpoint

To switch to a differnt endpoint for the AWS database service, call
Jsoda.setDbEndpoint().  E.g.

    jsoda.setDbEndpoint(DbType.SimpleDB, "http://sdb.us-west-1.amazonaws.com");
    jsoda.setDbEndpoint(DbType.DynamoDB, "http://dynamodb.us-east-1.amazonaws.com");

#### Dao API Object

A <kbd>Dao</kbd> object is a model class specific API object for doing
get/put/delete operations on individual model objects.  <kbd>Dao</kbd> only
accepts and returns the specific model type objects, reducing the chance of
operating on the wrong types of model objects.  To get a <kbd>Dao</kbd> for
a model class, make the following call.

    Dao<Sample1> dao1 = jsoda.dao(Sample1.class);

<kbd>Dao</kbd> is thread-safe.  The usual usage pattern is to get the model
specific Dao object from the Jsoda object.

#### Query API Object

A <kbd>Query</kbd> object is a model class specific API object for doing
querying operations on sets of model objects.  To query a model class,
create a model specific Query object from Jsoda.

    Query<Sample1> query1 = jsoda.query(Sample1.class);

<kbd>Query</kbd> supports DSL-style methods for constructing query.  The
methods can be chained together for brevity.

    query.like("product", "%Paper%")
         .between("price", 10.50, 30.50)
         .orderby("price")
         .run();

<kbd>Query</kbd> is **not** thread-safe.  It maintains querying state.
Multiple threads using the same Query object might cause unintended
conflicts.  The usage pattern is to create a new Query object from Jsoda
every time you need to query the model table.


## Modeling Data Classes with Jsoda

#### Annotate a Model Class

A class can be annotated with <kbd>@Model</kbd> to mark it as ready to store
in AWS database.

    @Model
    public class Sample1 {
    }

By default <kbd>@Model</kbd> marks a class to be stored in the SimpleDB.
Change its <kbd>dbtype</kbd> to store to a different database type.  For
example,

    @Model(dbtype = DbType.DynamoDB)        // store in DynamoDB
    public class Sample1 {
    }

    @Model(dbtype = DbType.SimpleDB)        // store in SimpleDB
    public class Sample1 {
    }

By default the table name used in the database will be the model class name.
The table name can be specified using the <kbd>table</kbd> attribute of the
annotation.

    @Model(table = "MySample1")
    public class Sample1 {
    }

<kbd>@Model.prefix</kbd> adds a prefix to the table name, either from class
name or the <kbd>table</kbd> attribute.  This can be used to group tables
together in a namespace when specified on a set of model classes.

    @Model(prefix = "Acct_")                // table name becomes Acct_Sample1
    public class Sample1 {
    }

DynamoDB's ProvisionedThroughput on a table can be specified with
<kbd>readThroughput</kbd> or <kbd>writeThroughput</kbd> in
<kbd>@Model</kbd>.  They have no effect on SimpleDB.

#### Key Field of a Model Class

At the minimum you need to identify one field in the model class as the
<kbd>@Key</kbd> field.  This serves as the primary key to store the object
in the database.  Jsoda supports int, Integer, long, Long, and String type data
as Key field.  For example,

    public class Hello {
        @Key
        public int      id;
    }

Since DynamoDB has the concept of composite primary key (hashKey +
rangeKey), <kbd>@Key</kbd> supports annotating two fields in the model class
to form the composite key.  For example,

    public class Hello2 {
        @Key(hashKey=true)      // Mark this field as the hashKey part of the composite key.
        public int      id;
        @Key(rangeKey=true)     // Mark this field as the rangeKey part of the composite key.
        public String   name;
    }

Composite key works in SimpleDB as well.  The value of the composite key
fields are combined to form the item name (primary key) of a record in
SimpleDB.

<kbd>Dao</kbd> and <kbd>Query</kbd> accept composite key value pair in their
API methods.

#### Field Data Types

Since SimpleDB and DynamoDB store only String type data, non-String data
needs to be encoded to ensure correct comparison and sorting in query.  Most
of the primitive Java type data are encoded automatically when used in the
fields of a model class: byte, char, short, int, long, float, boolean, and
java.util.Date.  Check the code in DataUtil.encodeValueToAttrStr() for
details.

Fields with complex data types, arrays, list, map, or any embedded objects,
are supported as well.  They are stored as JSON string.  However, they
cannot be searched or used in query condition.  

Note that SimpleDB has a limit of 1024 bytes per attribute.  Excessive
large complex objects might exceed the limitation after JSON-ified.

#### Model Class Registration

Model classes need to be registered first before they can be used.  There
are two ways to register model classes: auto-registration and explicit
registration.  When a model class has enough annotation information, it can
be auto-registered upon its first use.  For example,

    Dao<Sample1>    dao1 = jsoda.dao(Sample1.class);
    Query<Sample1>  query1 = jsoda.query(Sample1.class);

Either one of the above would auto-register the Sample1 model class with the
jsoda object.

When a model class doesn't have the <kbd>@Model</kbd> annotation or you want
to override the dbtype in the annotation (default or specified), you can
register it via the Jsoda.registerModel() method.

    jsoda.registerModel(Sample1.class, DbType.DynamoDB);

The above would register the Sample1 model to be stored in DynamoDB instead
of the default SimpleDB dbtype in <kbd>@Model</kbd>.

Note that a model class can only be registered against one dbtype in a Jsoda
object.  If the same model class needs to be stored in both SimpleDB and
DynamoDB, register the model class in a different Jsoda object.  E.g.

    jsodaSdb.registerModel(Sample1.class, DbType.SimpleDB);
    jsodaDyn.registerModel(Sample1.class, DbType.DynamoDB);


## Create, List, and Delete Model Tables

The table (domain) of a registered model class can be created via the
Jsoda.createModelTable() method.  Table creation only needs to be done once.

    jsoda.createModelTable(Hello.class);

Listing of the native table names in a database can be done via the
Jsoda.listNativeTables() method.  It lists all the tables in the database,
whether they are created via Jsoda or by other means.

    List<String> tables = jsoda.listNativeTables(DbType.SimpleDB);
    List<String> tables = jsoda.listNativeTables(DbType.DynamoDB);

Note that this returns the native table names, which might be different from
the model name of the model class depending on the @Model.table mapping.

A registered model's table can be deleted via Jsoda.deleteModelTable().
Native tables can be deleted via Jsoda.deleteNativeTable().  This can be
helpful when a model's table mapping has changed and you want to get rid of
the old native table.

Exercise extreme caution in deleting tables.  Data are gone once deleted.


## Storing, Getting, and Deleting Objects

Storing, getting, and deleting objects can be done via get/put/delete in
<kbd>Dao</kbd>.

#### Storing Objects

Saving objects of a model class is done via the Dao.put() method.

    Dao<Hello>  dao = jsoda.dao(Hello.class);
    dao.put(new Hello(101, "abc"));
    dao.put(new Hello(102, "def"));
    dao.put(new Hello(103, "ghi"));

Dao supports batch updates via batchPut.

    dao.batchPut( new Hello(50, "aa"), new Hello(51, "bb"), new Hello(52, "cc") );

#### Storing Steps

When an object is stored, a series of steps takes place.  It's good to know
them if you want to do validation or intercept the storing call.

* Pre-Storing Steps
    1. The <kbd>@PrePersist</kbd> method in the model class is called if one is
       annotated, giving you the chance to modify any data field.
    2. The data generators annotated on the fields are called to fill in the
       field value.  E.g. @DefaultGUID or @ModifiedTime.
    3. The composite data generatorson the fields are called to fill in the field
       value.  E.g. @DefaultComposite.
    4. The <kbd>@PreValidation</kbd> method in the model class is called if one
       is annotated, giving you the chance to modify the field after the data
       generators run and do any custom validation before the built-in ones run.
    5. Built-in validations annotated on the fields are called.
* The object is saved in the database.
* Post-Storing Step.  
  The cache service updates its cache with the new object if it's cacheable.


#### Getting Objects

Loading objects of a model class is simply done via the Dao.get() method.

    jsoda.dao(Hello.class).get(101);

Composite key object needs to pass both the hashKey and rangeKey in.

    jsoda.dao(Hello2.class).get(101, "abc");


#### Deleting Objects

Deleting objects is done via the Dao.delete() method.

    jsoda.dao(Hello.class).delete(101);

Composite key object needs to pass both the hashKey and rangeKey in.

    jsoda.dao(Hello2.class).delete(101, "abc");

Batch delete is done via Dao.batchDelete().

    jsoda.dao(Hello.class).batchDelete(101, 102, 103);

#### Conditional Update

Conditional update is done via the Dao.putIf() method.  The call would fail
if the expected value of a field is not matching.  This is the way AWS
implements optimistic locking to allow orderly concurrent updates from
multiple clients.

If conditional update fails, you should load the latest version of the
object, merge in the changes to the original object and save again.

Note that conditional update doesn't work with batchPut().

#### Object Versioning

Jsoda makes optimistic locking easier by doing all the work in Dao.  You
simply add a version field (of type int) to the model class and annotate it
with <kbd>@VersionLocking</kbd>.  Versioning works with both SimpleDB and
DynamoDB.

    public class Hello3 {
        @Key
        public int      id;
        public String   name;
        @VersionLocking
        public int      myVersion;
    }

    Dao<Hello3> dao = jsoda.dao(Hello3.class);
    dao.put(new Hello3(101, "abc"));
    Hello3  hello3 = dao.get(101);
    hello3.name = "xyz";
    dao.put(hello3)

Both dao.put()'s will increment the version and perform conditional update
on it to do optimistic locking.  If another client has updated the object
with a newer version, your put() will fail.

Note that object versioning doesn't work with batchPut().

## Queries

Query in Jsoda is done via the <kbd>Query</kbd> object.  Create a model
class specific Query object via the Jsoda object.

    Query<Sample1>  query = jsoda.query(Sample1.class);

The querying syntax is the set of DSL methods in Query.  Jsoda's query API
works on both SimpleDB and DynamoDB.  There's no need to learn another query
language.

#### Select All

To query all items with all the attributes for each item, run the query without
any condition.

    List<Sample1> objs = query.run();

#### Select Some Fields

To select only some attributes for each item, add the select method to
list the fields to return.  The following returns objects with only
the *name* field filled in.

    List<Sample1> objs = query.select("name").run();

This returns objects with the *id* and *name* fields filled in.

    List<Sample1> objs = query.select("id", "name").run();

#### Filtering Condition

Add conditions to query to filter out unwanted objects.  The following
returns only the objects whose *id* equals to the 101 value, and *age*
is greater than 20.

    List<Sample1> objs = query.eq("id", 101).run();
    List<Sample1> objs = query.gt("age, 20).run();

The supported comparison methods are: *eq*, *ne*, *le*, *lt*, *ge*, *gt*, *like*,
*not_like*, *contains*, *not_contains*, and *begins_with*.

The unary condition method for checking null in field: *is_null* and *is_not_null*.

    List<Sample1> objs = query.is_not_null("name").run();

The *between* method takes two value arguments to set the bounds of the range.

    List<Sample1> objs = query.between("age", 20, 30).run();

The *in* method checks if the field matches any value in the list of values.

    List<Sample1> objs = query.in("name", "Jack", "Jane", "Joe").run();

*Note that some conditional operators are not supported in SimpleDB or DynamoDB.*

Multiple conditions can be combined.  They have the AND effect.  The
following returns objects whose age is between 20 and 30 *and* whose
name has "ack" in it.

    List<Sample1> objs = query
                        .between("age", 20, 30)
                        .like("name", "%ack%")
                        .run();

#### Order By

The return order of the objects can be ordered via the *order_by* or
*order_by_desc* method.  The following orders the result in ascending
order of the *age* field.

    List<Sample1> objs = query.between("age", 20, 30).orde_by("age").run();

The following reverses the return order.

    List<Sample1> objs = query.between("age", 20, 30).orde_by_desc("age").run();

Note that since *order by* requires a sorted index in the underlying
database to work, an index is needed to be *in use*, which means the
*order by* field must be a field used in the condition, for both
SimpleDB and DynamoDB.

#### Chaining Methods

Most of the Query methods returns the Query object itself so that method
calls can be chained together for brevity.  E.g.

    query.select("name").between("age", 20, 30).like("name", "%ack%").order_by("age").run();

#### Iterating Result

SimpleDB and DynamoDB return only a fixed number of objects at each
request even if there are more items in the result set.  Query.run()
follows that limitation.  To get the next batch of items in the result
set, call Query.run() again.  Each call to Query.run() will return the
next batch of result until there are no more.  At that point
Query.run() returns an empty list.

The typical result processing loop is:

    List<T> items;
    while ((items = query.run()).size() != 0) {
        for (T item : items) {
            ...
        }
    }

The Query.hasNext() method can also be used for checking additional
result.  Using Query.hasNext() can produce a cleanup loop:

    while (query.hasNext()) {
        for (Model1 item : query.run()) {
            ...
        }
    }

#### Query vs Scan

DynamoDB has a limited index-based *query* capability since it has
only one or two indexes.  Querying DynamoDB table often results in a
*scan* of all the objects.  DynamoDB only supports index-based *query*
when the hashKey and rangeKey are involved in the condition.  See the
DynamoDB documentation for detail.


## Data generator

## Validation

## Caching

## Misc Annotation


# Resources

TBA

# License

Jsoda is licensed under the Mozilla Public License 2.0 (MPL).  See the
license.txt file for detail.  Basically you can incorporate Jsoda into your
work however you like (open source or proprietary), but when making change
to Jsoda itself, you need to release the changes under MPL.


