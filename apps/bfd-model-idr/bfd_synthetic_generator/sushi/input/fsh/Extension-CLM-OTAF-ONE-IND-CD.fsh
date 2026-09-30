Extension: CLM_OTAF_ONE_IND_CD
Title: "Revenue Center Obligation to Accept As Full Payment Code"
Description: "Revenue Center Obligation to Accept As Full Payment Code"
Id: CLM-OTAF-ONE-IND-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-OTAF-ONE-IND-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-OTAF-IND-CD"
