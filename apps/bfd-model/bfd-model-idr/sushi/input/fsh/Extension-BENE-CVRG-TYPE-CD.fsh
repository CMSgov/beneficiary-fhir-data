Extension: BENE_CVRG_TYPE_CD 
Title: "Beneficiary Coverage Type Code"
Description: "A code identifying the type of Medicare coverage for a beneficiary."
Id: BENE-CVRG-TYPE-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-CVRG-TYPE-CD"
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-CVRG-TYPE-CD" 