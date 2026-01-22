Extension: CLM_SBMTR_CNTRCT_PBP_NUM
Id: CLM-SBMTR-CNTRCT-PBP-NUM
Title: "Submitter Contract PBP Number"
Description: "Deprecated 2026-01-14. The PBP number of the submitter contract."
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-SBMTR-CNTRCT-PBP-NUM"
* ^status = #active
* ^context[0].type = #element
* ^context[0].expression = "ExplanationOfBenefit.insurance"
* value[x] only string 