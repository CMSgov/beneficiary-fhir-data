Extension: CLM_FI_ACTN_CD
Title: "FI or MAC Claim Action Code"
Description: "The type of action requested by the intermediary to be taken on an institutional claim."
Id: CLM-FI-ACTN-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-FI-ACTN-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-FI-ACTN-CD"
