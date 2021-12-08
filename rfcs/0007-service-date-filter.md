# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0007-service-date-filter` 
* Start Date: 2020-10-15
* RFC PR: [#376](https://github.com/CMSgov/beneficiary-fhir-data/pull/376)
* JIRA Ticket(s):
    * [BCDA-3871](https://jira.cms.gov/browse/BCDA-3871)

This proposal suggests adding an additional date range query on service completion date to EOB requests. This will allow users to guarantee that claims data falls within an expected date range.

## Status
[Status]: #status

* Implementation Status: Done
* JIRA Ticket(s):
    * [BCDA-3872](https://jira.cms.gov/browse/BCDA-3872)
    
## Table of Contents
[Table of Contents]: #table-of-contents

- [RFC Proposal](#rfc-proposal)
- [Status](#status)
  - [Table of Contents](#table-of-contents)
  - [Motivation](#motivation)
  - [Proposed Solution](#proposed-solution)
    - [Proposed Solution: Detailed Design](#proposed-solution-detailed-design)
    - [Proposed Solution: Unresolved Questions](#proposed-solution-unresolved-questions)
    - [Proposed Solution: Drawbacks](#proposed-solution-drawbacks)
    - [Proposed Solution: Notable Alternatives](#proposed-solution-notable-alternatives)
  - [Prior Art](#prior-art)
  - [Future Possibilities](#future-possibilities)
  - [Addendums](#addendums)
  - [Implementation](#implementation)

## Motivation
[Motivation]: #motivation

BCDA needs to ensure that they are not returning beneficiary data to an ACO to which that beneficiary is not attributed. There are a variety of use-cases where this could occur if not handled properly:
* End of year reassignment/attribution changes
* Runouts

## Proposed Solution
[Proposed Solution]: #proposed-solution

The solution will allow a client to specify a new query parameter (`service-date`) when searching EOB resources. When specified, the search filters resources against the passed in date range. 

The table below captures the claim type field used in the service-date filter.

| Claim Type                            | Date Field           | EOB Field          | CCW Field   |
|---------------------------------------|----------------------|--------------------|-------------|
| Carrier Claim                         | dateThrough          | billablePeriod#end | CLM_THRU_DT |
| DME (Durable Medical Equipment) Claim | dateThrough          | billablePeriod#end | CLM_THRU_DT |
| HHA (Home Health Agency) Claim        | dateThrough          | billablePeriod#end | CLM_THRU_DT |
| Hospice Claim                         | dateThrough          | billablePeriod#end | CLM_THRU_DT |
| Inpatient Claim                       | dateThrough          | billablePeriod#end | CLM_THRU_DT |
| Outpatient Claim                      | dateThrough          | billablePeriod#end | CLM_THRU_DT |
| PDE (Part D Event) Claim              | prescriptionFillDate | item#servicedDate  | SRVC_DT     |
| SNF (Skilled Nursing Facility) Claim  | dateThrough          | billablePeriod#end | CLM_THRU_DT |


### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

Currently, EOB lookup by patients accepts an optional date range filter (`_lastUpdated`). The lookup would be expanded to support an additional date range filter (`service-date`).

The `service-date` filter will be implemented at the application layer, not the data layer. No schema changes are needed. After querying the database by patientId and (optionally) lastUpdated, the `service-date` filter checks the claim entity's date field against the provided date range. All claim entities that pass the filter are returned to the caller.

If the caller does not supply `service-date`, no post query filtering occurs.

Similar to the `_lastUpdated` parameter, the `service-date` is supplied as a date range. Users can supply a lower bound, upper bound, or lower/upper bound. The supported parameters include: `ge`, `gt`, `le`, `lt`.

Sample Requests:
* https://\<hostname\>/v1/fhir/ExplanationOfBenefit?patient=\<beneficiaryId\>&service-date=ge2020-01-01
  * Returns claims data that occurred on or after 2020-01-01
* https://\<hostname\>/v1/fhir/ExplanationOfBenefit?patient=\<beneficiaryId\>&service-date=lt2020-01-01
  * Returns claims data that occurred before 2020-01-01
* https://\<hostname\>/v1/fhir/ExplanationOfBenefit?patient=\<beneficiaryId\>&service-date=ge2020-01-01&service-date=le2020-01-31
  * Returns claims data that occurred between 2020-01-01 and 2020-01-31 (inclusive)

If a claim entity's date field is unset/null and the caller supplies a `service-date`, then the claim **will not** be returned to the caller. Since we cannot guarantee that the claim falls within the supplied filter, we cannot return it.

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

None at this time

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

1. Since the filtering is on the application layer, instead of the database layer, we may roundtrip claim entities that will be filtered out.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

1. To ensure that we are not returning beneficiary data to an ACO to which that beneficiary is not attributed, we considered using the `_lastUpdated` query parameter, capped at the end of the current plan year. While this approach would achieve the guarantee of restricting claims data, it can lead to false negatives. Claims data that were completed before the end of the year but written to CCW in the new year would not be returned.
2. The `service-date` like filter was previously implemented, but removed due to lack of adoption. While BCDA has identified a use case for this functionality, it may be BCDA-specific and thus should be implemented on the BCDA side. Since BFD is the gatekeeper of data, server side filtering is appropriate.

## Prior Art
[Prior Art]: #prior-art

Implementation of the `service-date` filter will be based on the approach taken for `_lastUpdated` filter. Since the claim types have the necessary data, no new fields need to be added to achieve the functionality.

The `service-date` query parameter is based off of the `service-date` [search parameter](https://build.fhir.org/ig/HL7/carin-bb/SearchParameter-explanationofbenefit-service-date.html) listed in the Carin BB specification.

## Future Possibilities
[Future Possibilities]: #future-possibilities

1. Terminated ACOs - For terminated ACOs, we need to allow them to retrieve claims data up until their termination date. With the `service-date`, we can guarantee that they do not receive any data that completed after their termination date.

## Addendums
[Addendums]: #addendums

The following addendums are required reading before voting on this proposal:

* (none at this time)