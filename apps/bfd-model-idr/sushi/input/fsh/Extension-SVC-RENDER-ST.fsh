Extension: SVC_RENDER_ST
Title: "Rendering Service State"
Description: "USPS state code associated with the rendering service state."
Id: SVC-RENDER-ST
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/SVC-RENDER-ST"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://www.usps.com"
