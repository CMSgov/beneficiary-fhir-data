# BFD Loadtest

## Overview
The tests in this directory are designed to test the BFD server endpoints when under a certain load of requests, configurable
using parameters passable when running the tests. These tests use locust, a python-based concurrent test runner that is able to ramp
requests up to a maximum target number of users making requests against an endpoint. The tests are all written in python, with only a
few dependencies to install. The tests pull a sample of random data from the target environment's database, attempting to randomize
to avoid caching affecting the response times of repeat runs.

These tests are intended to be run from a server instance which has access permissions to reach out to the BFD endpoints. How to
do this is outside the scope of this document, but instructions for this can be found in this more detailed run guide on confluence:
https://confluence.cms.gov/display/BB2/Run+the+BFD+Load+Tests. 


## Dependencies

> **Important:** This project is targeting Python 3.8. Ensure that your local Python is version 3.8 **before** attempting to work in this project!
> A good utility for managing multiple Python versions along with [virtualenv](https://virtualenv.pypa.io/en/latest/)s is [`pew`](https://github.com/berdario/pew).

The tests use a few python libraries that will need to be installed on the run box prior to starting the tests. 
Below are the required libraries, what they do, and the command to install them.

To install everything below at the same time you can use the provided requirements file `pip3 install -r requirements.txt`:

### "Production" Dependencies

> Dependencies are considered _production dependencies_ if the dependency is used in _application_ code (i.e. the tests, scaffolding, etc.). They can be thought of as dependencies necessary to include when _deploying_ the project.

| Library | Description | Reason for Usage | References |
| - | - | - | - |
| `psycopg2` | Library for connecting to a postGres database programatically via python | The tests use this to connect to the environment's database in order to gather various randomized data sets to run each test | <https://pypi.org/project/psycopg2/> |
| `psycogreen` | Library that enables for `psycopg2` to work with coroutine libraries | `locust` uses `gevent` behind-the-scenes to spawn green theads. Without this package, data gathering will block threaded operations | <https://github.com/psycopg/psycogreen/> |
| `locust` | Test runner framework. Provides various utilities like configuration, argument parsing, and more. Allows for performance statistics collection | Runs the test execution and reporting | <http://docs.locust.io/en/stable/installation.html>
| `boto3` | Library for interfacing with AWS services | Store and load performance stats from S3 | <https://boto3.amazonaws.com/v1/documentation/api/latest/index.html>

### Developer Dependencies

> Dependencies are considered _developer dependencies_ if the dependency is used solely for the developer environment. Typically these dependencies are tooling, like linters, formatters, package sorters, etc. Unlike _production dependencies_, developer dependencies can be omitted before deploying the project.

| Library | Description | Reason for Usage | References |
| - | - | - | - |
| `black` | Opionated, automated Python code formatter | Used to enforce a consistent code style | <https://black.readthedocs.io/en/stable/> |
| `isort` | Automated, consistent Python import sorter | Used to enforce a consistent import order | <https://pycqa.github.io/isort/> |
| `pylint` | Static code analyser for Python | Used to help improve code quality by identifying issues ahead-of-time | <https://pylint.pycqa.org/en/latest/> |

## BFD Server TLS Certificate

In addition to the software dependencies listed above, you'll also need to copy a PEM (credentials) file and set the user to your SSH username on the test box. 
Run the following two commands on the box from your local directory:

```
sudo cp /usr/local/bfd-server/bluebutton-backend-test-data-server-client-test-keypair.pem ~/

sudo chown <SSH-username>:<SSH-username> ~/bluebutton-backend-test-data-server-client-test-keypair.pem
```

Once this is done you'll have a modified PEM to use for the tests in your user directory.

## Running a test

There are a number of tests which each test various endpoints (and param combinations for those endpoints) for different released versions of BFD.

Tests are run by invoking Locust locally, either via the `locust` binary or via the included `run_locally_distributed.py` helper script. A test run can be invoked with various arguments as well; reference for Locust's built-in arguments is available [here](https://docs.locust.io/en/stable/configuration.html#command-line-options), and the reference for custom arguments can be found below. 

### Custom arguments

> **Note 1:** The various arguments referred to here can also be defined in a `locust.conf` key-value file as well as through the command-line; see [Locust's documentation](https://docs.locust.io/en/stable/configuration.html#configuration-file) on configuration files for how that works.

> **Note 2:** The arguments here can also be defined via environment variables, similarly to Locust's built-in arguments.

| Command Line | Environment Variable | Config File | Required? | Default Value | Description |
| - | - | - | - | - | - |
| `--client-cert-path` | `LOCUST_BFD_CLIENT_CERT_PATH` | `client-cert-path` | Yes | N/A |  The path to the PEM file we copied/modified earlier |
| `--database-uri` | `LOCUST_BFD_DATABASE_URI` | `database-uri` | Yes | N/A | Specifies the necessary parameters for connecting to a database in a "connection-string"-like format: `<db_type>://<username>:<password>@<db_host>:<db_port>/<db_table>` |
| `--spawned-runtime` | `LOCUST_USERS_SPAWNED_RUNTIME` | `spawned-runtime` | No | `None` | Specifies the test runtime limit that begins after _all users have spawned_ when running tests with the custom `UserInitAwareLoadShape` load shape, which should be **all** of the tests in this repository. If unspecified, tests run **indefinitely** after _all users_ have spawned. Specifying `0<s/h/m>` will stop the tests **immediately** once _all users_ have spawned. Note that this is **not the same option** as `--run-time`, which handles the total runtime limit for the Locust run including non-test tasks and does not compensate for spawn rate. |
| `--server-public-key` | `LOCUST_BFD_SERVER_PUBLIC_KEY` | `server-public-key` | No | `""` | To allow the tests to trust the server responses, you can add the path to the public certificate here. This is not required to run the tests successfully, and may not be needed as a parameter at all. Does not cause any issues if omitted
| `--table-sample-percent` | `LOCUST_DATA_TABLE_SAMPLE_PERCENT` | `table-sample-percent` | No | `0.25` | Determines how big a slice of the Beneficiaries table we want to use when finding endpoints for testing. Defaults to 0.25 (one-quarter of one percent), which is plenty for production databases with millions of rows. Note that this is only meant to randomize and streamline the data query, but if this option is set too small, it would act as a cap on the number of rows available. For the Test environment or local testing, it might be best to set to 100, which will effectively *not* use table sampling
| `--stats-config` | `LOCUST_STATS_CONFIG` | `stats-config` | No | `None` | Argument specifying that aggregated performance statistics should be collected and stored to some location. This can either be to a local file or to an S3 bucket. This argument must be specified as a list of key-value pairs seperated by semi-colons: `--stats-config="store=<file/s3>;env=<TEST/PROD>;store_tag=;path=;bucket=;database=;table=;compare=<previous/average>;comp_tag=;"`. See [`--stats-config` Reference](#stats-config-ref) below for more information on this argument

#### <a id='stats-config-ref'>`--stats-config` Reference</a>

| Key | Required? | Possible Values | Description |
| :-: | :-: | :-: | - |
| `store` | Yes | `file`, `s3` | Specifies where the stats are stored. Note that the `file` `store` is primarily meant for _local debugging_ purposes and should not be used when running these tests as part of a process where the performance statistics should be stored for later retrieval. 
| `env` | Yes | `TEST`, `PROD` | Specifies which environment the test ran in.
| `store_tag` | Yes | Must be non-empty, consisting of letters, numbers and the `_` character | A tag that is used to bucket or partition statistics for more accurate performance validation between corresponding runs.
| `path` | No | N/A | The _local_ **parent directory** where JSON files will be written to. Used only if `store` is `file`, ignored if `store` is `s3`.
| `bucket` | Yes, _if `store` is `s3`_ | N/A | The AWS S3 Bucket that the JSON will be written to under a predetermined path following BFD Insights data organization standards.
| `database` | Yes, _if `store` is `s3`_ | N/A | Used to construct the key under which the JSON stats will be stored in S3. Additionally, if `compare` is set, this will be the name of the database to query using Athena. |
| `table` | Yes, _if `store` is `s3`_ | N/A | Used to construct the key under which the JSON stats will be stored in S3. Additionally, if `compare` is set, this will be the name of the table to query using Athena. |
| `compare` | No | `previous`, `average` | Specifies if the current run should be compared to existing statistics. These statistics will be retrieved from the same type of store as specified by `store`. If `previous` is specified, the most recent run from `comp_tag` will be compared against. If `average` is specified, the average of _all_ previous runs from `comp_tag` will be compared against. If unspecified, no comparisons will be done.
| `comp_tag` | No | Must be non-empty, consisting of letters, numbers and the `_` character | Tag from which comparison statistics will be loaded from the given `store`. Defaults to `store_tag` if unspecified. |

### Running via `locust` binary

If the test suite to run will _not_ be bottlenecked by the local machine's single-threaded performance (which is _very_ likely the case, as its atypical for the tests themselves to be the performance bottleneck), then running tests via Locust's binary is the best choice:

```
locust -f <locustfile> <command-line-args>...
```

It is recommended to always include the `--headless` command-line argument so that Locust [runs in "headless" mode](https://docs.locust.io/en/stable/quickstart.html#direct-command-line-usage-headless), without its included Web UI:

```
locust -f <locustfile> --headless <command-line-args>...
```

See [Locust's documentation](https://docs.locust.io/en/stable/quickstart.html#getting-started) for more information.

### Running via `run_locally_distributed.py`

If the tests will be bottlenecked by the runner's single-threaded performance and the runner has multiple CPU cores, then using the included `run_locally_distributed.py` helper script could help alleviate this bottlenecking. Typically, the Locust tests being the bottleneck will only occur with low-load tests, such as single endpoint test suites, running with a large number of simulated users.

The `run_locally_distributed.py` helper script simplifies the usage of Locust's [distributed mode](https://docs.locust.io/en/stable/running-distributed.html#distributed-load-generation) _locally_. This script does not work for running distributedly across multiple machines. Essentially, this script spawns one `locust` master process and `n` `locust` worker processes. The master process spreads the number of simulated users across the worker processes as evenly as possible, and then the worker processes actually run the tests. The master process does not run any tests, it only orchestrates the worker processes and aggregates the performance statistics from each worker.

The script has a single command-line argument, `--workers`, which is the number of worker processes to spawn and it defaults to `1`. Any additional command-line arguments will be passed to the Locust master process:

```
python run_locally_distributed.py -f <locustfile> --workers 3 <other-args>...
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
