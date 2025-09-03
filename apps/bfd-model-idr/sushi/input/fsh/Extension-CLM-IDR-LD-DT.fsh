Extension: CLM_IDR_LD_DT
Title: "Claim IDR Load Date"
Description: "When the claim was loaded into the IDR."
Id: CLM-IDR-LD-DT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-IDR-LD-DT" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.created"
* value[x] only date
