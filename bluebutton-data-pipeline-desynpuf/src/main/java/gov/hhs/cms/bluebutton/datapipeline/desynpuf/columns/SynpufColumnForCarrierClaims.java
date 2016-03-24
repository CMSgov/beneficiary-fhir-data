package gov.hhs.cms.bluebutton.datapipeline.desynpuf.columns;

import java.util.Arrays;

import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufFile;

/**
 * Enumerates the columns in the {@link SynpufFile#CLAIMS_CARRIER_FIRST} and
 * {@link SynpufFile#CLAIMS_CARRIER_SECOND} files.
 */
public enum SynpufColumnForCarrierClaims {
	/**
	 * Beneficiary Code
	 */
	DESYNPUF_ID,

	/**
	 * Claim ID
	 */
	CLM_ID,

	/**
	 * Claims start date
	 */
	CLM_FROM_DT,

	/**
	 * Claims end date
	 */
	CLM_THRU_DT,

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
	 * Provider Physician – National Provider Identifier Number
	 */
	PRF_PHYSN_NPI_1,

	/**
	 * Provider Physician – National Provider Identifier Number
	 */
	PRF_PHYSN_NPI_2,

	/**
	 * Provider Physician – National Provider Identifier Number
	 */
	PRF_PHYSN_NPI_3,

	/**
	 * Provider Physician – National Provider Identifier Number
	 */
	PRF_PHYSN_NPI_4,

	/**
	 * Provider Physician – National Provider Identifier Number
	 */
	PRF_PHYSN_NPI_5,

	/**
	 * Provider Physician – National Provider Identifier Number
	 */
	PRF_PHYSN_NPI_6,

	/**
	 * Provider Physician – National Provider Identifier Number
	 */
	PRF_PHYSN_NPI_7,

	/**
	 * Provider Physician – National Provider Identifier Number
	 */
	PRF_PHYSN_NPI_8,

	/**
	 * Provider Physician – National Provider Identifier Number
	 */
	PRF_PHYSN_NPI_9,

	/**
	 * Provider Physician – National Provider Identifier Number
	 */
	PRF_PHYSN_NPI_10,

	/**
	 * Provider Physician – National Provider Identifier Number
	 */
	PRF_PHYSN_NPI_11,

	/**
	 * Provider Physician – National Provider Identifier Number
	 */
	PRF_PHYSN_NPI_12,

	/**
	 * Provider Physician – National Provider Identifier Number
	 */
	PRF_PHYSN_NPI_13,

	/**
	 * Provider Institution Tax Number
	 */
	TAX_NUM_1,

	/**
	 * Provider Institution Tax Number
	 */
	TAX_NUM_2,

	/**
	 * Provider Institution Tax Number
	 */
	TAX_NUM_3,

	/**
	 * Provider Institution Tax Number
	 */
	TAX_NUM_4,

	/**
	 * Provider Institution Tax Number
	 */
	TAX_NUM_5,

	/**
	 * Provider Institution Tax Number
	 */
	TAX_NUM_6,

	/**
	 * Provider Institution Tax Number
	 */
	TAX_NUM_7,

	/**
	 * Provider Institution Tax Number
	 */
	TAX_NUM_8,

	/**
	 * Provider Institution Tax Number
	 */
	TAX_NUM_9,

	/**
	 * Provider Institution Tax Number
	 */
	TAX_NUM_10,

	/**
	 * Provider Institution Tax Number
	 */
	TAX_NUM_11,

	/**
	 * Provider Institution Tax Number
	 */
	TAX_NUM_12,

	/**
	 * Provider Institution Tax Number
	 */
	TAX_NUM_13,

	/**
	 * Line HCFA Common Procedure Coding System
	 */
	HCPCS_CD_1,

	/**
	 * Line HCFA Common Procedure Coding System
	 */
	HCPCS_CD_2,

	/**
	 * Line HCFA Common Procedure Coding System
	 */
	HCPCS_CD_3,

	/**
	 * Line HCFA Common Procedure Coding System
	 */
	HCPCS_CD_4,

	/**
	 * Line HCFA Common Procedure Coding System
	 */
	HCPCS_CD_5,

	/**
	 * Line HCFA Common Procedure Coding System
	 */
	HCPCS_CD_6,

	/**
	 * Line HCFA Common Procedure Coding System
	 */
	HCPCS_CD_7,

	/**
	 * Line HCFA Common Procedure Coding System
	 */
	HCPCS_CD_8,

	/**
	 * Line HCFA Common Procedure Coding System
	 */
	HCPCS_CD_9,

	/**
	 * Line HCFA Common Procedure Coding System
	 */
	HCPCS_CD_10,

	/**
	 * Line HCFA Common Procedure Coding System
	 */
	HCPCS_CD_11,

	/**
	 * Line HCFA Common Procedure Coding System
	 */
	HCPCS_CD_12,

	/**
	 * Line HCFA Common Procedure Coding System
	 */
	HCPCS_CD_13,

	/**
	 * Line NCH Payment Amount
	 */
	LINE_NCH_PMT_AMT_1,

	/**
	 * Line NCH Payment Amount
	 */
	LINE_NCH_PMT_AMT_2,

	/**
	 * Line NCH Payment Amount
	 */
	LINE_NCH_PMT_AMT_3,

	/**
	 * Line NCH Payment Amount
	 */
	LINE_NCH_PMT_AMT_4,

	/**
	 * Line NCH Payment Amount
	 */
	LINE_NCH_PMT_AMT_5,

	/**
	 * Line NCH Payment Amount
	 */
	LINE_NCH_PMT_AMT_6,

	/**
	 * Line NCH Payment Amount
	 */
	LINE_NCH_PMT_AMT_7,

	/**
	 * Line NCH Payment Amount
	 */
	LINE_NCH_PMT_AMT_8,

	/**
	 * Line NCH Payment Amount
	 */
	LINE_NCH_PMT_AMT_9,

	/**
	 * Line NCH Payment Amount
	 */
	LINE_NCH_PMT_AMT_10,

	/**
	 * Line NCH Payment Amount
	 */
	LINE_NCH_PMT_AMT_11,

	/**
	 * Line NCH Payment Amount
	 */
	LINE_NCH_PMT_AMT_12,

	/**
	 * Line NCH Payment Amount
	 */
	LINE_NCH_PMT_AMT_13,

	/**
	 * Line Beneficiary Part B Deductible Amount
	 */
	LINE_BENE_PTB_DDCTBL_AMT_1,

	/**
	 * Line Beneficiary Part B Deductible Amount
	 */
	LINE_BENE_PTB_DDCTBL_AMT_2,

	/**
	 * Line Beneficiary Part B Deductible Amount
	 */
	LINE_BENE_PTB_DDCTBL_AMT_3,

	/**
	 * Line Beneficiary Part B Deductible Amount
	 */
	LINE_BENE_PTB_DDCTBL_AMT_4,

	/**
	 * Line Beneficiary Part B Deductible Amount
	 */
	LINE_BENE_PTB_DDCTBL_AMT_5,

	/**
	 * Line Beneficiary Part B Deductible Amount
	 */
	LINE_BENE_PTB_DDCTBL_AMT_6,

	/**
	 * Line Beneficiary Part B Deductible Amount
	 */
	LINE_BENE_PTB_DDCTBL_AMT_7,

	/**
	 * Line Beneficiary Part B Deductible Amount
	 */
	LINE_BENE_PTB_DDCTBL_AMT_8,

	/**
	 * Line Beneficiary Part B Deductible Amount
	 */
	LINE_BENE_PTB_DDCTBL_AMT_9,

	/**
	 * Line Beneficiary Part B Deductible Amount
	 */
	LINE_BENE_PTB_DDCTBL_AMT_10,

	/**
	 * Line Beneficiary Part B Deductible Amount
	 */
	LINE_BENE_PTB_DDCTBL_AMT_11,

	/**
	 * Line Beneficiary Part B Deductible Amount
	 */
	LINE_BENE_PTB_DDCTBL_AMT_12,

	/**
	 * Line Beneficiary Part B Deductible Amount
	 */
	LINE_BENE_PTB_DDCTBL_AMT_13,

	/**
	 * Line Beneficiary Primary Payer Paid Amount
	 */
	LINE_BENE_PRMRY_PYR_PD_AMT_1,

	/**
	 * Line Beneficiary Primary Payer Paid Amount
	 */
	LINE_BENE_PRMRY_PYR_PD_AMT_2,

	/**
	 * Line Beneficiary Primary Payer Paid Amount
	 */
	LINE_BENE_PRMRY_PYR_PD_AMT_3,

	/**
	 * Line Beneficiary Primary Payer Paid Amount
	 */
	LINE_BENE_PRMRY_PYR_PD_AMT_4,

	/**
	 * Line Beneficiary Primary Payer Paid Amount
	 */
	LINE_BENE_PRMRY_PYR_PD_AMT_5,

	/**
	 * Line Beneficiary Primary Payer Paid Amount
	 */
	LINE_BENE_PRMRY_PYR_PD_AMT_6,

	/**
	 * Line Beneficiary Primary Payer Paid Amount
	 */
	LINE_BENE_PRMRY_PYR_PD_AMT_7,

	/**
	 * Line Beneficiary Primary Payer Paid Amount
	 */
	LINE_BENE_PRMRY_PYR_PD_AMT_8,

	/**
	 * Line Beneficiary Primary Payer Paid Amount
	 */
	LINE_BENE_PRMRY_PYR_PD_AMT_9,

	/**
	 * Line Beneficiary Primary Payer Paid Amount
	 */
	LINE_BENE_PRMRY_PYR_PD_AMT_10,

	/**
	 * Line Beneficiary Primary Payer Paid Amount
	 */
	LINE_BENE_PRMRY_PYR_PD_AMT_11,

	/**
	 * Line Beneficiary Primary Payer Paid Amount
	 */
	LINE_BENE_PRMRY_PYR_PD_AMT_12,

	/**
	 * Line Beneficiary Primary Payer Paid Amount
	 */
	LINE_BENE_PRMRY_PYR_PD_AMT_13,

	/**
	 * Line Coinsurance Amount
	 */
	LINE_COINSRNC_AMT_1,

	/**
	 * Line Coinsurance Amount
	 */
	LINE_COINSRNC_AMT_2,

	/**
	 * Line Coinsurance Amount
	 */
	LINE_COINSRNC_AMT_3,

	/**
	 * Line Coinsurance Amount
	 */
	LINE_COINSRNC_AMT_4,

	/**
	 * Line Coinsurance Amount
	 */
	LINE_COINSRNC_AMT_5,

	/**
	 * Line Coinsurance Amount
	 */
	LINE_COINSRNC_AMT_6,

	/**
	 * Line Coinsurance Amount
	 */
	LINE_COINSRNC_AMT_7,

	/**
	 * Line Coinsurance Amount
	 */
	LINE_COINSRNC_AMT_8,

	/**
	 * Line Coinsurance Amount
	 */
	LINE_COINSRNC_AMT_9,

	/**
	 * Line Coinsurance Amount
	 */
	LINE_COINSRNC_AMT_10,

	/**
	 * Line Coinsurance Amount
	 */
	LINE_COINSRNC_AMT_11,

	/**
	 * Line Coinsurance Amount
	 */
	LINE_COINSRNC_AMT_12,

	/**
	 * Line Coinsurance Amount
	 */
	LINE_COINSRNC_AMT_13,

	/**
	 * Line Allowed Charge Amount
	 */
	LINE_ALOWD_CHRG_AMT_1,

	/**
	 * Line Allowed Charge Amount
	 */
	LINE_ALOWD_CHRG_AMT_2,

	/**
	 * Line Allowed Charge Amount
	 */
	LINE_ALOWD_CHRG_AMT_3,

	/**
	 * Line Allowed Charge Amount
	 */
	LINE_ALOWD_CHRG_AMT_4,

	/**
	 * Line Allowed Charge Amount
	 */
	LINE_ALOWD_CHRG_AMT_5,

	/**
	 * Line Allowed Charge Amount
	 */
	LINE_ALOWD_CHRG_AMT_6,

	/**
	 * Line Allowed Charge Amount
	 */
	LINE_ALOWD_CHRG_AMT_7,

	/**
	 * Line Allowed Charge Amount
	 */
	LINE_ALOWD_CHRG_AMT_8,

	/**
	 * Line Allowed Charge Amount
	 */
	LINE_ALOWD_CHRG_AMT_9,

	/**
	 * Line Allowed Charge Amount
	 */
	LINE_ALOWD_CHRG_AMT_10,

	/**
	 * Line Allowed Charge Amount
	 */
	LINE_ALOWD_CHRG_AMT_11,

	/**
	 * Line Allowed Charge Amount
	 */
	LINE_ALOWD_CHRG_AMT_12,

	/**
	 * Line Allowed Charge Amount
	 */
	LINE_ALOWD_CHRG_AMT_13,

	/**
	 * Line Processing Indicator Code
	 */
	LINE_PRCSG_IND_CD_1,

	/**
	 * Line Processing Indicator Code
	 */
	LINE_PRCSG_IND_CD_2,

	/**
	 * Line Processing Indicator Code
	 */
	LINE_PRCSG_IND_CD_3,

	/**
	 * Line Processing Indicator Code
	 */
	LINE_PRCSG_IND_CD_4,

	/**
	 * Line Processing Indicator Code
	 */
	LINE_PRCSG_IND_CD_5,

	/**
	 * Line Processing Indicator Code
	 */
	LINE_PRCSG_IND_CD_6,

	/**
	 * Line Processing Indicator Code
	 */
	LINE_PRCSG_IND_CD_7,

	/**
	 * Line Processing Indicator Code
	 */
	LINE_PRCSG_IND_CD_8,

	/**
	 * Line Processing Indicator Code
	 */
	LINE_PRCSG_IND_CD_9,

	/**
	 * Line Processing Indicator Code
	 */
	LINE_PRCSG_IND_CD_10,

	/**
	 * Line Processing Indicator Code
	 */
	LINE_PRCSG_IND_CD_11,

	/**
	 * Line Processing Indicator Code
	 */
	LINE_PRCSG_IND_CD_12,

	/**
	 * Line Processing Indicator Code
	 */
	LINE_PRCSG_IND_CD_13,

	/**
	 * Line Diagnosis Code
	 */
	LINE_ICD9_DGNS_CD_1,

	/**
	 * Line Diagnosis Code
	 */
	LINE_ICD9_DGNS_CD_2,

	/**
	 * Line Diagnosis Code
	 */
	LINE_ICD9_DGNS_CD_3,

	/**
	 * Line Diagnosis Code
	 */
	LINE_ICD9_DGNS_CD_4,

	/**
	 * Line Diagnosis Code
	 */
	LINE_ICD9_DGNS_CD_5,

	/**
	 * Line Diagnosis Code
	 */
	LINE_ICD9_DGNS_CD_6,

	/**
	 * Line Diagnosis Code
	 */
	LINE_ICD9_DGNS_CD_7,

	/**
	 * Line Diagnosis Code
	 */
	LINE_ICD9_DGNS_CD_8,

	/**
	 * Line Diagnosis Code
	 */
	LINE_ICD9_DGNS_CD_9,

	/**
	 * Line Diagnosis Code
	 */
	LINE_ICD9_DGNS_CD_10,

	/**
	 * Line Diagnosis Code
	 */
	LINE_ICD9_DGNS_CD_11,

	/**
	 * Line Diagnosis Code
	 */
	LINE_ICD9_DGNS_CD_12,

	/**
	 * Line Diagnosis Code
	 */
	LINE_ICD9_DGNS_CD_13;

	/**
	 * @return a <code>String[]</code> containing all of the column names
	 */
	public static String[] getAllColumnNames() {
		return Arrays.stream(SynpufColumnForCarrierClaims.values()).map(v -> v.name()).toArray(size -> new String[size]);
	}

	/**
	 * @param lineNumber
	 *            the line number of the <code>PRF_PHYSN_NPI_#</code> column to
	 *            return, must be <code>1</code> through <code>13</code>,
	 *            inclusive
	 * @return the {@link SynpufColumnForCarrierClaims} constant for the specified
	 *         <code>PRF_PHYSN_NPI_#</code> column
	 */
	public static SynpufColumnForCarrierClaims getPrfPhysnNpi(int lineNumber) {
		for (SynpufColumnForCarrierClaims column : SynpufColumnForCarrierClaims.values())
			if (column.name().startsWith("PRF_PHYSN_NPI") && column.name().endsWith("_" + lineNumber))
				return column;
		throw new IllegalArgumentException("Undefined column for line: " + lineNumber);
	}

	/**
	 * @param lineNumber
	 *            the line number of the <code>TAX_NUM_#</code> column to
	 *            return, must be <code>1</code> through <code>13</code>,
	 *            inclusive
	 * @return the {@link SynpufColumnForCarrierClaims} constant for the specified
	 *         <code>TAX_NUM_#</code> column
	 */
	public static SynpufColumnForCarrierClaims getTaxNum(int lineNumber) {
		for (SynpufColumnForCarrierClaims column : SynpufColumnForCarrierClaims.values())
			if (column.name().startsWith("TAX_NUM") && column.name().endsWith("_" + lineNumber))
				return column;
		throw new IllegalArgumentException("Undefined column for line: " + lineNumber);
	}

	/**
	 * @param lineNumber
	 *            the line number of the <code>HCPCS_CD_#</code> column to
	 *            return, must be <code>1</code> through <code>13</code>,
	 *            inclusive
	 * @return the {@link SynpufColumnForCarrierClaims} constant for the specified
	 *         <code>HCPCS_CD_#</code> column
	 */
	public static SynpufColumnForCarrierClaims getHcpcsCd(int lineNumber) {
		for (SynpufColumnForCarrierClaims column : SynpufColumnForCarrierClaims.values())
			if (column.name().startsWith("HCPCS_CD") && column.name().endsWith("_" + lineNumber))
				return column;
		throw new IllegalArgumentException("Undefined column for line: " + lineNumber);
	}

	/**
	 * @param lineNumber
	 *            the line number of the <code>LINE_NCH_PMT_AMT_#</code> column
	 *            to return, must be <code>1</code> through <code>13</code>,
	 *            inclusive
	 * @return the {@link SynpufColumnForCarrierClaims} constant for the specified
	 *         <code>LINE_NCH_PMT_AMT_#</code> column
	 */
	public static SynpufColumnForCarrierClaims getLineNchPmtAmt(int lineNumber) {
		for (SynpufColumnForCarrierClaims column : SynpufColumnForCarrierClaims.values())
			if (column.name().startsWith("LINE_NCH_PMT_AMT") && column.name().endsWith("_" + lineNumber))
				return column;
		throw new IllegalArgumentException("Undefined column for line: " + lineNumber);
	}

	/**
	 * @param lineNumber
	 *            the line number of the <code>LINE_BENE_PTB_DDCTBL_AMT_#</code>
	 *            column to return, must be <code>1</code> through
	 *            <code>13</code>, inclusive
	 * @return the {@link SynpufColumnForCarrierClaims} constant for the specified
	 *         <code>LINE_BENE_PTB_DDCTBL_AMT_#</code> column
	 */
	public static SynpufColumnForCarrierClaims getLineBenePtbDdctblAmt(int lineNumber) {
		for (SynpufColumnForCarrierClaims column : SynpufColumnForCarrierClaims.values())
			if (column.name().startsWith("LINE_BENE_PTB_DDCTBL_AMT") && column.name().endsWith("_" + lineNumber))
				return column;
		throw new IllegalArgumentException("Undefined column for line: " + lineNumber);
	}

	/**
	 * @param lineNumber
	 *            the line number of the
	 *            <code>LINE_BENE_PRMRY_PYR_PD_AMT_#</code> column to return,
	 *            must be <code>1</code> through <code>13</code>, inclusive
	 * @return the {@link SynpufColumnForCarrierClaims} constant for the specified
	 *         <code>LINE_BENE_PRMRY_PYR_PD_AMT_#</code> column
	 */
	public static SynpufColumnForCarrierClaims getLineBenePrmryPyrPdAmt(int lineNumber) {
		for (SynpufColumnForCarrierClaims column : SynpufColumnForCarrierClaims.values())
			if (column.name().startsWith("LINE_BENE_PRMRY_PYR_PD_AMT") && column.name().endsWith("_" + lineNumber))
				return column;
		throw new IllegalArgumentException("Undefined column for line: " + lineNumber);
	}

	/**
	 * @param lineNumber
	 *            the line number of the <code>LINE_COINSRNC_AMT_#</code> column
	 *            to return, must be <code>1</code> through <code>13</code>,
	 *            inclusive
	 * @return the {@link SynpufColumnForCarrierClaims} constant for the specified
	 *         <code>LINE_COINSRNC_AMT_#</code> column
	 */
	public static SynpufColumnForCarrierClaims getLineCoinsrncAmt(int lineNumber) {
		for (SynpufColumnForCarrierClaims column : SynpufColumnForCarrierClaims.values())
			if (column.name().startsWith("LINE_COINSRNC_AMT") && column.name().endsWith("_" + lineNumber))
				return column;
		throw new IllegalArgumentException("Undefined column for line: " + lineNumber);
	}

	/**
	 * @param lineNumber
	 *            the line number of the <code>LINE_ALOWD_CHRG_AMT_#</code>
	 *            column to return, must be <code>1</code> through
	 *            <code>13</code>, inclusive
	 * @return the {@link SynpufColumnForCarrierClaims} constant for the specified
	 *         <code>LINE_ALOWD_CHRG_AMT_#</code> column
	 */
	public static SynpufColumnForCarrierClaims getLineAlowdChrgAmt(int lineNumber) {
		for (SynpufColumnForCarrierClaims column : SynpufColumnForCarrierClaims.values())
			if (column.name().startsWith("LINE_ALOWD_CHRG_AMT") && column.name().endsWith("_" + lineNumber))
				return column;
		throw new IllegalArgumentException("Undefined column for line: " + lineNumber);
	}

	/**
	 * @param lineNumber
	 *            the line number of the <code>LINE_PRCSG_IND_CD_#</code> column
	 *            to return, must be <code>1</code> through <code>13</code>,
	 *            inclusive
	 * @return the {@link SynpufColumnForCarrierClaims} constant for the specified
	 *         <code>LINE_PRCSG_IND_CD_#</code> column
	 */
	public static SynpufColumnForCarrierClaims getLinePrcsgIndCd(int lineNumber) {
		for (SynpufColumnForCarrierClaims column : SynpufColumnForCarrierClaims.values())
			if (column.name().startsWith("LINE_PRCSG_IND_CD") && column.name().endsWith("_" + lineNumber))
				return column;
		throw new IllegalArgumentException("Undefined column for line: " + lineNumber);
	}

	/**
	 * @param lineNumber
	 *            the line number of the <code>LINE_ICD9_DGNS_CD_#</code> column
	 *            to return, must be <code>1</code> through <code>13</code>,
	 *            inclusive
	 * @return the {@link SynpufColumnForCarrierClaims} constant for the specified
	 *         <code>LINE_ICD9_DGNS_CD_#</code> column
	 */
	public static SynpufColumnForCarrierClaims getLineIcd9DgnsCd(int lineNumber) {
		for (SynpufColumnForCarrierClaims column : SynpufColumnForCarrierClaims.values())
			if (column.name().startsWith("LINE_ICD9_DGNS_CD") && column.name().endsWith("_" + lineNumber))
				return column;
		throw new IllegalArgumentException("Undefined column for line: " + lineNumber);
	}
}
