Extension: CLM_REV_PACKG_IND_CD
Title: "Revenue Center Packaging Indicator Code"
Description: "The code used to identify those services that are packaged/bundled with another service."
Id: CLM-REV-PACKG-IND-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-REV-PACKG-IND-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-REV-PACKG-IND-CD"
