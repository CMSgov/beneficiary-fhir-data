Extension: CLM_IDR_LD_DT
Title: "Claim IDR Load Date"
Description: "Deprecated 2026-01-14. When the claim was loaded into the IDR."
Id: CLM-IDR-LD-DT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-IDR-LD-DT" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only date
