Logical: ProcedureComponent
Id: ProcedureComponent
* ^name = "Procedure info"
* ^status = #draft
* ^type = "ProcedureComponent"
* . ^label = "Procedure Info for IDR. Ignore in DD Generation"
* CLM_VAL_SQNC_NUM 1..1 string "Procedure Sequence Number" "A system generated sequence number for a combination of claim key and product."
* CLM_PRCDR_PRFRM_DT 1..1 string "Procedure Performed Date" "The date on which the principal or other procedure was performed. (NCH)"
* CLM_PRCDR_CD 1..1 string "Claim Procedure Code" "Stores procedure codes."
* CLM_DGNS_PRCDR_ICD_IND 1..1 string "Diagnosis Procedure ICD indicator" "This is the claim product version code which is spaces or 9 for ICD-9 and 0 for ICD-10."
