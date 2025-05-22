Extension: CLM_MDCR_NPMT_RSN_CD
Title: "Claim Non Payment Reason Code"
Description: "The reason that no Medicare payment is made for services on an institutional claim."
Id: CLM-MDCR-NPMT-RSN-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-MDCR-NPMT-RSN-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-MDCR-NPMT-RSN-CD"
