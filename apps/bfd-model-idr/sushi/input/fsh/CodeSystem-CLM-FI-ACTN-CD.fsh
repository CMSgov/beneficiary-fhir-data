CodeSystem: CLM_FI_ACTN_CD
Title: "Claim Fiscal Intermediary Action Code"
Id: CLM-FI-ACTN-CD
Description: "The type of action requested by the intermediary to be taken on an institutional claim."
* ^url = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-FI-ACTN-CD"
* ^status = #active
* ^content = #complete

* #1 "Original debit action (always a 1 for all regular bills)"
* #2 "Cancel by credit adjustment"
* #3 "Secondary debit adjustment"
* #4 "Cancel only adjustment"
* #5 "Force action code 3 (secondary debit adjustment)"
* #6 "Force action code 2"
* #7 "Outpatient history only"
* #8 "Benefits refused"
* #9 "Payment requested"
