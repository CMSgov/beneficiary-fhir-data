Extension: CLM_ADJSTMT_TYPE_CD
Title: "Claim Adjustment Type Code"
Description: "Deprecated 2026-01-14. DOMAIN OF CLAIM ADJUSTMENT TYPES, INCLUDING DENOTING ORIGINAL CLAIMS."
Id: CLM-ADJSTMT-TYPE-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-ADJSTMT-TYPE-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-ADJSTMT-TYPE-CD"
