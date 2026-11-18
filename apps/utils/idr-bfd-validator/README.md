<!-- markdownlint-disable MD013 -->

# `idr-bfd-validator`

This subdirectory contains the Python source code for the `idr-bfd-validator` utility.

This utility loads data from IDR using the internal extraction logic of the IDR Pipeline and then compares that data row-by-row against the BFD database. Any "mismatches" where a column value from IDR does not match BFD is considered a failure and the row's primary keys along with the mismatched columns are logged. Additionally, inconsistent row counts between IDR and BFD are also considered a failure for the given table and partition combination.

## Running the Validator Locally

```sh
# BFD_ENV can be any BFD environment, e.g. test, 1234-prod, etc.
just idr-bfd-validator --env prod
```

### Environment Variables

| Variable | Purpose | Default |
| --- | --- | --- |
| `IDR_LOG_LEVEL` | Sets the log level | `info` |
| `ALLOW_SENSITIVE_LOGS` | Allows sensitive values to be logged without redaction; combine with `IDR_LOG_LEVEL=debug` to get as much information about mismatches as possible | `false` |
| `IDR_TABLES` | Sets the tables _from the BFD database_ that are validated | N/A (all tables) |
| `IDR_EXCLUDE_TABLES` | Sets the tables _from the BFD database_ that are NOT validated | N/A (no tables) |
| `ROW_LIMIT` | Number of random rows to pull from IDR at a time for each table+partition | `1000` |
| `MAX_PARALLELISM` | Maximum number of table+partitions to validate at once | `12` |
| `BFD_ENV` | Environment to run against; used by `./run-validator.sh` to pull credentials and get DB host | N/A |
| `ALERT_SNS_TOPIC_ARN` | SNS Topic ARN to send alerts to; if unset, no alerts will be sent | N/A |


## Building the Docker Image

This utility depends on the IDR pipeline (`idr-pipeline`) as a workspace member. The image is built from the repository root. The `Dockerfile` copies both members' sources.

```sh
just docker-build bfd-platform-idr-bfd-validator
```

To build against a specific version of the base image, tag the build. The base images built along the way are tagged with the same version. This image is built against them:

```sh
just docker-build bfd-platform-idr-bfd-validator --tag 2.999.0
```

See [`ops/utils/scripts/README.md`](../../../ops/utils/scripts/README.md) for the other options.
