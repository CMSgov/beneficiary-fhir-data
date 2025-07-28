Extension: CLM_LINE_ANSTHSA_UNIT_CNT
Title: "Anesthesia Unit Count"
Description: "The base number of units assigned to the line item anesthesia procedure on the carrier claim (non-DMERC)."
Id: CLM-LINE-ANSTHSA-UNIT-CNT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-LINE-ANSTHSA-UNIT-CNT" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] 0..1
* value[x] only decimal
