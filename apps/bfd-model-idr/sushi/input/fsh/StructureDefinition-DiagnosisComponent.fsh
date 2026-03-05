Logical: DiagnosisComponent
Id: DiagnosisComponent
* ^name = "Diagnosis info"
* ^url = "http://hl7.org/fhir/StructureDefinition/DiagnosisComponent"
* ^status = #draft
* ^type = "DiagnosisComponent"
* . ^label = "Diagnosis Info for IDR. Ignore in DD Generation"
* CLM_VAL_SQNC_NUM 1..1 string "Diagnosis Sequence Number"
* CLM_VAL_SQNC_NUM ^label = "Diagnosis Sequence Number"
* ROW_NUM 1..1 string "This is a non-overlapping number that should be used as the sequence number for a diagnosis on a given claim."
* ROW_NUM ^label = "Generated Row Number"
* CLM_DGNS_CD 1..1 string "Claim Diagnosis Code"
* CLM_DGNS_CD ^label = "Claim Diagnosis Code"
* CLM_DGNS_PRCDR_ICD_IND 1..1 string "Diagnosis Procedure ICD indicator"
* CLM_DGNS_PRCDR_ICD_IND ^label = "Diagnosis Procedure ICD indicator"
* CLM_PROD_TYPE_CD 1..1 string "Diagnosis Prod Type Code"
* CLM_PROD_TYPE_CD ^label = "Diagnosis Prod Type Code"
* CLM_POA_IND 1..1 string "Claim Present on Admission Indicator"
* CLM_POA_IND ^label = "Claim Present on Admission Indicator"
