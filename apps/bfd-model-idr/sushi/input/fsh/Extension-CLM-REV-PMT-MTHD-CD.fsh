Extension: CLM_REV_PMT_MTHD_CD
Title: "Revenue Center Payment Method Indicator Code"
Description: "Extension used to provide the code used to identify how the service is priced for payment."
Id: CLM-REV-PMT-MTHD-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-REV-PMT-MTHD-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-REV-PMT-MTHD-CD"
