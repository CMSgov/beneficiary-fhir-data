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

This utility references the server-ng `security_labels.yml` file as a build-time dependency. That
file is the same source of truth the V3 server uses to decide which claims are SAMHSA-relevant, so
instead of vendoring a copy here, the build uses a
[named context](https://docs.docker.com/reference/cli/docker/buildx/build/#build-context) pointing
at `apps/bfd-server-ng/src/main/resources`, which the `Dockerfile` then references with
`COPY --from=server-ng-resources ...`.

### Using `dockerbuild.sh`

Typical local build:

```sh
./dockerbuild.sh
```

With arguments:

```sh
./dockerbuild.sh --build_arg base_version=2.999.0
```

### Using `podman`/`docker`

> [!NOTE]
> This is identical to what the `dockerbuild.sh` script does, so just use that. This is included
> for completeness.

```sh
DOCKER_BUILDKIT=1 podman buildx build "." \
  --file Dockerfile \
  --platform "linux/arm64" \
  --build-context server-ng-resources="../../bfd-server-ng/src/main/resources" \
  --tag "bfd-platform-samhsa-regression"
```

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
