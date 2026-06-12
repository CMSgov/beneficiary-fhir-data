Extension: PA_DECISION
Title: "Prior Authorization Decision Code"
Description: "Prior authorization decision code."
Id: PA-DECISION
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/PA-DECISION"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/PA-DECISION"
