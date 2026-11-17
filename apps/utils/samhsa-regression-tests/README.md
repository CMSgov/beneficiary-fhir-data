<!-- markdownlint-disable MD013 -->

# `samhsa-regression-tests`

This Python utility is composed of a single script, `main.py`, that when ran with the correct inputs will verify that SAMHSA filtering on the V3 Server is working as expected. This is a full E2E-test, so _real_ data is seeded from the appropriate database before sending requests to the V3 Server.

The script does the following:

1. Concurrently loads SAMHSA claims from the database by matching on known SAMHSA codes that exist in various columns and tables
2. Matches the aforementioned SAMHSA claims to a single beneficiary via their `bene_sk`
3. Sends concurrent requests using a certificate authorized to view SAMHSA claims and one that is not for the beneficiary with SAMHSA claims
4. Verifies that the "SAMHSA-allowed" request returned SAMHSA claims and the "SAMHSA-disallowed" request did not
5. If _all_ beneficiaries pass verification, the script returns a success result; otherwise, the script returns a failure result and exits with a non-zero error code

## Usage

```bash
just samhsa-regression-test --env test
```

## Building the Docker Image

This utility takes the server-ng `security_labels.yml` file as a build-time dependency. That file is what the V3 server uses to decide which claims are SAMHSA-relevant. The `Dockerfile` copies it from the repository-root build context instead of keeping a separate copy here.

```sh
just docker-build bfd-platform-samhsa-regression
```

Use `--tag <version>` to tag the image and the base images it is built from, or `--platform` to build for another architecture. See [`ops/utils/scripts/README.md`](../../../ops/utils/scripts/README.md) for the full list of options.

## Running as a Lambda

The image's entry point is `app.handler.handler`, which is invoked synchronously by the
`samhsa-hook` wrapper Lambda during an ECS service deployment. The handler expects:

- Environment variables `BFD_ENVIRONMENT` and `READER_ENDPOINT`
- An invoke payload with a `host` (the V3 server under test), plus optional `tablesample`,
  `limit`, and `concurrency` overrides
- SSM parameters `/bfd/{env}/server-ng/sensitive/db/username|password` for database access and
  `/bfd/{env}/server-ng/sensitive/test_client*` for the test certificates

It returns a result model (`result`, `message`, `log_stream_name`, `log_group_name`) that the
wrapper converts into an ECS `hookStatus`.
