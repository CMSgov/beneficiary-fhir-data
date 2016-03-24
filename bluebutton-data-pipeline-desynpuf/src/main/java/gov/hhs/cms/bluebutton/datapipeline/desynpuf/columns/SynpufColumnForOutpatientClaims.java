package gov.hhs.cms.bluebutton.datapipeline.desynpuf.columns;

import java.util.Arrays;

/**
 * Enumerates the columns in the CMS DE-SynPUF Beneficiary Summary files.
 */
public enum SynpufColumnForOutpatientClaims {
	/**
	 * Beneficiary Code
	 */
	DESYNPUF_ID,

	/**
	 * Claim ID
	 */
	CLM_ID,

	/**
	 * Claim Line Segment
	 */
	SEGMENT,

	/**
	 * Claims start date
	 */
	CLM_FROM_DT,

	/**
	 * Claims end date
	 */
	CLM_THRU_DT,

	/**
	 * Provider Institution
	 */
	PRVDR_NUM,

	/**
	 * Claim Payment Amount
	 */
	CLM_PMT_AMT,

	/**
	 * NCH Primary Payer Claim Paid Amount
	 */
	NCH_PRMRY_PYR_CLM_PD_AMT,

	/**
	 * Attending Physician – National Provider Identifier Number
	 */
	AT_PHYSN_NPI,

	/**
	 * Operating Physician – National Provider Identifier Number
	 */
	OP_PHYSN_NPI,

	/**
	 * Other Physician – National Provider Identifier Number
	 */
	OT_PHYSN_NPI,

	/**
	 * NCH Beneficiary Blood Deductible Liability Amount
	 */
	NCH_BENE_BLOOD_DDCTBL_LBLTY_AM,

	/**
	 * Claim Diagnosis Code
	 */
	ICD9_DGNS_CD_1,

	/**
	 * Claim Diagnosis Code
	 */
	ICD9_DGNS_CD_2,

	/**
	 * Claim Diagnosis Code
	 */
	ICD9_DGNS_CD_3,

	/**
	 * Claim Diagnosis Code
	 */
	ICD9_DGNS_CD_4,

	/**
	 * Claim Diagnosis Code
	 */
	ICD9_DGNS_CD_5,

	/**
	 * Claim Diagnosis Code
	 */
	ICD9_DGNS_CD_6,

	/**
	 * Claim Diagnosis Code
	 */
	ICD9_DGNS_CD_7,

	/**
	 * Claim Diagnosis Code
	 */
	ICD9_DGNS_CD_8,

	/**
	 * Claim Diagnosis Code
	 */
	ICD9_DGNS_CD_9,

	/**
	 * Claim Diagnosis Code
	 */
	ICD9_DGNS_CD_10,

	/**
	 * Claim Procedure Code
	 */
	ICD9_PRCDR_CD_1,

	/**
	 * Claim Procedure Code
	 */
	ICD9_PRCDR_CD_2,

	/**
	 * Claim Procedure Code
	 */
	ICD9_PRCDR_CD_3,

	/**
	 * Claim Procedure Code
	 */
	ICD9_PRCDR_CD_4,

	/**
	 * Claim Procedure Code
	 */
	ICD9_PRCDR_CD_5,

	/**
	 * Claim Procedure Code
	 */
	ICD9_PRCDR_CD_6,

	/**
	 * NCH Beneficiary Part B Deductible Amount
	 */
	NCH_BENE_PTB_DDCTBL_AMT,

	/**
	 * NCH Beneficiary Part B Coinsurance Amount
	 */
	NCH_BENE_PTB_COINSRNC_AMT,

	/**
	 * Claim Admitting Diagnosis Code
	 */
	ADMTNG_ICD9_DGNS_CD,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_1,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_2,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_3,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_4,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_5,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_6,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_7,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_8,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_9,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_10,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_11,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_12,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_13,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_14,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_15,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_16,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_17,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_18,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_19,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_20,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_21,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_22,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_23,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_24,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_25,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_26,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_27,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_28,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_29,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_30,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_31,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_32,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_33,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_34,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_35,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_36,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_37,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_38,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_39,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_40,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_41,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_42,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_43,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_44,

	/**
	 * Revenue Center HCFA Common Procedure Coding System
	 */
	HCPCS_CD_45;

	/**
	 * @return a <code>String[]</code> containing all of the column names
	 */
	public static String[] getAllColumnNames() {
		return Arrays.stream(SynpufColumnForOutpatientClaims.values()).map(v -> v.name())
				.toArray(size -> new String[size]);
	}
}
