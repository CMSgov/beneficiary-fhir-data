Extension: CLM_RNDRG_PRVDR_PRTCPTG_CD
Title: "Provider Participating Indicator Code"
Description: "Code indicating whether or not a provider is participating (accepting assignment) for this line-item service on the non-institutional claim."
Id: CLM-RNDRG-PRVDR-PRTCPTG-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-RNDRG-PRVDR-PRTCPTG-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-RNDRG-PRVDR-PRTCPTG-CD"
