Extension: CLM_NRLN_RIC_CD
Title: "Near Line Record Identification Code"
Description: "A code defining the type of claim record being processed."
Id: CLM-NRLN-RIC-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-NRLN-RIC-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-NRLN-RIC-CD"
