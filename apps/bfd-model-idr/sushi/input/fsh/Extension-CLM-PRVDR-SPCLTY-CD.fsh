Extension: CLM_PRVDR_SPCLTY_CD
Title: "Service Deductible Code"
Description: "Switch indicating whether or not the line-item service on the non-institutional claim is subject to a deductible."
Id: CLM-PRVDR-SPCLTY-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRVDR-SPCLTY-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-PRVDR-SPCLTY-CD"
