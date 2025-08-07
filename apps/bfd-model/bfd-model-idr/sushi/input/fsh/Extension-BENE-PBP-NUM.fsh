Extension: BENE_PBP_NUM 
Title: "Beneficiary PBP Number"
Description: "The PBP (Prescription Benefit Plan) number associated with the beneficiary's Medicare coverage."
Id: BENE-PBP-NUM
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-PBP-NUM"
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only string
* value[x] 1..1 