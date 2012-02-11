
## Introduction

Jsoda is a Java library providing a simple object layer over AWS API to
simplify the storing of Java objects in the Amazon's SimpleDB and DynamoDB
databases.  Java classes are used as table model to create the database
tables.  Ordinary object instances (POJO) are stored as records in the
table.  Java primitive data types are automatically encoded in the database
type to ensure correct indexing and sorting.  DSL-style query methods make
querying simple and easy.

### A Hello World Sample

Here's a quick example to illustrate the basic usage of Jsoda.

Annotate a Java class as a model class using the <kbd>@Model</kbd>
annotation.  Mark the _id_ field as the primary key using the
<kbd>@Key</kbd> annotation.

    @Model
    public class Hello {
        @Key
        public int      id;
        public String   message;
    }

That's it.  The class is ready to store in the AWS database.  (Class without
the <kbd>@Model</kbd> annotation also works.  See Development Guide below.)

To use the Jsoda API, first create a Jsoda object with your AWS credentials.

    Jsoda jsoda = new Jsoda(new BasicAWSCredentials(awsKey, awsSecret));

To create the corresponding table in the AWS database, call

    jsoda.createModelTable(Hello.class);

To store an object, call the Dao.put() method.

    jsoda.dao(Hello.class).put(new Hello(101, "Hello world"));

To load an object, call the Dao.get() method.

    Hello obj1 = jsoda.dao(Hello.class).get(101);

To load all the objects, call the Query.run() method.

    List<Hello> objs = jsoda.query(Hello.class).run();

To query the objects with conditions, build the query with filtering conditions.

    List<Hello> objs = jsoda.query(Hello.class)
                        .eq("id", 101)
                        .run();

    List<Hello> objs = jsoda.query(Hello.class)
                        .like("message", "Hello%")
                        .run();

To count the possible returned objects, call the Query.count() method.

    int objCount = jsoda.query(Hello.class)
                        .like("message", "Hello%")
                        .count();

By default <kbd>@Model</kbd> stores a class in SimpleDB.  To switch to store
the class in DynamoDB, change its <kbd>dbtype</kbd> as:

    @Model(dbtype = DbType.DynamoDB)
    public class Hello {
        ...
    }

That's it.  All the other API calls above stay the same.  Simple and easy.
(See the sample files in the _sample_ directory or the unit tests for more
examples.)

### Quick Rundown on Features

- One unified API and modeling for both SimpleDB and DynamoDB
- Model table with Java class
- Store records with plain Java objects
- Encode primitive data type and JSON-ify complex type on object fields
- Simple get/put/delete operations.  Batch operations supported.
- Conditional update
- Chainable DSL-style methods for query
- Cursor/iterator support for large query result
- Consistent read or Eventual Consistent read supported
- Object versioning for optimistic locking
- Pluggable object caching service with multi-attribute caching support
- Automatic field data generation
- Rich field data validation rules
- Aspect style @PrePersist, @PreValidation, and @PostLoad methods on object
  loading and saving

### Simplicity

Jsoda adds just a minimum layer over the AWS API to make it easy to work in
objects.  It doesn't try to implement the JPA, JDO, or EJB features on top
of AWS databases.  The AWS database concepts, modeling, operations, and
query feature are still there.  It's just a convenient object layer over the
bare metal in AWS.

### Unified API and Modeling

Jsoda aims to provide one unified API and modeling mechanism to both
SimpleDB and DynamoDB.  Switching between SimpleDB and DynamoDB is a matter
of changing the <kdb>@Model</kdb> annotation or one model registration API.
Storing the same model class in both SimpleDB and DynamoDB are supported as
well.


## Quick Start

### Setup

Place the <kbd>jsoda-*version*.jar</kbd> file in your classpath.  The jar
can be downloaded or found in the *dist* directory of the source zip file.

#### Dependency

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



## Development Guide

### Jsoda API Object Model

There are only a few simple objects in Jsoda to access the API.

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

A <kbd>Dao</kbd> object is a model class specific API object for doing
get/put/delete operations on individual model objects.  <kbd>Dao</kbd> only
accepts and returns the specific model type objects, reducing the chance of
operating on the wrong types of model objects.  To get a <kbd>Dao</kbd> for
a model class, make the following call.

    Dao<Sample1> dao1 = jsoda.dao(Sample1.class);

<kbd>Dao</kbd> is thread-safe.  The usual usage pattern is to get the model
specific Dao object from the Jsoda object.

A <kbd>Query</kbd> object is a model class specific API object for doing
querying operations on sets of model objects.  To query a model class,
create a model specific Query object from Jsoda.

    Query<Sample1> query1 = jsoda.query(Sample1.class);

<kbd>Query</kbd> supports DSL-style methods for constructing query.  The
methods can be chained together for brevity.

<kbd>Query</kbd> is *not* thread-safe.  It maintains querying state.
Multiple threads using the same Query object might cause unintended
conflicts.  The usage pattern is to create a new Query object from Jsoda
every time you need to query the model table.


### Modeling Data Classes with Jsoda

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

<kbd>@Model.prefix</kbd> adds a prefix to the tablename, either from class
name or the <kbd>table</kbd> attribute.  This can be used to group tables
together in a namespace when specified on a set of model classes.

    @Model(prefix = "Acct_")                // tablename becomes Acct_Sample1
    public class Sample1 {
    }

DynamoDB's ProvisionedThroughput on a table can be specified with
<kbd>readThroughput</kbd> or <kbd>writeThroughput</kbd> in
<kbd>@Model.prefix</kbd>.  They have no effect on SimpleDB.

#### Key Field a Model Class

At the minimum you need to identify one field in the model class as the
<kbd>@Key</kbd> field.  This serves as the primary key to store the object
in the database.  Jsoda supports int, Integer, long, Long, and String type data
as Key field.  For example,

    public class Hello {
        @Key
        public int      id;
    }

Since DynamoDB has the concept of composite primary key (hashKey +
rangeKey), <kdb>@Key</kdb> supports annotating two fields in the model class
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

The <kdb>Dao</kdb> and <kdb>Query</kdb> accept composite key value pair in
their API methods.

#### Field Data Types

Since SimpleDB and DynamoDB store only String type data, non-String data
needs to be encoded to ensure correct comparison and sorting in query.  Most
of the primitive Java type data are encoded automatically when used in the
fields of a model class: byte, char, short, int, long, float, boolean, and
java.util.Date.  Check the code in DataUtil.encodeValueToAttrStr() for
details.

Fields with complex data types, arrays, list, map, or any embedded objects,
are supported as well.  They are stored as JSON string.  However, they
cannot be searched or used in query condition.  SimpleDB indexes all columns
regardless data type.  Excessive complex objects in fields might take
up more index storage than necessary.

#### Model Class Registration

Model classes need to be registered first before they can be used.  There
are two ways to register model classes: auto-registration and explicit
registration.  When a model class has enough annotation with default dbtype,
it can be auto-registered upon its first use.  For example,

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

Note that a model can only be registered against one dbtype in a Jsoda
object.  If the same model needs to be stored in both SimpleDB and DynamoDB,
register the model class in a different Jsoda object.  For example,

    jsodaSdb.registerModel(Sample1.class, DbType.SimpleDB);
    jsodaDyn.registerModel(Sample1.class, DbType.DynamoDB);


### Creating, Getting, and Deleting Data in Jsoda

### Queries

### Data generator

### Validation

### Caching

### Annotation



## Resources

TBA

## License

Jsoda is licensed under the Mozilla Public License 2.0 (MPL).  See the
license.txt file for detail.  Basically you can incorporate Jsoda into your
work however you like (open source or proprietary), but when making change
to Jsoda itself, you need to release the changes under MPL.

