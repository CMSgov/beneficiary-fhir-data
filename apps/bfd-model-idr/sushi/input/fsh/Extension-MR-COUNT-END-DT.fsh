Extension: MR_COUNT_END_DT
Title: "Medical Review Count End Date"
Description: "Medical review count end date."
Id: MR-COUNT-END-DT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/MR-COUNT-END-DT"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only date
