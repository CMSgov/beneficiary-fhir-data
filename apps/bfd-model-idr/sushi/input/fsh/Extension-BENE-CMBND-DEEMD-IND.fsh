Extension: BENE_CMBND_DEEMD_IND
Id: BENE-CMBND-DEEMD-IND
Title: "Beneficiary LIS Deemed Code"
Description: "Indicates whether the beneficiary has been deemed for Low Income Subsidy (LIS) coverage"
* ^version = "1.0.0"
* ^status = #active
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-CMBND-DEEMD-IND"
* ^context[0].type = #element
* ^context[0].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-CMBND-DEEMD-IND"
