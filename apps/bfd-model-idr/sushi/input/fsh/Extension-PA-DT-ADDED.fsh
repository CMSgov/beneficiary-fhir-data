Extension: PA_DT_ADDED
Title: "Prior Authorization Date Added"
Description: "Date the prior authorization was added."
Id: PA-DT-ADDED
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/PA-DT-ADDED"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only date
