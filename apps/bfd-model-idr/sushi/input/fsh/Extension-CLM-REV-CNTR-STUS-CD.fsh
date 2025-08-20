Extension: CLM_REV_CNTR_STUS_CD
Title: "Revenue Center Payment Method Indicator Code"
Description: "Extension used to provide the variable that indicates how the service listed on the revenue center record was priced for payment purposes."
Id: CLM-REV-CNTR-STUS-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-REV-CNTR-STUS-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-REV-CNTR-STUS-CD"
