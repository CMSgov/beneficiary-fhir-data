Logical: ProcedureComponent
Id: ProcedureComponent
* ^name = "Procedure info"
* ^status = #draft
* ^type = "ProcedureComponent"
* . ^label = "Procedure Info for IDR. Ignore in DD Generation"
* CLM_VAL_SQNC_NUM 1..1 string "Procedure Sequence Number" "A SYSTEM GENERATED SEQUENCE NUMBER FOR A COMBINATION OF CLAIM KEY AND PRODUCT"
* CLM_PRCDR_PRFRM_DT 1..1 string "Procedure Performed Date" "THE DATE ON WHICH THE PRINCIPAL OR OTHER PROCEDURE WAS PERFORMED. (NCH)"
* CLM_PRCDR_CD 1..1 string "Claim Procedure Code" "STORES PROCEDURE CODES"
* CLM_DGNS_PRCDR_ICD_IND 1..1 string "Diagnosis Procedure ICD indicator" "THIS IS THE CLAIM PROD VERSION CODE WHICH IS SPACES OR 9 FOR ICD 9 AND 0 FOR ICD 10."
