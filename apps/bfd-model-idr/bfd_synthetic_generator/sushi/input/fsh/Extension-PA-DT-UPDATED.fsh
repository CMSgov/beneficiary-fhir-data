Extension: PA_DT_UPDATED
Title: "Prior Authorization Date Updated"
Description: "Date the prior authorization was updated in CWF."
Id: PA-DT-UPDATED
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/PA-DT-UPDATED"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only date
