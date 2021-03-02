# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0007-filtering-fields`
* Start Date: 2020-08-21
* RFC PR: [rfcs#0007](https://github.com/CMSgov/beneficiary-fhir-data/pull/345)
* JIRA Ticket(s):
    * [https://jira.cms.gov/browse/AB2D-1863](https://jira.cms.gov/browse/AB2D-1863)

This proposal adds field selection (i.e. projection) to API calls that return EOBs to the client in order to 
reduce the amount of data serialized and transmitted.  This approach is designed to be more comprehensive and general,
allowing for simplification of implementation.

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
* [Future Possibilities](#future-possibilities)

## Motivation
[Motivation]: #motivation

A single BFD EOB retrieval call (UHC) with 2 million beneficiaries took 24 hours to retrieve three months of data.
Larger PDPs can have in excess of 6 million beneficiaries.  To have a request take over 3 days is not acceptable and in
fact the downloaded EOB files will have already been removed from S3.

AB2D receives more data than needed from BFD when we make API calls to gather EOBs (Explanation of Benefits).  AB2D
currently filters out irrelevant fields.  This might not be terrible for dozens of API calls, but for 
millions of requests it can impact our performance significantly.  Since we only need a projection of the data
(10 out of 69 top-level getters; 6 out of 42 fields in the ItemComponent), it makes
sense for the server to send us only the data consumed.  This reduces bandwidth requirements for receiving the data.

For example, GraphQL solves this problem by allowing clients to specify field selection (e.g. a database projection) at
invocation time.

## Proposed Solution
[Proposed Solution]: #proposed-solution

The solution adds code in the BFD server to configure which fields AB2D requires.  Non-required fields are nulled out
before being serialized in the result.  The specific set of required fields should remain stable
and not require modifications.

The filtering will need to happen towards the end of processing. This is to avoid
spaghetti code since transformations from the entity object to the FHIR object happen in several places throughout
the codebase. 


### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

The FIELD_SELECTION_PROFILE header enables this feature.  A value of **ALL** is current behavior, will be the default
(if header is missing) and indicates to return all EOB fields.  A value of **AB2D_STU3_VERSION1** indicates return only
the fields specified in this proposal for a **STU3** request.  A value of **AB2D_R4_VERSION1** indicates return only
the fields specified in this proposal for a **R4** request.  A HTTP Status of 400 is returned if there is a mismatch
between the profile given and the invoked endpoint (R4 vs. STU3).

Returned fields from ItemDef are specifically enumerated in the tables below.  Only specified fields have values.  All
other fields are either null or an appropriate default (such as 0).  If a field represents an object, all fields of
that object unless there is a specific definition in the profile sub-selecting desired fields.

#### AB2D_STU3_VERSION1 Profile

| ItemDef Field |
| ------------- |
| sequence |
| careTeamLinkId |
| service |
| serviced |
| location |
| quantity |

| EOB Field |
| --------- |
| id |
| identifier |
| billablePeriod |
| patient |
| provider |
| organization |
| facility |
| type |
| resourceType |
| diagnosis |
| procedure |
| item |
| careTeam |
| status |
| precedence |
| meta |

#### AB2D_R4_VERSION1 Profile

For a given ItemDef, the following fields are specifically returned.  If new fields are added in the future,
they are ignored.  If a field represents an object, all fields of that object are return unless that object type 

| ItemDef Field |
| ------------- |
| sequence |
| careTeamSequence |
| productOrService |
| serviced |
| location |
| quantity |
| extension |

| EOB Field |
| --------- |
| identifier |
| type |
| meta |
| text |
| language |
| id |
| implicitRules |
| patient |
| provider |
| facility |
| careTeam |
| diagnosis |
| procedure |
| billablePeriod |
| item |
| status |

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

How much will this save us?

As part of a larger effort to reduce job times from days to minutes, AB2D is constructing a performance test harness
which will at its basic element, perform a single EOB retrieval.  Anecdotally, BFD EOB retrievals have been calibrated
in the 500 msec range.  This harness will allow a more deliberate measurement of the actual response times, independent
of the current AB2D architecture.  Once the basic harness is ready, before and after retrieval times can be obtained to
provide a firm answer to the question.

Additional goals for the harness are to assess how much can concurrency increase.  Currently, AB2D limits EOB
retrievals to 32 concurrent requests.  The plans are to run cooperative experiments to establish a maximum concurrent
limit.

The harness will make it much more convenient to characterize current performance and to confirm or disprove any
expected benefits of changes.

Proposed profiling is invoking BFD with at least 10,000 EOB Requests by Patient ID (ExplanationOfBenefit?patient=) and to measure
the aggregate time of completion of all calls.  In other words, sum up the time taken by each call over all calls.

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

This solution incurs a nominal amount of extra processing to traverse the EOB object and null out fields that do not
need to be returned.

This solution adds AB2D specific code within the BFD source base.  Any new required fields results in changes to BFD
code.  The expectation is that this is rare and involves minimal effort.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

GraphQL is a notable alternative where the invocation explicitly specifies the requested fields.  Since GraphQL
supports introspection, the client can actively determine the available fields.  A hybrid approach using
*registered queries* requires queries to be specified in advance so that they can be locked down by a production server.

The downside is that this would be a significant amount of work for BFD to adopt a different API style while continuing
to support the REST calls.

An alternative for using a library like PropertyUtils would be JXPath, but initial benchmarks showed that PropertyUtils
was consistently faster.

## Future Possibilities
[Future Possibilities]: #future-possibilities

There could be dynamic projections on the database side as well. Right now there are 194 columns
in the beneficiary table, and we certainly do not need all of them, so retrieving only the needed columns
would reduce the load on the database.

Other groups would likely want to utilize filtering as well, but that is out of scope for this RFC.