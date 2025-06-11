# BFD Loadtest

## Overview
The tests in this directory are designed to test the BFD server endpoints when under a certain load of requests, configurable
using parameters passable when running the tests. These tests use locust, a python-based concurrent test runner that is able to ramp
requests up to a maximum target number of users making requests against an endpoint. The tests are all written in python, with only a
few dependencies to install. The tests pull a sample of random data from the target environment's database, attempting to randomize
to avoid caching affecting the response times of repeat runs.

These tests are intended to be run from a server instance which has access permissions to reach out to the BFD endpoints. How to
do this is outside the scope of this document, but instructions for this can be found in this more detailed run guide on confluence:
https://confluence.cms.gov/display/BFD/2022+Run+the+BFD+Load+Tests.

The following test suites ([Locustfiles](https://docs.locust.io/en/stable/writing-a-locustfile.html)) are available to run:

| Suite Name                 | Path                          | Purpose                                                                                                                                                        |
| -------------------------- | ----------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| V2 Regression Suite        | `v2/regression_suite.py`      | Used to test performance regressions for BFD Server's most popular V2 API endpoints                                                                            |
| V2 Regression Suite (POST) | `v2/regression_suite_post.py` | Used to test performance regressions for BFD Server's most popular V2 API endpoints using POST requests                                                        |
| V1 Regression Suite        | `v1/regression_suite.py`      | Used to test performance regressions for BFD Server's most popular V1 API endpoints                                                                            |
| Load Testing Suite         | `high_volume_suite.py`        | Used to exhibit distributed load on a target BFD Server to produce performance metrics under stress. Invoked by the `bfd-run-server-load` Jenkins pipeline job |
| Load Testing Suite (POST)  | `high_volume_suite_post.py`   | Used to exhibit distributed load on a target BFD Server to produce performance metrics under stress using POST requests                                        |

## Dependencies

### Installing Dependencies

The Locust tests use the `uv` package manager to manage dependencies.

1. Ensure `uv` is installed

   ```sh
   brew install uv
   ```

2. (Not necessary, `uv run ...` does this automatically on first invocation) Sync dependencies

   ```sh
   uv sync
   ```

## BFD Server TLS Certificate

In addition to the software dependencies listed above, you'll also need to copy a PEM (credentials) file.
Run the following two commands on the box from your local directory:

Save a copy of the mTLS certificate that matches the environment you want to test against. The certificate should be in PEM format. Keys can be found in AWS parameter store.

**IMPORTANT**: Be sure the certificate includes both the public and private key (the two keys should be concatenated into a single file).

## Running a test

There are a number of tests which each test various endpoints (and param combinations for those endpoints) for different released versions of BFD.

Tests are run by invoking Locust locally, either via the `locust` binary or via the included `run_locally_distributed.py` helper script. A test run can be invoked with various arguments as well; reference for Locust's built-in arguments is available [here](https://docs.locust.io/en/stable/configuration.html#command-line-options), and the reference for custom arguments can be found below.

### Custom arguments

> **Note 1:** The various arguments referred to here can also be defined in the `pyrpoject.toml` file well as through the command-line; see [Locust's documentation](https://docs.locust.io/en/stable/configuration.html#pyproject-toml) on configuration files for how that works.

> **Note 2:** The arguments here can also be defined via environment variables, similarly to Locust's built-in arguments.

> **Note 3:** `--host`, a Locust-provided parameter, _must_ include the protocol (i.e. HTTP/HTTPS), port. For example, `https://google.com` is a _valid_ value for `--host` whereas `google.com/` **is not**!

> **Note 4:** If an environment is delimited by either `-` or `_` depending on context (e.g., `prod-sbx` or
> `prod_sbx` re: Glue Table partition columns) a better way of handling its string representation may need to be
> considered. For now, `prod-sbx` is the only string representation expected to be encountered by this code
> and other contexts.

| Command Line                   | Environment Variable                    | Config File                  | Required?                                                            | Default Value | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| ------------------------------ | --------------------------------------- | ---------------------------- | -------------------------------------------------------------------- | ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `--client-cert-path`           | `LOCUST_BFD_CLIENT_CERT_PATH`           | `client-cert-path`           | Yes                                                                  | N/A           | The path to the PEM file we copied/modified earlier                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| `--database-connection-string` | `LOCUST_BFD_DATABASE_CONNECTION_STRING` | `database-connection-string` | Yes                                                                  | N/A           | Specifies the necessary parameters for connecting to a database in a "connection-string"-like format: `<db_type>://<username>:<password>@<db_host>:<db_port>/<db_table>`. `password` _must_ be url-encoded                                                                                                                                                                                                                                                                                                                                                            |
| `--spawned-runtime`            | `LOCUST_USERS_SPAWNED_RUNTIME`          | `spawned-runtime`            | No                                                                   | `None`        | Specifies the test runtime limit that begins after _all users have spawned_ when running tests with the custom `UserInitAwareLoadShape` load shape, which should be **all** of the tests in this repository. If unspecified, tests run **indefinitely** after _all users_ have spawned. Specifying `0<s/h/m>` will stop the tests **immediately** once _all users_ have spawned. Note that this is **not the same option** as `--run-time`, which handles the total runtime limit for the Locust run including non-test tasks and does not compensate for spawn rate. |
| `--server-public-key`          | `LOCUST_BFD_SERVER_PUBLIC_KEY`          | `server-public-key`          | No                                                                   | `""`          | To allow the tests to trust the server responses, you can add the path to the public certificate here. This is not required to run the tests successfully, and may not be needed as a parameter at all. Does not cause any issues if omitted                                                                                                                                                                                                                                                                                                                          |
| `--table-sample-percent`       | `LOCUST_DATA_TABLE_SAMPLE_PERCENT`      | `table-sample-percent`       | No                                                                   | `0.25`        | Determines how big a slice of the Beneficiaries table we want to use when finding endpoints for testing. Defaults to 0.25 (one-quarter of one percent), which is plenty for production databases with millions of rows. Note that this is only meant to randomize and streamline the data query, but if this option is set too small, it would act as a cap on the number of rows available. For the Test environment or local testing, it might be best to set to 100, which will effectively *not* use table sampling                                               |
| `--stats-store-file`           | `LOCUST_STATS_STORE_TO_FILE`            | `stats-store-file`           | No, _but is required when specifying other --stats* flags_           | `False`       | Specifies that JSON stats will be stored to a file. Note that the `file` `store` is primarily meant for _local debugging_ purposes and should not be used when running these tests as part of a process where the performance statistics should be stored for later retrieval. **Cannot be specified with `--stats-store-s3`**                                                                                                                                                                                                                                        |
| `--stats-store-s3`             | `LOCUST_STATS_STORE_TO_S3`              | `stats-store-s3`             | No, _but is required when specifying other --stats* flags_           | `False`       | Specifies that JSON stats will be stored to S3. If specified, `--stats-store-s3-bucket`, `--stats-store-s3-database`, and `--stats-store-s3-table` _must_ be specified. **Cannot be specified with `--stats-store-file`**                                                                                                                                                                                                                                                                                                                                             |
| `--stats-env`                  | `LOCUST_STATS_ENVIRONMENT`              | `stats-env`                  | No, _but is required when specifying other --stats* flags_           | `None`        | Specifies the test running environment which the tests are running against. May be any of `test`, `prod-sbx`, or `prod`, or any ephemeral environment whose name ends with one of those values, e.g `[TICKET_NUM]-test`. Case-insensitive.                                                                                                                                                                                                                                                                                                                            |
| `--stats-store-tag`            | `LOCUST_STATS_STORE_TAG`                | `stats-store-tag`            | No, _but is required when specifying other --stats* flags_           | `[]`          | Specifies the tags under which collected statistics will be stored. Can be specified multiple times to store stats under multiple tags                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `--stats-store-file-path`      | `LOCUST_STATS_STORE_FILE_PATH`          | `stats-store-file-path`      | No                                                                   | `./`          | Specifies the parent directory where JSON stats will be written to. _Only used if `--stats-store-file` is specified_                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| `--stats-store-s3-bucket`      | `LOCUST_STATS_STORE_S3_BUCKET`          | `stats-store-s3-bucket`      | No, _but is required when specifying `--stats-store-s3`_             | `None`        | Specifies the S3 bucket that JSON stats will be written to. _Only used if `--stats-store-s3` is specified_                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `--stats-store-s3-database`    | `LOCUST_STATS_STORE_S3_DATABASE`        | `stats-store-s3-database`    | No, _but is required when specifying `--stats-store-s3`_             | `None`        | Specifies the Athena database that is queried upon when comparing statistics. Also used as part of the S3 key/path when storing stats to S3. _Only used if `--stats-store-s3` is specified_                                                                                                                                                                                                                                                                                                                                                                           |
| `--stats-store-s3-table`       | `LOCUST_STATS_STORE_S3_TABLE`           | `stats-store-s3-table`       | No, _but is required when specifying `--stats-store-s3`_             | `None`        | Specifies the Athena table that is queried upon when comparing statistics. Also used as part of the S3 key/path when storing stats to S3. _Only used if `--stats-store-s3` is specified_                                                                                                                                                                                                                                                                                                                                                                              |
| `--stats-compare-previous`     | `LOCUST_STATS_COMPARE_PREVIOUS`         | `stats-compare-previous`     | No, _but is required when specifying other `--stats-compare*` flags_ | `False`       | Specifies that the current run's performance statistics will be compared against the previous matching run's performance statistics. **Cannot be specified with `--stats-compare-average`**                                                                                                                                                                                                                                                                                                                                                                           |
| `--stats-compare-average`      | `LOCUST_STATS_COMPARE_AVERAGE`          | `stats-compare-average`      | No, _but is required when specifying other `--stats-compare*` flags_ | `False`       | Specifies that the current run's performance statistics will be compared against an average of the last, by default, 5 (controlled by `--stats-compare-load-limit`) matching runs. **Cannot be specified with `--stats-compare-previous`**                                                                                                                                                                                                                                                                                                                            |
| `--stats-compare-tag`          | `LOCUST_STATS_COMPARE_TAG`              | `stats-compare-tag`          | No, _but is required when specifying other `--stats-compare*` flags_ | `None`        | Specifies the tag that matching runs will be found under to compare against                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `--stats-compare-load-limit`   | `LOCUST_STATS_COMPARE_LOAD_LIMIT`       | `stats-compare-load-limit`   | No                                                                   | `5`           | Specifies the limit for number of previous stats to load when when doing comparisons, defaults to 5. Used solely for limiting stats loaded during average comparisons                                                                                                                                                                                                                                                                                                                                                                                                 |
| `--stats-compare-meta-file`    | `LOCUST_STATS_COMPARE_META_FILE`        | `stats-compare-meta-file`    | No                                                                   | `None`        | Specifies the file path to a JSON file containing metadata about how stats should be compared for a given test suite. Overrides the default path specified by a test suite, if any                                                                                                                                                                                                                                                                                                                                                                                    |

### Running via `locust` binary

If the test suite to run will _not_ be bottlenecked by the local machine's single-threaded performance (which is _very_ likely the case, as its atypical for the tests themselves to be the performance bottleneck), then running tests via Locust's binary is the best choice:

```sh
uv run locust -f <locustfile> <command-line-args>...
```

It is recommended to always include the `--headless` command-line argument so that Locust [runs in "headless" mode](https://docs.locust.io/en/stable/quickstart.html#direct-command-line-usage-headless), without its included Web UI:

```sh
uv run locust -f <locustfile> --headless <command-line-args>...
```

#### Example

```sh
uv run locust -f v2/regression_suite.py --headless --client-cert=/path/to/your/cert --users 10 --spawn-rate 10 --spawned_runtime 1h30m --host https://test.bfd.cms.gov
```

### Debugging

There is a sample configuration in the `.vscode-sample` folder.
Copy this into your `.vscode` folder and change the parameters as desired to run locust with a debugger.

See [Locust's documentation](https://docs.locust.io/en/stable/quickstart.html#getting-started) for more information.

### Using the Web UI

You can omit the `--headless` flag to run the tests with the Web UI enabled. The UI can show the changes in throughput over time.
Open `http://localhost:8089` to view the UI.

### Running via `run_locally_distributed.py`

If the tests will be bottlenecked by the runner's single-threaded performance and the runner has multiple CPU cores, then using the included `run_locally_distributed.py` helper script could help alleviate this bottlenecking. Typically, the Locust tests being the bottleneck will only occur with low-load tests, such as single endpoint test suites, running with a large number of simulated users.

The `run_locally_distributed.py` helper script simplifies the usage of Locust's [distributed mode](https://docs.locust.io/en/stable/running-distributed.html#distributed-load-generation) _locally_. This script does not work for running distributedly across multiple machines. Essentially, this script spawns one `locust` master process and `n` `locust` worker processes. The master process spreads the number of simulated users across the worker processes as evenly as possible, and then the worker processes actually run the tests. The master process does not run any tests, it only orchestrates the worker processes and aggregates the performance statistics from each worker.

The script has a single command-line argument, `--workers`, which is the number of worker processes to spawn and it defaults to `1`. Any additional command-line arguments will be passed to the Locust master process:

```
uv run run_locally_distributed.py -f <locustfile> --workers 3 <other-args>...
```

## Reading the results

After running the tests, you'll get a results printout that looks like this:

```
 Name                                                                              # reqs      # fails  |     Avg     Min     Max  Median  |   req/s failures/s
----------------------------------------------------------------------------------------------------------------------------------------------------------------
 GET /v2/fhir/Patient search by coverage contract (all pages)                         427     0(0.00%)  |      11       7      88      10  |  425.87    0.00
----------------------------------------------------------------------------------------------------------------------------------------------------------------
 Aggregated                                                                           427     0(0.00%)  |      11       7      88      10  |  425.87    0.00

Response time percentiles (approximated)
 Type     Name                                                                                  50%    66%    75%    80%    90%    95%    98%    99%  99.9% 99.99%   100% # reqs
--------|--------------------------------------------------------------------------------|---------|------|------|------|------|------|------|------|------|------|------|------|
 GET      /v2/fhir/Patient search by coverage contract (all pages)                               10     11     11     12     15     19     25     77     89     89     89    427
--------|--------------------------------------------------------------------------------|---------|------|------|------|------|------|------|------|------|------|------|------|
 None     Aggregated                                                                             10     11     11     12     15     19     25     77     89     89     89    427

```

You can use this to check against the expected SLAs and see if there were any failures as a result of the load placed against the endpoint. If the endpoint encountered any failures,
determined by an HTTP response code >200, they will be reported here.

The results will look the same whether running in distributed or non-distributed modes.

## Troubleshooting
- The run box may not have permissions to hit another single node instance, in which case the test will repeat 0 requests with no error message.
    - If this occurs, you may need to adjust the ACLs in AWS to allow the box to connect to other single instances. This silent error may occur with other types of connection issues as well.
- If there is any issue with the call, you'll also see 0 requests, often with no error message.
    - You'll need to do some additional debugging per endpoint to figure out the issue here; adjusting the LOCUST_LOGLEVEL in the runtests.py file to be DEBUG may help detect a problem.
