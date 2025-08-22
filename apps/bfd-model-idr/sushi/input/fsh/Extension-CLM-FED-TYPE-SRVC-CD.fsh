Extension: CLM_FED_TYPE_SRVC_CD
Title: "CMS Service Type Code"
Description: "Code indicating the type of service, as defined in the CMS Medicare Carrier Manual, for this line item on the non-institutional claim."
Id: CLM-FED-TYPE-SRVC-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-FED-TYPE-SRVC-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-FED-TYPE-SRVC-CD"
