# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0007-filtering-fields`
* Start Date: 2020-08-21
* RFC PR: [rfcs#0007](https://github.com/CMSgov/beneficiary-fhir-data/pull/345)
* JIRA Ticket(s):
    * [https://jira.cms.gov/browse/AB2D-1863](https://jira.cms.gov/browse/AB2D-1863)

This proposal strengthens the guarantee made by the use of the _elements parameter for the **fhir/ExplanationOfBenefit** endpoint.

## Table of Contents
[Table of Contents]: #table-of-contents

* [RFC Proposal](#rfc-proposal)
* [Table of Contents](#table-of-contents)
* [Motivation](#motivation)
* [Proposal](#proposal)
    * [Proposed Solution: Unresolved Questions](#proposed-solution-unresolved-questions)
    * [Proposed Solution: Drawbacks](#proposed-solution-drawbacks)
    * [Proposed Solution: Notable Alternatives](#proposed-solution-notable-alternatives)
* [Future Possibilities](#future-possibilities)

## Motivation
[Motivation]: #motivation

A single BFD EOB retrieval call (UHC) with 2 million beneficiaries took 24 hours to retrieve three months of data.
Larger PDPs can have in excess of 6 million beneficiaries.  To have a request take over 3 days is not acceptable.

AB2D receives more data than needed from BFD when we make API calls to gather EOBs (Explanation of Benefits).  AB2D
currently filters out irrelevant fields.  This might not be terrible for dozens of API calls, but for 
millions of requests it can impact our performance significantly.  Since we only need a projection of the data
(10 out of 69 top-level getters; 6 out of 42 fields in the ItemComponent), it makes
sense for the server to send us only the data consumed.  This reduces bandwidth requirements for receiving the data.

For example, GraphQL solves this problem by allowing clients to specify field selection (e.g. a database projection) at
invocation time.

While not as powerful as the GraphQl offering where one can field select as deep in the hierarchy as desired, FHIR does offer a top level projection through the use of the _elements parameter supported both in R3 and R4.

## Proposal
[Proposal]: #proposal

FHIR section 3.1.1.5.9 specifies the behavior of _elements as follows:
```Servers are not obliged to return just the requested elements. Servers SHOULD always return mandatory elements whether they are requested or not. Servers SHOULD mark the resources with the tag SUBSETTED to ensure that the incomplete resource is not actually used to overwrite a complete resource.```

This proposal changes `not obliged` to `obliged` in the FHIR specification for the **fhir/ExplanationOfBenefit** endpoint, thus providing a guarantee of the performance saving behavior.  The mandatory elements and tag marking behavior are unchanged.  This proposal does not address reducing the number of fields returned in ItemComponent as the FHIR specification makes no provision for selecting fields of contained objects.

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

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

GraphQL is a notable alternative where the invocation explicitly specifies the requested fields.  Since GraphQL
supports introspection, the client can actively determine the available fields.  A hybrid approach using
*registered queries* requires queries to be specified in advance so that they can be locked down by a production server.

The downside is that this would be a significant amount of work for BFD to adopt a different API style while continuing
to support the REST calls.

## Future Possibilities
[Future Possibilities]: #future-possibilities

There could be dynamic projections on the database side as well. Right now there are 194 columns
in the beneficiary table, and we certainly do not need all of them, so retrieving only the needed columns
would reduce the load on the database.

Other groups would likely want to utilize field selection as well for other endpoints, but that is out of scope for this RFC.