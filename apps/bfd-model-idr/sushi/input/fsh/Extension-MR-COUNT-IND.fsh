Extension: MR_COUNT_IND
Title: "Medical Review Count Indicator"
Description: "Medical review count indicator."
Id: MR-COUNT-IND
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/MR-COUNT-IND"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only decimal
