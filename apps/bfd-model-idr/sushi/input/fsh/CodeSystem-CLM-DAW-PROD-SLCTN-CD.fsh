CodeSystem: CLM_DAW_PROD_SLCTN_CD
Title: "Dispense as Written Selection Code."
Id: CLM-DAW-PROD-SLCTN-CD
Description: "Indicates the prescriber's instructions regarding generic substitution or how those instructions were followed"
* ^url = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-DAW-PROD-SLCTN-CD"
* ^status = #active
* ^content = #complete

* #0 "No product selection indicated"
* #1 "Supplemental drugs (reported by plans that provide Enhanced Alternative coverage)"
* #2 "Substitution allowed – Patient requested that brand be dispensed"
* #3 "Substitution allowed – Pharmacist selected product dispensed"
* #4 "Substitution allowed - Generic not in stock"
* #5 "Substitution allowed – Brand drug dispensed as generic"
* #6 "Override"
* #7 "Substitution not allowed – Brand drug mandated by law"
* #8 "Substitution allowed – Generic drug not available in marketplace"
* #9 "Other"
