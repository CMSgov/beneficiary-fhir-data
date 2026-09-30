Extension: DGNS_DRG_OUTLIER_CD
Title: "Claim Diagnosis Related Group Outlier Stay Code"
Description: "On an institutional claim, the code that indicates the beneficiary stay under the prospective payment system (PPS) which, although classified into a specific diagnosis related group, has an unusually long length (day outlier) or exceptionally high cost (cost outlier)."
Id: DGNS-DRG-OUTLIER-CD
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/DGNS-DRG-OUTLIER-CD" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit"
* value[x] only Coding
* value[x] 0..1
* value[x].system = "https://bluebutton.cms.gov/fhir/CodeSystem/DGNS-DRG-OUTLIER-CD"
