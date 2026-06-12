Extension: PA_DECISION_EXP_DT
Title: "Prior Authorization Decision Expiration Date"
Description: "Expiration date of the prior authorization decision."
Id: PA-DECISION-EXP-DT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/PA-DECISION-EXP-DT"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only date
