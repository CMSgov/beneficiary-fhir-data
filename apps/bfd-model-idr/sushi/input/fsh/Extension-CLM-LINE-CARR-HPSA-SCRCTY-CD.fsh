Extension: CLM_LINE_CARR_HPSA_SCRCTY_CD
Title: "HPSA Scarcity Code"
Description: "The code on the carrier (non-DMERC) line item that identifies the line items that have been paid a reduced fee schedule amount (65%, 75% or 85%) because a physician's assistant performed the service."
Id: CLM-LINE-CARR-HPSA-SCRCTY-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-LINE-CARR-HPSA-SCRCTY-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-LINE-CARR-HPSA-SCRCTY-CD"
