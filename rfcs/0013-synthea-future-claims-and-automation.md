# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0013-synthea-future-claims-and-automation`
* Start Date: 2022-04-08
* RFC PR: [beneficiary-fhir-data/rfcs#0013]()
* JIRA Ticket(s):
  * [BFD-1616](https://jira.cms.gov/browse/BFD-1616)

The demand for realistic synthetic beneficiaries with recent claim data, for BFD users, continues to grow after each synthea release. While the process to generate, load, test, and release synthetic data has become more refined and convenient, it is prone to human error that can cause significant delays and miscommunication during release. This RFC proposes a streamlined, automated process for generating, loading, testing, and releasing synthea data to BFD users, while taking into account changes to synthetic benficiary properties with recent and future claim dates. 

## Status
[Status]: #status

* Status: Proposed
* Implementation JIRA Ticket(s):
  * [BFD-XXXX](https://jira.cms.gov/browse/BFD-XXXX)

## Table of Contents
[Table of Contents]: #table-of-contents

* [RFC Proposal](#rfc-proposal)
* [Status](#status)
* [Table of Contents](#table-of-contents)
* [Motivation](#motivation)
* [Proposed Solution](#proposed-solution)
    * [Proposed Solution: Detailed Design](#proposed-solution-detailed-design)
    * [Proposed Solution: Unresolved Questions](#proposed-solution-unresolved-questions)
    * [Proposed Solution: Drawbacks](#proposed-solution-drawbacks)
    * [Proposed Solution: Notable Alternatives](#proposed-solution-notable-alternatives)
* [Prior Art](#prior-art)
* [Future Possibilities](#future-possibilities)
* [Addendums](#addendums)

## Motivation
[Motivation]: #motivation

For those unfamiliar with Mitre's Synthea application for generating synthetic beneficiary and claim data, before discussing motivation and a proposed solution, here is some background on Synthea and how BFD uses it. 

### Background on Synthea

Mitre Synthea:

* [Synthea](https://github.com/synthetichealth/synthea) -- for generating synthetic beneficiary and claim data to load into BFD DB.

Generating & Loading Synthea Data: 
- BFD clones the latest version of the synthea codebase, and generates the RIF files via the `run_synthea` command, where the batch size and geography can be set, as well as a synthea.properties file, which is important, for defining numeric ranges of key claim and beneficiary properties i.e. beneficiary ID, claim group ID, claim date, etc.
- The RIF files are copied into the BFD Server codebase, and loaded into the local database. 
- If no errors loading locally, the RIF files are uploaded to AWS S3 and loaded into TEST, PROD SBX, PROD databses via the same ETL process with CCW RIF files. 
- Synthetic data is now available to BFD users.

* In addition to these steps, BFD follows a thorough test plan before generating or loading data into TEST, PROD SBX, or PROD environments. 
  * [Synthea Test Plan](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-model/bfd-model-rif-samples/dev/synthea-test-plan.md) 

Motivation:
With previous Synthea data releases having spanned about 3 months apart, and only 20,000 synthetic beneficiaries released, data becomes easily outdated, and the amount of data being released doesn't justify the usecase of synthetic data. So, there is inefficiency involved with producing and releasing synthetic data when done without automation. Inefficiency that in part is caused by a process full of human erorr, causing incidents, such as accidental overlap of beneficiary IDs between different synthetic data batches. Therefore, there are more than enough reasons to look into streamlining and scaling up synthetic data production to maximize the benefit to users and partners. 

## Proposed Solution
[Proposed Solution]: #proposed-solution

The proposed solution is to automate the generation of synthetic data with a set batch size on a weekly basis using the latest version of the synthea codebase.
Each quarter, the expectation is to have significant changes to the synthetic data properties from Mitre's code. So the communications to users and partners on a quarterly basis will be more in depth compared to that of on a weekly basis. The goal is to primarly generate data on a regular basis to increase the benefit and quality of the data to the user at a given point in time. 

On a weekly basis, synthetic data with a future claim date will also be generated. This data will be staged until it can be loaded properly with the claim date. 

Additionally, for this data to be ingested on a weekly basis, the BFD ingestion pipeline will need to work with Synthea data as it does with CCW data, for which back-dated beneficiary filtering will be turned off for Synthea data. 

When producing and loading a large set of synthetic data and performance testing:
    - Size and shape of the data will reflect production
    - Will be available for on-demand performance and load testing
    - Will not be released. Specifically, not released to PROD.
    - Will be isolated from other released synthetic data. 
    - Will have the capability to be re-generated with future versions of synthea
    - Synthetic beneficiaries for load and performance testing will not overlap with any beneficiaries used by end users

For Part D contract IDs, the value will not need to change on a regular basis unless there is a specific customer use case presented.

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

Weekly Generation Plan:

* On a weekly basis a CRON job within Jenkins will be set up to run scripts that:
  - Clone latest version of Mitre Synthea code
  - Run currently manual steps in the `Synthea Test Plan` such as accessing and comparing the latest Synthea end-properties file, running queries in PROD SBX to determine database ranges for beneficiary and claim properities, and ensuring the next batch of data will not overlap with existing data ranges.
  - Generate a batch of synthetic data and load into AWS S3 to trigger the BFD pipeline to transform and load synthetic data into database for TEST, PROD SBX, and PROD.

* A new database table 'synthetic-future-claims' will be created for staging future claims in TEST to be loaded at a later time:

* Synthea data will be compatible with the ETL ingestion pipeline by modifying the manifest to inform that a load with back-dated data should be loaded.

Load and Performance Testing Plan:
- Resources: The BFD TEST and/or PROD SBX servers will be more utilized for on-demand performance and load testing

- Scalability: Size and shape of the data will reflect production i.e. 1 million+ beneficiaries. This could take several days, however from previous load testing, it is possible to extrapolate how much time different load sizes will approximately take. 

- Availability: Data will not be released to PROD without further approval. New data will be isolated from previously released synthetic data by a dynamic range of benficiary IDs, which also prevents overlap. 

- Repeatability: Several delete SQL queries will be executed after load tests are completed, which will allow for beneficiary and claim data to be re-generated with future versions of synthea

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

What exactly will vary between weekly and quarterly communications to partners?

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

The need for regular TEST, PROD SBX, and PROD backups will be imperative in case issues arise with loading and removing data to prevent overlap.

Larger load sizes will require possibly an algorithm to optimize database constraint ranges for beneficiary and claim parameters, so they do not overlap. This in addition to longer load times will put emphasis on testing and efficiency to not create bottlenecks or delays on weekly data releases. 

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

## Prior Art
[Prior Art]: #prior-art

Gitlab migration style guide:
https://docs.gitlab.com/ee/development/migration_style_guide.html

## Future Possibilities
[Future Possibilities]: #future-possibilities

## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* (none at this time)
