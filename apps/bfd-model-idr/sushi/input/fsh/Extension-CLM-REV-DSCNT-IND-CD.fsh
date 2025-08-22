Extension: CLM_REV_DSCNT_IND_CD
Title: "Claim Revenue Discount Indicator Code"
Description: "Item-level discount indicator code."
Id: CLM-REV-DSCNT-IND-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-REV-DSCNT-IND-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-REV-DSCNT-IND-CD"
