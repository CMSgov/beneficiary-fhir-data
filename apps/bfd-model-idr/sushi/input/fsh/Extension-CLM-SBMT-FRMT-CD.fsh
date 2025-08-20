Extension: CLM_SBMT_FRMT_CD
Title: "Claim Submission Format Code"
Description: "A code indicating the method or format used to submit the claim."
Id: CLM-SBMT-FRMT-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-SBMT-FRMT-CD"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-SBMT-FRMT-CD" 