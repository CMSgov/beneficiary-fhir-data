Extension: CLM_DISP_CD 
Title: "Claim Disposition Code"
Id: CLM-DISP-CD
Description: "Deprecated 2026-01-14. Code indicating the disposition or outcome of the processing of the claim record."
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-DISP-CD"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-DISP-CD"
