Extension: CNTRCT_PBP_SGMT_NUM
Title: "Contract PBP Segment Number"
Description: "This variable is the segment number that CMS assigns to identify a geographic market segment or subdivision of a Part D plan; the segment number allows you to determine the market area covered by the plan."
Id: CNTRCT-PBP-SGMT-NUM
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CNTRCT-PBP-SGMT-NUM"
* ^context[+].type = #element
* ^context[=].expression = "Coverage"
* value[x] only string
* value[x] 1..1 
