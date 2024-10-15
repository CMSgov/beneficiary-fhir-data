# RFC Proposal

* RFC Proposal ID: `0023-samhsa-v2`
* Start Date: 2024-10-11
* RFC PR: [beneficiary-fhir-data/rfcs#0023](https://github.com/CMSgov/beneficiary-fhir-data/pull/2464)
* JIRA Ticket(s):
    * [BFD-3660](https://jira.cms.gov/browse/BFD-3660)

A proposal to rewrite the SAMHSA filter to be more reliable and easier to maintain.

## Status

* Status: Proposed <!-- (Proposed/Approved/Rejected/Implemented) -->
* Implementation JIRA Ticket(s):

## Table of Contents

- [RFC Proposal](#rfc-proposal)
  - [Status](#status)
  - [Table of Contents](#table-of-contents)
  - [Background](#background)
  - [Implementation Overview](#implementation-overview)
  - [Database Schema](#database-schema)
    - [Optional Addition: Claim Details](#optional-addition-claim-details)

## Background

The SAMHSA filter currently operates on the transformed representation of the FHIR payloads.
It looks at the diagnosis, procedure, and detail components of the payload and checks the list of ICD, CPT, and DRG codes. 
When `excludeSAMHSA=true` is supplied to the EOB, Claim, or ClaimResponse endpoints, any claim that contains at least one code that matches against the SAMHSA list will be excluded.

The current filter logic is tightly coupled to the implementation of the FHIR transformers.
Any change to the system URLs, additional functionality that adds a potentially sensitive code to a new field in the FHIR response, or a bug that causes certain fields to not be mapped in the FHIR payload may cause the filter to behave incorrectly. 

Instead of basing the SAMHSA filter on the FHIR payload, it would be safer to check any database column that could contain sensitive info and explicitly mark that claim as sensitive at ingestion time.

## Implementation Overview

The implementation will utilize [FHIR Security Labels](https://build.fhir.org/security-labels.html) to tag claims as 'sensitive'.
SAMHSA claims will include a [Confidentiality](https://terminology.hl7.org/4.0.0/CodeSystem-v3-Confidentiality.html) tag of `R` for 'restricted' and an [Act Code](https://terminology.hl7.org/6.0.2/CodeSystem-v3-ActCode.html) tag of `42CFRPart2`.
Non-SAMHSA claims will include a Confidentiality tag of `N` (normal).

These tags will be stored in the database using a new set of tables.
The usage of these tags can be extended beyond just SAMHSA data in the future.

## Database Schema

The tag values themselves can be stored as a Postgres enum:

```sql
CREATE TYPE tag_code AS ENUM('R', '42CFRPart2');
```

We include the tags for sensitive data, but not the one for normal claims (`N`) since any claim not flagged as sensitive can assumed to be normal.
This prevents us from storing an extra tag for every single claim in the database.

Each claim table will have a corresponding table that will store the associated tags.

Each SAMHSA claim will have two corresponding rows in the tags table. Ex:

| tag_id | clm_id | code       |
| ------ | ------ | ---------- |
| 1      | 1000   | R          |
| 2      | 1000   | 42CFRPart2 |

```mermaid
erDiagram
    "ccw.carrier_claims" {
        clm_id BIGINT
    }

    "ccw.dme_claims" {
        clm_id BIGINT
    }

    "ccw.hha_claims" {
        clm_id BIGINT
    }

    "ccw.hospice_claims" {
        clm_id BIGINT
    }

    "ccw.inpatient_claims" {
        clm_id BIGINT
    }

    "ccw.outpatient_claims" {
        clm_id BIGINT
    }

    "ccw.carrier_tags" {
        tag_id BIGINT
        clm_id BIGINT
        code Enum "Enum(R, 42CFRPart2)"
    }

     "ccw.dme_tags" {
        tag_id BIGINT
        clm_id BIGINT
        code Enum "Enum(R, 42CFRPart2)"
    }

     "ccw.hha_tags" {
        tag_id BIGINT
        clm_id BIGINT
        code Enum "Enum(R, 42CFRPart2)"
    }

     "ccw.hospice_tags" {
        tag_id BIGINT
        clm_id BIGINT
        code Enum "Enum(R, 42CFRPart2)"
    }
    
    "ccw.inpatient_tags" {
        tag_id BIGINT
        clm_id BIGINT
        code Enum "Enum(R, 42CFRPart2)"
    }

    "ccw.outpatient_tags" {
        tag_id BIGINT
        clm_id BIGINT
        code Enum "Enum(R, 42CFRPart2)"
    }
    
    "rda.fiss_claims" {
        claim_id VARCHAR
    }

    "rda.mcs_claims" {
        idr_clm_hd_icn VARCHAR
    }
    
    "rda.fiss_tags" {
        tag_id BIGINT
        claim_id VARCHAR
        code Enum "{R, 42CFRPart2}"
    }

    "rda.mcs_tags" {
        tag_id BIGINT
        idr_clm_hd_icn VARCHAR
        code Enum "{R, 42CFRPart2}"
    }

    "ccw.carrier_claims" ||--o{ "ccw.carrier_tags": ""
    "ccw.dme_claims" ||--o{ "ccw.dme_tags": ""
    "ccw.hha_claims" ||--o{ "ccw.hha_tags": ""
    "ccw.hospice_claims" ||--o{ "ccw.hospice_tags": ""
    "ccw.inpatient_claims" ||--o{ "ccw.inpatient_tags": ""
    "ccw.outpatient_claims" ||--o{ "ccw.outpatient_tags": ""
    "rda.fiss_claims" ||--o{ "rda.fiss_tags": ""
    "rda.mcs_claims" ||--o{ "rda.mcs_tags": ""
```

### Optional Addition: Claim Details

We have no requirement to return which specific field in the claim contains SAMHSA data.
However, it may be useful to track this information for observability purposes.
To do so, we can include a `JSONB` column on the tags table to store any additional information that may be useful for engineers to look at.

JSONB is useful in this case because the information is not used in the implementation itself, so we can favor flexibility over strictness.

```mermaid
erDiagram
     "tags" {
        tag_id BIGINT
        clm_id BIGINT
        code Enum "Enum(R, 42CFRPart2)"
        details JSONB "[{table: string, column: string, clm_line_num: number, type: string (CPT, DRG, ICD)}]"
    }
```

An example `details` field may look like this:

```json
[
    {
        "table": "ccw.inpatient_claims",
        "column": "clm_drg_cd",
        "type": "DRG"
    },
    {
        "table": "ccw.inpatient_claim_lines",
        "column": "hcpcs_cd",
        "clm_line_num": 2,
        "type": "CPT"
    }
]
```

