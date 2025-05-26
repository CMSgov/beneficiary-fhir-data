# build-bfd

The `build-bfd` script is a maven front end that makes it easy to combine multiple maven command line options
to build and/or test all or part of the BFD system in useful ways.
The script must be executed from either the root BFD source directory or the `apps` subdirectory.

The script's `-z` command line option can be used to learn how to accomplish the same thing
as the script using plain maven commands.  This option just prints whatever maven command
or commands the script would have executed on your behalf and then exits without actually
running them.

```
$ ./utils/scripts/build-bfd -z 
mvn -e install -DskipTests=true -DskipITs -Dmaven.javadoc.skip=true -Dcheckstyle.skip
```

The script's `-h` command line option can be used to print all available options:

```
$ ./utils/scripts/build-bfd -h
Runs a maven install.  Use command line options to change behaviors.

build-bfd [script_options...] [maven_args...]

The following options are supported.  All options start with a hyphen.  Any arguments 
after the script options are passed to maven as additional arguments.  To pass a
hyphenated option to maven use -- to mark an end of the script options and beginning
of maven arguments.

-a          Build all. Same as -ijs.  Use before commit!
-t          Enable unit tests.
-i          Enable unit and IT tests.
-j          Enable javadoc generation.
-J          Builds docker image using jib plugin.
-s          Enable checkstyle.
-c          Once to use write-only build cache.  Twice to disable build cache.
-r project  Resume from project (after failed tests for example).
-m          Enable minio (-Ds3.local=true).
-x          Add clean goal.
-X          Add clean goal. Deletes cache and all repo artifacts prior to build.
-I arg      Run the specified integration test.  Implies -i and -C.  Use with -P.
-T arg      Run the specified unit test.  Implies -t and -C.  Use with -P.
-p project  Only compile for project (with dependencies, can use multiple times).
-P project  Only compile for project (no dependencies, can use multiple times).
-z          Dry run.  Just print the maven command line without running maven.
-d          Just print the dependency tree (no build).
-D          Download data projects. (SLOW)
-F          Just run code formatter and exit (no build).
-h          Prints this help message.
```

By default the script does the minimal amount of effort to get a build.

* It skips running these non-build related tasks:
  * unit tests
  * integration tests
  * javadoc generation
  * checkstyle checks
* It uses the maven build cache.

This default behavior is useful for quick turn around when changing branches or building code while editing.
However all of the skipped tasks are very important and should always be executed before committing code.
The `-a` command line option can be used to ensure that all of the normally skipped tasks are executed.
Do this prior to commit.

Any build that includes tests, etc puts the build cache into write-only mode to ensure
that tests are always executed.  This is to avoid issues with maven deciding not to execute
tests for modules that it already has build artifacts for.


# Command Line Options

## Option: `-a`

This option enables all build steps.  Do this before committing to avoid checking in invalid code.

## Option: `-t`

Ensures that unit tests are executed as part of a build.

## Option: `-i`

Ensures that unit and integration tests are executed as part of a build.
You cannot execute just integration tests because the same maven property used to disable unit tests
also disabled integration tests.

## Option: `-j`

Enables javadoc generation as part of a build.

## Option `-s`

Enables checkstyle execution as part of a build.

## Option: `-J`

Enables the Google Jib plugin to generate docker images as part of the build.
This will only work on a branch that includes the Jib plugin support.

## Option: `-c`, `-cc`

Changes the maven build cache mode.

A single `-c` puts the maven cache into write only mode.
In this mode artifacts will be added to the cache but will not be pulled from the cache to avoid building them again.
This mode is used when tests etc need to be executed.

Two `-c` options completely disable the build cache.
In this mode the build cache will be neither read from nor written to.

## Option: `-r`

By default maven will build every module and its dependencies.
Sometimes you don't want to sit through a build of low level dependencies because you are focusing on the last
couple of modules in the build.
This option tells maven to start at the named module and build it and later modules only.

For example, the `bfd-pipeline-rda-grpc` module is a dependency of several other modules.
If you are making changes to `bfd-pipeline-rda-grpc` and want to run the tests in dependent modules
to ensure those changes did not break them, but you do not want to sit through tests of the earlier
modules like `bfd-model-rda` you could use this command line to start the build at `bfd-pipeline-rda-grpc`:

```
$ ./utils/scripts/build-bfd -t -r bfd-pipeline-rda-grpc
... skipping some output to get to this from maven:
[INFO] Reactor Build Order:
[INFO] 
[INFO] bfd-pipeline-rda-grpc                                              [jar]
[INFO] bfd-pipeline-rda-grpc-apps                                         [jar]
[INFO] bfd-pipeline-app                                                   [jar]
[INFO] bfd-pipeline-rda-bridge                                            [jar]
[INFO] bfd-server-parent                                                  [pom]
[INFO] bfd-server-launcher-sample                                         [war]
[INFO] bfd-server-shared-utils                                            [jar]
[INFO] bfd-server-launcher                                                [jar]
[INFO] bfd-server-war                                                     [war]
[INFO] bfd-db-migrator                                                    [jar]
```

You can see that the build started from the expected module.

Obviously this will only work properly if nothing is broken in the earlier modules so its use
is situational.  When it's needed it can save considerable time.

## Option: `-p`

Often you are working on a specific app and do not want to build and/or sit through the tests in 
other apps.  For example, if you are working on `bfd-server-war` you probably don't want to sit
though repeated tests of `bfd-pipeline-app`.

The `-p` option tells maven to only build the specific module and its dependencies.
It can be used multiple times to build more than one module.
For example:

```
$ ./utils/scripts/build-bfd -p bfd-pipeline-app
... skipping some output to get to this from maven:
[INFO] Reactor Build Order:
[INFO] 
[INFO] bfd-parent                                                         [pom]
[INFO] bfd-shared-test-utils                                              [jar]
[INFO] bfd-shared-utils                                                   [jar]
[INFO] bfd-model-parent                                                   [pom]
[INFO] bfd-model-codebook-extractor                                       [jar]
[INFO] bfd-model-codegen-annotations                                      [jar]
[INFO] bfd-model-codegen                                                  [jar]
[INFO] bfd-model-codebook-data                                            [jar]
[INFO] bfd-model-dsl-codegen-parent                                       [pom]
[INFO] bfd-model-dsl-codegen-library                                      [jar]
[INFO] bfd-model-dsl-codegen-plugin                              [maven-plugin]
[INFO] bfd-model-rda                                                      [jar]
[INFO] bfd-model-rif                                                      [jar]
[INFO] bfd-model-rif-samples                                              [jar]
[INFO] bfd-pipeline-parent                                                [pom]
[INFO] bfd-pipeline-shared-utils                                          [jar]
[INFO] bfd-pipeline-shared-test-utils                                     [jar]
[INFO] bfd-pipeline-ccw-rif                                               [jar]
[INFO] bfd-pipeline-rda-grpc                                              [jar]
[INFO] bfd-pipeline-app                                                   [jar]
[INFO] 
```

Notice that `bfd-server-war` and friends are not included in maven's build plan.
Likewise it can be used to only build the `bfd-server-war` and its dependencies.

```
$ ./utils/scripts/build-bfd -p bfd-server-war
... skipping some output to get to this from maven:
[INFO] ------------------------------------------------------------------------
[INFO] Detecting the operating system and CPU architecture
[INFO] ------------------------------------------------------------------------
[INFO] os.detected.name: osx
[INFO] os.detected.arch: x86_64
[INFO] os.detected.version: 13.4
[INFO] os.detected.version.major: 13
[INFO] os.detected.version.minor: 4
[INFO] os.detected.classifier: osx-x86_64
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Build Order:
[INFO] 
[INFO] bfd-parent                                                         [pom]
[INFO] bfd-shared-test-utils                                              [jar]
[INFO] bfd-shared-utils                                                   [jar]
[INFO] bfd-model-parent                                                   [pom]
[INFO] bfd-model-codebook-extractor                                       [jar]
[INFO] bfd-model-codegen-annotations                                      [jar]
[INFO] bfd-model-codegen                                                  [jar]
[INFO] bfd-model-codebook-data                                            [jar]
[INFO] bfd-model-dsl-codegen-parent                                       [pom]
[INFO] bfd-model-dsl-codegen-library                                      [jar]
[INFO] bfd-model-dsl-codegen-plugin                              [maven-plugin]
[INFO] bfd-model-rda                                                      [jar]
[INFO] bfd-model-rif                                                      [jar]
[INFO] bfd-model-rif-samples                                              [jar]
[INFO] bfd-pipeline-parent                                                [pom]
[INFO] bfd-pipeline-shared-utils                                          [jar]
[INFO] bfd-pipeline-shared-test-utils                                     [jar]
[INFO] bfd-pipeline-ccw-rif                                               [jar]
[INFO] bfd-server-parent                                                  [pom]
[INFO] bfd-server-shared-utils                                            [jar]
[INFO] bfd-server-war                                                     [war]
```

## Option `-P`

The `-P` option is similar to the `-p` option but does not build any dependencies.
This can be useful when you are in the process of editing a single module and just want to
run all of its tests without waiting for any dependencies.

```
$ ./utils/scripts/build-bfd -a -P bfd-server-war
... skipping some output to get to this from maven:
[INFO] 
[INFO] ---------------------< gov.cms.bfd:bfd-server-war >---------------------
[INFO] Building bfd-server-war 1.0.0-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ war ]---------------------------------
```

Notice that maven leapt forward to the specified module and skipped all of its dependencies.
Obviously this will only work properly if all of the dependencies have already been built.

## Option `-m`

Adds a system property to the maven command line to tell tests to use minio rather than S3.

## Option `-x`

Adds `clean` as a maven goal.
This will cause maven to delete `target` directories.
It also puts the build cache into write only mode.

## Option `-X`

This powerful option does several things to ensure a clean build:

* Adds `clean` as a maven goal
* Deletes virtually all BFD related artifacts from your `$HOME/.m2/repository`
* Deletes your maven build cache directory `$HOME/.m2/build-cache`

Because they are expensive to produce, `-X` preserves any fda or npi data artifacts in your maven repository.

## Option `-d`

Sometimes you need to see what dependencies are being used in your build.
This option tells maven to print a text tree containing all modules and their
dependencies to stdout and then exit without building anything.
It can be combined with `-P` to get the dependencies for a single module.

```
$ ./utils/scripts/build-bfd -d -P bfd-model-dsl-codegen-library
... skipping some output to get to this from maven:
[INFO] -------------< gov.cms.bfd:bfd-model-dsl-codegen-library >--------------
[INFO] Building bfd-model-dsl-codegen-library 1.0.0-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- dependency:3.6.0:tree (default-cli) @ bfd-model-dsl-codegen-library ---
[INFO] gov.cms.bfd:bfd-model-dsl-codegen-library:jar:1.0.0-SNAPSHOT
[INFO] +- com.google.protobuf:protobuf-java-util:jar:3.22.3:compile
[INFO] |  +- com.google.protobuf:protobuf-java:jar:3.22.3:compile
[INFO] |  +- com.google.code.findbugs:jsr305:jar:3.0.2:compile
[INFO] |  +- com.google.code.gson:gson:jar:2.8.9:compile
[INFO] |  +- com.google.error-prone:error_prone_annotations:jar:2.11.0:compile
[INFO] |  +- com.google.guava:guava:jar:32.0.0-jre:compile
[INFO] |  |  +- com.google.guava:failureaccess:jar:1.0.1:compile
[INFO] |  |  +- com.google.guava:listenablefuture:jar:9999.0-empty-to-avoid-conflict-with-guava:compile
[INFO] |  |  \- org.checkerframework:checker-qual:jar:3.33.0:compile
[INFO] |  \- com.google.j2objc:j2objc-annotations:jar:1.3:compile
[INFO] +- org.projectlombok:lombok:jar:1.18.26:provided
[INFO] +- org.apache.commons:commons-csv:jar:1.10.0:compile
[INFO] +- org.junit.jupiter:junit-jupiter-engine:jar:5.9.2:test
[INFO] |  +- org.junit.platform:junit-platform-engine:jar:1.9.2:test
[INFO] |  |  +- org.opentest4j:opentest4j:jar:1.2.0:test
[INFO] |  |  \- org.junit.platform:junit-platform-commons:jar:1.9.2:test
[INFO] |  +- org.junit.jupiter:junit-jupiter-api:jar:5.9.2:test
[INFO] |  \- org.apiguardian:apiguardian-api:jar:1.1.2:test
[INFO] \- gov.cms.bfd:bfd-shared-utils:jar:1.0.0-SNAPSHOT:compile
[INFO]    +- org.flywaydb:flyway-core:jar:9.16.1:compile
[INFO]    |  \- com.fasterxml.jackson.dataformat:jackson-dataformat-toml:jar:2.14.0:compile
[INFO]    |     +- com.fasterxml.jackson.core:jackson-databind:jar:2.15.0:compile
[INFO]    |     \- com.fasterxml.jackson.core:jackson-core:jar:2.14.0:compile
[INFO]    +- software.amazon.awssdk:ssm:jar:2.20.62:compile
... skipping a lot more output...
```

## Option `-D`

Does a build in both the `bfd-data-fda` and `bfd-data-npi` modules to download the latest data.
This can take a while to run so it is best done from time to time but not every day.

## Option `-F`

This runs the google format plugin and then exits without building anything.
This is useful when you want to update all code to the latest format standard.
It can take a while to run.

## Option `-I`

This allows you to specify a regular expression to limit which integration tests are executed.
This should always be used with a single `-P` option since maven will complain if it
fails to find a matching test within a module.

For example this will execute all unit tests in `bfd-pipeline-rda-grpc` but will skip all
integration tests except for `McsClaimStreamCallerIT`.

```
$ ./utils/scripts/build-bfd -P bfd-pipeline-rda-grpc -I McsClaimStreamCallerIT
```

## Option `-T`

Similar to `-I` but applies to unit tests instead of integration tests.
This should always be used with a single `-P` option since maven will complain if it
fails to find a matching test within a module.

For example this will execute only the `StandardGrpcRdaSourceTest` unit test
in `bfd-pipeline-rda-grpc`.

```
$ ./utils/scripts/build-bfd -P bfd-pipeline-rda-grpc -T StandardGrpcRdaSourceTest
```
