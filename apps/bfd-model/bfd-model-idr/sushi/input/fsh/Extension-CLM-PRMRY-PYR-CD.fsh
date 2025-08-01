Extension: CLM_PRMRY_PYR_CD
Title: "Line Primary Payer Code"
Description: "The code specifying a federal non-Medicare program or other source that has primary responsibility for the payment of the Medicare beneficiary's medical bills relating to the line-item service on the non-institutional claim."
Id: CLM-PRMRY-PYR-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRMRY-PYR-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-PRMRY-PYR-CD"
