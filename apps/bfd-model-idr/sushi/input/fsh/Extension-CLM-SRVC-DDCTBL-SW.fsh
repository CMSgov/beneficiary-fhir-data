Extension: CLM_SRVC_DDCTBL_SW
Title: "Service Deductible Code"
Description: "Switch indicating whether or not the line-item service on the non-institutional claim is subject to a deductible."
Id: CLM-SRVC-DDCTBL-SW
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-SRVC-DDCTBL-SW" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-SRVC-DDCTBL-SW"
