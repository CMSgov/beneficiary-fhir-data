# CMS Blue Button Server

The CMS Blue Button project provides Medicare beneficiaries with access to their health care data, and supports an ecosystem of third-party applications that can leverage that data.

This project provides the FHIR server used as part of Blue Button.

## Development Environment

Going to work on this project? Great! You can follow the instructions in [Development Environment Setup](./dev/devenv-readme.md) to get going.

## Configuration

This application has the following configuration parameters:

* `bbfhir.db.url`: The JDBC URL of the database to use. Supports HSQL, Derby, and PostgreSQL. Samples:
    * `jdbc:hsqldb:mem:test`
    * `jdbc:derby:directory:target/jpaserver_derby_files;create=true`
    * `jdbc:postgresql://example.com:5432/fhir`
* `bbfhir.db.username`: The JDBC username to use with the database.
* `bbfhir.db.password`: The JDBC password to use with the database.

These parameters should be specified as Java system properties on the command line (i.e. "`-Dkey=val`" arguments).

## Running Locally

This project can be built and run, as follows:

    $ mvn clean install
    $ mvn --projects bbonfhir-server-app jetty:run -Dbbfhir.db.url=jdbc:hsqldb:mem:test -Dbbfhir.db.username="" -Dbbfhir.db.password=""

This will start the server using a local, in-memory database that will be deleted once the server is stopped. If you need a different database or permanent storage, see the options listed above.    

The server can take a few minutes to finish starting up. Wait for the following message to appear at the bottom of the console output:

    [INFO] Started Jetty Server

Once that appears, the server is ready to go. A couple lines above, it will mention the port being used, but the default is `8080`, so you can generally access the web frontend for HAPI FHIR (if you need to) at <http://localhost:8080/hapi-fhir>.

Once the server is no longer needed, you can stop it by pressing `ctrl+c` in the console.

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.
