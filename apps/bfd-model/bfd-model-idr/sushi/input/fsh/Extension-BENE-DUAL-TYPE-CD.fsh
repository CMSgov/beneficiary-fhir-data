Extension: BENE_DUAL_TYPE_CD 
Title: "Beneficiary Dual Type Code"
Description: "A code identifying the dual eligibility type for a beneficiary."
Id: BENE-DUAL-TYPE-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-DUAL-TYPE-CD"
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-DUAL-TYPE-CD" 