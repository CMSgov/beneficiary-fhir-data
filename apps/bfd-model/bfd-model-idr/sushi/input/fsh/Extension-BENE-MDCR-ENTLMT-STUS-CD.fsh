Extension: BENE_MDCR_ENTLMT_STUS_CD 
Title: "Beneficiary Entitlement Status Code"
Description: "The reason for entitlement or termination of a beneficiary's benefits during a period of coverage."
Id: BENE-MDCR-ENTLMT-STUS-CD 
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-MDCR-ENTLMT-STUS-CD" //see above
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-MDCR-ENTLMT-STUS-CD"
