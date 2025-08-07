Extension: BENE_LIS_PTD_PRM_PCT
Id: BENE-LIS-PTD-PRM-PCT
Title: "Beneficiary LIS Part D Premium Percentage"
Description: "Indicates the percentage of Part D premium covered by Low Income Subsidy (LIS)"
* ^version = "1.0.0"
* ^status = #active
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-LIS-PTD-PRM-PCT"
* ^context[0].type = #element
* ^context[0].expression = "Coverage"
* value[x] 1..1
* value[x] only decimal 