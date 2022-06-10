# BFD Loadtest

## Overview
The tests in this directory are designed to test the BFD server endpoints when under a certain load of requests, configurable
using parameters passable when running the tests. These tests use locust, a python-based concurrent test runner that is able to ramp
requests up to a maximum target number of users making requests against an endpoint. The tests are all written in python, with only a
few dependencies to install. The tests pull a sample of random data from the target environment's database, attempting to randomize
to avoid caching affecting the response times of repeat runs.

These tests are intended to be run from a server instance which has access permissions to reach out to the BFD endpoints. How to
do this is outside the scope of this document, but instructions for this can be found in this more detailed run guide on confluence:
https://confluence.cms.gov/display/BB2/Run+the+BFD+Load+Tests


## Setup
The tests use a few python libraries that will need to be installed on the run box prior to starting the tests. Below are the required
libraries, what they do, and the command to install them.

To install everything below at the same time you can use the provided requirements file
`pip3 install -r requirements.txt`

**Pyyaml**

What: Library for reading/writing yaml files
Why: Used to save the configuration values to a yaml file that can be used for future runs.
Install: _pip3 install pyyaml_
See: https://pypi.org/project/PyYAML/

**Psycopg2**

What: Library for connecting to a postGres database programatically via python.
Why: The tests use this to connect to the environment's database in order to gather various randomized data sets to run each test.
Install: _pip3 install psycopg2-binary_
See: https://pypi.org/project/psycopg2/

**Requests**

What: Library for making http requests easily
Why: Making calls to gather data for the coverage tests from the patient endpoint. 
Offers convenience over python's default http request get().
Install: _pip3 install requests_
See: https://pypi.org/project/requests/

**Locust**

What: Library for running the test and reporting the results
Why: Runs the test execution and reporting
Install: _pip3 install locust_
See: http://docs.locust.io/en/stable/installation.html


In addition to these libraries, you'll also need to copy a PEM (credentials) file and set the user to your SSH username on the test box. 
Run the following two commands on the box from your local directory:

```
sudo cp /usr/local/bfd-server/bluebutton-backend-test-data-server-client-test-keypair.pem ~/

sudo chown <SSH-username>:<SSH-username> ~/bluebutton-backend-test-data-server-client-test-keypair.pem
```

Once this is done you'll have a modified PEM to use for the tests in your user directory.

## Running a test

There are a number of tests which each test various endpoints (and param combinations for those endpoints) for different released versions of BFD.

The tests run headless, i.e. without the web GUI, because we'll be executing the tests over ssh in the terminal on a remote box.

The tests are run with parameters that specify all the test params and test file to use, so the test can be run in a single line. This is primarily meant to allow support for running tests in Jenkins, but can also be used to automate test suites.

A single-line test will look like this (placeholders to replace in brackets):

    python3 runtests.py --homePath="<home_directory_path>" --clientCertPath="<home_directory_path>/bluebutton-backend-test-data-server-client-test-keypair.pem" --databaseUri="postgres://<db-username>:<db-password>@<db-host>:<port>/<db-name>" --testHost="https://test.bfd.cms.gov" --testFile="./v2/eob_test_id_count.py" --testRunTime="1m" --maxClients="100" --clientsPerSecond="5"

If you have existing configuration in `./config.yml`, you can also run the tests via:

```
python3 runtests.py --testFile="<your-test-file>"
```

Or, if you have some YAML configuration in a different file (note that these values will be saved to the root `./config.yml`, so subsequent runs can omit the `configPath` if you are not changing anything):

```
python3 runtests.py --configPath="config/<your-config-here>.yml" --testFile="<your-test-file>" <other-cli-args-here>...
```

Essentially, all the items you would set up in the config file are set in a single line. There are some optional, and some required parameters here:

**--homePath** : (Required) : The path to your home directory on the local box, such as /home/logan.mitchell/

**--clientCertPath** : (Required) : The path to the PEM file we copied/modified earlier. Should be located in your home directory like this: ~/bluebutton-backend-test-data-server-client-test-keypair.pem

**--databaseUri** : (Required) : The URI used for connecting to the database server. Needs to include username, password (both can be found in Keybase, make sure to use the correct one for the environment you're connecting to), hostname and port.

**--testHost** : (Required) : The load balancer or single node to run the tests against. The environment used here should match the same environment (test, prod-sbx, prod) as the database, so we pull the appropriate data for the environment tested against. Note that when using a single node, you should specity the Ip AND the port for the application.

**--testFile** : (Required) : The path to the test file we want to run.

**--configPath** : (Optional) : The path to a YAML configuration file that will be read from for the values specified here. The values in this configuration file will be merged with values from the CLI, with the CLI values taking priority. The resulting merged values will be written to the repository's root `config.yml`, so if `--configPath` is specified as a YAML file other than `config.yml` the YAML file at that path will not be modified (only read from). If not provided, defaults to `config.yml` (the root YAML configuration file). 

**--serverPublicKey** : (Optional) : To allow the tests to trust the server responses, you can add the path to the public certificate here. This is not required to run the tests successfully, and may not be needed as a parameter at all. If not provided, defaults to an empty string (does not cause test issues.)

**--testRunTime** : (Optional) : How long the test will run. This uses values such as 1m, 30s, 1h, or combinations of these such as 1m 30s. If not provided, defaults to 1m. **Note**: We automatically adjust the run time so that the test runs for the specified amount of time *after* spawning all clients (see `--maxClients` and `--clientsPerSecond` below). For example, with the defaults of a one-minute test, 100 clients, and a spawn rate of 5 clients per second, then the script will spend twenty seconds ramping up its clients and *then* run for the one minute specified. It is optional whether or not to use the `--resetStats` flag to drop the statistics covering this ramp-up period.

**--maxClients** : (Optional) : The maximum number of clients that will be spawned to hit the test host at the same time. If not provided, defaults to 100.

**--clientsPerSecond** : (Optional) : The number of clients to spawn per second, until maxClients is reached. If not provided, defaults to 5.

**--workerThreads** : (Optional) : Controls running in distributed mode; If this is set to >1, will spawn that many worker threads to run the tests across multiple cores. If not provided, defaults to 1. See section on running in distributed mode for more info.

**--tableSamplePct** : (Optional) : Determines how big a slice of the Beneficiaries table we want to use when finding endpoints for testing. Defaults to 0.25 (one-quarter of one percent), which is plenty for production databases with millions of rows. Note that this is only meant to randomize and streamline the data query, but if this option is set too small, it would act as a cap on the number of rows available. For the Test environment or local testing, it might be best to set to 100, which will effectively *not* use table sampling.

**--resetStats** : (Optional) : If this flag is included, the test statistics will reset to zero after clients have finished spawning. **Note:** There are many reasons why we might want to capture statistics while new load is being added. There might be performance problems accepting the connection or new connections might affect users already connected to the system.

**--stats**: (Optional) : Argument specifying that aggregated performance statistics should be collected and stored to some location. This can either be to a local file or to an S3 bucket. This argument must be specified as a list of key-value pairs seperated by semi-colons: `--stats="store=<file/s3>;env=<TEST/PROD>;tag=;path=;bucket=;compare=<previous/average>;comp_tag=;athena_db="`

| Key | Required? | Possible Values | Description |
| :-: | :-: | :-: | - |
| `store` | Yes | `file`, `s3` | Specifies where the stats are stored. Note that the `file` `store` is primarily meant for _local debugging_ purposes and should not be used when running these tests as part of a process where the performance statistics should be stored for later retrieval. 
| `env` | Yes | `TEST`, `PROD` | Specifies which environment the test ran in.
| `store_tag` | Yes | Must be non-empty, consisting of letters, numbers and the `_` character | A tag that is used to bucket or partition statistics for more accurate performance validation between corresponding runs.
| `path` | No | N/A | The _local_ **parent directory** where JSON files will be written to. Used only if `store` is `file`, ignored if `store` is `s3`.
| `bucket` | Yes, _if `store` is `s3`_ | N/A | The AWS S3 Bucket that the JSON will be written to under a predetermined path following BFD Insights data organization standards.
| `compare` | No | `previous`, `average` | Specifies if the current run should be compared to existing statistics. These statistics will be retrieved from the same type of store as specified by `store`. If `previous` is specified, the most recent run from `comp_tag` will be compared against. If `average` is specified, the average of _all_ previous runs from `comp_tag` will be compared against. If unspecified, no comparisons will be done.
| `comp_tag` | No | Must be non-empty, consisting of letters, numbers and the `_` character | Tag from which comparison statistics will be loaded from the given `store`. Defaults to `store_tag` if unspecified. |
| `athena_tbl` | Yes, _if `store` is `s3` **and** `compare` is set_ | N/A | Name of the table to query using Athena if `store` is `s3` and `compare` is set. |

### Quick Run

Once you've run a test once and the configuration file is set, you can "quick run" a test by calling locust directly, like this:

```
locust -f <path/to/test/file.py>
```

This will run the test with the parameters of the last test that was run. If you want to set up a configuration file manually, a sample file is included in this directory to copy and modify; simply copy the file and remove -sample from the name so its named config.yml.

## Distributed Mode

Python natively only runs code on a single thread; this limits the ability of the test to properly scale up because too many requests will quickly overload the box's cpu and bottleneck the cadence of requests.
In order to avoid this limitation, the tests support distribution over multiple cpu cores by spinning up multiple threads that each handle a portion of the data at once.
By doing this, we get the following benefits:
- Python handles each new thread on a separate core, allowing us to run a larger number of requests without bottlenecking the cpu
- Locust runs each request task subsequently, so splitting the request tasks across threads increases the parallelism of each call and allows for more requests to hit simultaneously
  - This only matters slightly; locust only waits to _start_ the request to make the next, so it still goes through the requests quickly with few threads

Distributed mode is controlled by using the --workerThreads parameter on the runtests.py test script. By setting this parameter, that many worker threads will be spawned and automatically split up the test data amongst themselves.
Note that each worker will still ramp up to the same number of maxClients and clientsPerSecond, so keep this in mind when setting the test up. If you have 4 workers and want to have 100 users total hitting the server,
you should set --maxClients to 25. (25 clients across 4 threads = 100 target)

Since the test script controls making the child threads, automated distributed mode is not runnable using "quick run" by calling locust directly.
Locust does support distributing the tests natively using a master and worker threads using its own parameters that could be used to manually make a distributed test run outside
the runtests.py script.

See https://docs.locust.io/en/stable/running-locust-distributed.html#options for how to use distributed mode calling locust directly. This can allow us to distribute the load
across multiple boxes instead of threads by using a host bind port, master/worker parameters, and other options noted there if we wanted to run a much larger scale test.

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
    
## Improvements
This is a list of some improvements that could be made to the tests moving forward:
- (Consider) Move all tests into one script with each test being a @Task
- Set up tests to all run at once to simulate actual load across the whole system (i.e. during big spike loads)
