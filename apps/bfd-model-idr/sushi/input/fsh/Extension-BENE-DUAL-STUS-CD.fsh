Extension: BENE_DUAL_STUS_CD 
Title: "Beneficiary Dual Status Code"
Description: "A code identifying the dual eligibility status for a beneficiary."
Id: BENE-DUAL-STUS-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-DUAL-STUS-CD"
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-DUAL-STUS-CD" 