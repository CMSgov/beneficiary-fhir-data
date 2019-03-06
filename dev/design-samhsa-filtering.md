# Data Server Design: SAMHSA Filtering

## Introduction

"SAMHSA" technically stands for the "Substance Abuse and Mental Health Services Administration" office in HHS. Colloquially, phrases like "SAMHSA-related events" are used to refer to healthcare encounters in which a patient is receiving treatment for substance abuse or mental health issues.

Some CMS systems which use the Blue Button API's backend Data Server need to ensure that they exclude SAMHSA-related claims. The [Beneficiary Claims Data API](https://sandbox.bcda.cms.gov/) for Accountable Care Organizations (ACOs) is one such system. In fact, the regulations governing CMS data sharing with ACOs ([U.S. Code of Federal Regulations: Title 42, Chapter IV, Part 425, Subpart H - Data Sharing With ACOs](https://www.govregs.com/regulations/42/425.708)) mandate SAMHSA filtering:

> In accordance with 42 U.S.C. 290dd-2 and the implementing regulations at 42 CFR part 2, CMS does not share beneficiary identifiable claims data relating to the diagnosis and treatment of alcohol and substance abuse without the explicit written consent of the beneficiary.

## Filtering Approach

In order to determine which claims are SAMHSA-related, CMS has previously identified various diagnosis and procedure codes as being indicative of SAMHSA-related treatment. Any Part A or Part B claims referencing those codes are thus suppressed, which accomplishes the required SAMHSA filtering. This project takes the same approach:

1. The `/bluebutton-server-app/src/main/resources/samhsa-related-codes/codes-*` CSV files store the designated SAMHSA-related codes.
2. The `gov.hhs.cms.bluebutton.server.app.stu3.providers.SamhsaMatcher` compares individual claims against those codes to determine which claims are SAMHSA-related.
3. The `gov.hhs.cms.bluebutton.server.app.stu3.providers.ExplanationOfBenefitResourceProvider` class uses `SamhsaMatcher` when the `excludeSAMHSA=true` URL query parameter is passed to the `/ExplanationOfBenefit?...` search endpoint/operation.

Wherever possible, default to a fail-safe mode of processing that filters out a claim when uncertain whether or not it is really SAMHSA-related. Opportunities for this will be limited due to the overall blacklist-based approach, but it's nonetheless a good goal.

## SAMHSA-Related Code Fields

The following table details all of the fields in our system which can contain SAMHSA-related codes.

|Claim Type(s)|Codeset|RIF Column(s)|RIF Column Format|Notes|FHIR EOB Field(s)|FHIR EOB Field Format|
|---|---|---|---|---|---|---|
|Carrier, DME|DRG|_(none)_|_(none)_|_(none)_|_(none)_|_(none)_|
|Carrier, DME|CPT|`HCPCS_CD`|`\w{4-5}`|CPT codes are a subset ("Level I") of HCPCSs.|`ExplanationOfBenefit.item.service`|`"service": { "coding": [{ "system": "https://bluebutton.cms.gov/resources/codesystem/hcpcs", "version": "7", "code": "12345" }] }`|
|Carrier, DME|CPT|`HCPCS_1ST_MDFR_CD`|`\w{2}`|Modifier codes provide additional information on the procedure performed.|_N/A_|_N/A_|
|Carrier, DME|CPT|`HCPCS_2ND_MDFR_CD`|`\w{2}`|Modifier codes provide additional information on the procedure performed.|_N/A_|_N/A_|
|Carrier, DME|CPT|`CARR_CLM_HCPCS_YR_CD`|`\w`|Identifies the HCPCS year/version in use.|_N/A_|_N/A_|
|Carrier, DME|ICD Diagnosis|`PRNCPAL_DGNS_CD`|`\w{1-7}`|_N/A_|`ExplanationOfBenefit.diagnosis`|`"diagnosis": [{ "sequence": 1, "diagnosisCodeableConcept": { "coding": [{ "system": "http://hl7.org/fhir/sid/icd-10", "code": "A1234", "display": "SHORT DESCRIPTION" }] }, "type": [{ "coding": [{ "system": "https://bluebutton.cms.gov/resources/codesystem/diagnosis-type", "code": "principal", "display": "..." }] }] }]`|
|Carrier, DME|ICD Diagnosis|`PRNCPAL_DGNS_VRSN_CD`|`\w`|Identifies the ICD version (9 or 10).|_N/A_|_N/A_|
|Carrier, DME|ICD Diagnosis|`ICD_DGNS_CD[1-12]`|_(same as `PRNCPAL_DGNS_CD`)_|_(same as `PRNCPAL_DGNS_CD`)_|_(same as `PRNCPAL_DGNS_CD`)_|_(same as `PRNCPAL_DGNS_CD` but without `type`)_|
|Carrier, DME|ICD Diagnosis|`ICD_DGNS_VRSN_CD[1-12]`|_(same as `PRNCPAL_DGNS_VRSN_CD`)_|_(same as `PRNCPAL_DGNS_VRSN_CD`)_|_(same as `PRNCPAL_DGNS_VRSN_CD`)_|_(same as `PRNCPAL_DGNS_VRSN_CD`)_|
|Carrier, DME|ICD Diagnosis|`LINE_ICD_DGNS_CD`|_(same as `PRNCPAL_DGNS_CD`)_|_(same as `PRNCPAL_DGNS_CD`)_|_(same as `PRNCPAL_DGNS_CD` but referenced via `EOB.item.diagnosisLinkId`)_|_(same as `PRNCPAL_DGNS_CD` but without `type`)_|
|Carrier, DME|ICD Diagnosis|`LINE_ICD_DGNS_VRSN_CD`|_(same as `PRNCPAL_DGNS_VRSN_CD`)_|_(same as `PRNCPAL_DGNS_VRSN_CD`)_|_(same as `PRNCPAL_DGNS_VRSN_CD`)_|_(same as `PRNCPAL_DGNS_VRSN_CD`)_|
|Carrier, DME|ICD Procedure|_(none)_|_(none)_|_(none)_|_(none)_|_(none)_|
|HHA|DRG|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|HHA|CPT|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|HHA|ICD Diagnosis|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|HHA|ICD Procedure|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Hospice|DRG|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Hospice|CPT|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Hospice|ICD Diagnosis|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Hospice|ICD Procedure|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Inpatient|DRG|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Inpatient|CPT|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Inpatient|ICD Diagnosis|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Inpatient|ICD Procedure|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Outpatient|DRG|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Outpatient|CPT|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Outpatient|ICD Diagnosis|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Outpatient|ICD Procedure|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|SNF|DRG|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|SNF|CPT|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|SNF|ICD Diagnosis|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|SNF|ICD Procedure|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Part D Events|DRG|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Part D Events|CPT|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Part D Events|ICD Diagnosis|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|
|Part D Events|ICD Procedure|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|_(TODO)_|


## Potential Future Improvements

1. Currently, the `SamhsaMatcher` only fully supports Carrier and DME claims; all other claim types are completely suppressed.
2. Move away from URL query parameters and instead enable filtering based on client identity/role, e.g. "if the query is coming from BCDA, always exclude SAMHSA-related claims."
3. Switch the system's default mode to suppress SAMHSA-related claims and instead make including SAMHSA-related claims require a specific opt-in. (This would break backwards compatibility but is a more fail-safe approach.)
