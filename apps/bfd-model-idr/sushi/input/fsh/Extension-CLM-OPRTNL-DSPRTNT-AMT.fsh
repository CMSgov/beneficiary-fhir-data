Extension: CLM_OPRTNL_DSPRTNT_AMT
Title: "Operating Disproportionate Share Amount"
Description: "This is one component of the total amount that is payable on prospective payment system (PPS) claims and reflects the DSH (disproportionate share hospital) payments for operating expenses (such as labor) for the claim."
Id: CLM-OPRTNL-DSPRTNT-AMT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-OPRTNL-DSPRTNT-AMT" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.adjudication"
* value[x] only Money
