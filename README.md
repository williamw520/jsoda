
## Introduction

Jsoda is a Java library providing a thin object layer over AWS API to
simplify the storing of Java objects in the Amazon's SimpleDB and DynamoDB
databases.  Simple Java classes are used as table model to create the
database tables.  Ordinary object instances (POJO) are stored as records in
the table.  Java primitive data types are automatically encoded in the
database type to ensure correct indexing and sorting.  DSL-style query
methods make querying simple and easy.

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


### Hello World Sample

Here's a quick example to illustrate the basic usage of Jsoda.

Annotate the Java class as the model class using the <kdb>@Model</kdb>
annotation.  Mark the _id_ field as the primary key using the @Key
annotation.

    @Model
    public class Hello {
        @Key
        public int      id;
        public String   message;
    }

That's it.  The class is ready to work with Jsoda.  (Class without the
<kdb>@Model</kdb> annotation also works.  See Development Guide below.)

To use the Jsoda API, first create a Jsoda object with your AWS credentials.

    Jsoda   jsoda = new Jsoda(new BasicAWSCredentials(key, secret));

To create the corresponding table in the AWS database, use createModelTable.

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

That's it.  Simple and easy to use.  See the sample files in the _sample_
directory or the unit tests for more examples.


## Development Guide

### Modeling Data Classes with Jsoda

#### Annotate a Model Class

A class can be annotated with <kdb>@Model</kdb> to mark it as ready to store
in AWS database.

    @Model
    public class Sample1 {
    }

By default <kdb>@Model</kdb> marks a class to be stored in the SimpleDB.
Change its <kbd>dbtype</kdb> to store to a different database type.  For
example,

    @Model(dbtype = DbType.DynamoDB)        // store in DynamoDB
    public class Sample1 {
    }

    @Model(dbtype = DbType.SimpleDB)        // store in SimpleDB
    public class Sample1 {
    }

By default the table name used in the database will be the model class name.
The table name can be specified using the <kdb>table</kdb> attribute of the
annotation.

    @Model(table = "MySample1")
    public class Sample1 {
    }

<kdb>@Model.prefix</kdb> adds a prefix to the tablename, either from class
name or the <kdb>table</kdb> attribute.  This can be used to segment tables
together in a namespace when specified on a set of model classes.

    @Model(prefix = "Acct_")                // tablename becomes Acct_Sample1
    public class Sample1 {
    }

DynamoDB's ProvisionedThroughput on a table can be specified with <kdb>readThroughput</kdb>
or <kdb>writeThroughput</kdb>.  They don't have effect on SimpleDB.


### Defining Data Model Classes with Jsoda


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

