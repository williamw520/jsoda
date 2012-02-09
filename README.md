
------------------------------------------------------------------

# Introduction

Jsoda is a Java library providing a thin object layer over AWS API to
simplify the storing of Java objects in the Amazon's SimpleDB and DynamoDB
databases.  Simple Java classes are used as table model to create database
tables.  Ordinary object instances (POJO) are stored as records in the
table.  Java primitive data types are automatically encoded in the database
type to ensure correct indexing and sorting.  DSL-style query methods make
querying simple and easy.

## Quick Rundown on Features

- One unified API and modeling for both SimpleDB and DynamoDB
- Model table with class
- Store records with plain Java objects
- Encode primitive data type and JSON-ify complex type on field data
- Simple get/put/delete operations.  Batch operations supported.
- Conditional update
- DSL-style methods for query
- Cursor/iterator support for long query result
- Consistent read or Eventual Consistent read support
- Object versioning support for optimistic locking
- Pluggable object caching with multi-attribute caching support
- Automatic field data generation
- Rich data validation rules
- Aspect style @PrePersist, @PreValidation, and @PostLoad methods on object
  loading and saving


## Simplicity

Jsoda aims to add just a minimum layer over the AWS API to make it easy to
work in objects.  It doesn't try to implement the JPA, JDO, or EJB features
on top of AWS databases.  The AWS database concepts, modeling, operations,
and query feature are still there.  It's just a convenient object layer over
the bare metal in AWS.

## Unified API and Modeling

Jsoda aims to provide one unified API and modeling mechanism to both
SimpleDB and DynamoDB.  Switching between SimpleDB and DynamoDB is a matter
of changing the @Model annotation or one model registration API.  Storing
the same model class in both SimpleDB and DynamoDB are supported as well.




------------------------------------------------------------------

# Quick Start

## Setup

## Quick Sample


------------------------------------------------------------------

# Development Guide

    public static void main() {
        abc();
    }

## Defining Data Model Classes with Jsoda

## Creating, Getting, and Deleting Data in Jsoda

## Queries in JDO

## Data generator

## Validation

## Caching


------------------------------------------------------------------

# Resources

------------------------------------------------------------------

# License

Jsoda is licensed under the Mozilla Public License 2.0 (MPL).  See the
license.txt file for detail.  Basically you can incorporated Jsoda into your
work however you like (free or commercial), but when making change to Jsoda
itself, you need to release the changes under MPL as well.

