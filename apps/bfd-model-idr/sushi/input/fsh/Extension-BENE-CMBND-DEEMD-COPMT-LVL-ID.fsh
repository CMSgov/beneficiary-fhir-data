Extension: BENE_CMBND_DEEMD_COPMT_LVL_ID
Title: "Beneficiary LIS Copayment Level Code"
Description: "Indicates the copayment level for Low Income Subsidy (LIS) beneficiaries"
Id: BENE-CMBND-DEEMD-COPMT-LVL-ID
* ^version = "1.0.0"
* ^status = #active
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-CMBND-DEEMD-COPMT-LVL-ID"
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-CMBND-DEEMD-COPMT-LVL-ID"