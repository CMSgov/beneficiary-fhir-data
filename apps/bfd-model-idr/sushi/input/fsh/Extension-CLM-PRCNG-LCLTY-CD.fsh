Extension: CLM_PRCNG_LCLTY_CD
Title: "Line Pricing Locality Code"
Description: "Code denoting the carrier-specific locality used for pricing the service for this line item on the carrier claim (non-DMERC)."
Id: CLM-PRCNG-LCLTY-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRCNG-LCLTY-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-PRCNG-LCLTY-CD"
