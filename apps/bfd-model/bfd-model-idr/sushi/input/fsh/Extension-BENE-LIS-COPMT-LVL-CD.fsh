Extension: BENE_LIS_COPMT_LVL_CD
Title: "Beneficiary LIS Copayment Level Code"
Description: "Indicates the copayment level for Low Income Subsidy (LIS) beneficiaries"
Id: BENE-LIS-COPMT-LVL-CD
* ^version = "1.0.0"
* ^status = #active
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-LIS-COPMT-LVL-CD"
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-LIS-COPMT-LVL-CD"