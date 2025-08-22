Extension: CLM_OPRTNL_IME_AMT
Title: "Operating Indirect Medical Education Amount"
Description: "This is one component of the total amount that is payable on PPS claims, and reflects the IME (indirect medical education) payments for operating expenses (such as labor) for the claim."
Id: CLM-OPRTNL-IME-AMT
* ^url = "https://bluebutton.cms.gov/fhir/StructureDefinition/CLM-OPRTNL-IME-AMT" 
* ^context[+].type = #element
* ^context[=].expression = "ExplanationOfBenefit.adjudication"
* value[x] only Money
