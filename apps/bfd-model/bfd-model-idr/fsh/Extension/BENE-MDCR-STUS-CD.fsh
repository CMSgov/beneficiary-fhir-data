// @Name: Medicare Status Code FSH
// @Description: Example of beneficiary medicare status code.

Extension: BENE_MDCR_STUS_CD //We can use _ in the name
Description: "Medicare Beneficiary Status Code"
Id: BENE-MDCR-STUS-CD //FHIR ids do not allow _ in them
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-MDCR-STUS-CD" //see above
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-MDCR-STUS-CD"
