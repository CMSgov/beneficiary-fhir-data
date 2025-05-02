Extension: BENE_ENRLMT_RSN_CD 
Title: "Beneficiary Enrollment Reason Code"
Description: "The reason for a beneficiary's enrollment in Part A/Part B benefits."
Id: BENE-ENRLMT-RSN-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-ENRLMT-RSN-CD"
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-ENRLMT-RSN-CD"
