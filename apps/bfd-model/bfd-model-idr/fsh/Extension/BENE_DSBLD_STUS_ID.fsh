// @Name: Bene disabed indicator status code
// @Description: Beneficiary disabled status indicator code.

Extension: BENE_DSBLD_STUS_ID //We can use _ in the name
Description: "Beneficiary Disabled Status Indicator Code"
Id: BENE-DSBLD-STUS-ID //FHIR ids do not allow _ in them
* ^url = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-DSBLD-STUS-ID" //see above
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-DSBLD-STUS-ID"
