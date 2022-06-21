# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0013-Synthea-future-claims-and-automation`
* Start Date: 2022-04-08
* RFC PR: [beneficiary-fhir-data/rfcs#0013]()
* JIRA Ticket(s):
  * [BFD-1616](https://jira.cms.gov/browse/BFD-1616)

The demand for realistic synthetic beneficiaries with recent claim data, for BFD users, continues to grow after each Synthea release. While the process to generate, load, test, and release synthetic data has become more refined and convenient, it is prone to human error that can cause significant delays and miscommunication during release. This RFC proposes a streamlined, automated process for generating, loading, testing, and releasing Synthea data to BFD users, while taking into account changes to synthetic benficiary properties with recent and future claim dates. 

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

  - Quarterly - Synthetic data will be generated with a set batch size using the up-to-date master branch of the Synthea codebase in a hosted cloud instance, along with the Synthea end-state properties file from the most recent generated batch, which is hosted in AWS S3. The end-state properties file is critical for making sure the next batch of Synthea data will not overlap with released Synthea data.

  - Future Claim Data - A batch of similar size for claim data with a future date will also be generated in a similar fashion. 

  - Special data parameters of interest: One parameter, which has not changed in previous releases of Synthea is Part D Event ID (PDE ID). Despite monotonically increasing other parameters i.e. bene ID, claim group ID, the value of PDE ID will not need to change on a regular basis, unless there is a specific customer use case presented. Other parameters of interest, are in the end-state properties, and Synthea.properties files i.e. claim id, claim group id starts, etc, which do change, and can cause collisions. 

### Automated Recurring Loading
  - Once the synthetic data is generated, within the same hosted cloud instance, the latest version of the BFD application from github, and dockerized database, will have been installed prior, and will be used to run the BFD pipeline integration test (IT), to load the new batch of synthetic data, ensuring that the newly generated data can be inserted properly.

  - Additionally, the RIF files from the previous quarterly batch of Synthea data will be pulled from AWS S3, and loaded into the database with the new batch of Synthea data.  

  - Automated load testing will occur on this instance, and when complete, a script will run that modifies the new batch's generated manifest file, and uploads the RIF files, end-state properties, and manifest files to S3 via command line file transfer. This will trigger the BFD ingestion pipeline in PROD SBX or TEST so that the data is stored in the database.

  - The separate RIF files associated with future claim data for the given release will be uploaded to a designated folder in S3 for staging until loading in the future. Updates to the pipeline application will be made to scan for these folders and trigger an incoming load job after a certain period of time has passed. 

  - Additional testing - As of 04/21/2022, there is a plan in place to remove 6 million de-duped non-synthetic benes, along with ~10-15 3 digit benes that were used for some other testing in the past. The cleanup will be removing the 6 million non-negative non-synthetic benes, along with the 3 digit benes, but will be leaving the existing 50k Synthea benes. Claim data from this cleanup is largely responsible for the certain parameter collisions that have taken place in the past, when loading new Synthea data. Beneficiary id however will still have potential for collision, as most of the data being removed has non-negative beneficiary ids. Outside of setting beneficiary id ranges, once this data is permenantly removed, it will likely take many years for there to be enough data to cause overlap. The amount and queries and checks in the script that automates the Synthea test plan will not have to be as thorough. 

Large Synthetic Data Generation:
  - Each PI (Program Increment), approximately every 8 weeks, 10 - 60 million Synthea beneficiaries will be generated and available for on-demand performance and load testing on TEST and PROD SBX databases. The number of beneficiaries in the dataset size is meant to reflect the size and shape of production. The process will be similar to that of automated recurring generation, however the memory and computational power of the AWS instance will be larger for time and cost effectiveness. 
  
  - Using a m5a.24xlarge EC2 instance, it took 6 hours to generate 1 million synthetic beneficiaries. If one extrapolates from this benchmark, it would take 60 hours or a little under 3 days to generate 10 million, and 360 hours, or 15 days to generate 60 million beneficiaries.

  - For the time being, a single reusable instance will be used. Pipelining multiple EC2 instances to generate data more efficiently is something to explore at a later point, as well as running the generation and load testing of large data manually every PI. 

Required BFD Application Changes:

- When CCW data is ingested by the BFD pipeline, the application has filter logic for beneficiary UPDATEs with reference years prior to the current year by default. However, Synthea data must be able to UPDATE prior years. Additionally, future claims that are staged in S3 will need to be automatically ingested by the pipeline. For both Synthea and CCW data to be ingested by the pipeline application, the back-dated beneficiary filtering will have to be turned off for Synthea data, and a timestamp for future claims needs to be recognized. 

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

Automated Generation & Load Plan:

On a quarterly basis a cron job within Jenkins will be set up to start up and SSH into a provisioned AWS EC2 instance that will be turned off when not used, to run an Ansible script that takes a batch size and randomly generated year between 1 and 5 and will execute other scripts that:
  - Run currently manual steps in the `Synthea Test Plan` such as accessing and comparing the latest Synthea end-properties file, running queries in PROD SBX to determine database ranges for beneficiary and claim properities, and ensuring the next batch of data will not overlap with existing data ranges.
  - Generate batches of both synthetic current and future data
  - Load into AWS S3 for ingestion in TEST, PROD SBX, and PROD environments.

For future claims and new Synthea data to be ingested, the manifest.xml file will need additional fields for both a timestamp and whether there Synthea data. The pipeline will then check for these two fields. For future claims, the data that has been staged in S3 will be ingested once it is the date in the timestamp in the manifest. With these changes in place, on a weekly basis, future claims will be loaded into the database.

Large Synthetic Data Generation:
  - Every PI, a m5a.24xlarge, or more powerful instance will be spun up to generate 10 - 60 million beneficiaries. 
  - In order to distiguish load test data when stored in TEST or PROD SBX, the beneficiary ID range will start 1 million less than the smallest released Synthea beneficary ID 
  - After the load tests are complete, and data is reported, for beneficiary and claim data to be re-generated with future versions of Synthea, there needs to be a tool for deleting this data that generates and executes DELETE and SELECT statements in the database environments. This tool will take the upper and lower bounds of the benficiary, claim, claim group, and PDE ID ranges found in the RIF files that were loaded as an input. To ensure the data is properly deleted, SELECT statements with the ranges used to delete the data will be run, and the final output of the tool will be the counts of beneficiary and various claim tables. The counts in the output should be zero to indicate the data cannot be found.

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

- Which of these is a more viable failsafe/revert option if data fails to be loaded:
    - Revert to an earlier backup of the EC2 instance.
    - Create a unique marker for each Synthea dataset to easily query and delete the data. 
    - DELETE query tool.

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

- The need for regular TEST, PROD SBX, and PROD backups will be imperative in case issues arise with loading and removing data to prevent overlap.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

## Prior Art
[Prior Art]: #prior-art

## Future Possibilities
[Future Possibilities]: #future-possibilities

## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* (none at this time)
