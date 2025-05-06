Extension: CLM_CMS_PROC_DT
Title: "FI Claim Process Date"
Description: "The date the fiscal intermediary completes processing and releases the institutional claim to the CMS common working file (CWF; stored in the NCH)."
Id: CLM-CMS-PROC-DT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-CMS-PROC-DT" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only date
