Extension: CLM_SBMTR_CNTRCT_NUM
Id: CLM-SBMTR-CNTRCT-NUM
Title: "Submitter Contract Number"
Description: "The contract number of the submitter."
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-SBMTR-CNTRCT-NUM"
* ^status = #active
* ^context[0].type = #element
* ^context[0].expression = "ExplanationOfBenefit.insurance"
* value[x] only string 