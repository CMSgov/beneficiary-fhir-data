# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0003-coverage-identifier-search`
* Start Date: 2019-11-15
* RFC PR: [#156](https://github.com/CMSgov/beneficiary-fhir-data/pull/156)
* JIRA Ticket(s):
    * [BLUEBUTTON-1517](https://jira.cms.gov/browse/BLUEBUTTON-1517)

Search by Coverage.extension, initially only supporting the Part D coverage identities.

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
* [Future Work](#future-work)
* [Addendums](#addendums)

## Motivation
[Motivation]: #motivation

To increase the usability of the API
provided by the BFD
by enabling searching for beneficiaries
by certain attributes.
In this case
their Part D enrollment status
for a given month of the current year.

## Proposed Solution
[Proposed Solution]: #proposed-solution

### Patient centric interface

Following the fhir pattern of [reverse chaining](https://www.hl7.org/fhir/search.html#has) the interface will request a list of patients that `have` `Coverage` identified by the `prdcntrct<month code>` extension.

Calls to

`v1/fhir/Patient/?_has:Coverage.extension=https://bluebutton.cms.gov/resources/variables/ptdcntrct<month code>|<contract id>`

will return a [bundle](https://www.hl7.org/fhir/bundle.html) of [patient resources](https://www.hl7.org/fhir/patient.html)
that have the specified `contract_id` for the specified `month_code`.

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

#### Implementation Goals

- a pattern for patient centric data filters
- low risk of change to current external contract
	- do partners currently expect a `Patient` call without a `patient_id` parameter to error?

#### valid parameters

- [ptdcntrct01](https://bluebutton.cms.gov/resources/variables/ptdcntrct01/)
- [ptdcntrct02](https://bluebutton.cms.gov/resources/variables/ptdcntrct02/)
- [ptdcntrct03](https://bluebutton.cms.gov/resources/variables/ptdcntrct03/)
- [ptdcntrct04](https://bluebutton.cms.gov/resources/variables/ptdcntrct04/)
- [ptdcntrct05](https://bluebutton.cms.gov/resources/variables/ptdcntrct05/)
- [ptdcntrct06](https://bluebutton.cms.gov/resources/variables/ptdcntrct06/)
- [ptdcntrct07](https://bluebutton.cms.gov/resources/variables/ptdcntrct07/)
- [ptdcntrct08](https://bluebutton.cms.gov/resources/variables/ptdcntrct08/)
- [ptdcntrct09](https://bluebutton.cms.gov/resources/variables/ptdcntrct09/)
- [ptdcntrct10](https://bluebutton.cms.gov/resources/variables/ptdcntrct10/)
- [ptdcntrct11](https://bluebutton.cms.gov/resources/variables/ptdcntrct11/)
- [ptdcntrct12](https://bluebutton.cms.gov/resources/variables/ptdcntrct12/)

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

While technically all Patient search responses
_could_ contain more than one entry
per the specification,
there is a convention within the BFD
that they only ever contain one entry.
This proposal
breaks that convention
by necessity of the requested behavior.

### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

#### [_tag](https://www.hl7.org/fhir/STU3/search.html#tag)
Fetch patient resources
via a precomputed tag
based on their coverage
contract enrollment.

`<base_url>/Patient/?_tag=partdcontract-01-1234`

#### [_list](https://www.hl7.org/fhir/STU3/search.html#list)

Fetch
lists of patients
precomputed
based on their coverage
contract enrollment.

`<base_url>/Patient?_list=42`

#### Notes:

[migration for contract numbers](https://github.com/CMSgov/beneficiary-fhir-data/blob/master/apps/bfd-model/bfd-model-rif/src/main/resources/db/migration/V10__Add_beneficiary_beneficiaryHistory_colmns.sql#L112-L123)

[release exposing part d contract numbers](https://github.com/CMSgov/beneficiary-fhir-data/blob/a995592c40d77e3974ade09eb5017492e0865608/apps/bfd-server/dev/api-changelog.md#bluebutton-926-exposing-additional-beneficiary-coverage-fields)

> Monthly Part D Contract Number: The Part D contract number for the  beneficiary’s Part D plan for a given month. CMS assigns an identifier  to each contract that a Part D plan has with CMS. 
>
> - Documentation: <https://bluebutton.cms.gov/resources/variables/ptdcntrct01>
> - Found at: `Coverage[?(grouping.subPlan =~ /^Part  D$/)].extension[?url =~  /https:\/\/bluebutton.cms.gov\/resources\/variables\/ptdcntrct\d\d/].valueCoding.code`

[bb docs on part D contract id](https://bluebutton.cms.gov/resources/variables/ptdcntrct01/)

https://bluebutton.cms.gov/resources/variables/ptdcntrct01 is a valid coding

[example coverage response with codes](https://github.com/CMSgov/beneficiary-fhir-data/blob/b541b973bf21c925f80df9c154263eb0b6ae4483/apps/bfd-server/bfd-server-war/src/test/resources/endpoint-responses/coverageSearchByPatientId.json#L588-L660)

## Future Work
[Future Work]: #future-work

### Improve synthetic data

Current synthetic data is invalid for this usecase.
Synthetic contract numbers of 5 characters will have to be generated and assigned to beneficiaries.
The assignments should be spred out so that a range of different numbers of results are returned.

### Store coverage in a more robust way

In the current system Coverage information
is stored accross columns of the beneficiary table.
This results in a few weaknesses.

Coverage information is recorded by month,
one column for each month.
This loses any year or day information
for that coverage relationship.

The Coverage relationship is stored as a string within those columns.
This means that missmatches,
typos,
etc.,
are possible.

The coverage information living
within the beneficiary table
also increases the table size.
Spreading the data out,
and potentially normalizing it,
would decrease the table size
and potentially response times
while simplifying the system.

### Coverage centric interface

`/Coverage/?extension=https://bluebutton.cms.gov/resources/variables/ptdcntrct<month_code>|<contract_id>`

returns a set of Coverage resources
for that `contract_id` and `month_code`.
Each resource would contain a
`beneficiary` entry
with a `Patient/<fhir_id>` entry.
