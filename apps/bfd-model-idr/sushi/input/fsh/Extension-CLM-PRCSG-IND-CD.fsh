Extension: CLM_PRCSG_IND_CD
Title: "Processing Indicator Code"
Description: "The code on a non-institutional claim indicating to whom payment was made or if the claim was denied."
Id: CLM-PRCSG-IND-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRCSG-IND-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-PRCSG-IND-CD"
