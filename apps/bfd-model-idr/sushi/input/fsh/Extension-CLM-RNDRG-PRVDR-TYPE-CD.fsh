Extension: CLM_PRVDR_TYPE_CD
Title: "Provider Type Code"
Description: "Code identifying the type of provider furnishing the service for this line item on the claim. Also used for conveying supplier types."
Id: CLM-PRVDR-PRTCPTG-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRVDR-TYPE-CD" 
* ^context[+].type = #element
* ^context[=].expression = "Practitioner"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PRVDR-TYPE-CD"
