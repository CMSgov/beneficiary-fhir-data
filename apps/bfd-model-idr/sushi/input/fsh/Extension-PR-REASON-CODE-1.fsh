Extension: PR_REASON_CODE_1
Title: "Claim Adjustment Reason Code 1"
Description: "Reason code associated with a prior authorization decision."
Id: PR-REASON-CODE-1
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/PR-REASON-CODE-1"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
