Extension: CLM_FAC_PRVDR_OSCAR_NUM
Title: "Claim Facility Provider OSCAR Number"
Description: "A facility's Medicare/Medicaid identification number, also known as a Medicare/Medicaid Provider Number, or CCN. This number verifies that a provider has been Medicare certified for a particular type of services."
Id: CLM-FAC-PRVDR-OSCAR-NUM
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-FAC-PRVDR-OSCAR-NUM"
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.item"
* value[x] 0..1
* value[x] only string
