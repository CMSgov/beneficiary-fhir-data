Extension: CLM_QUERY_CD 
Title: "Claim Query Code"
Description: "Code indicating the type of claim record being processed with respect to payment (debit/credit indicator; interim/final indicator)."
Id: CLM-QUERY-CD 
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-QUERY-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.billablePeriod"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-QUERY-CD"
