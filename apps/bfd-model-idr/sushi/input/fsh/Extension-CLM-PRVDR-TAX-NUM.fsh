Extension: CLM_PRVDR_TAX_NUM
Title: "Claim Provider Tax Number"
Description: "Extension for expressing the tax number of a provider on a line item."
Id: CLM-PRVDR-TAX-NUM
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRVDR-TAX-NUM" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Identifier
* value[x] 1..1
