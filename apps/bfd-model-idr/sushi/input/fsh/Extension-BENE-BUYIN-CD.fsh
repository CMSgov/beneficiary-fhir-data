Extension: BENE_BUYIN_CD
Title: "Beneficiary Buy-in Indicator"
Description: "Monthly Part A and/or Part B entitlement indicator."
Id: BENE-BUYIN-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-BUYIN-CD" //see above
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-BUYIN-CD"
