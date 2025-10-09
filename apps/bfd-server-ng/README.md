# BFD Server NG

This project contains the Spring Boot-based implementation of the BFD server project.
Eventually, this should replace the existing BFD server implementation that currently serves traffic for the V1 and V2
endpoints.

## Configuration

We use Spring profiles to modify the behavior of the application:

- `local` - configures the application to run against a local database, optionally using localstack for SSM-based
  configuration

## Java / Amazon Corretto upgrade (21 → 25)</span>

## Summary

This repository has been upgraded from Amazon Corretto JDK 21 to Amazon Corretto JDK 25.
The upgrade was applied across multiple modules and top-level POMs.
## Installing / selecting Corretto 25 (macOS)

If you use Homebrew, install and select Corretto 25. Otherwise install the
Amazon Corretto 25 .pkg from Amazon and set your `JAVA_HOME`.

Example (macOS):

```sh
# If Amazon Corretto 25 is installed via the macOS JDK installer, set JAVA_HOME via /usr/libexec/java_home
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
export PATH="$JAVA_HOME/bin:$PATH"

# Verify
java -version
mvn -v
```

If you installed Corretto using a package that created a JVM under `/Library/Java/JavaVirtualMachines/`, you can set
`JAVA_HOME` directly, for example:

```sh
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-25.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
```

### Using Maven Toolchains (optional)

If you use Maven toolchains to select the JDK for builds, create or update the file `~/.m2/toolchains.xml` with a JDK entry for version 25. Example:

```xml
<toolchains>
  <!-- JDK toolchains -->
  <toolchain>
   <type>jdk</type>
   <provides>
     <version>25</version>
   </provides>
   <configuration>
      <jdkHome>/path/to/amazon-corretto-25.jdk/Contents/Home</jdkHome>
   </configuration>
  </toolchain>
</toolchains>
```

- `aws` - configures the application to run against an RDS database and requires configuration to be loaded from SSM

The `BFD_ENV` environment variable is required to be set when running the application with the `aws` profile enabled.
This controls the prefix for loading SSM parameters.

Environment variables can be used to override parameters:

```sh
export BFD_SENSITIVE_DB_USERNAME=someUser
```

### Local SSM configuration

You can verify SSM configuration is working correctly using `localstack`.

using [`awslocal`](https://github.com/localstack/awscli-local):

```sh
awslocal ssm put-parameter --name "/bfd/local/server/nonsensitive/some_key" --value "some_value" --overwrite --type String
```

## Running the application

Run the application using the default profile (`local`)

### From Intellij

You can start the API from the run menu. Just make sure the option to include "provided" dependencies is checked.

On the entrypoint:

- Edit run configuration
- Click "modify options"
- Check "Add dependencies with 'provided' scope to classpath"

Note that Intellij's support for Spring Boot features requires the licensed version,
but the community edition can still run it without all the bells and whistles.

### From the CLI

```sh
mvn spring-boot:run
```

Run the application with a specific profile

```sh
export BFD_ENV=1000-test
mvn spring-boot:run -Dspring-boot.run.profiles=aws
```

or using environment variables only

```sh
export BFD_ENV=1000-test
export SPRING_PROFILES_ACTIVE=aws
mvn spring-boot:run
```

### Connecting to a specific database

Override the database connection parameters to change the database from the default local configuration

```sh
export BFD_SENSITIVE_DB_USERNAME=user 
export BFD_SENSITIVE_DB_PASSWORD=password 
export BFD_LOCAL_DB_HOST=host 
mvn spring-boot:run
```

### Profiling SQL queries

The `sqlprofile` profile can be used to log SQL queries and output metrics.

```sh
mvn spring-boot run -Dspring-boot.run.profiles=local,sqlprofile
```

## Swagger

The Swagger UI is available at `/v3/fhir/swagger-ui`

## Tests

Note that certain Postgres environment variables can interfere with the database connection in the tests,
namely `PGSSLMODE`.

You'll probably want to unset these when running the tests from the terminal.

### Regenerating snapshots

Run with `-DupdateSnapshot=` to update the snapshots (yes, the trailing equals is required, unfortunately).

If snapshot tests fail, they will generate a `.patch` file with the difference that can be viewed using your diff tool
of choice.

```sh
mvn clean verify -DupdateSnapshot=
```

### Debugging Tests

Log messages from the server process will not be shown in the console output.
To see detailed log info, look at the files created under `target/failsafe-reports/logs`.
