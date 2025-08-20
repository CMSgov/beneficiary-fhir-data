CodeSystem: Supporting_Information
Title: "Supporting Information"
Id: Supporting-Information
Description: "A selection of codes that provide supporting information about the claim."
* ^url = "https://bluebutton.cms.gov/fhir/CodeSystem/Supporting-Information"
* ^status = #active
* ^content = #complete

* #CLM_NCH_WKLY_PROC_DT "Weekly Process Date" "The date the weekly NCH database load process cycle begins, during which the claim records are loaded into the Nearline file. This date will always be a Friday, although the claims will actually be appended to the database subsequent to the date."
* #CLM_BLOOD_PT_FRNSH_QTY "Blood Pints Furnished Quantity" "Number of whole pints of blood furnished to the beneficiary, as reported on the carrier claim (non-DMERC)."
* #CLM_MDCR_INSTNL_MCO_PD_SW "MCO Paid Switch" "A switch indicating whether or not a Managed Care Organization (MCO) has paid the provider for an institutional claim."
* #CLM_MDCR_NCH_PTNT_STUS_IND_CD "Patient Status Code" "This variable is a recoded version of the discharge status code."
* #CLM_ACTV_CARE_THRU_DT "Covered Care Through Date" "The date on a claim for which the covered level of care ended in a general hospital or the active care ended in a psychiatric/tuberculosis hospital."
* #CLM_NCVRD_FROM_DT "Noncovered Stay From Date" "The beginning date of the beneficiary's non-covered stay."
* #CLM_NCVRD_THRU_DT "Noncovered Stay Through Date" "The ending date of the beneficiary's non-covered stay."
* #CLM_MDCR_EXHSTD_DT "Medicare Benefits Exhausted Date" "The last date for which the beneficiary has Medicare coverage."
* #CLM_PPS_IND_CD "Claim PPS Indicator Code" "The code indicating whether or not: (1) the claim is from the prospective payment system (PPS), and/or (2) the beneficiary is a deemed insured MQGE (Medicare Qualified Government Employee)"
* #CLM_NCH_PRMRY_PYR_CD "NCH Primary Payer Code" "Code system for NCH primary payer code"
* #CLM_QLFY_STAY_FROM_DT "Qualified Stay From Date" "The beginning date of the beneficiary's qualifying Medicare stay."
* #CLM_QLFY_STAY_THRU_DT "Qualified Stay Through Date" "The ending date of the beneficiary's qualifying Medicare stay."
* #CLM_HHA_LUP_IND_CD "Claim Lupa Indicator Code" "The code used to identify those Home Health PPS claims that have 4 visits or less in a 60-day episode."
* #CLM_HHA_RFRL_CD "Claim Referral Code" "The code used to identify the means by which the beneficiary was referred for Home Health services."
* #CLM_PHRMCY_SRVC_TYPE_CD "Pharmacy Service Type Code" "The type of pharmacy used. This variable indicates the type of pharmacy that dispensed the prescription, as recorded on the PDE."
* #CLM_PTNT_RSDNC_CD "Patient Residence Code" "This variable indicates where the beneficiary lived when the prescription was filled, as reported on the PDE record."
* #CLM_LTC_DSPNSNG_MTHD_CD "Submission Clarification Code" "For beneficiaries living in long-term care (LTC) facilities, this variable indicates how many days' supply of the medication was dispensed by the long-term care pharmacy and provides some details about the dispensing event."
* #CLM_DRUG_CVRG_STUS_CD "Drug Coverage Status Code" "This field indicates whether or not the drug is covered by Medicare Part D."
* #CLM_CTSTRPHC_CVRG_IND_CD "Catastrophic Coverage Code" "This variable indicates whether the PDE occurred within the catastrophic benefit phase of the Medicare Part D benefit, according to the source PDE."
* #CLM_LINE_HCT_HGB_RSLT_NUM "HCT/HGB Test Result" "The HCT/HGB Test Result"
