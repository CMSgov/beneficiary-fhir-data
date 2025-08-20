Extension: CLM_PHYSN_ASTNT_CD
Title: "Reduced Payment Physician Assistant Code"
Description: "The code on the carrier (non-DMERC) line item that identifies the line items that have been paid a reduced fee schedule amount (65%, 75% or 85%) because a physician's assistant performed the service."
Id: CLM-PHYSN-ASTNT-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-PHYSN-ASTNT-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-PHYSN-ASTNT-CD"
