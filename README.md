# CMS Blue Button Server

The CMS Blue Button project provides Medicare beneficiaries with access to their health care data, and supports an ecosystem of third-party applications that can leverage that data.

This project provides the FHIR server used as part of Blue Button.

## Configuration

This application has the following configuration parameters:

* `bbfhir.db.url`: The JDBC URL of the database to use. Supports HSQL, Derby, and PostgreSQL. Samples:
    * `jdbc:hsqldb:mem:test`
    * `jdbc:derby:directory:target/jpaserver_derby_files;create=true`
    * `jdbc:postgresql://example.com:5432/fhir`
* `bbfhir.db.username`: The JDBC username to use with the database.
* `bbfhir.db.password`: The JDBC password to use with the database.

These parameters should be specified as Java system properties on the command line (i.e. "`-Dkey=val`" arguments).