Extension: BENE_MDCR_STUS_CD 
Title: "Medicare Beneficiary Status Code"
Description: "A code identifying the reason for a beneficiary's entitlement to Medicare benefits."
Id: BENE-MDCR-STUS-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-MDCR-STUS-CD" //see above
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-MDCR-STUS-CD"
