# BFD Server NG

This project contains the Spring Boot-based implementation of the BFD server project.
Eventually, this should replace the existing BFD server implementation that currently serves traffic for the V1 and V2
endpoints.

## Configuration

We use Spring profiles to modify the behavior of the application:

- `local` - configures the application to run against a local database, optionally using localstack for SSM-based
  configuration
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

### Regenerating snapshots

Run with `-DupdateSnapshot=` to update the snapshots (yes, the trailing equals is required, unfortunately).

If snapshot tests fail, they will generate a `.patch` file with the difference that can be viewed using your diff tool of choice.

```sh
mvn clean verify -DupdateSnapshot=
```
