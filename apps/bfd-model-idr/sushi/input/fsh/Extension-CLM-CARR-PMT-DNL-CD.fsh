Extension: CLM_CARR_PMT_DNL_CD
Title: "Claim Payment Denial Code"
Description: "Deprecated 2026-01-14. The code on a non-institutional claim indicating who receives payment or if the claim was denied."
Id: CLM-CARR-PMT-DNL-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-CARR-PMT-DNL-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only Coding
