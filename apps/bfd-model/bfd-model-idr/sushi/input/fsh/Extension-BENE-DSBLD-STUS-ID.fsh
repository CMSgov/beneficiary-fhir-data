Extension: BENE_DSBLD_STUS_ID
Title: "Beneficiary Disabled Status Indicator Code"
Description: "An code indicating if the Beneficiary is entitled to medicare due to a disability."
Id: BENE-DSBLD-STUS-ID
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-DSBLD-STUS-ID" //see above
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-DSBLD-STUS-ID"
