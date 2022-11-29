# How to Run the Regression and Load Test Suites

Follow this runbook to run the regression and load test suites either locally or against a
particular BFD Server host.

- [How to Run the Regression and Load Test Suites](#how-to-run-the-regression-and-load-test-suites)
  - [Glossary](#glossary)
  - [FAQ](#faq)
    - [I specified `--host` with a valid URL like so `example.com`, but my tests aren't running. What am I doing wrong?](#i-specified---host-with-a-valid-url-like-so-examplecom-but-my-tests-arent-running-what-am-i-doing-wrong)
  - [Prerequisites](#prerequisites)
  - [Instructions](#instructions)
    - [How to Run the Regression Suite Locally Against a Local BFD Server](#how-to-run-the-regression-suite-locally-against-a-local-bfd-server)
    - [How to Run the Regression Suite Locally Against any BFD Server SDLC Environment](#how-to-run-the-regression-suite-locally-against-any-bfd-server-sdlc-environment)
    - [How to Run the Regression Suite On a Detached Instance Against any BFD Server SDLC Environment](#how-to-run-the-regression-suite-on-a-detached-instance-against-any-bfd-server-sdlc-environment)
    - [How to Run a Scaling Load Test Using the `bfd-run-server-load` Jenkins Job](#how-to-run-a-scaling-load-test-using-the-bfd-run-server-load-jenkins-job)
    - [How to Run a Static Load Test Using the `bfd-run-server-load` Jenkins Job](#how-to-run-a-static-load-test-using-the-bfd-run-server-load-jenkins-job)

## Glossary

|             Term             |                                    Definition                                    |
| :--------------------------: | :------------------------------------------------------------------------------: |
| [Locust](https://locust.io/) | A load testing library that allows for performance tests to be written in Python |

## FAQ

### I specified `--host` with a valid URL like so `example.com`, but my tests aren't running. What am I doing wrong?

Firstly, `--host` is a default [Locust argument][locust-args] that is a _bit_ of a misnomer; valid
`--host` values _must_ include the protocol (i.e. `https`), hostname (i.e. `example.com`) and,
optionally, the port in the following format: `PROTOCOL://HOSTNAME:PORT`. Be aware that _Locust_
does not trim trailing slashes after the `PORT`; however, we have implemented a check for trailing
slashes in `--host` and remove them ourselves. So, it is recommended that `--host` does not include
any trailing characters after `PORT` as well.

## Prerequisites

- A global installation of Python 3
- An installation of the AWS CLI that is configured properly for access to the BFD/CMS AWS account
- An installation of [`jq`](https://stedolan.github.io/jq/)
- A tool for creating virtual environments (`virtualenv`s) such as
  [`virtualenv`](https://virtualenv.pypa.io/en/latest/) or [`pew`](https://github.com/berdario/pew)
  - This runbook will assume you are using `pew` as it is fairly simple to work with and has a
    relatively intuitive UX
- Access to AWS
- Access to the CMS VPN

## Instructions

> **Note:** This runbook assumes you have cloned the `beneficiary-fhir-data` repository locally and
> are relatively comfortable with the command-line (CLI).

### How to Run the Regression Suite Locally Against a Local BFD Server

> **Note:** These steps assume you have followed the top-level README.md's steps for running BFD
> locally (including running the database using PostgreSQL in Docker).
>
> Additionally, it is _highly_ recommended to read the entirety of the `locust_test` `README.md`
> before continuing.

1. Navigate to the root of the `beneficiary-fhir-data` repository in any terminal application
2. From the root of the `beneficiary-fhir-data` repository, set `BFD_ROOT` to the current working
   directory:

   ```bash
   BFD_ROOT=$(pwd)
   ```

3. Navigate to `apps/utils/locust_tests`:

   ```bash
   cd $BFD_ROOT/apps/utils/locust_tests
   ```

4. _If not already created_, create a new virtual environment for `locust_tests`:

   ```bash
   pew new -p 3.8 -a . -r requirements.txt py3.8__locust_tests
   ```

   1. This will create a new virtual environment using Python 3.8 named `py3.8__locust_tests` and
      will automatically install the necessary Python dependencies to _run_ the various Locust test
      suites in the `locust_tests` directory. It will also associate this new virtual environment
      to the `$BFD_ROOT/apps/utils/locust_tests` directory

5. Open a new _subshell_ that uses the `py3.8__locust_tests` virtual environment:

   ```bash
   pew workon py3.8__locust_tests
   ```

6. Set `CLIENT_CERT_PATH` to the unsecured certificate available in the repository:

   ```bash
   CLIENT_CERT_PATH=$BFD_ROOT/apps/bfd-server/dev/ssl-stores/client-unsecured.pem
   ```

7. Set `DATABASE_CONSTR` to the database connection string pointing to your locally running
   PostgreSQL instance:

   ```bash
   DATABASE_CONSTR="postgres://bfd:InsecureLocalDev@localhost:5432/fhirdb"
   ```

   1. Running the above command assumes you have followed the README.md's "Native Setup" section
      and are running your local PostgreSQL instance in a Docker container with the defaults
      provided in that section. If you are not, you will need to change the connection string above
      to point to your local BFD database instance

8. Run the Locust tests using `locust`. Replace `<NUM_USERS>` with the number of simulated users
   (amount of load) to run with, `<SPAWN_RATE>` with the rate at which you would like the simulated
   users to spawn per-second, and `<RUNTIME>` with the amount of time you would like to run the
   performance tests for _once all users have spawned_ (you can specify runtime like "10m30s" or
   "30s"):

   ```bash
   locust -f v2/regression_suite.py \
     --users=<NUM_USERS> \
     --host="localhost:$BFD_PORT" \
     --spawn-rate=<SPAWN_RATE> \
     --spawned-runtime="<RUNTIME>" \
     --client-cert-path="$CLIENT_CERT_PATH" \
     --database-connection-string="$DATABASE_CONSTR"
     --headless
   ```

9. Once the regression tests have ended, Locust will print a summary table with the performance
   statistics of the previous run for each endpoint as well as an aggregated total of all endpoint
   performance

### How to Run the Regression Suite Locally Against any BFD Server SDLC Environment

> **Note:** These steps assume you will be testing against the `TEST` environment, but you are able
> to test against _all_ SDLC environments following these instructions.
>
> Additionally, it is _highly_ recommended to read the entirety of the `locust_test` `README.md`
> before continuing.

1. Set `BFD_ENV` to the environment you want to test:

   ```bash
   BFD_ENV="test"
   ```

   1. Other valid values are `"prod-sbx"` and `"prod"`, however it is unlikely you will need to run
      the regression suite _manually_ against environments other than `TEST`

2. Navigate to the root of the `beneficiary-fhir-data` repository in any terminal application
3. From the root of the `beneficiary-fhir-data` repository, set `BFD_ROOT` to the current working
   directory:

   ```bash
   BFD_ROOT=$(pwd)
   ```

4. Navigate to `apps/utils/locust_tests`:

   ```bash
   cd $BFD_ROOT/apps/utils/locust_tests
   ```

5. _If not already created_, create a new virtual environment for `locust_tests`:

   ```bash
   pew new -p 3.8 -a . -r requirements.txt py3.8__locust_tests
   ```

   1. This will create a new virtual environment using Python 3.8 named `py3.8__locust_tests` and
      will automatically install the necessary Python dependencies to _run_ the various Locust test
      suites in the `locust_tests` directory. It will also associate this new virtual environment
      to the `$BFD_ROOT/apps/utils/locust_tests` directory

6. Open a new _subshell_ that uses the `py3.8__locust_tests` virtual environment:

   ```bash
   pew workon py3.8__locust_tests
   ```

7. Ensure your AWS credentials are valid
8. Ensure you are connected to the CMS VPN
9. Download and decrypt the testing certificate from SSM and store it to a local file:

   ```bash
   aws ssm get-parameter --name "/bfd/$BFD_ENV/server/sensitive/test_client_key" \
      --region "us-east-1" \
      --with-decryption | jq -r '.Parameter.Value' > $HOME/bfd-test-cert.pem
   aws ssm get-parameter --name "/bfd/$BFD_ENV/server/sensitive/test_client_cert" \
      --region "us-east-1" \
      --with-decryption | jq -r '.Parameter.Value' >> $HOME/bfd-test-cert.pem
   ```

   1. Running the above commands assume you have appropriate permissions to _read_ and _decrypt_
      sensitive SSM parameters in the environment under test

10. Set `CLIENT_CERT_PATH` to the downloaded testing certificate from the previous step:

    ```bash
    CLIENT_CERT_PATH=$HOME/bfd-test-cert.pem
    ```

11. Set `DATABASE_CONSTR` to the database connection string for the reader endpoint of the
    environment under test:

    ```bash
    DB_CLUSTER_ID=$(aws ssm get-parameter --name "/bfd/$BFD_ENV/common/nonsensitive/rds_cluster_identifier" \
                     --region "us-east-1" | jq -r '.Parameter.Value')
    DB_USERNAME=$(aws ssm get-parameter --name "/bfd/$BFD_ENV/server/sensitive/vault_data_server_db_username" \
                     --with-decryption \
                     --region "us-east-1" | jq -r '.Parameter.Value')
    DB_RAW_PASSWORD=$(aws ssm get-parameter --name "/bfd/$BFD_ENV/server/sensitive/vault_data_server_db_password" \
                     --with-decryption \
                     --region "us-east-1" | jq -r '.Parameter.Value')
    DB_PASSWORD=$(printf %s "$DB_RAW_PASSWORD" | jq -sRr @uri)
    DB_READER_URI=$(aws rds describe-db-clusters --db-cluster-identifier "$DB_CLUSTER_ID" \
                     --region "us-east-1" | jq -r '.DBClusters[0].ReaderEndpoint')
    DATABASE_CONSTR="postgres://$DB_USERNAME:$DB_PASSWORD@$DB_READER_URI:5432/fhirdb"
    ```

    1. Running the above commands assume you have appropriate permissions to _read_ and _decrypt_
       sensitive SSM parameters in the environment under test

12. Run the Locust tests using `locust`. Replace `<NUM_USERS>` with the number of simulated users
    (amount of load) to run with, `<SPAWN_RATE>` with the rate at which you would like the simulated
    users to spawn per-second, and `<RUNTIME>` with the amount of time you would like to run the
    performance tests for _once all users have spawned_ (you can specify runtime like "10m30s" or
    "30s"):

    ```bash
    locust -f v2/regression_suite.py \
      --users=<NUM_USERS> \
      --host="https://$BFD_ENV.bfd.cms.gov" \
      --spawn-rate=<SPAWN_RATE> \
      --spawned-runtime="<RUNTIME>" \
      --client-cert-path="$CLIENT_CERT_PATH" \
      --database-connection-string="$DATABASE_CONSTR"
      --headless
    ```

    1. Note that `--host` can be anything (including the IP address of another instance that you
       would like to target specifically). However, if you are targeting a particular environment,
       you should stick with only testing instances under that environment (or the default provided
       here)

13. Once the regression tests have ended, Locust will print a summary table with the performance
    statistics of the previous run for each endpoint as well as an aggregated total of all endpoint
    performance
14. Delete the `bfd-test-cert.pem`:

    ```bash
    rm -f $CLIENT_CERT_PATH
    ```

### How to Run the Regression Suite On a Detached Instance Against any BFD Server SDLC Environment

> **Note:** These steps assume you will be testing against the `TEST` environment, but you are able
> to test against _all_ SDLC environments following these instructions.
>
> Additionally, it is _highly_ recommended to read the entirety of the `locust_test` `README.md`
> before continuing.

1. First, detach an instance from the desired environment under test's auto-scaling group:
   1. Go to the AWS website and sign-in
   2. Click Services > EC2
   3. Click Auto scaling groups
   4. Click an active group corresponding to the desired environment under test within this list
      1. I.e. if `TEST` is the desired environment, click on `bfd-test-fhir...`
   5. In the Details area below the node you clicked, click the Instance Management tab
   6. Pick one of the instances here using the checkbox on the left
   7. In the actions dropdown, click Detach
   8. In the popup, check the Add a new instance to the Auto Scaling group to balance the load
      checkbox to add a new instance in its place
   9. Confirm the detachment by clicking Detach instance
   10. In the list, the detached instance will still exist in the group; open the detached instance
       in a new tab to keep track of it
   11. Go to Services > EC2 and click Instances
   12. Find the detached instance by comparing its ID in the tab you opened against the ID in the
       list
   13. Click the Edit button near the ID of the detached instance and rename it something that
       indicates your name so the instance is marked as yours for others information
   14. Click on the newly-created Instance ID to open a details page
   15. Copy the private IP address from "Private IPv4 addresses"
2. Ensure you are connected to the CMS VPN
3. SSH into the detached instance using the private IP address copied from the previous step:

   ```bash
   ssh -i <YOUR PRIVATE KEY HERE> <YOUR USER HERE>@<DETACHED IP ADDRESS HERE>
   ```

4. Become the `root` user on the detached instance:

   ```bash
   sudo su
   ```

   1. As the `root` user you are able to do many dangerous things, _especially_ if you are connected
      to a detached instance from `PROD-SBX` or `PROD`. Be _**very**_ careful while logged-in as the
      `root` user!

5. Set `BFD_ENV` to the environment under test:

   ```bash
   BFD_ENV="test"
   ```

6. Navigate to the root of the `beneficiary-fhir-data` repository in any terminal application
   1. For detached instances, this will be at `/beneficiary-fhir-data`
7. From the root of the `beneficiary-fhir-data` repository, set `BFD_ROOT` to the current working
   directory:

   ```bash
   BFD_ROOT=$(pwd)
   ```

8. Navigate to `apps/utils/locust_tests`:

   ```bash
   cd $BFD_ROOT/apps/utils/locust_tests
   ```

9. Install the Python dependencies necessary to run the regression suite:

   ```bash
   pip3 install -r requirements.txt
   ```

   1. The instance comes with an installation of Python 3, and since we will be destroying the
      instance after running the regression suite we do not need to use virtual environments

10. Download and decrypt the testing certificate from SSM and store it to a local file:

    ```bash
    aws ssm get-parameter --name "/bfd/$BFD_ENV/server/sensitive/test_client_key" \
       --region "us-east-1" \
       --with-decryption | jq -r '.Parameter.Value' > $BFD_ROOT/bfd-test-cert.pem
    aws ssm get-parameter --name "/bfd/$BFD_ENV/server/sensitive/test_client_cert" \
       --region "us-east-1" \
       --with-decryption | jq -r '.Parameter.Value' >> $BFD_ROOT/bfd-test-cert.pem
    ```

11. Running the above commands assume you have appropriate permissions to _read_ and _decrypt_
    sensitive SSM parameters in the environment under test

12. Set `CLIENT_CERT_PATH` to the testing certificate from the previous step:

    ```bash
    CLIENT_CERT_PATH=$BFD_ROOT/bfd-test-cert.pem
    ```

13. Set `DATABASE_CONSTR` to the database connection string for the reader endpoint of the
    environment under test:

    ```bash
    DB_CLUSTER_ID=$(aws ssm get-parameter --name "/bfd/$BFD_ENV/common/nonsensitive/rds_cluster_identifier" \
                     --region "us-east-1" | jq -r '.Parameter.Value')
    DB_USERNAME=$(aws ssm get-parameter --name "/bfd/$BFD_ENV/server/sensitive/vault_data_server_db_username" \
                     --with-decryption \
                     --region "us-east-1" | jq -r '.Parameter.Value')
    DB_RAW_PASSWORD=$(aws ssm get-parameter --name "/bfd/$BFD_ENV/server/sensitive/vault_data_server_db_password" \
                     --with-decryption \
                     --region "us-east-1" | jq -r '.Parameter.Value')
    DB_PASSWORD=$(printf %s "$DB_RAW_PASSWORD" | jq -sRr @uri)
    DB_READER_URI=$(aws rds describe-db-clusters --db-cluster-identifier "$DB_CLUSTER_ID" \
                     --region "us-east-1" | jq -r '.DBClusters[0].ReaderEndpoint')
    DATABASE_CONSTR="postgres://$DB_USERNAME:$DB_PASSWORD@$DB_READER_URI:5432/fhirdb"
    ```

    1. Running the above commands assume you have appropriate permissions to _read_ and _decrypt_
       sensitive SSM parameters in the environment under test

14. Run the Locust tests using `locust`. Replace `<NUM_USERS>` with the number of simulated users
    (amount of load) to run with, `<SPAWN_RATE>` with the rate at which you would like the simulated
    users to spawn per-second, and `<RUNTIME>` with the amount of time you would like to run the
    performance tests for _once all users have spawned_ (you can specify runtime like "10m30s" or
    "30s"):

    ```bash
    locust -f v2/regression_suite.py \
      --users=<NUM_USERS> \
      --host="https://$BFD_ENV.bfd.cms.gov" \
      --spawn-rate=<SPAWN_RATE> \
      --spawned-runtime="<RUNTIME>" \
      --client-cert-path="$CLIENT_CERT_PATH" \
      --database-connection-string="$DATABASE_CONSTR"
      --headless
    ```

    1. Note that `--host` can be anything (including the IP address of another instance that you
       would like to target specifically). However, if you are targeting a particular environment and
       have detached an instance from that environment's ASG, you should stick with only testing
       instances under that environment (or the default provided here)

15. Once the regression tests have ended, Locust will print a summary table with the performance
    statistics of the previous run for each endpoint as well as an aggregated total of all endpoint
    performance
16. Delete the `bfd-test-cert.pem`:

    ```bash
    rm -f $CLIENT_CERT_PATH
    ```

17. In your web browser, navigate back to AWS and sign-in (if necessary)
18. Navigate to Services > EC2
19. Navigate to Instances in the navigation bar on the left side
20. Find the detached instance by searching for its name or Instance ID
21. Select the checkbox to the left of the name of the detached instance
22. Click on the "Instance state" dropdown in the top right of the screen
23. Select "Terminate instance" and accept any dialogs that appear

### How to Run a Scaling Load Test Using the `bfd-run-server-load` Jenkins Job

1. Ensure you are connected to the CMS VPN
2. Navigate to the Jenkins CloudBees instance in your web browser and sign-in
3. From the main page, select "bfd". A list of jobs should load
4. From this list of jobs, click on "bfd-run-server-load". A new page should load showing the "Stage
   View" and a list of actions on the left side of the screen
5. Click on "Build with Parameters" on the left side of the screen. A new page should load showing a
   variety of input fields
6. Choose the desired SDLC environment to load test from the "ENVIRONMENT" dropdown list
7. Adjust the default parameters according to the desired load test. For this particular case, it is
   assumed the desired load test is to continuously ramp-up load until a scaling event occurs and so
   the _defaults can be used_
8. Click "Build" at the bottom of the page. The page from Step #4 should load again, however an
   in-progress build should appear in the "Build History" list on the left side of the screen
9. Click on the _build number_ of the in-progress build. A new page should load showing an overview
   of the current build
10. Click on "Console Output" on the left side of the screen. A new page should load showing
    realtime log output from the job
11. Monitor the log output until the following prompt appears in the output:

    ```
    Once the run is finished, click either Abort or Proceed to cleanup the test
    Proceed or Abort
    ```

12. Scroll up in the log output and find the line starting with:

    ```
    aws_instance.this[0]: Creation complete after...
    ```

13. Note the instance ID within square brackets -- use this later to follow the log output from the
    controller in CloudWatch
14. In your web browser, navigate to AWS and sign-in (if necessary)
15. Navigate to Services > CloudWatch
16. Navigate to "Log groups" by clicking on the link in the navigation tree
17. Search for "server-load-controller.log" and select the corresponding log group in the SDLC
    environment currently under test
18. Refresh the log group until a log stream with the name of the instance ID noted down in Step 13
    appears
19. Open the log stream corresponding to the instance ID noted down in Step 13
20. Monitor the log continuously by clicking "Resume" at the bottom of the log output. The log
    should automatically update in realtime as the load test runs. You may need to continuously
    scroll to view the log
21. Wait until the load tests finish running. If at anytime something goes wrong, return to the
    running Jenkins job and click either the "Proceed" or "Abort" prompt in the log output to
    immediately end the test and start cleaning up
22. Once Locust prints the summary table and has finished, indicated by the "Locust master process
    has stopped" message, return to the Jenkins job and click "Proceed". This will cleanup the test,
    destroying the controller instance and stopping any orphaned Lambda nodes
23. View the stats of the run under the following log groups (the log stream corresponding to the
    current run will be named according to the instance ID noted down in Step 13). Note
    "{ENVIRONMENT}" should be replaced with the environment under test (i.e. "test"):
    - /bfd/{ENVIRONMENT}/bfd-server-load/load_exceptions.csv
    - /bfd/{ENVIRONMENT}/bfd-server-load/load_failures.csv
    - /bfd/{ENVIRONMENT}/bfd-server-load/load_stats.csv
    - /bfd/{ENVIRONMENT}/bfd-server-load/load_stats_history.csv

### How to Run a Static Load Test Using the `bfd-run-server-load` Jenkins Job

1. Ensure you are connected to the CMS VPN
2. Navigate to the Jenkins CloudBees instance in your web browser and sign-in
3. From the main page, select "bfd". A list of jobs should load
4. From this list of jobs, click on "bfd-run-server-load". A new page should load showing the "Stage
   View" and a list of actions on the left side of the screen
5. Click on "Build with Parameters" on the left side of the screen. A new page should load showing a
   variety of input fields
6. Choose the desired SDLC environment to load test from the "ENVIRONMENT" dropdown list
7. Adjust the default parameters according to the desired load test. For this particular case, the
   default values will _need to be changed_:
   1. Set `INITIAL_WORKER_NODES` to the number of worker nodes/Lambdas desired _in total_
   2. Set `MAX_SPAWNED_NODES` equal to `INITIAL_WORKER_NODES`
   3. Set `MAX_SPAWNED_USERS` to the desired number of simulated users _in total_
      1. Note that a ratio of 10 simulated users to 1 worker node should be followed for best
         performance
   4. Set `USER_SPAWN_RATE` equal to `MAX_SPAWNED_USERS` if no ramp-up is desired
   5. Unselect `STOP_ON_SCALING` if the load test should _not_ stop when a scaling event is
      encountered -- for a static test, this should probably be false
   6. Deselect `STOP_ON_NODE_LIMIT` to ensure that the load test does not end immediately due to the
      node limit being hit
8. Click "Build" at the bottom of the page. The page from Step #4 should load again, however an
   in-progress build should appear in the "Build History" list on the left side of the screen
9. Click on the _build number_ of the in-progress build. A new page should load showing an overview
   of the current build
10. Click on "Console Output" on the left side of the screen. A new page should load showing
    realtime log output from the job
11. Monitor the log output until the following prompt appears in the output:

    ```
    Once the run is finished, click either Abort or Proceed to cleanup the test
    Proceed or Abort
    ```

12. Scroll up in the log output and find the line starting with:

    ```
    aws_instance.this[0]: Creation complete after...
    ```

13. Note the instance ID within square brackets -- use this later to follow the log output from the
    controller in CloudWatch
14. In your web browser, navigate to AWS and sign-in (if necessary)
15. Navigate to Services > CloudWatch
16. Navigate to "Log groups" by clicking on the link in the navigation tree
17. Search for "server-load-controller.log" and select the corresponding log group in the SDLC
    environment currently under test
18. Refresh the log group until a log stream with the name of the instance ID noted down in Step 13
    appears
19. Open the log stream corresponding to the instance ID noted down in Step 13
20. Monitor the log continuously by clicking "Resume" at the bottom of the log output. The log
    should automatically update in realtime as the load test runs. You may need to continuously
    scroll to view the log
21. Wait until the load tests finish running. If at anytime something goes wrong, return to the
    running Jenkins job and click either the "Proceed" or "Abort" prompt in the log output to
    immediately end the test and start cleaning up
22. Once Locust prints the summary table and has finished, indicated by the "Locust master process
    has stopped" message, return to the Jenkins job and click "Proceed". This will cleanup the test,
    destroying the controller instance and stopping any orphaned Lambda nodes
23. View the stats of the run under the following log groups (the log stream corresponding to the
    current run will be named according to the instance ID noted down in Step 13). Note
    "{ENVIRONMENT}" should be replaced with the environment under test (i.e. "test"):
    - /bfd/{ENVIRONMENT}/bfd-server-load/load_exceptions.csv
    - /bfd/{ENVIRONMENT}/bfd-server-load/load_failures.csv
    - /bfd/{ENVIRONMENT}/bfd-server-load/load_stats.csv
    - /bfd/{ENVIRONMENT}/bfd-server-load/load_stats_history.csv

[locust-args]: https://docs.locust.io/en/stable/configuration.html
