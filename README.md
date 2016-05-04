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

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.
