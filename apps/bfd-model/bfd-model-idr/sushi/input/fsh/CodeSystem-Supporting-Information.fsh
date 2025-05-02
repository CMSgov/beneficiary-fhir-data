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
