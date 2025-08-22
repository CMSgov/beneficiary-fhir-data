Extension: BENE_CNTRCT_NUM 
Title: "Beneficiary Contract Number"
Description: "The contract number associated with the beneficiary's Medicare coverage."
Id: BENE-CNTRCT-NUM
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-CNTRCT-NUM"
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only string
* value[x] 1..1 