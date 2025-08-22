Extension: CLM_LINE_RX_NUM
Title: "Claim Line RX Number"
Description: "The number used to identify the prescription order number for drugs and biologicals purchased through the competitive acquisition program (CAP)."
Id: CLM-LINE-RX-NUM
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-LINE-RX-NUM" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] 0..1
* value[x] only string
