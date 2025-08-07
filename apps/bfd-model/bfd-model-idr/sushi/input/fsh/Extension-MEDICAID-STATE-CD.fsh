Extension: MEDICAID_STATE_CD 
Title: "Medicaid State Code"
Description: "The USPS state code associated with the beneficiary's dual eligible status."
Id: MEDICAID-STATE-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/MEDICAID-STATE-CD"
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://www.usps.com"