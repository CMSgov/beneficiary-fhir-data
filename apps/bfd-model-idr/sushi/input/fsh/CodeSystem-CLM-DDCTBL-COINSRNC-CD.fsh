CodeSystem: CLM_DDCTBL_COINSRNC_CD
Title: "Claim Deductible Coinsurance Code"
Id: CLM-DDCTBL-COINSRNC-CD
Description: "CODE INDICATING WHETHER THE REVENUE CENTER CHARGES ARE SUBJECT TO DEDUCTIBLE AND/OR COINSURANCE."
* ^url = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-DDCTBL-COINSRNC-CD"
* ^status = #active
* ^content = #complete

* #0 "Charges are subject to deductible and coinsurance"
* #1 "Charges are not subject to deductible"
* #2 "Charges are not subject to coinsurance"
* #3 "Charges are not subject to deductible or coinsurance"
* #4 "No charge or units associated with this revenue center code. (For multiple HCPCS per single revenue center code) For revenue center code 0001, the following MSP override values may be present:"
* #5 "RHC or CORF psychiatric"
* #A "voluntary agreement"
* #H "JMO cell rate (55555)"
* #I "IRS/SSA/CMS Data Match project (77777)"
* #L "litigation"
* #M "Override code; EGHP (employer group health plan) services involved"
* #N "Override code; non-EGHP services involved"
* #Q "initial enrollment questionnaire (IEQ 99999)"
* #X "Override code: MSP (Medicare is secondary payer) cost avoided"
* #Y "MSP cost avoided" 