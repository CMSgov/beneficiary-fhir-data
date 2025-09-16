CodeSystem: DGNS_DRG_OUTLIER_CD
Title: "Claim Diagnosis Related Group Outlier Stay Code"
Id: DGNS-DRG-OUTLIER-CD
Description: "On an institutional claim, the code that indicates the beneficiary stay under the prospective payment system (PPS) which, although classified into a specific diagnosis related group, has an unusually long length (day outlier) or exceptionally high cost (cost outlier)."
* ^url = "https://bluebutton.cms.gov/fhir/CodeSystem/DGNS-DRG-OUTLIER-CD"
* ^status = #active
* ^content = #complete

* #0 "No outlier"
* #1 "Day outlier (condition code 60)"
* #2 "Cost outlier (condition code 61)"
* #6 "Valid diagnosis related groups (DRG) received from the intermediary *** Non-PPS Only ***"
* #7 "CMS developed DRG *** Non-PPS Only ***"
* #8 "CMS developed DRG using patient status code *** Non-PPS Only ***"
* #9 "Not groupable *** Non-PPS Only ***"
