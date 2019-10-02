# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0004-since-parameter-support` 
* Start Date: October 1, 2019
* RFC PR: TBD
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

Early feedback from both BCDA and DPC customers have nearly unanimously pointed out the need for _since parameter support \[[2](#ref2)\]. For BCDA, where an export operation can take many hours and result in 100's GB of data, BCDA customers have stated that they need '_since' support to move to production. BB 2.0 app developers would like a similar feature as well. 

## Proposed Solution

This proposal adds 3 changes to the BFD API that are needed for downstream partners to implement the `_since` parameter. 

1. The `lastUpdated` metadata field of EOB, Patient, and Coverage FHIR resources contains the time they were written to the master DB by the ETL process. 
2. The search operation of the EOB, Patient, and Coverage resources support `_lastUpdated` query parameter. When specified, the search operation filters resources against the passed in timestamp. 
3. The API adds a feed containing the start and finish times of the ETL job into BFD and a list of beneficiaries updated in the ETL. This feed enables BFD partners to correctly and efficiently implement the `_since` parameter. 

Using the new information provided by the BFD, a partner can implement `_since` exports. To improve the efficiency of the export operation and its performance, partners must avoid calling the BFD if a particular beneficiary doesn't have an updated resource (see [Since Implementors Details](#since-implementors-details)). The following sequence diagram shows how this interaction should work. 

![Bulk ](https://www.websequencediagrams.com/files/render?link=zfMUJyQaf18DNUb6IQoPN2EBeq9tMctYXXupx6T5Co8gB3t9ysmhat0ToalxZ6p2)

### BFD API Details

All the proposed API changes follow the FHIR specification. The first improvement is to add the `lastUpdated` to the metadata field of a resource. The current implementation returns the time the that the request was made. The proposal alters this field to be the timestamp that the ETL process wrote to the master DB. Like all instant timestamps, this timestamp must include timezone \[[4](#ref4)\]. 

The second change is to support the `_lastUpdated` query parameter for resource searches \[[5](#ref5)\]. FHIR specifies a set of comparison operators to go along with this filter. Although bulk exporters will only use the `gt` or greater-than operator, a complete set will be useful for other use-cases. 

### BFD Feed Details

The ETL server will write to a S3 bucket a feed of NDJSON records. Each record contains fields for the ETL job's id, status, start and end timestamps. The record also contains a pointer to a list of beneficiaries which were udpated in the ETL job. The list is a ASCII encoded comma seperated list of MBIs. The MBI list will be written in a sorted in order. 

```
{
    "id": 100,
    "status": "done",
    "start": "2019-02-07T12:28:17-05:00",
    "end": "2019-02-07T13:28:17-05:00" 
    "beneficiaries": "s3://bfd-prod-etl-feed/bene20191101.mbi"
}
```
The S3 bucket with the feed and the MBI lists are private, with only access to BFD partners. S3 cross-account access policies will be used to lock down access. 

### Since Implementors Details

To add since support, downstream BFD partners must:
1. Update their export operation end-point to support '_since.' 
2. At the start of a job, read the ETL feed from S3. From this feed retrieve: 
   1. The start and end time of the last ETL job
   2. The list of beneficiaries updated in the last ETL job. 
3. Based on the timestamp passed in the `_since` parameter, optimize the queries to BFD in the following ways: 
   1. If BFD did not run ETL jobs since the passed timestamp, do not call the BFD and return an empty result set. 
   2. If BFD ran one ETL job since the passed-in timestamp, call BFD only for those beneficiaries included in the beneficiary list.
   3. If BFD ran multiple jobs since the passed-in timestamp, it is acceptable not to optimize BFD calls and call the BFD for all beneficiaries in the export job.  

The scalability of BFD could be a limiting factor in the expansion of DPC and BCDA to more customers. Every week less than 5% of beneficiaries have a new claim. By only calling BFD when a beneficiary has an updated claim, the partner is effectively increasing the capacity of BFD.

### ETL Corner Case

Every export job completion record contains a `transactionTime` field that bulk-export clients use as the `_since` parameter in a subsequent export job. The `transactionTime` is the time that the export job starts. The FHIR bulk export specification states that an export SHALL only contain resources updated before the `transactionTime` time. The specification further says that an exporter should delay a job until all pending writes have finished to satisfy this constraint. The ELT feed allows bulk-export implementors to know when the BFD ETL finishes. Since both the bulk export operation and the BFD ETL process take several hours, the delay may be significant. 

To avoid this delay, a bulk-export implementor may take an optimistic approach by immediately starting an export job, but monitoring if it receives a resource updated after the job start time. If it does, the it should delay and restart the job.  

### Roster Change Corner Case

The resources returned by a group export operation is the current roster of the group at the time of an export call. A group's roster may change between successive export calls. At this time, the importer does not have any data for the added beneficiaries. So, how should an export call with a `_since` parameter handle new beneficiaries? The FHIR specification states that export should only include data updated after the passed in `_since` parameter. However, the specification does not contemplate this use-case, nor does it offer any hint on how to correctly implement this use-case. 

Since the BFD service does not track groups, the BFD partners have to work out solutions for this problem. Please see the authors for a discussion on solutions. 

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

<a id="ref4"></a>
[4] Meta.lastUpdated defintion: <https://www.hl7.org/fhir/resource-definitions.html#meta.lastupdated> 

<a id="ref5"></a>
[5] Search operation: <https://www.hl7.org/fhir/search.html>   


