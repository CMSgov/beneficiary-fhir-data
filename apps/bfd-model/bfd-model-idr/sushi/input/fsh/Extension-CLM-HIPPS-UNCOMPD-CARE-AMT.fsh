Extension: CLM_HIPPS_UNCOMPD_CARE_AMT
Title: "Claim Uncompensated Care Payment Amount"
Description: "This field identifies the payment for disproportionate share hospitals (DSH). It represents the uncompensated care amount of the payment."
Id: CLM-HIPPS-UNCOMPD-CARE-AMT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-HIPPS-UNCOMPD-CARE-AMT" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.adjudication"
* value[x] only Money
