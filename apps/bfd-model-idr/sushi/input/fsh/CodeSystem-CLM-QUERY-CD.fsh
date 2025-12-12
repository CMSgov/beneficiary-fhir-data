CodeSystem: CLM_QUERY_CD
Title: "Claim Query Code"
Id: CLM-QUERY-CD
Description: "Code indicating the type of claim record being processed with respect to payment (debit/credit indicator; interim/final indicator)."
* ^url = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-QUERY-CD"
* ^status = #active
* ^content = #complete

* #0 "CREDIT ADJUSTMENT"
* #1 "INTERIM BILL"
* #2 "HOME HEALTH AGENCY (HHA) BENEFITS EXHAUSTED (OBSOLETE 7/98)"
* #3 "FINAL BILL"
* #4 "DISCHARGE NOTICE (OBSOLETE 7/98)"
* #5 "DEBIT ADJUSTMENT"
* #C "CREDIT"
* #D "DEBIT"
* #~ "NO DESCRIPTION AVAILABLE"
