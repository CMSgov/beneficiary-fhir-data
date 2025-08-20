Extension: CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW
Title: "Provider Assignement Indicator"
Description: "Variable indicates whether or not the provider accepts assignment for the noninstitutional claim."
Id: CLM-MDCR-PRFNL-PRVDR-ASGNMT-SW
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-MDCR-PRFNL-PRVDR-ASGNMT-SW" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only Coding
