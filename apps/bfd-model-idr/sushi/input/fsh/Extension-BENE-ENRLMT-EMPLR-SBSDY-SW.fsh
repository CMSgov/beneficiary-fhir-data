Extension: BENE_ENRLMT_EMPLR_SBSDY_SW
Title: "Beneficiary Retiree Drug Subsidy Switch"
Description: "A switch indicating enrollment by a beneficiary who has an employer subsidy."
Id: BENE-ENRLMT-EMPLR-SBSDY-SW
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-ENRLMT-EMPLR-SBSDY-SW" 
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-ENRLMT-EMPLR-SBSDY-SW"
