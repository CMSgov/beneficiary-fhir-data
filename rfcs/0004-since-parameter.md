# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0004-since-parameter-support` 
* Start Date: October 1, 2019
* RFC PR: 
* JIRA Ticket(s): 
    - [BlueButton 1181](https://jira.cms.gov/browse/BLUEBUTTON-1181)


This RFC proposal adds features to BFD's API to allow BFD's partners to implement the Bulk Export `_since` parameter. Specifically, it provides for a `lastUpdated` query parameter to FHIR resource search operation and a feed of metadata about BFD's data loads. The proposal discusses these new features as well as the logic that BFD's partners need to implement the `_since` parameter correctly. 

## Table of Contents

* [RFC Proposal](#rfc-proposal)
* [Table of Contents](#table-of-contents)
* [Motivation](#motivation)
* [Proposed Solution](#proposed-solution)
    * [BFD API Details](#bfd-api-details)
    * [BFD Feed Details](#bfd-feed-details)
    * [Since Implementors Details](#since-implementors-details)
    * [ETL Corner Case](#etl-corner-case)
    * [Roster Change Corner Case](#roster-change-corner-case)
    * [Replication Lag Corner Case](#replication-lag-corner-case)
    * [Alternatives Considered](#alternatives-considered)
* [Future Possibilities](#future-possibilities)
* [Addendums](#addendums)

## Motivation
[Motivation]: #motivation

Consumers of CMS's beneficiary data APIs whether they call BlueButton 2.0, BCDA or DPC's want the most up-to-date information. Ideally, these apps and services would like to call a CMS API right as CMS updates its claim information. When they do call, they only want new data from CMS, not the information they already have. 

FHIR \[[1](#ref1)\] has provisions for an "update me about new information" pattern in FHIR APIs. For the bulk export operation, exports with a `_since` parameter specified should only return resources that have changed after the date and time specified in the `_since` parameter. For synchronous resource searches, there exists a `lastUpdated` parameter that has similar semantics. 

Today, BFD only supports returning all EOB resources associated with a single beneficiary. EOB calls return more than 5 years of beneficiary data, where only the last weeks of data is needed. This behavior is highly inefficient for the bulk export calls that happen weekly. On average, each call is returning 260 times as much information as is needed. 

Early feedback from both BCDA and DPC customers have nearly unanimously pointed out the need for _since parameter support. For BCDA, where an export operation can take many hours and result in 100's GB of data, BCDA customers have stated that they need '_since' support to move to production. Also, BB 2.0 app developers would like a similar feature  \[[2](#ref2)\]. 

## Proposed Solution

This proposal adds 3 changes to the BFD API that are needed for downstream partners to implement the `_since` parameter. 

1. The `lastUpdated` metadata field of EOB, Patient, and Coverage FHIR resources contains the time they were written to the master DB by the ETL process. 
2. The search operation of the EOB, Patient, and Coverage resources support `_lastUpdated` query parameter. When specified, the search operation filters resources against the passed in timestamp. 
3. The API adds a feed containing the start and finish times of the ETL job into BFD and a list of beneficiaries updated in the ETL. This feed enables BFD partners to correctly and efficiently implement the `_since` parameter. 

Using the new information provided by the BFD, a partner can implement `_since` exports efficiently. A key to correctness is handling while an ETL is taking place (see []()). A solution to efficiency is for partners to avoid calling the BFD if a particular beneficiary doesn't have an updated resource (see []()).

![Bulk ](https://www.websequencediagrams.com/files/render?link=zfMUJyQaf18DNUb6IQoPN2EBeq9tMctYXXupx6T5Co8gB3t9ysmhat0ToalxZ6p2)

### BFD API Details

All the proposed API changes follow the 


### BFD Feed Details

### Since Implementors Details

### ETL Corner Case

Every export job completion record contains a `transactionTime` field that bulk-export clients use as the `_since` parameter in a subsequent export job. The `transactionTime` is the time that the export job starts. The FHIR bulk export specification states that an export SHALL only contain resources updated before the `transactionTime` time. The specification further says that an exporter should delay a job until all pending writes have finished to satisfy this constraint. The ELT feed allows bulk-export implementors to know when the BFD ETL finishes. Since both the bulk export operation and the BFD ETL process take several hours, the delay may be significant. 

To avoid this delay, a bulk-export implementor (e.g., DPC) may take an optimistic approach by immediately starting an export job, but if it receives a resource updated after the job start time, then delaying and restarting the job.  

### Roster Change Corner Case

The resources returned by a group export operation is the current roster of the group at the time of an export call. A group's roster may change between successive export calls. At this time, the importer does not have any data for the added beneficiaries. So, how should an export call with a `_since` parameter handle new beneficiaries? The FHIR specification states that export should only include data updated after the passed in `_since` parameter. However, the specification does not contemplate this use-case, nor does it offer any hint on how to correctly implement this use-case. 

Since the BFD service does not track groups, the BFD partners have to work out solutions for this problem. Please see the authors for a discussion. 

### Replication Lag Corner Case

Because of differing replication delays between the master database and the replicas, there is the potential for race conditions between the ETL feed and the DB replicas.  The BFD ETL process avoids this problem by delaying marking a BFD ETL job complete in the ETL feed until all writes have had time to propagate. The BFD ETL process can query the replication lag directly from the Postgres DB. 

### Alternatives Considered

1. Instead of an ETL feed served by S3, BFD could convey the ETL information in other ways. For example, BFD could add an API for this information. Given the low volume of events and the low number of subscribers, the proposal chooses the S3 approach because it requires less code, while still meeting the needs of the problem

2. The ETL feed could contain more ETL metadata, such as the FHIR resource touched, the DB table updated, or the care team involved. In the end, it was decided not to put this information into the
feed because it was not general enough or universally useful. 

## Future Possibilities

Scale out

PUB-SUB to customers

More frequent updates 

Richer feeds 

## References

The following references are required to fully understand and implement this proposal. They should be read before voting on this proposal.

<a id="ref1"></a>
[1] FHIR - Fast Health Interoperability Resources: <https://www.fhir.org>

<a id="ref2"></a>
[2] Rick Hawes: Conversations with customers of DPC and BCDA

<a id="ref3"></a>
[3] Working copy of the Bulk Export specification: <https://build.fhir.org/ig/HL7/bulk-data/export/index.html>


<a id="ref3"></a>
[3]   


