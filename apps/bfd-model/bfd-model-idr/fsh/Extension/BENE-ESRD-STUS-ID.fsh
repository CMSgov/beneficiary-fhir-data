// @Name: Bene ESRD indicator status code
// @Description: Beneficiary ESRD status indicator code.

Extension: BENE_ESRD_STUS_ID //We can use _ in the name
Description: "Beneficiary ESRD Status Indicator Code"
Id: BENE-ESRD-STUS-ID //FHIR ids do not allow _ in them
* ^url = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-ESRD-STUS-ID" //see above
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-ESRD-STUS-ID"
