Extension: RRB-EXCL-IND
Title: "RRB Exclusion Indicator"
Description: "RRB Exclusion Indicator."
Id: RRB-EXCL-IND
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/RRB-EXCL-IND"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only boolean