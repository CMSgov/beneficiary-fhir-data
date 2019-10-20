# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0000-since-parameter-support` 
* Start Date: October 1, 2019
* RFC PR: <https://github.com/CMSgov/beneficiary-fhir-data/pull/155>
* JIRA Ticket(s): 
    - [BlueButton-1506: Bulk Export Since Support](https://jira.cms.gov/browse/BLUEBUTTON-1506)


This RFC proposal adds features to BFD's API to allow BFD's partners to implement the Bulk Export `_since` parameter. Specifically, it provides for a `lastUpdated` query parameter to FHIR resource search operation and a feed of metadata about BFD's data loads. The proposal discusses these new features as well as the logic that BFD's partners need to implement the `_since` parameter correctly. 

## Table of Contents

* [RFC Proposal](#rfc-proposal)
* [Table of Contents](#table-of-contents)
* [Motivation](#motivation)
* [Proposed Solution](#proposed-solution)
    * [BFD API Details](#bfd-api-details)
    * [Bulk Export Implementors Details](#bulk-export-implementors-details)
    * [BFD Implementation Details](#bfd-implementation-details)
    * [Roster Change Corner Case](#roster-change-corner-case)
    * [Internal Database Corner Case](#internal-database-corner-cases)
    * [Alternatives Considered](#alternatives-considered)
* [Future Possibilities](#future-possibilities)
* [References](#references)

## Motivation
[Motivation]: #motivation

Consumers of CMS's beneficiary data APIs, whether they call BlueButton 2.0, ACO API, or DPC want the most up-to-date information. Ideally, these apps and services would like to call a CMS API right as CMS updates its claim information. When they do call, they only want new data from CMS, not the information they already have. 

FHIR \[[1](#ref1)\] has provisions for an "update me about new information" pattern in FHIR APIs. For the bulk export operation, exports with a `_since` parameter specified should only return resources that have changed after the date and time specified in the `_since` parameter. For synchronous resource searches, there exists a `lastUpdated` parameter that has similar semantics. 

Today, BFD only supports returning all EOB resources associated with a single beneficiary. EOB calls return more than 5 years of beneficiary data, where only the last weeks of data is needed. This behavior is highly inefficient for the bulk export calls that happen weekly. On average, each call is returning 260 times as much information as is needed. 

Early feedback from both ACO API and DPC customers have nearly unanimously pointed out the need for _since parameter support \[[2](#ref2)\]. 
For the ACO API, where an export operation can take many hours and result in 100's GB of data, ACO API customers have stated that they need '_since' support to move to production. 
BB 2.0 app developers would like a similar feature as well. 

## Proposed Solution

This proposal adds 3 changes to the BFD API that are needed for downstream partners to implement the `_since` parameter. 

1. The `lastUpdated` metadata field of EOB, Patient, and Coverage FHIR resources contains the time they were written to the master DB by the ETL process. 
2. The search operation of the EOB, Patient, and Coverage resources support a `_lastUpdated` query parameter. When specified, the search filters resources against the passed in the date range. The capabilities statement includes the `_lastUpdated` as a search parameter. 
3. The BFD server adds optimizations on resource searches with `_lastUpdated` for the case where the result set is empty. These searches should return results in a similar time to the time taken by a metadata query. 

### BFD API Details

BFD's end-points follow the FHIR specification. All the proposed API changes are extensions to these end-points which follow the FHIR specification. 

The first improvement is to add the `lastUpdated` field to the metadata object of a resource. The current implementation does not return any `lastUpdated` field. The proposal adds this field with the timestamp that the ETL process wrote to the master DB. Like all FHIR date fields, this timestamp must include the server's timezone \[[4](#ref4)\]. Resources based on records loaded before this RFC do not have a `lastUpdated` field. 

The second change is to support the `_lastUpdated` query parameter for resource searches per the FHIR specification \[[5](#ref5)\]. FHIR specifies a set of comparison operators to go along with this filter. BFD supports the `eq`, `lt`, `le`, `gt` and `ge` operators. Two `_lastUpdated` parameters can be specified to form the upper and lower bounds of a time interval. 

Many records in the BFD database where loaded before this RFC and have a null `lastUpdated` field. The BFD treats these records as if they have an early `lastUpdated` value. Searches with a `_lastUpdated` parameter without a lower bound match these records; Likewise, searches with a lower bound never match these records. This design allows a single query to retrieve both records with and without a `lastUpdate`. 

### Bulk Export Implementors Details

Implementing `_since` support should be straight forward for BFD's partners that implement the FHIR Bulk Export specification. The following sequence diagram shows how this interaction should work. 

![Bulk ](https://www.websequencediagrams.com/files/render?link=zfMUJyQaf18DNUb6IQoPN2EBeq9tMctYXXupx6T5Co8gB3t9ysmhat0ToalxZ6p2)

For each beneficiary in the export group, the partner searches within a time interval. 
The lower bound of the interval is the `_since` parameter time passed by the bulk-export client. 
The upper bound of the time interval is the start of the bulk-export job. 
The start time is called the `transactionTime`, and the partner reports this time back to the client. 
The client uses the  `transactionTime` as the `_since` time of the next bulk-export. An example URL where the time period requested is a week is: 

```
https://<hostname>/v1/fhir/ExplanationOfBenefit
  ?patient=<beneficiaryId>
  &_lastUpdated=gt2018-11-22T14:01:01-05:00
  &_lastUpdated=le2018-18-22T15:00:00-05:00
  &_format=application%2Fjson%2Bfhir
```

The FHIR specification allows to bulk-export implementors to choose the `transactionTime` of their jobs. For efficient and fast queries, it is recommended that partners query with a time interval which the BFD has indexed the `lastUpdated` field. 
The indexing of `lastUpdated` is typically delayed by 1-10 seconds when RIF files are not being loaded and by 5-15 seconds when RIF files are being loaded. 
It is recommended that implementors choose a `transactionTime` that is 20 seconds in the past to ensure that all of their fetches will run against an index in BFD. 
As long a partners query within these recommendation, they can recommend to their clients a daily frequency for their bulk export jobs with `_since` without taxing the BFD. 


### BFD Implementation Details

For all top level-tables in the BFD, the RFC adds a new column for `lastUpdated`. 
When the BFD processes a new RIF file from the CCW, the `lastUpdated` column is updated. The BFD uses Hibernate's facility for this update. 
Because of the large size of the BFD tables, the BFD database does not index the `lastUpdated` column. This design avoids some of the indexing problems that the BFD has experienced. 

Most bulk-export clients intend to call CMS on at least a weekly basis. 
Since only a small set of records change in a given week, the most common result of a search is an empty set. 
The BFD implements a filter that allows the BFD data server to avoid querying the database in this case. 

The BFD pipeline tracks the beneficiaries that it updates in each RIF file load along with the interval of the load. 
The BFD data server uses this list of past RIF file loads and their associated beneficiaries to build its filters. 
The filter management process works on a background thread on the data server. 
It never interferes with the data serving process. 

![Filters](https://www.lucidchart.com/publicSegments/view/d8d09787-3968-43af-a974-05bd03472fdc/image.png) 

The filters internally use a Bloom filter data structure. Bloom filters are very memory efficient and commonly used in databases like Postgres \[[8](#ref8)\]. In essence, the filter design takes an optimization out of the database and implements it in the data server. 

### Roster Change Corner Case


The resources returned by a group export operation is the current roster of the group at the time of an export call. A group's roster may change between successive export calls. At this time, the importer does not have any data for the added beneficiaries. So, how should an export call with a `_since` parameter handle new beneficiaries? The FHIR specification states that export should only include data updated after the passed in `_since` parameter. However, the specification does not contemplate this use-case, nor does it hint on how to implement this use-case correctly. 

Since the BFD service does not track groups, the BFD partners have to work out solutions for this problem. The FHIR community Please see the authors for a discussion on solutions. 

### Internal Database Corner Cases

FHIR Resources are projections from the BFD's internal records, based on the CCW's RIF files. As a result, the FHIR Resources may have their `lastUpdated` field change when other fields do not change. 

Records created before this RFC do not have a `lastUpdated` value. FHIR resources derived from these records do not have a last updated field. 

### Alternatives Considered

Instead of optimizing at the BFD data server, an earlier design had the empty result set optimization done at the partner level. An ETL feed served by the BFD would allow the partner to implement the bloom filters now found in BFD data server. Although this design is slightly more efficient, the current design is simpler to run and requires less partner work. 

## Future Possibilities

This proposal should scale as BFD, and it's partners serve more beneficiaries and clients. It should continue to work as BFD adds more partners and data sources. 

In future releases, BFD may receive claim data faster than the current weekly updates. Care has been taken to make sure
the lastUpdated indexing works correctly during RIF file processing. This means that there should be no need to change
the algorithm when the BFD moves to daily or even hourly batches. 

Much of the design choices in this RFC was done to avoid taxing the database at the center of the BFD.
If the BFD database changes to allow the database to index `lastUpdated`, much of the optimizations done in this RFC can be removed.

In discussions with DPC customers, they have asked for notification when the DPC has new beneficiary data. Instead of polling for updates, they would like to have the ability for a push update. Similarly, FHIR is developing a subscription model that supports webhooks \[[6](#ref6)\]. If a BFD partner wants to develop these features, the file loaded tables can form a basis for this work. 

## References

The following references are required to fully understand and implement this proposal. They should be read before voting on this proposal.

<a id="ref1"></a>
[1] FHIR - Fast Health Interoperability Resources: <https://www.fhir.org>

<a id="ref2"></a>
[2] Rick Hawes: Conversations with customers of DPC and BCDA

<a id="ref3"></a>
[3] Working copy of the Bulk Export specification: <https://build.fhir.org/ig/HL7/bulk-data/export/index.html>

<a id="ref4"></a>
[4] FHIR Meta.lastUpdated definition: <https://www.hl7.org/fhir/resource-definitions.html#meta.lastupdated> 

<a id="ref5"></a>
[5] FHIR Search operation: <https://www.hl7.org/fhir/search.html>   

<a id="ref6"></a>
[6] FHIR Subscriptions: <https://www.hl7.org/fhir/subscription.html>

<a id="ref7"></a>
[7] Original Confluence page with an implementation outline: <https://confluence.cms.gov/pages/viewpage.action?pageId=189269516>

<a id="ref8"></a>
[8] Bloom Filter: <https://en.wikipedia.org/wiki/Bloom_filter>