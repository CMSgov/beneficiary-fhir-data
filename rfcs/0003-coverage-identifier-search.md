# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0003-coverage-identifier-search`
* Start Date: 2019-11-15
* RFC PR: []()
* JIRA Ticket(s):
    * [BLUEBUTTON-1517](https://jira.cms.gov/browse/BLUEBUTTON-1517)

Search by Coverage.identity, initially only supporting the part D coverage identities.

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

To increase the usability of the API
provided by the BFD
by enabaling searching for beneficiaries
by certain attributes.
In this case
their Part D entrollment status.

## Proposed Solution
[Proposed Solution]: #proposed-solution

### Patient centric interface

`/Patient/?ptdcntrct<month_code>=<contract id>`

returns a set of patient resources
that have that `contract_id` for that `month_code`.

`/Patient/?Coverage.identifier=https://bluebutton.cms.gov/resources/variables/ptdcntrct<month_code>|<contract_id>`

Same return value.

### Coverage centric interface

`/Coverage/?identifier=https://bluebutton.cms.gov/resources/variables/ptdcntrct<month_code>|<contract_id>`

returns a set of Coverage resources
for that `contract_id` and `month_code`.
Each resource would contain a
`beneficiary` entry
with a `Patient/<fhir_id>` entry.

### Proposed Solution: Detailed Design
[Proposed Solution: Detailed Design]: #proposed-solution-detailed-design

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

### Proposed Solution: Unresolved Questions
[Proposed Solution: Unresolved Questions]: #proposed-solution-unresolved-questions

### Proposed Solution: Drawbacks
[Proposed Solution: Drawbacks]: #proposed-solution-drawbacks

TODO


### Proposed Solution: Notable Alternatives
[Proposed Solution: Notable Alternatives]: #proposed-solution-notable-alternatives

TODO

## Prior Art
[Prior Art]: #prior-art

TODO

## Future Possibilities
[Future Possibilities]: #future-possibilities

TODO

## Addendums
[Addendums]: #addendums

TODO
