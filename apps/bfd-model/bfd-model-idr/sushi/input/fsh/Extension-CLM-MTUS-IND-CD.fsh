Extension: CLM_MTUS_IND_CD
Title: "Carrier MTUS Indicator Code"
Description: "Code indicating the units associated with services needing unit reporting on the line item for the carrier claim (non-DMERC)."
Id: CLM-MTUS-IND-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-MTUS-IND-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-MTUS-IND-CD"
