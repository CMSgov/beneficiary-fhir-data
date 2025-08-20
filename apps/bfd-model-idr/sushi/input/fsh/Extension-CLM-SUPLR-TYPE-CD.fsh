Extension: CLM_SUPLR_TYPE_CD
Title: "Supplier Type Code"
Description: "The type of DMERC supplier."
Id: CLM-SUPLR-TYPE-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-SUPLR-TYPE-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-SUPLR-TYPE-CD"
