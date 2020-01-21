# RFC Proposal
[RFC Proposal]: #rfc-proposal

* RFC Proposal ID: `0005-mbi-search` 
* Start Date: 2020-1-15
* RFC PR: [](https://github.com/rust-lang/rfcs/pull/0000)
* JIRA Ticket(s):
    * [BLUEBUTTON-1516](https://jira.cms.gov/browse/BLUEBUTTON-1516)

The addition of a search by Medicare Beneficiary Identifier (MBI) facility to the BFD's Patient search end-point.   

## Table of Contents
[Table of Contents]: #table-of-contents

* [RFC Proposal](#rfc-proposal)
* [Table of Contents](#table-of-contents)
* [Motivation](#motivation)
* [Proposed Solution](#proposed-solution)
    * [Patient Search](#patient-search)
    * [Include Identifiers](#include-identifiers)
    * [Hash Algorithm](#hash-algorithm)
    * [Synthetic Beneficiaries](#synthetic-beneficiaries)
* [Prior Art](#prior-art)

## Motivation
[Motivation]: #motivation

The Medicare Beneficiary Identifier (MBI) is replacing the Health Insurance Claim Identifier (HICN). See [New Medicare Card Project](https://www.cms.gov/Medicare/New-Medicare-Card/9-13-18-NMC-ODF-Slides.pptx) for some background on this change. This change to the BFD adds to BFD the same capabilities for MBIs as currently exist for HICNs. 

## Proposed Solution
[Proposed Solution]: #proposed-solution

The proposed solution consists of a new identifier for the Patient search FHIR end-point, a new include-identifiers projection, and the same hashing algorithm that BFD uses for HICN.

### Patient Search
The `Patient` end-point supports searching for resources with a matching MBI. The CMS classifies the MBI as personally identifiable information (PII). The request must hash the MBI to prevent sensitive PII from entering access logs. A new MBI based identifier system is recognized: `https://bluebutton.cms.gov/resources/identifier/mbi-hash`. 

An example request for a sythentic beneficiary `1S00A00AA00` is:
```
curl 'https://<bfd address>:443/v1/fhir/Patient?identifier=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fidentifier%2Fmbi-hash%7C37c37d08d239f7f1da60e949674c8e4b5bb2106077cb0671d3dfcbf510ec3248&_format=application%2Fjson%2Bfhir'
```

### Include Identifiers
The returned patient resource includes the HICN and MBI identifiers if the request has the `IncludeIdentifiers` HTTP header set. BFD supports new values for the `IdentifierIncludes` header. 

| value    | identifiers added | Comments                   |
|----------|-------------------|----------------------------|
| <empty>  |                   | Default value              |
| true     | HICN and MBI      | MBI is added to the result |
| hicn     | HICN              | New value supported        |
| mbi      | MBI               | New value supported        |
| hicn,mbi | HICN and MBI      | New value supported        |

### Hash Algorithm
Requests use a hashed version of the MBI instead of the actual MBI of the resource being requested. Hashing prevents the actual MBI from appearing in logs. For convenience, MBI hashing uses the existing HICN hashing algorithm, pepper and iterations. 

### Synthetic Beneficiaries
The BFD has a set of 30,000 synthetic beneficiary records for use for development and testing. These records now contain random MBIs. To distinguish these identifiers from real MBIs, they have `S` as the second letter. `1S00A00AA00` is an example of a synthetic MBI. 

## Prior Art
The MBI searches follow the patterns set by the existing HICN search facilities. 




