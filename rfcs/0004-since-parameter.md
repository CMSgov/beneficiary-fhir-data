# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0004-since-parameter-support` 
* Start Date: October 1, 2019
* RFC PR: 
* JIRA Ticket(s):


This RFC proposal adds features to BFD's API to allow BFD's partners to implement the Bulk Export `_since` parameter [[1]](#ref1). Specifically, it provides for a `lastUpdated` query parameter to FHIR resource search operation and a feed of metadata about BFD's data loads. The proposal discusses these new features as well as the logic that BFD's partners need to implement the `_since` parameter correctly. 

## Table of Contents
[Table of Contents]: #table-of-contents

* [RFC Proposal](#rfc-proposal)
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

Consumers of CMS's beneficiary data whether they call BlueBotton 2.0, BCDA or DPC's APIs want the most up-to-date information. Ideally, these apps and services would like to call a CMS API right after BFD updates it's beneficiary data. However, they only want new data from CMS, not the information they already have[[2]](#ref2). 

FHIR has provisions for an "update me about new information" pattern in FHIR APIs. For the bulk export operation, exports with a `_since` parameter specified should only return resources that have changed after the date and time specified in the `_since` parameter [[3]](#ref3). For synchronous resource searches, there exists a `lastUpdated` parameter that has similar semantics [[4]](#ref4). 

Today, BFD only supports returning all EOB resources associated with a single beneficiary. EOB calls return more than 5 years of beneficiary data, where only the last weeks of data is needed. This behavior is highly inefficient for the bulk export calls that happen weekly. On average, each call is returning 260 times as much information as is needed. 

Early feedback from both BCDA and DPC customers have nearly unanimously pointed out the need for _since parameter support. For BCDA, where an export operation can take many hours and result in 100's GB of data, BCDA customers have stated that they need '_since' support to move to production. Also, BB 2.0 app developers would like a similar feature. 

## Proposed Solution
[Proposed Solution]: #proposed-solution

This proposal adds 3 changes to the BFD API that are needed for downstream partners to implement the `_since` parameter. 

1. The `lastUpdated` metadata field of EOB, Patient, and Coverage FHIR resources contains the time they were written to the master DB by the ETL process. 
2. The search operation of the EOB, Patient, and Coverage resources support `_lastUpdated` query parameter. When specified, the search operation filters resources against the passed in timestamp. 
3. The API adds a feed containing the start and finish times of the ETL job into BFD and a list of beneficiaries updated in the ETL. This feed enables BFD partners to correctly and efficiently implement the `_since` parameter. 

Using the new information provided by the BFD, a partner can implement `_since` exports efficiently. A key to correctness is handling while an ETL is taking place (see []()). A solution to efficiency is for partners to avoid calling the BFD if a particular beneficiary doesn't have an updated resource (see []()).

![Bulk ](https://www.websequencediagrams.com/files/render?link=zfMUJyQaf18DNUb6IQoPN2EBeq9tMctYXXupx6T5Co8gB3t9ysmhat0ToalxZ6p2)

### BFD API Details
[API Details]: #api-details


### BFD Feed Details
[Feed Details]: #feed-details

### Since Implementors Details
[Implementors Details]: #implementors-details


### ETL Corner Case
[ETL Corner Case]: #ETL-corner-case

### Roster Change Corner Case
[R]

### Alternative Considered
Instead of an ETL feed served by S3, BFD could convey the ETL information in other ways. For example, BFD could add an API for this information. Given the low volume of events and the low number of subscribers, the proposal chooses the S3 approach because it requires less code, while still meeting the needs of the problem

## Future Possibilities
[Future Possibilities]: #future-possibilities

PUB-SUB to customers

More frequent updates 

## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* (none at this time)

Please note that some of these addendums may be encrypted. If you are unable to decrypt the files, you are not authorized to vote on this proposal.
