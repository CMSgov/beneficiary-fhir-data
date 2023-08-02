# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0013-Synthea-future-claims-and-automation`
* Start Date: 2022-04-08
* RFC PR: [beneficiary-fhir-data/rfcs#0013]()
* JIRA Ticket(s):
  * [BFD-1616](https://jira.cms.gov/browse/BFD-1616)
  * [BFD-2100](https://jira.cms.gov/browse/BFD-2100)

The demand for realistic synthetic beneficiaries with recent claim data, for BFD users, continues to grow after each Synthea release. While the process to generate, load, test, and release synthetic data has become more refined and convenient, it is prone to human error that can cause significant delays and miscommunication during release. This RFC proposes a streamlined, automated process for generating, loading, testing, and releasing Synthea data to BFD users, while taking into account changes to synthetic beneficiary properties with recent and future claim dates. 

## Status
[Status]: #status

* Status: Implemented
* Implementation JIRA Ticket(s):
  * [BFD-2526](https://jira.cms.gov/browse/BFD-2526)
  * [BFD-2105](https://jira.cms.gov/browse/BFD-2105)
  * [BFD-2281](https://jira.cms.gov/browse/BFD-2281)
  * [BFD-2104](https://jira.cms.gov/browse/BFD-2104)
  * [BFD-2106](https://jira.cms.gov/browse/BFD-2106)
  * [BFD-2234](https://jira.cms.gov/browse/BFD-2234)
  * [BFD-2099](https://jira.cms.gov/browse/BFD-2099)
  * [BFD-2126](https://jira.cms.gov/browse/BFD-2126)
  * [BFD-2196](https://jira.cms.gov/browse/BFD-2196)
  * [BFD-2207](https://jira.cms.gov/browse/BFD-2207)
  * [BFD-2208](https://jira.cms.gov/browse/BFD-2208)
  * [BFD-2209](https://jira.cms.gov/browse/BFD-2209)
  * [BFD-2210](https://jira.cms.gov/browse/BFD-2210)
  * [BFD-2135](https://jira.cms.gov/browse/BFD-2135)
  * [BFD-2158](https://jira.cms.gov/browse/BFD-2158)
  * [BFD-2525](https://jira.cms.gov/browse/BFD-2525)
  * [BFD-2450](https://jira.cms.gov/browse/BFD-2450)
  
See the [Synthetic Data Guide](https://github.com/CMSgov/beneficiary-fhir-data/wiki/Synthetic-Data-Guide#datasets-with-weekly-updates) for details on the implementation of the rolling updates.

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

As previous synthetic data releases were separated by 3 months, and only 20,000 synthetic beneficiaries released, end-users have expressed interest in a larger synthetic dataset to work with, that has consistently current claim dates. Additionally, there is inefficiency involved with producing and releasing synthetic data when done without automation. Manual process are prone to human error, and can cause incidents, such as accidental overlap of beneficiary IDs between different synthetic data batches. There are more than enough reasons to look into streamlining and scaling up synthetic data production to maximize the benefit to users and partners. This involves automated recurring Synthea data generation, automated recurring loading, large Synthea data generation, and potential required changes to the BFD server and pipeline application.

For those unfamiliar with Mitre's Synthea application for generating synthetic beneficiary and claim data, before discussing a proposed solution, here is some background on Synthea and how BFD uses it. 

### Background on Synthea

Mitre Synthea: 

- [Synthea](https://github.com/synthetichealth/synthea) -- for generating synthetic beneficiary and claim data to load into BFD DB. 

- How To Differentiate Synthea And Real Data:
    - Synthetic beneficiary IDs are a negative number.
    - Synthetic MBIs have the character 'S' in a certain position.

Generating & Loading Synthea Data: 
- BFD downloads the master branch of the Synthea codebase, generates the RIF files via the `run_Synthea` command, where the batch size and geography can be set, as well as a Synthea.properties file, which is important, for defining numeric ranges of key claim and beneficiary properties i.e. beneficiary ID, claim group ID, claim date, etc.
- The RIF files are copied from the Synthea output folder to a local master branch of the BFD codebase in `bfd-model/bfd-model-rif-samples/src/main/resources/rif-synthea` for load testing into the local BFD database. 
- If there are no errors loading, the RIF files are uploaded to AWS S3 and loaded into TEST, PROD SBX, PROD databses via the same ETL process with CCW RIF files. 
- Synthetic data is now available to BFD users.

In addition to these steps, BFD follows a thorough [Synthea Test Plan](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-model/bfd-model-rif-samples/dev/synthea-test-plan.md)  before generating or loading data into TEST, PROD SBX, or PROD environments.

## Proposed Solution
[Proposed Solution]: #proposed-solution

### Automated Recurring Generation

Automating the generation and loading of Synthea data will remove a lot of error-prone manual work and also help ensure that users are regularly receiving the benefits of the ongoing improvements to Synthea. This operation will be scheduled to run once per quarter.

  - Quarterly - Synthetic data for current and future claims will be generated with a set batch size using the up-to-date master branch of the Synthea codebase in a hosted cloud instance, along with the Synthea end-state properties file from the most recent generated batch, which is hosted in AWS S3. The end-state properties file is critical for making sure the next batch of Synthea data will not overlap with released Synthea data.

  - Special data parameters of interest: One parameter, which has not changed in previous releases of Synthea is Part D contract ID. Despite monotonically increasing other parameters i.e. bene ID, claim group ID, PDE event ID, the value of the PDE contract ID will not need to change on a regular basis, unless there is a specific customer use case presented. Other parameters of interest, are in the end-state properties, and Synthea.properties files i.e. claim id, claim group id starts, etc, which do change, and can cause collisions. 

### Automated Recurring Loading

  - The synthetic data will be tested in isolation by (on the same system that was used to generate the data):
    1. Cloning the latest version of BFD from GitHub.
    2. Launching a local PostgreSQL database via Docker.
    3. Downloaing the RIF files from the previous quarterly batch of Synthea data from AWS S3, and running the BFD build and integration tests, using the newly generated Synthea data and previous batch data, to verify that the data loads as expected.

  - Once loaded, a script will run that modifies the new batch's generated manifest file, and uploads the RIF files, end-state properties, and manifest files to S3 via command line file transfer. This will trigger the BFD ingestion pipeline in TEST so that the data is stored in the database.

  - The separate RIF files associated with future claim data for the given release will be uploaded to a designated folder in S3 for staging until loading in the future. Updates to the pipeline application will be made to scan for these folders and trigger an incoming load job each weekend, for the claims that have "matured" that week (similar to how new claims are loaded each week in `prod`).

Additional testing - As of 07/05/2022, 6 million de-duped non-synthetic benes, along with ~10-15 3 digit benes that were used for some other testing in the past have been removed. The cleanup removed the 6 million non-negative non-synthetic benes, along with the 3 digit benes, and left the existing 50k Synthea benes. Claim data from this cleanup was largely responsible for the certain parameter collisions that have taken place in the past, when loading new Synthea data. There's also ongoing efforts to clean up other data ranges to avoid all future collisions, with properties such as claim group id. Beneficiary id, claim group id, and other properties will still have potential for collision, as most of the data that remains will have distinct Synthea characteristics i.e. negative beneficiary ids. With these cleanups in place, it will likely take many years for there to be enough data to cause overlap. The amount and queries and checks in the script that automates the Synthea test plan will not have to be as thorough. 

### Large Synthetic Data Generation
  - Each PI (Program Increment), approximately every 8 weeks, 10 - 60 million Synthea beneficiaries will be generated and available for on-demand performance and load testing on TEST and PROD SBX databases. The number of beneficiaries in the dataset size is meant to reflect the size and shape of production. The process will be similar to that of automated recurring generation, however the memory and computational power of the AWS instance will be larger for time and cost effectiveness. 
  
  - Using a m5a.24xlarge EC2 instance, it took 6 hours to generate 1 million synthetic beneficiaries. If one extrapolates from this benchmark, it would take 60 hours or a little under 3 days to generate 10 million, and 360 hours, or 15 days to generate 60 million beneficiaries.

  - A new instance will be spun up with Terraform and Jenkins to avoid introducing security vulnerbilities and patching with a reusuable instance. 

### Required BFD Application Changes

The following changes to existing behavior will be implemented:
	
	1. RIF manifests will be updated with optional attributes to indicate:
	    1. When a data set should have back-dated beneficiary filtering disabled.
	    2. When a data set has a "do not process until" date.
	2. The BFD Pipeline application will be updated to accept, parse, and honor those attributes.

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

Automated Generation & Load Plan:

On a quarterly basis a cron job within Jenkins will be set up to spin up and SSH into a provisioned AWS EC2 instance, and run an Ansible script that takes a batch size and generation year, and will execute other scripts that:
  - Run currently manual steps in the `Synthea Test Plan` such as accessing and comparing the latest Synthea end-properties file, running queries in PROD SBX to determine database ranges for beneficiary and claim properities, and ensuring the next batch of data will not overlap with existing data ranges.
  - Generate batch containing both synthetic historical and future data
  - Load into AWS S3 for ingestion in TEST, PROD SBX, and PROD environments.

For future claims and new Synthea data to be ingested, the manifest.xml file will need additional fields for both a timestamp and whether there Synthea data. The pipeline will then check for these two fields. For future claims, the data that has been staged in S3 will be ingested once it is the date in the timestamp in the manifest. With these changes in place, on a weekly basis, future claims will be loaded into the database.

Large Synthetic Data Generation:
  - Every PI, a m5a.24xlarge, or more powerful instance will be spun up to generate 10 - 60 million beneficiaries. 
  - In order to distiguish load test data when stored in TEST or PROD SBX, the beneficiary ID range will start 1 million less than the smallest released Synthea beneficary ID 
  - After the load tests are complete, and data is reported, the data will persist in the database. If at some point Synthea's codebase involves major changes to beneficiary and claim data properties, this data will need be deleted and re-generated the newer version of Synthea. There needs to be a tool for deleting this data that generates and executes DELETE and SELECT statements in the database environments. This tool will take the upper and lower bounds of the benficiary, claim, claim group, and PDE ID ranges found in the RIF files that were loaded as an input. To ensure the data is properly deleted, SELECT statements with the ranges used to delete the data will be run, and the final output of the tool will be the counts of beneficiary and various claim tables. The counts in the output should be zero to indicate the data cannot be found.

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

- Which of these is a more viable failsafe/revert option if data fails to be loaded:
    - Revert to an earlier backup of the EC2 instance.
    - Create a unique marker for each Synthea dataset to easily query and delete the data. 
    - DELETE query tool.

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

- The need for regular TEST, PROD SBX, and PROD backups will be imperative in case issues arise with loading and removing data to prevent overlap, and when running in idempotent mode with the pipeline does not work.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

## Prior Art
[Prior Art]: #prior-art

## Future Possibilities
[Future Possibilities]: #future-possibilities

- Pipelining multiple EC2 instances to generate data more efficiently is something to explore at a later point.

## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* (none at this time)
