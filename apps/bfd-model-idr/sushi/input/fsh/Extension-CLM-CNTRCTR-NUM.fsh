Extension: CLM_CNTRCTR_NUM
Title: "Claim Contractor Number"
Description: "Deprecated 2026-01-14. The identification number assigned by CMS to a fiscal intermediary (FI) authorized to process claim records."
Id: CLM-CNTRCTR-NUM
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-CNTRCTR-NUM" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-CNTRCTR-NUM"
