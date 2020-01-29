# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0000-since-parameter-support` 
* Start Date: October 1, 2019
* Version 2
* RFC PR: <https://github.com/CMSgov/beneficiary-fhir-data/pull/155>
* JIRA Ticket(s): 
    - [BlueButton-1506: Bulk Export Since Support](https://jira.cms.gov/browse/BLUEBUTTON-1506)


This RFC proposal adds features to BFD's API to allow BFD's partners to implement the Bulk Export `_since` parameter. The proposal discusses these new features as well as the logic that BFD's partners need to implement the `_since` parameter correctly. 

## Table of Contents

* [RFC Proposal](#rfc-proposal)
* [Table of Contents](#table-of-contents)
* [Motivation](#motivation)
* [Proposed Solution](#proposed-solution)
    * [BFD API Details](#bfd-api-details)
    * [Single Beneficiary Implementors Details](#single-beneficiary-implementors-details)
    * [Bulk Export Implementors Details](#bulk-export-implementors-details)
    * [BFD Implementation Details](#bfd-implementation-details)
    * [Roster Change Corner Case](#roster-change-corner-case)
    * [Time Corner Cases](#time-corner-cases)
    * [Database Schema Corner Case](#database-schema-corner-cases)
    * [Alternatives Considered](#alternatives-considered)
* [Future Possibilities](#future-possibilities)
* [References](#references)

## Motivation
[Motivation]: #motivation

Consumers of CMS's beneficiary data APIs, whether they call BlueButton 2.0, ACO API, or DPC, want the most up-to-date information. 
Ideally, these apps and services would like to call a CMS API as soon as CMS updates its claim information. 
When they do call, they only want new data from CMS, not the information they already have. 

Before this RFC, BFD only supported returning all resources for a single beneficiary. Calls were returning more than 5 years of beneficiary data, when only the last weeks of data may be needed. This behavior is highly inefficient, especially for the bulk export jobs that happen weekly. In this case, each call is returning 260 times as much information as is needed on average. 

The FHIR \[[1](#ref1)\] standard has provisions for an "update me about new information" pattern in its APIs. For bulk export operations, this feature is called the `_since` parameter. 
For single beneficiary operations, it is called the `_lastUpdated` parameter. 

Early feedback from both ACO API and DPC customers have nearly unanimously pointed out the need for `_since` parameter support \[[2](#ref2)\]. 
For the ACO API, where an export operation can take many hours and result in 100's GB of data, ACO API customers have stated that they need `_since` support to move to production. 

## Proposed Solution

This proposal adds 4 changes to the BFD API that are needed for downstream partners to implement the `_since` parameter. 

1. The `lastUpdated` metadata field of EOB, Patient, and Coverage FHIR resources contains the time they were written to the master DB by the ETL process. 
2. The search operation of the EOB, Patient, and Coverage resources support a `_lastUpdated` query parameter. When specified, the search filters resources against the passed in the date range. The capabilities statement includes the `_lastUpdated` search parameter. 
3. The BFD tracks the currency of the updates to BFD. To correctly handle clock skew and data propagation problems, it reports this information back to clients.  
4. The BFD server adds optimizations on resource searches with `_lastUpdated` for the case where the result set is empty. These searches should return results in a similar time to the time taken by a metadata query. 

### BFD API Details

All the proposed API changes follow the FHIR standard. Keeping compability with the FHIR specification allows BFD clients to use existing FHIR libraries and tools.  

The first improvement is a new `lastUpdated` field in the metadata object of a resource. This field contains the timestamp that the ETL process wrote resource to the BFD DB. Like all FHIR date fields, this timestamp must include the server's timezone \[[4](#ref4)\]. Resources based on records loaded before this RFC do not have a `lastUpdated` field. 

The second change is a `_lastUpdated` query parameter for resource searches per the FHIR specification \[[5](#ref5)\]. FHIR specifies a large set of comparison operators. 
BFD supports a subset of these operators: `eq`, `lt`, `le`, `gt` and `ge` operators. Two `_lastUpdated` parameters can be specified to form the upper and lower bounds of a time interval. 

The BFD tracks the timestamp of the last write to the BFD database.
The third change to BFD's API is returning the database last update timestamp in the `lastUpdated` field of Bundle resources. See the [Time Corner Cases](#time-corner-cases) section for more discussion on the motivation for this design decision. 

If client asks for resources with a time interval that is beyond the database last update timestamp, the BFD will return all the resources it has that match and are bound inclusively by the database last update timestamp. From the BFD's perspective it is like the client asked for information from the future. 
It is important for result consistency that the returned bundle does not return resources beyond the BFD's last updated timestamp.

The BFD database records loaded before this RFC implementation will have a null `lastUpdated` field. The BFD treats these records as if they have a very early `lastUpdated` value. 
Searches with a `_lastUpdated` parameter without a lower bound match these records; Likewise, searches with a lower bound never match these records. 
This design allows a single query to retrieve both records with and without a `lastUpdate` metadata field. 

### Single Beneficiary Implementors Details

Partners can use the `_lastUpdated` parameter to get a single beneficiary's recently added FHIR resources. 
To do this without missing a resource, the partner remembers (or has it's client remember) the returned Bundle's `lastUpdated` field. 
This date represents the last update timestamp of the service database. All future updates to the database happen after this timestamp.

![Last Updated](https://www.websequencediagrams.com/files/render?link=JJsgr8NxcInDVKyKqwtBiC5bTjdEafpXz4G1vbW2QbwtiveJ8IdHKl3r35lCdKdn)

### Bulk Export Implementors Details

BFD partner's which are implementing bulk export can add `_since` support. Just as it was in the single beneficiary case, keeping track of timestamps is important to avoid missing data. The following sequence diagram shows how this interaction should work. 

![Bulk Export](https://www.websequencediagrams.com/files/render?link=zfMUJyQaf18DNUb6IQoPN2EBeq9tMctYXXupx6T5Co8gB3t9ysmhat0ToalxZ6p2)

For each beneficiary in the export group, the partner searches within a time interval. 
The lower bound of the interval is the `_since` parameter time passed by the bulk-export client. 
The upper bound timestamp is called the `transactionTime`.
To establish the `transactionTime` of the export job, the partner should query the BFD for a valid database's last update time. 
One way to obtain a valid database timestamp is by fetching a Bundle resource for a synthetic user as is done in the example. 
The client uses the `transactionTime` as the `_since` time of the next bulk-export. An example URL where the time period requested is a week is: 

```
https://<hostname>/v1/fhir/ExplanationOfBenefit
  ?patient=<beneficiaryId>
  &_lastUpdated=ge2018-11-22T14:01:01-05:00
  &_lastUpdated=lt2018-18-22T15:00:00-05:00
  &_format=application%2Fjson%2Bfhir
```

The BFD maintains a partial index of the `lastUpdate` field for each resource that was updated in the last 30 days. The index lets the service to be fast and efficient when a query has an empty result set. 
Partners can query the BFD frequently if they are using recent time intervals. In other words, they can adopt a daily or hourly polling pattern without putting a significant load on the BFD service. 

### BFD Implementation Details

For all top level-tables in the BFD, the RFC adds a new column for `lastUpdated`, which reflects the time BFD loaded a RIF file from the CCW. 
Because of the large size of the BFD tables, the BFD's Postgres database does not index the `lastUpdated` column. This design avoids some of the indexing capacity problems that the BFD has experienced in the past. 

Most bulk-export clients intend to call CMS on at least a weekly basis. 
Since only a small set of records change in a given week, the most common result of a search is an empty set. 
The BFD implements a filter that allows the BFD data server to avoid querying the database in this case. 

The BFD pipeline tracks the beneficiaries that it updates in each RIF file load along with the time interval of the write. 
The BFD data server uses this list of past RIF file loads and their associated beneficiaries to build its filters. 
The filter management process works on a background thread on the data server. 
It never interferes with the data serving process. 

![Filters](https://www.lucidchart.com/publicSegments/view/d8d09787-3968-43af-a974-05bd03472fdc/image.png) 

The filters internally use a Bloom filter data structure. Bloom filters are very memory efficient and commonly used in databases like Postgres \[[8](#ref8)\]. In essence, the filter design takes an optimization out of the database and implements it in the data server. 

### Roster Change Corner Case

The resources returned by a group export operation is the current roster of the group at the time of an export call. A group's roster may change between successive export calls. At this time, the importer does not have any data for the added beneficiaries. So, how should a group export call with a `_since` parameter handle new beneficiaries added to the group? The FHIR specification states that export should only include data updated after the passed in `_since` parameter. However, the specification does not contemplate this use-case, nor does it hint on how to implement this use-case correctly. 

Since the BFD service does not track groups, the BFD partners have to work out solutions for this problem. The FHIR community is aware of this issue and is considering an amendment to Bulk Export specification. 

### Time Corner Cases

In production, the BFD service runs on multiple computers, each with a different clock. Time differences in these clocks lead to a class of computing problems known as clock skew that are hard to detect and solve. 

Moreover, the BFD service in production uses replica databases to scale up the capacity of the service. There is a delay between the writes to the master database and the data appearing on the replica. The effect of this replica delay is very similar to or the same as clock skew. In practice, replica delay is a more significant problem than clock skew; In the BFD service, clock skew measures to be a few milliseconds, while replica delay has spiked in the past to over 30 minutes. 

This RFC's design avoids most clock skew and replica delay problems by using ETL's process computer's clock for values of the `lastUpdated` field. This fact and the atomic nature of the BFD writes ensures that a Bundle's `lastUpdated` value, which represents the timestamp of the entire database, is consistent with the timestamp of single resources. A long as a client use time values that come from the service, it avoids clock skew issues. 

One place where consistency issues appear is due to differing replica delay between availability zones. Because of load balancing between zones, successive calls to the BFD may hit different replica databases. If each replica has a different data set, this behavior results in inconsistencies between successive bundles in an export job. Usually, this is very unlikely to happen because replica delays typically are under a second, and the database is only updated once a week. Furthermore, export jobs have an upper bound in their search intervals, the `transactionTime`. Nevertheless, clients should check that the database timestamp returned is after the upper bound timestamp of the call to prevent to ensure consistency in the result set of successive calls.  

### Database Schema Corner Cases

FHIR Resources are projections from the BFD's internal records, based on the CCW's RIF files. As a result, the FHIR Resources may have their `lastUpdated` field change when other fields do not change. 

Records created before this RFC do not have a last updated value. FHIR resources derived from these records do not have a `lastUpdated` field. 

### Alternatives Considered

Instead of optimizing at the BFD data server, an earlier design had the empty result set optimization done at the partner level. An ETL feed served by the BFD would allow the partner to implement the bloom filters now found in BFD data server. Although this design is slightly more efficient, the current design is simpler to run and requires less partner work. 

A simpler approach would be use a database index on the `lastUpdated` field in the database. This approach should be reconsidered if the database technology changes from Postgres on RDS. 

## Future Possibilities

This proposal should scale as BFD and it's partners serve more beneficiaries and clients. It should continue to work as BFD adds more partners and data sources. 

In future releases, BFD may receive claim data faster than the current weekly updates. Care has been taken to make sure
the lastUpdated indexing works correctly during RIF file processing. This means that there should be no need to change
the algorithm when the BFD moves to daily or even hourly batches. 

Much of the design choices in this RFC was done to avoid taxing the database at the center of the BFD.
If the BFD database changes to allow the database to index `lastUpdated`, much of the optimizations done in this RFC can be removed.

In future releases of the BFD, it may support operations for lists of beneficiries instead of single beneficiary. 

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