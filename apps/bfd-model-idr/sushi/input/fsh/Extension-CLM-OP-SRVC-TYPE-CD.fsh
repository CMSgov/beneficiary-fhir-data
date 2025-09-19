Extension: CLM_OP_SRVC_TYPE_CD
Title: "Claim Outpatient Service Type Code"
Description: "A code reported by the provider that indicates the specific type of claim."
Id: CLM-OP-SRVC-TYPE-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-OP-SRVC-TYPE-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-OP-SRVC-TYPE-CD"
