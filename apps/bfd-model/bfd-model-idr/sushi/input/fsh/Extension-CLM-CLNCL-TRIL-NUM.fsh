Extension: CLM_CLNCL_TRIL_NUM
Title: "Clinical Trial Number"
Description: "The number used to identify all items and line-item services provided to a beneficiary during their participation in a clinical trial."
Id: CLM-CLNCL-TRIL-NUM
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-CLNCL-TRIL-NUM" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only string
