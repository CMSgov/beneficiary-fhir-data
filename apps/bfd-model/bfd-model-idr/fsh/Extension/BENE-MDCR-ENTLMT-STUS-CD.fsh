Extension: BENE_MDCR_ENTLMT_STUS_CD //We can use _ in the name
Description: "Beneficiary Entitlement Status Code"
Id: BENE-MDCR-ENTLMT-STUS-CD // But FHIR ids do not allow _ in them
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-MDCR-ENTLMT-STUS-CD" //see above
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-MDCR-ENTLMT-STUS-CD"
