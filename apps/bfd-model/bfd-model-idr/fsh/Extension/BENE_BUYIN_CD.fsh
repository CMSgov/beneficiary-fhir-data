Extension: BENE_BUYIN_CD //We can use _ in the name
Description: "Beneficiary Buy-in Indicator"
Id: BENE-BUYIN-CD // But FHIR ids do not allow _ in them
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-BUYIN-CD" //see above
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-BUYIN-CD"
