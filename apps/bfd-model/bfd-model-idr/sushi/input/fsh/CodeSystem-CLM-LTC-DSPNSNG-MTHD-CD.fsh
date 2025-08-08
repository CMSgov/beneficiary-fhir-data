CodeSystem: CLM_LTC_DSPNSNG_MTHD_CD
Title: "Submission clarification code"
Id: CLM-LTC-DSPNSNG-MTHD-CD
Description: "For beneficiaries living in long-term care (LTC) facilities, this variable indicates how many days' supply of the medication was dispensed by the long-term care pharmacy and provides some details about the dispensing event.

This variable is only populated when beneficiary lives in an LTC facility (i.e., when the CLM_PTNT_RSDNC_CD variable equals 03)"
* ^url = "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-LTC-DSPNSNG-MTHD-CD"
* ^status = #active
* ^content = #complete

* #00 "(Unknown value – rarely populated)"
* #05 "Therapy change. Physician determined that a change in therapy was required – either the medication was used faster than expected, or a different dosage form is needed."
* #07 "Emergency supply of non-formulary drugs (or formulary drugs which typically require step therapy or prior authorization). Medication has been determined by the physician to be medically necessary."
* #08 "Process compound for approved ingredients"
* #14 "LTC leave of absence – short fill required for take-home use"
* #16 "LTC emergency box (e box) /automated dispensing machine"
* #17 "LTC emergency supply remainder (remainder of drug from the emergency supply)"
* #18 "LTC patient admit/readmission indicator. This status required new dispensing of medication."
* #19 "Split billing. The quantity dispensed is the remainder billed to a subsequent payer after Medicare Part A benefits expired (partial payment under Part A)."
* #21 "LTC dispensing rule for <=14 day supply is not applicable due to CMS exclusion or the fact that the manufacturer’s packaging does not allow for special dispensing"
* #22 "LTC dispensing, 7-day supply"
* #23 "LTC dispensing, 4-day supply"
* #24 "LTC dispensing, 3-day supply"
* #25 "LTC dispensing, 2-day supply"
* #26 "LTC dispensing, 1-day supply"
* #27 "LTC dispensing, 4-day supply, then 3-day supply"
* #28 "LTC dispensing, 2-day supply, then 2-day supply, then 3-day supply"
* #29 "LTC dispensing, daily during the week then multiple days (3) for weekend"
* #30 "LTC dispensing, per shift (multiple medication passes)"
* #31 "LTC dispensing, per medication pass"
* #32 "LTC dispensing, PRN on demand"
* #33 "LTC dispensing, other <=7 day cycle"
* #34 "LTC dispensing, 14-day supply"
* #35 "LTC dispensing, other 8-14 day dispensing not listed above"
* #36 "LTC dispensing, outside short cycle, determined to be Part D after originally submitted to another payer"
* #42 "The prescriber ID submitted has been validated and is active (rarely populated)"
* #43 "For the prescriber ID submitted, the associated DEA number has been renewed or the renewal is in progress (rarely populated)"
* #44 "(Unknown value – rarely populated)"
* #45 "For the prescriber ID submitted, the associated DEA number is a valid hospital DEA number with suffix (rarely populated)"