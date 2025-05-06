Extension: BENE_ESRD_STUS_ID
Title: "Beneficiary ESRD Status Indicator Code"
Description: "A beneficiary's ESRD Status indicator."
Id: BENE-ESRD-STUS-ID
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-ESRD-STUS-ID" //see above
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-ESRD-STUS-ID"
