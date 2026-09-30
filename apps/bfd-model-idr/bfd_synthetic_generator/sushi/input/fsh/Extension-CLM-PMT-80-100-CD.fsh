Extension: CLM_PMT_80_100_CD
Title: "Payment Code"
Description: "The code indicating that the amount shown in the payment field on the non-institutional line item represents either 80% or 100% of the allowed charges less any deductible, or 100% limitation of liability only."
Id: CLM-PMT-80-100-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PMT-80-100-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-PMT-80-100-CD"
