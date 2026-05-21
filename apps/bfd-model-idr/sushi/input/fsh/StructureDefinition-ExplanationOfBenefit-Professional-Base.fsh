Logical: ExplanationOfBenefit-Professional-Base
Id: ExplanationOfBenefit-Professional-Base
* ^status = #draft
* ^type = "ExplanationOfBenefit-Professional-Base"
* . ^label = "Explanation of Benefit Institutional Base Resource for IDR. Ignore in DD Generation"
* CLM_MDCR_PRFNL_PRMRY_PYR_AMT 0..1 string "Carrier Primary Payer Paid Amount" "Carrier Primary Payer Paid Amount"
* CLM_AUDT_TRL_STUS_CD 0..1 string "Claim Status Code" "Claim Status Code"
* CLM_PRVDR_ACNT_RCVBL_OFST_AMT 0..1 string "Provider Account Receivable Offset Amount" "Provider Account Receivable Offset Amount"
* CLM_CARR_PMT_DNL_CD 0..1 string "Claim Payment Denial Code" "REASONS FOR DENYING PAYMENT TO CARRIERS."
* CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW 0..1 string "Provider Assignement Indicator" "A SWITCH INDICATING WHETHER OR NOT THE PROVIDER ACCEPTS ASSIGNMENT FOR THE NONINSTITUTIONAL CLAIM. [NCH]"
* CLM_CLNCL_TRIL_NUM 0..1 string "Clinical Trial Number" "EFFECTIVE SEPTEMBER 1, 2008 WITH THE IMPLEMENTATION OF CR#3, THE NUMBER USED TO IDENTIFY ALL ITEMS AND SERVICES PROVIDED TO A BENEFICIARY DURING THEIR PARTICIPATION IN A CLINICAL TRIAL. (FROM NCH CLAIM CLINICAL TRIAL NUMBER)"
