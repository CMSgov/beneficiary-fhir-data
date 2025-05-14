Extension: CLM_CNTRCTR_NUM
Title: "Claim Contractor Number"
Description: "The identification number assigned by CMS to a fiscal intermediary (FI) authorized to process claim records."
Id: CLM-CNTRCTR-NUM
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-CNTRCTR-NUM" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-CNTRCTR-NUM"
