# Blue Button Data Pipeline Design Decisions

This document details the reasoning behind some of the design decisions that have been made in the development of this system.

## Configuration and Secrets Management

There are several common options for passing in configuration values to an application:

1. Required command line arguments, e.g `myapp --db-server-url="jdbc:foo:bar"`
2. Java system properties, e.g. `java -jar myapp.jar -Ddb.server.url="jdbc:foo:bar"`
3. Environment variables, e.g. `DB_SERVER_URL="jdbc:foo:bar" myapp`
4. A network configuration service such as [ZooKeeper](https://zookeeper.apache.org/), e.g. `myapp --config=1.2.3.4:5678`.
    * The address of such a service should almost always itself be configurable at runtime. Options 1 through 3 here can be used to specify that address.
5. A configuration file, e.g. `myapp --config=/etc/myapp/foo.conf`
    * As in the example above, the location of an application's configuration file should almost always itself be configurable at runtime. Options 1 through 3 here can be used to specify that path.

There does not appear to a solid industry consensus stating that one of these approaches is always the correct one. Probably the most authoritative source on the subject, [The Twelve-Factor App](http://12factor.net), calls for Option #3 (environment variables) to be adopted universally in [The Twelve-Factor App: Config](http://12factor.net/config). It's a reasonable position. However, there are plenty of equally persuasive arguments against this approach. The following Hacker News discussion is an excellent discussion around the problem in general, with a particular focus on security: [Hacker News: Environment Variables Considered Harmful for Your Secrets](https://news.ycombinator.com/item?id=8826024). Overall, there's lots of debate around this issue and not enough consensus to declare a clear "best choice."

A brief sidebar on the relative security of these approaches: Most discussions of this issue that focus on security fail to include any real [threat modeling](https://alexgaynor.net/2016/jul/29/intro-to-threat-modeling/). There are two main classes of vectors here: accidental leaking, and targeted compromise by an active attacker. All of the options except for #4 (network config service) are subject to accidental leaking of one form or another, such as in error reporting, logging, not-properly-secured backups, etc. With active attackers, most of the options here are equally vulnerable. If an attacker can inject code into the application or gain shell access as the application user, they can gain access to the processes' memory, arguments, environment, configuration files, and network interface. The only exception to this line of reasoning is that option #1 (command line arguments) is vulnerable to shell access from *any* user on the system (as command line arguments  for all processes are visible to all users). Overall, security isn't a real factor in this decision, aside from eliminating option #1 for secrets.

As with most things, this decision likely comes down to "what makes the most sense for our application and organization?" The simplest approach of the five would be #3 (environment variables), which are reasonably secured and widely supported. The most user friendly approach is likely #1 (command line arguments), which are easily discoverable if a `--help` option is supported. The most flexible approaches are #4 (network config service) and #5 (configuration files), which easily allow for hierarchical, repeating, and structured configuration options.

Some additional considerations:

* Most of these options can be combined in various ways.
    * For example: a configuration file with property expressions (e.g. `<someSetting>${someEnvVariable}</someSetting>`) that are resolved at runtime.
* Any application that will be deployed as a WAR has more slightly different options.
    * Command line arguments are unavailable.
    * The canonical approach with WARs is to use the servlet context's `<init-params/>`.
        * This is usually a **terrible** option, as those values end up being baked into the WAR; they're not easily configurable at runtime.
* With the recent proliferation of microservices and auto-scaling deployments, network configuration services are becoming more popular.
    * The main benefit here seems to be the centralization provided by such services. This absolutely comes with an increased cost for complexity, though.
* Java applications cache the complete environment variables' state (`System.getenv()`) at startup.
    * This can present a problem in tests, where config values often need to be modified dynamically. This can be managed by ensuring configuration management code using environment variables also "falls back" to another source that *is* modifiable at runtime, such as option #2 (Java system properties). 

Overall, the simplest approach, option #3 (environment variables) is the correct choice for the Blue Button applications. If our applications had more complex configuration needs, such as requiring highly structured configuration data, the correct choice would be option #5 (configuration files). We'll keep an eye out for use-cases that would be particularly well-met by option #4 (network configuration services), but at this time, no such use cases are envisioned. As is typical, the simplest approach is the correct one here.

Of course, there are some caveats to this approach, based on the above discussion:

* For test purposes, some applications may need to allow Java system properties to override environment variable values. This allows configuration settings to be easily passed in during tests.
* Care must be taken to ensure that any environment variable that includes a password, API key, etc. include the word "secret" in its name. The application must then filter any environment variables dumps in error reports, diagnostic pages, etc. to mask such variables' values, to prevent accidental disclosure.
* Security practices must account for the fact that any attacker that can run commands or code on the system as the application user will be able to gain access to that application's secrets.

## Performance Tests

Benchmarks are hard to get right, and getting them wrong can lead to bad decisions: committing "performance improvements" that, in reality, are performance-neutral, or even performance-negative. The main problem is that program execution times are noisy, which means that benchmark runs must be viewed in a statistical context. The results of a single run can't be trusted, even when it's on the same hardware. In fact, from some preliminary research, it seems that around 30 runs of a given test may be needed to generate a result that one can be confident in. The following paper is strongly recommended reading: [The Speedup-Test: A Statistical Methodology for Program Speedup Analysis and Computation](https://hal.inria.fr/hal-00764454/document/).

The execution environments used across benchmark runs is also a significant concern: results from different environments aren't really comparable. In particular, moving from a single-system environment (e.g. a developer's workstation) to a multi-system environment (e.g. the Blue Button AWS environments, which have at least three separate machines) can **wildly** change a program's execution profile: CPU processing time may be dominant in one environment, while network latencies might be dominant in another. Again: bad test design can lead to performance-negative decisions and outcomes.

To address these concerns, our benchmarks take the following approach:

* Run the benchmarks on production-like clones in AWS. Each benchmark iteration will stand up its own set environment clone and run on it.
    * Because it's by far the fastest tool to develop this kind of automated configuration management in, we use Ansible to provision and configure the environment.
* To minimize data collection time, multiple benchmark iterations will be run in parallel.

### Data Collection

A practical concern is how to best format and store the data collected in performance test runs. This is an area where the [KISS principle](https://en.wikipedia.org/wiki/KISS_principle) seems to be the most important concern. I'd recommend the following:

* This is tabular data, and I think can be easily represented in only two dimensions: test executions over time. Let's go with CSV files.
* This data needs to be permanently recorded, to allow us to track our performance over time.
* Whatever runs the tests should be responsible for automatically recording the data. If we instead rely on humans to record the data, we'll likely end up with a very skewed picture of our results over time.
* Let's just store it in Git for now. That's much simpler than logging results to an external database, for instance.
    * We can't do this on the build server, as attempting to automatically commit any changes to this file would lead to an infinite build loop. We'll have to ensure that benchmark runs on the build server write to a different file.

The following data points should be stored for each test execution being recorded:

* Test Case ID: We will likely end up having more than one performance test case. This column identifies the source of every record.
* Git Branch Name: Seems prudent to collect this, mostly to identify which executions are from the `master` branch.
* Git Commit: Also seems prudent to collect this. Note, though, that local test executions made by developers may also include uncommitted changes.
* POM Version: The `${project.version}` from the `bluebutton-data-pipeline` parent POM file that the test run execution was performed for/against. (Mostly useful for runs performed by the build server, as this value will indicate which Jenkins build the execution was run as part of.
* Environment ID: Identify _where_ the test was run. This allows results on "Karl's laptop" to be distinguished from results on "AWS Three-System Setup".
* Test Case Timestamp: The ISO 8601 date-time (in UTC) that the test case executions started at.
* Test Case Execution Number: The (1-based) index of this specific test case execution in the batch of _N_ run for this test case.
* Duration: The number of milliseconds the single test run execution took.
* Record Count: _Optional._ The number of beneficiaries, claims, etc. that were processed in the test run.
* Beneficiary Count: _Optional._ The number of beneficiaries that were processed in the test run.
* Test Case Execution Data: _Optional._ A JSON-encoded field containing additional data from the test execution. The set of fields included here will vary by test case.

This data should be recorded in the `bluebutton-data-pipeline.git/dev/benchmark-data.csv` file, at least for the time being. This won't work on the build system (committing the file would cause an infinite build loop), so builds should write it somewhere outside of the build workspace on the build server, that we can periodically go slurp the data from.

### Facts to Consider

Here are some miscellaneous facts considered in this decision:

* As of 2016-09-27, the only performance/benchmark data we have is generated by the `FhirLoaderIT.loadRifDataSampleB()` test case.
    * This is loading 3672 records, but only includes beneficiaries, carrier claims, and Part D events.
    * Due to bad archiving directives in the project's `Jenkinsfile`, the results of this from past builds aren't available.
    * Running it right now three times on the same developer system, I observed significant variability in execution time: the slowest run was 35% longer than the fastest.
        * The fastest execution took 12 minutes for data processing, and 22 minutes total (including teardown time).
        * AWS is notorious for inconsistent performance, so that amount of noise is likely significantly lower than what we can expect to observe there.
* In the dev, test, and production HealthAPT environments, our data pipeline application is spread across three separate EC2 instances: an ETL server, a FHIR app server, and a PostgreSQL database server. None of these are dedicated hosts; they're all sharing hardware with other AWS users and applications.
    * Dedicated hosts are cost-prohibitive. Ballpark, they start at $2/hour, rounded up to the nearest hour.
* Google Sheets have a limit of 400K cells. With our 11 columns, and 30 executions/rows per test case, this would allow us to store over 1200 test case runs.

## Linking Frontend Users to Backend Resources

### Requirements and Goals

There are several requirements to be met:

1. Do not store any operational data in the backend that cannot be discarded.
    * The disaster recovery plan of record for the backend database is to rebuild it from scratch.
2. Ensure that the frontend has simple queries that can be run to retrieve the resources for a given beneficiary.

In addition to those requirements, it would be nice to also achieve the following goals:

* Stick with standard FHIR: avoid the use of custom operations or storage mechanisms in the backend FHIR system.
* Ensure that the backend responses do not return any sensitive information, e.g. the beneficiary's HICN (or hashes of it, even).

There are also these additional "nice to haves" that are known to be impossible due to requirement #1 (the backend DB can't be treated as if it were durable):

* Avoid the need for the frontend to maintain any sort of crosswalk table to keep track of which backend `Patient` resource is associated with each frontend user account.
* Be able to identify which backend resources are associated with active frontend user accounts.

### Proposed Workflow

The frontend users and backend records will be linked using each Medicare beneficiary's unique HICN. Both systems will create cryptographic hashes of the HICN using a mutually agreed-upon process and will store those hashes in their systems, associated with the records for the beneficiary. This will allow both systems to avoid storing the actual HICN (which is sensitive) while still using it to coordinate.

When registering a new user, the frontend shall issue a "Search `Patient`" request (per below) to the backend to determine whether or not that user's records exist in the backend. Assuming that the request is successful (as defined below), the user's account will be activated and the registration process can complete normally.

After registration, the backend will support queries like the following for that frontend user:

* Request the user's `Patient` resource:
    
    ```
    $ curl --cert-type pem --cert /home/dummyuser/client-cert-stacked.pem --header "Content-Type: application/x-www-form-urlencoded" --data "identifier=http%3A%2F%2Fbluebutton.cms.hhs.gov%2Fidentifier%23hicnHash%7CA1234567890ABCDEF" --request POST https://backend.example.com:1234/baseDstu3/Patient/_search
    ```
    
    * Before being encoded, the `--data` parameter's value was: "`http://bluebutton.cms.hhs.gov/identifier#hicnHash|A1234567890ABCDEF`".
        * This sample request assumes that the beneficiary's HICN hashed to a value of "`A1234567890ABCDEF`".
    * The frontend shall verify that every response from the backend for such a query meets the following conditions:
        * That the response uses an HTTP `200` code.
        * That the response's body JSON/XML has a `total` field with a value of exactly `1`.
            * A value of `0` indicates that no matching `Patient` resource was found for the specified frontend account. Could occur if:
                * The user isn't actually a Medicare beneficiary.
                * The user's data isn't yet in the CCW or the backend FHIR system for some reason.
                * If an incorrect HICN hash was supplied for the beneficiary.
            * A value greater than `1` indicates that multiple backend `Patient` records were found for the specified HICN hash. Congratulations -- we've had a hash collision! This should trigger an administrative alert to the developers/system operators. The user's account should be disabled.
* Request the user's `Coverage` resources:

    ```
    $ curl --cert-type pem --cert /home/dummyuser/client-cert-stacked.pem --header "Content-Type: application/x-www-form-urlencoded" --data "beneficiary.identifier=http%3A%2F%2Fbluebutton.cms.hhs.gov%2Fidentifier%23hicnHash%7CA1234567890ABCDEF" --request POST https://backend.example.com:1234/baseDstu3/Coverage/_search
    ```
    
    * Alternatively, the frontend could search by Patient `id` (as recorded in an earlier search, e.g. at the start of the frontend session):
        
        ```
        $ curl --cert-type pem --cert /home/dummyuser/client-cert-stacked.pem --request GET https://backend.example.com:1234/baseDstu3/Coverage?beneficiary=Patient/42
        ```
        
* Request the user's `ExplanationOfBenefit` resources:
    
    ```
    $ curl --cert-type pem --cert /home/dummyuser/client-cert-stacked.pem --header "Content-Type: application/x-www-form-urlencoded" --data "patient.identifier=http%3A%2F%2Fbluebutton.cms.hhs.gov%2Fidentifier%23hicnHash%7CA1234567890ABCDEF" --request POST https://backend.example.com:1234/baseDstu3/ExplanationOfBenefit/_search
    ```
    
    * Alternatively, the frontend could search by Patient `id` (as recorded in an earlier search, e.g. at the start of the frontend session):
        
        ```
        $ curl --cert-type pem --cert /home/dummyuser/client-cert-stacked.pem --request GET https://backend.example.com:1234/baseDstu3/ExplanationOfBenefit?patient=Patient/42
        ```
        
Note that all of the above queries are standard FHIR searches, as specified in [FHIR Latest Release: Search](http://hl7.org/fhir/search.html).

### HICN Hashing

The frontend and the backend must use the exact same method for hashing the HICN, to allow them to coordinate using that value.

Here are the constraints on our hashing approach:

* NIST guidelines (in SP 800-132) call for using the PBKDF2-HMAC algorithm key derivation, with any approved hash function (e.g. SHA256).
    * This guideline is intended for use in hashing stored passwords.
    * It calls for a salt of at least 128 bits.
    * It calls for an iteration count of at least 1,000, though suggests that an iteration count of 10,000,000 may be appropriate in some use cases.
        * Also: "The  number of iterations should be set as high as can be tolerated for the environment, while maintaining acceptable performance."
* No salt may be used for our purposes, as these would not be shared between the frontend and backend.
    * Salts are typically a non-secret value that is randomly generated and prepended/appended to the hash output in plaintext.
    * Instead, a single shared "pepper" will be used for all HICNs.
        * Unlike with salts, peppers must **never** be included in plaintext with the hash results.
        * The pepper is a sensitive secret and must be stored separately from the hashes, e.g. in a file on a different server from the DB that the hashes live in.
* The Python Django framework being used by the frontend has builtin support for PBKDF2-HMAC-SHA256. The backend Java libraries being used also support this algorithm.

Based on the following constraints, the following system design has been arrived at:

1. The HICNs will be hashed using PBKDF2-HMAC-SHA256.
    1. No salt will be used.
    1. Instead of a salt, a secret pepper of at least 128 bits will be generated and shared out-of-band between the frontend and backend environments.
    1. A number of iterations no less than 1,000 will be used: the highest number possible that will not add more than five days to the overall beneficiary initial load time.
1. To reduce the danger of these hashes being leaked or broken, future iterations of our design will move them into a separate FHIR resource linked to the `Patient` resources, e.g. `Linkage` or `Basic`.

Here are some sample hashes produced using the above scheme:

* `iterations=1000`, `pepper=nottherealpepper`, `hicn=123456789A`: `d95a418b0942c7910fb1d0e84f900fe12e5a7fd74f312fa10730cc0fda230e9a`
* `iterations=1000`, `pepper=nottherealpepper`, `hicn=987654321E`: `6357f16ebd305103cf9f2864c56435ad0de5e50f73631159772f4a4fcdfe39a5`

A note on performance: my Intel i7-3740QM Processor (6M Cache, up to 3.70 GHz) development laptop computes hashes using a 128-bit salt and 1000 iterations at a rate of 107 per second _on a single thread_. If we assume that's the performance for the initial load, it would add about 6.5 days to the time required for the initial load of 60M beneficiaries.

### Proposed Backend Design

The above design can actually all be accomplished using only the standard FHIR mechanisms. The backend must simply ensure that each `Patient` resource includes a `http://bluebutton.cms.hhs.gov/identifier#hicnHash` identifier.

## Idempotency and HTTP Error Handling

**"Idempotent"**: An idempotent operation is one that can be retried/replayed without affecting the outcome; the result will be the same if the operation is applied one time or a hundred times.

The FHIR HTTP REST API does not guarantee delivery. Additionally, AWS has proven to be a very lossy environment: a non-trivial percentage of HTTP requests from the ETL service to the FHIR server have been observed to fail. This poses an important problem: what should the ETL service do when it submits transactions to the server and fails to receive a response?

The ETL service **must** guarantee that all data is loaded succesfully. Therefore, the ETL service **must** retry submissions that fail for transport-related reasons.

Unfortunately, the semantics of the FHIR API make retrying submissions tricky. Most of the FHIR write operations aren't idempotent. Here's a breakdown of the relevant FHIR operations (from [FHIR Latest Release: RESTful API](https://www.hl7.org/fhir/http.html)):

* `update`: There are two forms of `update` operations:
    * `PUT [base]/[type]/[id]` (_upsert by ID_): Will update the resource specified, resulting in a new version, even if the new content supplied exactly matches the previous version.
        * FHIR allows (and HAPI supports) servers to allow this operation to act as an "upsert", creating the specified resource at the specified ID, if it was not already present.
    * `PUT [base]/[type]?[search parameters]` (_conditional update_): If the search finds no matches, this will result in a `create` operation. If the search finds exactly one match, this will update that resource. If the search finds more than one match, this will return an error.
* `create`: There are two forms of `create` operations:
    * `POST [base]/[type]` (_normal create_): Will result in a new resource, with a server-specified ID.
    * `POST [base]/[type]` with `If-None-Exist: [search parameters]` (_conditional create_): Will only create the resource if the specified search returns no results.

Of these four operations/variants, only the last one, _conditional create_, is truly idempotent. It may seem like the first _update by ID_ option is also idempotent, but it's not: it will result in duplicate versions being created. (Indeed, the ETL service was attempting to use this option for quite a while, until this was discovered.)

Unfortunately though, of those four operations/variants, only the first one allows clients to specify the logical ID of the resource being created. For the Blue Button API, having predictable logical IDs is a definite nice-to-have: it makes it immediately obvious what's in a given resource, and also which resource to select.

This presents a choice: achieve idempotency via use of the _conditional create_ operation and lose the ability to specify logical IDs, or specify logical IDs using the _upsert by ID_ operation and lose idempotency. Idempotency is more important, so we've opted to use _conditional create_.
