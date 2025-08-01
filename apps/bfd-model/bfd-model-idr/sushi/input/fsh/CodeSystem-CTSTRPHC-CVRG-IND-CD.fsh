CodeSystem: CLM_CTSTRPHC_CVRG_IND_CD
Title: "Catastrophic Coverage Code"
Id: CLM-CTSTRPHC-CVRG-IND-CD
Description: "This variable indicates whether the PDE occurred within the catastrophic benefit phase of the Medicare Part D benefit, according to the source PDE.

When the value equals C (above attachment point), then the PDE is in the catastrophic phase. When the value equals A (attachment point), the PDE has caused the beneficiary to move into the catastrophic phase (i.e., this is the 'triggering' PDE)."
* ^url = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-CTSTRPHC-CVRG-IND-CD"
* ^status = #active
* ^content = #complete

* #A "Attachment point met on this event"
* #C "Above attachment point"