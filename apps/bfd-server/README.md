# CMS Blue Button 2.0 API Backend Data Server

The CMS Blue Button 2.0 API project provides Medicare beneficiaries with access to their health care data, and supports an ecosystem of third-party applications that can leverage that data.

This project provides the backend Data Server that takes the raw relational claims data and hosts it in a FHIR-compliant REST web service.

## Design

Design documentation for the project can be found in the following files:

* [Data Server Design: SAMHSA Filtering](./dev/design-samhsa-filtering.md)

## Development Environment

Going to work on this project? Great! You can follow the instructions in [Development Environment Setup](./dev/devenv-readme.md) to get going.

## Configuration

This application has the following configuration parameters:

* `bfdServer.logs.dir`: The directory that the application will write its log files out to, which defaults to `./target/bluebutton-server/`.
* `bfdServer.db.url`: The JDBC URL of the database to use. Supports HSQL and PostgreSQL. Samples:
    * `jdbc:hsqldb:mem:test`
    * `jdbc:postgresql://example.com:5432/fhir`
* `bfdServer.v2.enabled`: Enabled V2 of the BFD API. Supports "true" or "false".
* `bfdServer.db.username`: The JDBC username to use with the database.
* `bfdServer.db.password`: The JDBC password to use with the database.

These parameters should be specified as Java system properties on the command line (i.e. "`-Dkey=val`" arguments).

## Running Locally

This project can be built and run, as follows:

    $ mvn clean install
    $ mvn --projects bfd-server-war package dependency:copy antrun:run org.codehaus.mojo:exec-maven-plugin:exec@server-start

This will start the server using a local, in-memory database that will be deleted once the server is stopped. The server can take a few minutes to finish starting up, and Maven will exit with a "`BUILD SUCCESSFUL`" message once it's ready. 

There are currently two versions of the API available. V1 is based on FHIR dstu3 while V2 is based on FHIR R4 with considerations made to adhere to the CARIN Blue Button Implementation Guide. 

The server will be running at <https://localhost:9094/v1/fhir> or at <https://localhost:9094/v2/fhir>. Please note that it is set by default to require SSL mutual authentication, so accessing it via a browser isn't simple. See [Development Environment Setup](./dev/devenv-readme.md) for details on how to work with this, if needed.

Once the server is no longer needed, you can stop it by running the following command:

    $ mvn --projects bfd-server-war org.codehaus.mojo:exec-maven-plugin:exec@server-stop

## Profiling Performance

This project can be run using the open source [Java VisualVM profiler](https://visualvm.github.io/), which can be used to analyze its performance, memory usage, etc. Please note that VisualVM only supports profiling locally (remote applications can be inspected and possibly even sampled, but not profiled).

While the JDK ships with an older, stable build of VisualVM, it's recommended that developers instead go the site and download the latest version (version 1.3.9, as of 2016-10-27). This latest version provides support via a plugin for profiling applications beginning at launch, rather than just connecting the profiler to the application after it's already started running. See the [VisualVM Startup Profiler plugin page](http://visualvm.java.net/startupprofiler.html) for more details. Note that the alternative, starting profiling after application startup, does not seem to work correctly with Wildfly: it seems to cause the application to crash and the profiler to hang.

To ease the use of VisualVM with the startup plugin, developers can do the following:

1. Launch VisualVM before starting the application (as described in the "Running Locally" section, above).
1. Ensure that the Startup Profiler plugin is installed, and configure it as follows:
    * _Start profiling from classes_: (leave the field blank).
    * _Do not profile classes_: `java.*, javax.*, sun.*, sunw.*, com.sun.*, org.jboss.*, org.xnio.*, io.undertow.*`
        * The overhead of attempting to profile these classes is too high; Wildfly never finishes launching if they're profiled. The excluded classes will have their execution time from these classes "rolled up" into the reported "Total Time" of whatever calls them.
1. When starting the application (using the command specified above, add the following arguments to the command (update the path to match your system):
    
    ```
    -DvisualVm=/home/karl/workspaces/tools/visualvm_139"
    ```

Note: On Ubuntu systems, developers should first investigate whether their system is configured to use dynamic CPU frequency scaling (it probably is, and will make profiling results useless). See <https://wiki.debian.org/HowTo/CpuFrequencyScaling> for details. The `cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor` command can be used to determine the current CPU mode. The `sudo cpufreq-set -g performance` command can be used to disable scaling until the next reboot.

## License

This project is in the worldwide [public domain](LICENSE.md). As stated in [CONTRIBUTING](CONTRIBUTING.md):

> This project is in the public domain within the United States, and copyright and related rights in the work worldwide are waived through the [CC0 1.0 Universal public domain dedication](https://creativecommons.org/publicdomain/zero/1.0/).
>
> All contributions to this project will be released under the CC0 dedication. By submitting a pull request, you are agreeing to comply with this waiver of copyright interest.
