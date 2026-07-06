# BFD Server NG

This project contains the Spring Boot-based implementation of the BFD server project.
Eventually, this should replace the existing BFD server implementation that currently serves traffic for the V1 and V2
endpoints.

## Configuration

We use Spring profiles to modify the behavior of the application:

- `local` - configures the application to run against a local database, optionally using miniStack for SSM-based
  configuration
- `aws` - configures the application to run against an RDS database and requires configuration to be loaded from SSM

The `BFD_ENV` environment variable is required to be set when running the application with the `aws` profile enabled.
This controls the prefix for loading SSM parameters.

Environment variables can be used to override parameters:

```sh
export BFD_SENSITIVE_DB_USERNAME=someUser
```

### Local SSM configuration

You can verify SSM configuration is working correctly using `ministack`.

```sh
aws --endpoint-url=http://localhost:4566 ssm put-parameter --name "/bfd/local/server/nonsensitive/some_key" --value "some_value" --overwrite --type String
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

The `sql-profile` profile can be used to log SQL queries and output metrics.

```sh
mvn spring-boot run -Dspring-boot.run.profiles=local,sql-profile
```

### Structured Logging

The `structured-log` profile can be used to test structured logs.

```sh
mvn spring-boot run -Dspring-boot.run.profiles=local,structured-log
```

## Swagger

The Swagger UI is available at `/v3/fhir/swagger-ui`

## Sample Requests

Sample requests are available under the `sample-requests` folder.

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

## mTLS, SAMHSA Authorization, and Security Configuration

BFD utilizes mTLS for authentication, authorization (specifically for SAMHSA data), and transport security.

### How it works
1. **Infrastructure**: In V3, the Load Balancer does mTLS for us, passing the certificate body to the application via the `X-Amzn-Mtls-Clientcert` HTTP header.
2. **Authentication**: The certificate is validated against our truststore.
3. **Authorization**: The application hashes the certificate body and looks it up in the SSM hierarchy: `/bfd/<env>/server(-ng)/nonsensitive/client_certificates/*`.
4. **SAMHSA Access**: If a match is found, the certificate's alias is checked against a SAMHSA-authorized alias list in SSM. If present, the request is authorized to retrieve SAMHSA data.

### Development & Testing
*   **Remote/Deployed v3 Servers**: Requests must include the client certificate (e.g., `curl --cert ...`) to pass the LB's mTLS handshake.
*   **Local**: Local servers operate at the HTTP layer without active mTLS. To test SAMHSA logic locally:
    *   The `local` profile uses `application-local.properties`.
    *   A mock certificate with alias `samhsa_allowed` and body `samhsa_allowed` is pre-configured as authorized.
    *   **Manual Testing**: Use `xh` or `curl` to manually set the header: `X-Amzn-Mtls-Clientcert: samhsa_allowed`.

```bash
xh \
  'http://localhost:8080/v3/fhir/ExplanationOfBenefit?patient=<bene>' \
  'X-Amzn-Mtls-Clientcert:samhsa_allowed' | jq
```

### Disabling Endpoints

Occasionally, we may need to disable specific endpoints for security reasons or to allow for safely testing new features without allowing external users to access them.
To do this, we can use two parameters in SSM:

* `/bfd/${env}/server-ng/nonsensitive/internal_certificate_aliases_json` - lists which certificates are used only internally and should be able to access disabled endpoints.
* `/bfd/${env}/server-ng/nonsensitive/disabled_uris_json` - list of disabled URIs. This uses a prefix match, so disabling `/v3/fhir/Patient` would disable `/v3/fhir/Patient/1` as well.

## Generating enum values

We utilize enums frequently for representing discrete sets of values.
The values are typically enumerated in the sushi files, which means we can utilize the output from the FSH transformations
to generate these enums for us.

To use the codegen script (replace `CodeSystem-CLM-ADJSTMT-TYPE-CD` with the corresponding FSH file that you want to generate values from):

```bash
python ./codegen/main.py --file CodeSystem-CLM-ADJSTMT-TYPE-CD
```

The output will be under `./codegen/out`

The output will not be a complete java class, but just a fragment that you can copy inside of an `Enum` block.
The output may require some modifications.

## Audit Events
BFD server captures audit events on the Patient/$idi-match operation. These events are stored in a DynamoDB not captured locally. If you want to test of make any changes that require interaction with the DynamoDB the following steps are required:

- Pull down a local dynamoDB docker image and run it locally. You can use the following command to do that:

```bash
docker pull amazon/dynamodb-local:3.3.0
docker run -p 8000:8000 amazon/dynamodb-local
```

- Install AWS CLI v2 and configure it to point to the local DynamoDB instance with instructions from the [AWS documentation](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html).
- Create a local profile for AWS with the CLI
```bash
aws configure set aws_access_key_id local --profile local
aws configure set aws_secret_access_key local --profile local
aws configure set region us-east-1 --profile local
aws configure set output json --profile local
```

- Install the table schema using the AWS CLI
```bash
AWS_ACCESS_KEY_ID=dummy \
AWS_SECRET_ACCESS_KEY=dummy \
AWS_REGION=us-east-1 \
aws dynamodb create-table \
  --table-name bfd-local-patient-match-audit \
  --attribute-definitions \
    AttributeName=matchedBeneSk,AttributeType=N \
    AttributeName=timestamp,AttributeType=S \
  --key-schema \
    AttributeName=matchedBeneSk,KeyType=HASH \
    AttributeName=timestamp,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://localhost:8000
```
- Verify that the table was created successfully by running the following command:
```bash
AWS_ACCESS_KEY_ID=dummy \
AWS_SECRET_ACCESS_KEY=dummy \
AWS_REGION=us-east-1 \
aws dynamodb list-tables --endpoint-url http://localhost:8000
```

