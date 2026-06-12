Extension: PA_DECISION_DT
Title: "Prior Authorization Decision Date"
Description: "Date of the prior authorization decision."
Id: PA-DECISION-DT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/PA-DECISION-DT"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only date
