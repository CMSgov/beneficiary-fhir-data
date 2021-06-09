# BFD Loadtest

## Beneficiary IDs
In order to run the BFD Load test, you will need a couple of files at hand. First, you'll want a random set of beneficiary IDs. To avoid benefitting from database and application caches, a large enough set to not recycle those IDs over the course of a test is desirable. To get a random sample, you can connect to a database in an arbitrary environment with `psql`. From there, run the following query:
```
\COPY (SELECT "beneficiaryId" FROM "Beneficiaries" TABLESAMPLE SYSTEM (0.1) LIMIT 100000) TO '/path/on/your/host/ids.txt' DELIMITER ',';
```
Let's break down what this is doing. Inside the parens, the query is selecting IDs from the `Beneficiaries` table. Straightforward! 

`TABLESAMPLE SYSTEM (0.1)` is where it gets interesting. `TABLESAMPLE` is just what is says on the tin: [a way to sample values from the table](https://www.postgresql.org/docs/11/tablesample-method.html). The two sampling methods available are `SYSTEM` and `BERNOULLI`, the former of which is much faster on large datasets, though less random. `SYSTEM` does block/page level sampling while `BERNOULLI` does a sequential scan on the whole table. For our purposes, `SYSTEM` is sufficiently random. 

The number after `SYSTEM` represents the percent of the table to return. In this case 0.1 means 0.1 percent, as in one-tenth of one percent. To figure out how many records that is, you'll need to know the count of records in the table. Count is one way to handle that, though it can take minutes to complete. It's faster, though a bit less accurate to use the following query:
```
SELECT reltuples AS ct FROM pg_class WHERE oid = 'public."Beneficiaries"'::regclass;
```
If you get the product of the table size and the percentage to sample in the ballpark of what you're looking for, the `LIMIT` will take care of returning an exact count, if that matters.

The rest of the command is all about taking that query and copying the results to a file on the host that is running the `psql` client. If you're running `psql` from a laptop, then the `TO` path should refer to something on your laptop's filesystem. Note that `/COPY` is different from `COPY`, which will attempt to save a file to the database host.

## Test certificate
BFD uses mTLS to authenticate clients. The load test will need to send a valid certificate with each request. A test certificate can be provisioned from deployed BFD Server hosts in the app server's directory under `/usr/local`. That, or another valid test certificate, will need to have corresponding entry in the Java truststore on the host being tested.

It may also be useful to have the load test client trust the responses from the BFD Server. If you have the public certificate available for [each environment here](https://github.com/CMSgov/beneficiary-fhir-data/tree/master/ops/ansible/playbooks-ccs/files) as `*-bluebutton-appserver-public-cert.pem`, you can provide that to the Locust script with the `SERVER_PUBLIC_KEY` environment variable. Note that if you're hitting the host directly with a private DNS name, the public certificate will fail with a SAN mismatch.

## Request script
Locust uses files written in Python to generate "users" that make requests to an application. You can look at an example [locustfile.py](./locustfile.py).
You can also read more about [how to write a locustfile in their docs](https://docs.locust.io/en/stable/writing-a-locustfile.html). There are more options than specified in our existing file to issue multiple types of requests, weight them, and change wait times. 

## Running locust
To run locust, you start a server locally with `IDS_FILE='path/to/file.txt' CLIENT_CERT='path/to/client/cert.pem' locust -f locustfile.py`. Note the environment variables, which are used the provide a file with Beneficiary IDs and a client cert. You then navigate to the UI in the browser to specify the host to hit, the number of users, and how fast they ramp up. There are also options to run the test headless.

Single-instance tests should be run against a host that has been detached from the load-balancer and ASG.

## Monitoring the results
Locust will track the requests per second, response times, and error rates. You can also monitor the instance in New Relic by visiting the BFD Prod dashboard and filtering to only the host and timeframe that you care about. CPU utilization for the host can be viewed in Cloudwatch.

## Storing test results
Reports on test results should be stored in Confluence under `Operations -> Tests -> Load` in a similar format other entries.

## Patient Cursors
To test batch requests for Patient resources, you'll need offsets for the requests to feed to Locust so that it can run different requests at the same time. There is a `patient-cursors.go` script that does that gathering, albeit very slowly, since each request takes roughly 8 seconds to complete. To gather them up from a deployed box, build a binary with `GOOS=linux GOARCH=amd64 go build patient-cursors.go`. Then you can copy the binary to the box. It requires certain environment variables and can be run with a command like the following: `CLIENT_CERT='/path/to/cert.pem' CONTRACT_ID='Z0012' CONTRACT_MONTH='01' BFD_PORT='6500' BFD_HOST='localhost' ./patient-cursors`. The `Z0012` contract ID is from the synthetic data and will work locally.
