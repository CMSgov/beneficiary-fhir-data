Extension: PA_REQ_REC_DT
Title: "Prior Authorization Request Received Date"
Description: "Date the prior authorization request was received."
Id: PA-REQ-REC-DT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/PA-REQ-REC-DT"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only date
