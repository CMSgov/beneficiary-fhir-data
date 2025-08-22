Extension: BENE_LIS_EFCTV_CD
Id: BENE-LIS-EFCTV-CD
Title: "Beneficiary LIS Effective Code"
Description: "Indicates whether the beneficiary has effective Low Income Subsidy (LIS) coverage"
* ^version = "1.0.0"
* ^status = #active
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/BENE-LIS-EFCTV-CD"
* ^context[0].type = #element
* ^context[0].expression = "Coverage"
* value[x] only Coding
* value[x] 1..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/BENE-LIS-EFCTV-CD" 