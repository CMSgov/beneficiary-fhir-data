Extension: SERVICE_CNTS
Title: "Number of Services"
Description: "Number of services for the segment."
Id: SERVICE-CNTS
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/SERVICE-CNTS"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only decimal
