Extension: MR_COUNT_ST_DT
Title: "Medical Review Count Start Date"
Description: "Medical review count start date."
Id: MR-COUNT-ST-DT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/MR-COUNT-ST-DT"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only date
