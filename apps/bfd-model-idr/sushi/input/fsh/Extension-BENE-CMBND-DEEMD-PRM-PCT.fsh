Extension: BENE_CMBND_DEEMD_PRM_PCT
Id: BENE-CMBND-DEEMD-PRM-PCT
Title: "Beneficiary LIS Part D Premium Percentage"
Description: "Indicates the percentage of Part D premium covered by Low Income Subsidy (LIS)"
* ^version = "1.0.0"
* ^status = #active
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-CMBND-DEEMD-PRM-PCT"
* ^context[0].type = #element
* ^context[0].expression = "Coverage"
* value[x] 1..1
* value[x] only decimal 