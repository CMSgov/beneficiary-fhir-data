Extension: PA_REQ_SUB_DT
Title: "Prior Authorization Request Submitted Date"
Description: "Date the prior authorization request was submitted."
Id: PA-REQ-SUB-DT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/PA-REQ-SUB-DT"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only date
