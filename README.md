# CMS Blue Button Server

The CMS Blue Button project provides Medicare beneficiaries with access to their health care data, and supports an ecosystem of third-party applications that can leverage that data.

This project provides the FHIR server used as part of Blue Button.

## Development Environment

Going to work on this project? Great! You can follow the instructions in [Development Environment Setup](./dev/devenv-readme.md) to get going.

## Configuration

This application has the following configuration parameters:

* `bbfhir.db.url`: The JDBC URL of the database to use. Supports HSQL and PostgreSQL. Samples:
    * `jdbc:hsqldb:mem:test`
    * `jdbc:postgresql://example.com:5432/fhir`
* `bbfhir.db.username`: The JDBC username to use with the database.
* `bbfhir.db.password`: The JDBC password to use with the database.

These parameters should be specified as Java system properties on the command line (i.e. "`-Dkey=val`" arguments).

## Running Locally

This project can be built and run, as follows:

    $ mvn clean install
    $ mvn --projects bluebutton-server-app package dependency:copy antrun:run org.codehaus.mojo:exec-maven-plugin:exec@server-start

This will start the server using a local, in-memory database that will be deleted once the server is stopped. The server can take a few minutes to finish starting up, and Maven will exit with a "`BUILD SUCCESSFUL`" message once it's ready. The server will be running at <https://localhost:9094/baseDstu2>. Please note that it is set by default to require SSL mutual authentication, so accessing it via a browser isn't simple. See [Development Environment Setup](./dev/devenv-readme.md) for details on how to work with this, if needed.

Once the server is no longer needed, you can stop it by running the following command:

    $ mvn --projects bluebutton-server-app org.codehaus.mojo:exec-maven-plugin:exec@server-stop

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.
