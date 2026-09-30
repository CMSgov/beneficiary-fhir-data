Logical: ExplanationOfBenefit-Professional-Base
Id: ExplanationOfBenefit-Professional-Base
* ^status = #draft
* ^type = "ExplanationOfBenefit-Professional-Base"
* . ^label = "Explanation of Benefit Institutional Base Resource for IDR. Ignore in DD Generation"
* CLM_MDCR_PRFNL_PRMRY_PYR_AMT 0..1 string "Carrier Primary Payer Paid Amount" "Carrier Primary Payer Paid Amount"
* CLM_AUDT_TRL_STUS_CD 0..1 string "Claim Status Code" "Claim Status Code"
* CLM_PRVDR_ACNT_RCVBL_OFST_AMT 0..1 string "Provider Account Receivable Offset Amount" "Provider Account Receivable Offset Amount"
* CLM_CARR_PMT_DNL_CD 0..1 string "Claim Payment Denial Code" "Reasons for denying payment to carriers."
* CLM_MDCR_PRFNL_PRVDR_ASGNMT_SW 0..1 string "Provider Assignement Indicator" "A switch indicating whether or not the provider accepts assignement for the noninstitutional claim. [NCH]"
* CLM_CLNCL_TRIL_NUM 0..1 string "Clinical Trial Number" "Effective September 1, 2008 with the implementation of CR#3, the number used to identify all items and services provided to a beneficiary during their participation in a clinical trial. (From NCH Claim Clinical Trial Number)"
