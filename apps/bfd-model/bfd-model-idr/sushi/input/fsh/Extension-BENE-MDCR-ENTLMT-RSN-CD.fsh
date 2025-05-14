Extension: BENE_MDCR_ENTLMT_RSN_CD
Title: "Beneficiary Entitlement Reason Code"
Description: "A code identifying the basis in determining a beneficiary's entitlement to Medicare benefits."
Id: BENE-MDCR-ENTLMT-RSN-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-MDCR-ENTLMT-RSN-CD" //see above
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-MDCR-ENTLMT-RSN-CD"
