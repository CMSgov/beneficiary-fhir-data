package gov.hhs.cms.bluebutton.datapipeline.rif.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Models rows from {@link RifFileType#CARRIER} RIF files. Rows in this file are
 * grouped, such that there is one group per claim, with multiple rows: one for
 * each claim line.
 * </p>
 * <p>
 * The RIF file layout used here is specific to the Blue Button API's ETL
 * process. The layouts of these files is detailed in the
 * <code>bluebutton-data-pipeline-rif/dev/rif-record-layout.xlsx</code> file.
 * The columns contained in the files are largely similar to those detailed in
 * <a href="https://www.ccwdata.org/web/guest/data-dictionaries">CCW: Data
 * Dictionaries</a>.
 * </p>
 * <p>
 * Design Note: This class is too painful to maintain as a bean, so I've
 * stripped it down to just a struct. To be clear, this is <strong>not</strong>
 * immutable and thus <strong>not</strong> thread-safe (and it really shouldn't
 * need to be).
 * </p>
 */
public final class DMEClaimGroup {

	/**
	 * @see Column#VERSION
	 */
	public int version;

	/**
	 * @see Column#DML_IND
	 */
	public RecordAction recordAction;

	/**
	 * @see Column#BENE_ID
	 */
	public String beneficiaryId;

	/**
	 * @see Column#CLM_ID
	 */
	public String claimId;

	/**
	 * @see Column#NCH_NEAR_LINE_REC_IDENT_CD
	 */
	public Character nearLineRecordIdCode;

	/**
	 * @see Column#NCH_CLM_TYPE_CD
	 */
	public String claimTypeCode;

	/**
	 * @see Column#CLM_FROM_DT
	 */
	public LocalDate dateFrom;

	/**
	 * @see Column#CLM_THRU_DT
	 */
	public LocalDate dateThrough;

	/**
	 * @see Column#NCH_WKLY_PROC_DT
	 */
	public LocalDate weeklyProcessDate;

	/**
	 * @see Column#CARR_CLM_ENTRY_CD
	 */
	public Character claimEntryCode;

	/**
	 * @see Column#CLM_DISP_CD
	 */
	public String claimDispositionCode;

	/**
	 * @see Column#CARR_NUM
	 */
	public String carrierNumber;

	/**
	 * @see Column#CARR_CLM_PMT_DNL_CD
	 */
	public String paymentDenialCode;

	/**
	 * @see Column#CLM_PMT_AMT
	 */
	public BigDecimal paymentAmount;

	/**
	 * @see Column#CARR_CLM_PRMRY_PYR_PD_AMT
	 */
	public BigDecimal primaryPayerPaidAmount;

	/**
	 * @see Column#CARR_CLM_PRVDR_ASGNMT_IND_SW
	 */
	public Character providerAssignmentIndicator;

	/**
	 * @see Column#NCH_CLM_PRVDR_PMT_AMT
	 */
	public BigDecimal providerPaymentAmount;

	/**
	 * @see Column#NCH_CLM_BENE_PMT_AMT
	 */
	public BigDecimal beneficiaryPaymentAmount;

	/**
	 * @see Column#NCH_CARR_CLM_SBMTD_CHRG_AMT
	 */
	public BigDecimal submittedChargeAmount;

	/**
	 * @see Column#NCH_CARR_CLM_ALOWD_AMT
	 */
	public BigDecimal allowedChargeAmount;

	/**
	 * @see Column#CARR_CLM_CASH_DDCTBL_APLD_AMT
	 */
	public BigDecimal beneficiaryPartBDeductAmount;

	/**
	 * @see Column#CARR_CLM_HCPCS_YR_CD
	 */
	public Character hcpcsYearCode;

	/**
	 * @see Column#PRNCPAL_DGNS_CD
	 * @see Column#PRNCPAL_DGNS_VRSN_CD
	 */
	public IcdCode diagnosisPrincipal;

	/**
	 * See {@link Column#ICD_DGNS_CD1} through {@link Column#ICD_DGNS_CD12} and
	 * {@link Column#ICD_DGNS_VRSN_CD1} through
	 * {@link Column#ICD_DGNS_VRSN_CD12}.
	 */
	public List<IcdCode> diagnosesAdditional = new LinkedList<>();

	/**
	 * @see Column#RFR_PHYSN_UPIN
	 */
	public String referringPhysicianUpin;

	/**
	 * @see Column#RFR_PHYSN_NPI
	 */
	public String referringPhysicianNpi;

	/**
	 * @see Column#CLM_CLNCL_TRIL_NUM
	 */
	public String clinicalTrialNumber;

	/**
	 * Represents the data contained in {@link Column#LINE_NUM} and subsequent
	 * columns: one entry for every "claim line" in the claim represented by
	 * this {@link DMEClaimGroup} instance.
	 */
	public List<DMEClaimLine> lines = new LinkedList<>();

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DMEClaimGroup [version=");
		builder.append(version);
		builder.append(", recordAction=");
		builder.append(recordAction);
		builder.append(", beneficiaryId=");
		builder.append(beneficiaryId);
		builder.append(", claimId=");
		builder.append(claimId);
		builder.append(", nearLineRecordIdCode=");
		builder.append(nearLineRecordIdCode);
		builder.append(", claimTypeCode=");
		builder.append(claimTypeCode);
		builder.append(", dateFrom=");
		builder.append(dateFrom);
		builder.append(", dateThrough=");
		builder.append(dateThrough);
		builder.append(", weeklyProcessDate=");
		builder.append(weeklyProcessDate);
		builder.append(", claimEntryCode=");
		builder.append(claimEntryCode);
		builder.append(", claimDispositionCode=");
		builder.append(claimDispositionCode);
		builder.append(", carrierNumber=");
		builder.append(carrierNumber);
		builder.append(", paymentDenialCode=");
		builder.append(paymentDenialCode);
		builder.append(", paymentAmount=");
		builder.append(paymentAmount);
		builder.append(", primaryPayerPaidAmount=");
		builder.append(primaryPayerPaidAmount);
		builder.append(", providerAssignmentIndicator=");
		builder.append(providerAssignmentIndicator);
		builder.append(", providerPaymentAmount=");
		builder.append(providerPaymentAmount);
		builder.append(", beneficiaryPaymentAmount=");
		builder.append(beneficiaryPaymentAmount);
		builder.append(", submittedChargeAmount=");
		builder.append(submittedChargeAmount);
		builder.append(", allowedChargeAmount=");
		builder.append(allowedChargeAmount);
		builder.append(", beneficiaryPartBDeductAmount=");
		builder.append(beneficiaryPartBDeductAmount);
		builder.append(", hcpcsYearCode=");
		builder.append(hcpcsYearCode);
		builder.append(", diagnosisPrincipal=");
		builder.append(diagnosisPrincipal);
		builder.append(", diagnosesAdditional=");
		builder.append(diagnosesAdditional);
		builder.append(", referringPhysicianUpin=");
		builder.append(referringPhysicianUpin);
		builder.append(", referringPhysicianNpi=");
		builder.append(referringPhysicianNpi);
		builder.append(", clinicalTrialNumber=");
		builder.append(clinicalTrialNumber);
		builder.append(", lines=");
		builder.append(lines);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Models individual claim lines within a {@link DMEClaimGroup} instance.
	 */
	public static final class DMEClaimLine {

		/**
		 * @see Column#LINE_NUM
		 */
		public Integer number;

		/**
		 * @see Column#TAX_NUM
		 */
		public String providerTaxNumber;

		/**
		 * @see Column#PRVDR_SPCLTY
		 */
		public String providerSpecialityCode;

		/**
		 * @see Column#PRTCPTNG_IND_CD
		 */
		public Character providerParticipatingIndCode;

		/**
		 * @see Column#LINE_SRVC_CNT
		 */
		public BigDecimal serviceCount;

		/**
		 * @see Column#LINE_CMS_TYPE_SRVC_CD
		 */
		public String cmsServiceTypeCode;

		/**
		 * @see Column#LINE_PLACE_OF_SRVC_CD
		 */
		public String placeOfServiceCode;

		/**
		 * @see Column#LINE_1ST_EXPNS_DT
		 */
		public LocalDate firstExpenseDate;

		/**
		 * @see Column#LINE_LAST_EXPNS_DT
		 */
		public LocalDate lastExpenseDate;

		/**
		 * @see Column#HCPCS_CD
		 */
		public String hcpcsCode;

		/**
		 * @see Column#HCPCS_1ST_MDFR_CD
		 */
		public Optional<String> hcpcsInitialModifierCode;

		/**
		 * @see Column#HCPCS_2ND_MDFR_CD
		 */
		public Optional<String> hcpcsSecondModifierCode;

		/**
		 * @see Column#BETOS_CD
		 */
		public String betosCode;

		/**
		 * @see Column#LINE_NCH_PMT_AMT
		 */
		public BigDecimal paymentAmount;

		/**
		 * @see Column#LINE_BENE_PMT_AMT
		 */
		public BigDecimal beneficiaryPaymentAmount;

		/**
		 * @see Column#LINE_PRVDR_PMT_AMT
		 */
		public BigDecimal providerPaymentAmount;

		/**
		 * @see Column#LINE_BENE_PTB_DDCTBL_AMT
		 */
		public BigDecimal beneficiaryPartBDeductAmount;

		/**
		 * @see Column#LINE_BENE_PRMRY_PYR_CD
		 */
		public Optional<Character> primaryPayerCode;

		/**
		 * @see Column#LINE_BENE_PRMRY_PYR_PD_AMT
		 */
		public BigDecimal primaryPayerPaidAmount;

		/**
		 * @see Column#LINE_COINSRNC_AMT
		 */
		public BigDecimal coinsuranceAmount;

		/**
		 * @see Column#LINE_PRMRY_ALOWD_CHRG_AMT
		 */
		public BigDecimal primaryPayerAllowedChargeAmount;

		/**
		 * @see Column#LINE_SBMTD_CHRG_AMT
		 */
		public BigDecimal submittedChargeAmount;

		/**
		 * @see Column#LINE_ALOWD_CHRG_AMT
		 */
		public BigDecimal allowedChargeAmount;

		/**
		 * @see Column#LINE_PRCSG_IND_CD
		 */
		public String processingIndicatorCode;

		/**
		 * @see Column#LINE_PMT_80_100_CD
		 */
		public Character paymentCode;

		/**
		 * @see Column#LINE_SERVICE_DEDUCTIBLE
		 */
		public Character serviceDeductibleCode;

		/**
		 * @see Column#LINE_ICD_DGNS_CD
		 * @see Column#LINE_ICD_DGNS_VRSN_CD
		 */
		public IcdCode diagnosis;

		/**
		 * @see Column#LINE_DME_PRCHS_PRICE_AMT
		 */
		public BigDecimal purchasePriceAmount;

		/**
		 * @see Column#PRVDR_NPI
		 */
		public String providerNPI;

		/**
		 * @see Column#PRVDR_STATE_CD
		 */
		public String providerStateCode;

		/**
		 * @see Column#DMERC_LINE_MTUS_CNT
		 */
		public BigDecimal mtusCount;

		/**
		 * @see Column#DMERC_LINE_MTUS_CD
		 */
		public Character mtusCode;

		/**
		 * @see Column#LINE_NDC_CD
		 */
		public Optional<String> nationalDrugCode;

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("DMEClaimLine [number=");
			builder.append(number);
			builder.append(", providerTaxNumber=");
			builder.append(providerTaxNumber);
			builder.append(", providerSpecialityCode=");
			builder.append(providerSpecialityCode);
			builder.append(", providerParticipatingIndCode=");
			builder.append(providerParticipatingIndCode);
			builder.append(", serviceCount=");
			builder.append(serviceCount);
			builder.append(", cmsServiceTypeCode=");
			builder.append(cmsServiceTypeCode);
			builder.append(", placeOfServiceCode=");
			builder.append(placeOfServiceCode);
			builder.append(", firstExpenseDate=");
			builder.append(firstExpenseDate);
			builder.append(", lastExpenseDate=");
			builder.append(lastExpenseDate);
			builder.append(", hcpcsCode=");
			builder.append(hcpcsCode);
			builder.append(", hcpcsInitialModifierCode=");
			builder.append(hcpcsInitialModifierCode);
			builder.append(", hcpcsSecondModifierCode=");
			builder.append(hcpcsSecondModifierCode);
			builder.append(", betosCode=");
			builder.append(betosCode);
			builder.append(", paymentAmount=");
			builder.append(paymentAmount);
			builder.append(", beneficiaryPaymentAmount=");
			builder.append(beneficiaryPaymentAmount);
			builder.append(", providerPaymentAmount=");
			builder.append(providerPaymentAmount);
			builder.append(", beneficiaryPartBDeductAmount=");
			builder.append(beneficiaryPartBDeductAmount);
			builder.append(", primaryPayerCode=");
			builder.append(primaryPayerCode);
			builder.append(", primaryPayerPaidAmount=");
			builder.append(primaryPayerPaidAmount);
			builder.append(", coinsuranceAmount=");
			builder.append(coinsuranceAmount);
			builder.append(", primaryPayerAllowedChargeAmount=");
			builder.append(primaryPayerAllowedChargeAmount);
			builder.append(", submittedChargeAmount=");
			builder.append(submittedChargeAmount);
			builder.append(", allowedChargeAmount=");
			builder.append(allowedChargeAmount);
			builder.append(", processingIndicatorCode=");
			builder.append(processingIndicatorCode);
			builder.append(", paymentCode=");
			builder.append(paymentCode);
			builder.append(", serviceDeductibleCode=");
			builder.append(serviceDeductibleCode);
			builder.append(", diagnosis=");
			builder.append(diagnosis);
			builder.append(", purchasePriceAmount=");
			builder.append(purchasePriceAmount);
			builder.append(", providerNPI=");
			builder.append(providerNPI);
			builder.append(", providerStateCode=");
			builder.append(providerStateCode);
			builder.append(", mtusCount=");
			builder.append(mtusCount);
			builder.append(", mtusCode=");
			builder.append(mtusCode);
			builder.append(", nationalDrugCode=");
			builder.append(nationalDrugCode);
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * Enumerates the raw RIF columns represented in {@link DMEClaimGroup},
	 * where {@link Column#ordinal()} values represent each column's position in
	 * the actual data.
	 */
	public static enum Column {
		/**
		 * Type: (unknown), max chars: (unknown).
		 */
		VERSION,

		/**
		 * Type: (unknown), max chars: (unknown).
		 */
		DML_IND,

		/**
		 * Type: <code>CHAR</code>, max chars: 15. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/bene_id.txt">
		 * CCW Data Dictionary: BENE_ID</a>, though note that this instance of
		 * the field is unencrypted.
		 */
		BENE_ID,

		/**
		 * Type: <code>CHAR</code>, max chars: 15. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_id.txt">
		 * CCW Data Dictionary: CLM_ID</a>.
		 */
		CLM_ID,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ric_cd.txt">
		 * CCW Data Dictionary: RIC_CD</a>.
		 */
		NCH_NEAR_LINE_REC_IDENT_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 2. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_type.txt">
		 * CCW Data Dictionary: CLM_TYPE</a>.
		 */
		NCH_CLM_TYPE_CD,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/from_dt.txt">
		 * CCW Data Dictionary: FROM_DT</a>.
		 */
		CLM_FROM_DT,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/thru_dt.txt">
		 * CCW Data Dictionary: THRU_DT</a>.
		 */
		CLM_THRU_DT,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/wkly_dt.txt">
		 * CCW Data Dictionary: WKLY_DT</a>.
		 */
		NCH_WKLY_PROC_DT,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/entry_cd.txt">
		 * CCW Data Dictionary: ENTRY_CD</a>.
		 */
		CARR_CLM_ENTRY_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 2. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/disp_cd.txt">
		 * CCW Data Dictionary: DISP_CD</a>.
		 */
		CLM_DISP_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 5. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carr_num.txt">
		 * CCW Data Dictionary: CARR_NUM</a>.
		 */
		CARR_NUM,

		/**
		 * Type: <code>CHAR</code>, max chars: 2. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmtdnlcd.txt">
		 * CCW Data Dictionary: PMTDNLCD</a>.
		 */
		CARR_CLM_PMT_DNL_CD,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmt_amt.txt">
		 * CCW Data Dictionary: PMT_AMT</a>.
		 */
		CLM_PMT_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prpayamt.txt">
		 * CCW Data Dictionary: PRPAYAMT</a>.
		 */
		CARR_CLM_PRMRY_PYR_PD_AMT,
		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/asgmntcd.txt">
		 * CCW Data Dictionary: ASGMNTCD</a>.
		 */
		CARR_CLM_PRVDR_ASGNMT_IND_SW,
		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prov_pmt.txt">
		 * CCW Data Dictionary: PROV_PMT</a>.
		 */
		NCH_CLM_PRVDR_PMT_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/bene_pmt.txt">
		 * CCW Data Dictionary: BENE_PMT</a>.
		 */
		NCH_CLM_BENE_PMT_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/sbmtchrg.txt">
		 * CCW Data Dictionary: SBMTCHRG</a>.
		 */
		NCH_CARR_CLM_SBMTD_CHRG_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/alowchrg.txt">
		 * CCW Data Dictionary: ALOWCHRG</a>.
		 */
		NCH_CARR_CLM_ALOWD_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/dedapply.txt">
		 * CCW Data Dictionary: DEDAPPLY</a>.
		 */
		CARR_CLM_CASH_DDCTBL_APLD_AMT,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcpcs_yr.txt">
		 * CCW Data Dictionary: HCPCS_YR</a>.
		 */
		CARR_CLM_HCPCS_YR_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prncpal_dgns_cd.txt">
		 * CCW Data Dictionary: PRNCPAL_DGNS_CD</a>.
		 */
		PRNCPAL_DGNS_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prncpal_dgns_vrsn_cd.txt">
		 * CCW Data Dictionary: PRNCPAL_DGNS_VRSN_CD</a>.
		 */
		PRNCPAL_DGNS_VRSN_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd1.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD1</a>.
		 */
		ICD_DGNS_CD1,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd1.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD1</a>.
		 */
		ICD_DGNS_VRSN_CD1,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd2.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD2</a>.
		 */
		ICD_DGNS_CD2,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd2.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD2</a>.
		 */
		ICD_DGNS_VRSN_CD2,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd3.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD3</a>.
		 */
		ICD_DGNS_CD3,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd3.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD3</a>.
		 */
		ICD_DGNS_VRSN_CD3,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd4.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD4</a>.
		 */
		ICD_DGNS_CD4,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd4.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD4</a>.
		 */
		ICD_DGNS_VRSN_CD4,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd5.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD5</a>.
		 */
		ICD_DGNS_CD5,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd5.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD5</a>.
		 */
		ICD_DGNS_VRSN_CD5,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd6.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD6</a>.
		 */
		ICD_DGNS_CD6,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd6.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD6</a>.
		 */
		ICD_DGNS_VRSN_CD6,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd7.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD7</a>.
		 */
		ICD_DGNS_CD7,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd7.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD7</a>.
		 */
		ICD_DGNS_VRSN_CD7,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd8.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD8</a>.
		 */
		ICD_DGNS_CD8,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd8.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD8</a>.
		 */
		ICD_DGNS_VRSN_CD8,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd9.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD9</a>.
		 */
		ICD_DGNS_CD9,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd9.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD9</a>.
		 */
		ICD_DGNS_VRSN_CD9,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd10.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD10</a>.
		 */
		ICD_DGNS_CD10,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd10.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD10</a>.
		 */
		ICD_DGNS_VRSN_CD10,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd11.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD11</a>.
		 */
		ICD_DGNS_CD11,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd11.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD11</a>.
		 */
		ICD_DGNS_VRSN_CD11,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd12.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD12</a>.
		 */
		ICD_DGNS_CD12,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd12.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD12</a>.
		 */
		ICD_DGNS_VRSN_CD12,

		/**
		 * Type: <code>CHAR</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rfr_upin.txt">
		 * CCW Data Dictionary: RFR_UPIN</a>.
		 */
		RFR_PHYSN_UPIN,

		/**
		 * Type: <code>CHAR</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rfr_npi.txt">
		 * CCW Data Dictionary: RFR_NPI</a>.
		 */
		RFR_PHYSN_NPI,

		/**
		 * Type: <code>CHAR</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ccltrnum.txt">
		 * CCW Data Dictionary: CCLTRNUM</a>.
		 */
		CLM_CLNCL_TRIL_NUM,

		/**
		 * Type: <code>NUM</code>, max chars: 13. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/line_num.txt">
		 * CCW Data Dictionary: LINE_NUM</a>.
		 */
		LINE_NUM,

		/**
		 * Type: <code>CHAR</code>, max chars: 10. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/tax_num.txt">
		 * CCW Data Dictionary: TAX_NUM</a>.
		 */
		TAX_NUM,

		/**
		 * Type: <code>CHAR</code>, max chars: 3. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcfaspcl.txt">
		 * CCW Data Dictionary: HCFASPCL</a>.
		 */
		PRVDR_SPCLTY,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prtcptg.txt">
		 * CCW Data Dictionary: PRTCPTG</a>.
		 */
		PRTCPTNG_IND_CD,

		/**
		 * Type: <code>NUM</code>, max chars: 4. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/srvc_cnt.txt">
		 * CCW Data Dictionary: SRVC_CNT</a>.
		 */
		LINE_SRVC_CNT,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/typsrvcb.txt">
		 * CCW Data Dictionary: TYPSRVCB</a>.
		 */
		LINE_CMS_TYPE_SRVC_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 2. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plcsrvc.txt">
		 * CCW Data Dictionary: PLCSRVC</a>.
		 */
		LINE_PLACE_OF_SRVC_CD,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/expnsdt1.txt">
		 * CCW Data Dictionary: EXPNSDT1</a>.
		 */
		LINE_1ST_EXPNS_DT,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/expnsdt2.txt">
		 * CCW Data Dictionary: EXPNSDT2</a>.
		 */
		LINE_LAST_EXPNS_DT,

		/**
		 * Type: <code>CHAR</code>, max chars: 5. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcpcs_cd.txt">
		 * CCW Data Dictionary: HCPCS_CD</a>.
		 */
		HCPCS_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 5 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd1.txt">
		 * CCW Data Dictionary: MDFR_CD1</a>.
		 */
		HCPCS_1ST_MDFR_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 5 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mdfr_cd2.txt">
		 * CCW Data Dictionary: MDFR_CD2</a>.
		 */
		HCPCS_2ND_MDFR_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 3. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/betos.txt">
		 * CCW Data Dictionary: BETOS</a>.
		 */
		BETOS_CD,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/linepmt.txt">
		 * CCW Data Dictionary: LINEPMT</a>.
		 */
		LINE_NCH_PMT_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lbenpmt.txt">
		 * CCW Data Dictionary: LBENPMT</a>.
		 */
		LINE_BENE_PMT_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lprvpmt.txt">
		 * CCW Data Dictionary: LPRVPMT</a>.
		 */
		LINE_PRVDR_PMT_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ldedamt.txt">
		 * CCW Data Dictionary: LDEDAMT</a>.
		 */
		LINE_BENE_PTB_DDCTBL_AMT,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lprpaycd.txt">
		 * CCW Data Dictionary: LPRPAYCD</a>.
		 */
		LINE_BENE_PRMRY_PYR_CD,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lprpdamt.txt">
		 * CCW Data Dictionary: LPRPDAMT</a>.
		 */
		LINE_BENE_PRMRY_PYR_PD_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/coinamt.txt">
		 * CCW Data Dictionary: COINAMT</a>.
		 */
		LINE_COINSRNC_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prpyalow.txt">
		 * CCW Data Dictionary: PRPYALOW</a>.
		 */
		LINE_PRMRY_ALOWD_CHRG_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lsbmtchg.txt">
		 * CCW Data Dictionary: LSBMTCHG</a>.
		 */
		LINE_SBMTD_CHRG_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lalowchg.txt">
		 * CCW Data Dictionary: LALOWCHG</a>.
		 */
		LINE_ALOWD_CHRG_AMT,

		/**
		 * Type: <code>CHAR</code>, max chars: 2. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcngind.txt">
		 * CCW Data Dictionary: PRCNGIND</a>.
		 */
		LINE_PRCSG_IND_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pmtindsw.txt">
		 * CCW Data Dictionary: PMTINDSW</a>.
		 */
		LINE_PMT_80_100_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ded_sw.txt">
		 * CCW Data Dictionary: DED_SW</a>.
		 */
		LINE_SERVICE_DEDUCTIBLE,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/line_icd_dgns_cd.txt">
		 * CCW Data Dictionary: LINE_ICD_DGNS_CD</a>.
		 */
		LINE_ICD_DGNS_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/line_icd_dgns_vrsn_cd.txt">
		 * CCW Data Dictionary: LINE_ICD_DGNS_VRSN_CD</a>.
		 */
		LINE_ICD_DGNS_VRSN_CD,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/dme_purc.txt">
		 * CCW Data Dictionary: DME_PURC</a>.
		 */
		LINE_DME_PRCHS_PRICE_AMT,

		/**
		 * Type: <code>CHAR</code>, max chars: 10. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/suplrnum.txt">
		 * CCW Data Dictionary: SUPLRNUM</a>.
		 */
		PRVDR_NUM,

		/**
		 * Type: <code>CHAR</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/sup_npi.txt">
		 * CCW Data Dictionary: SUP_NPI</a>.
		 */
		PRVDR_NPI,

		/**
		 * Type: <code>CHAR</code>, max chars: 2. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcng_st.txt">
		 * CCW Data Dictionary: PRCNG_ST</a>.
		 */
		DMERC_LINE_PRCNG_STATE_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 2. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prvstate.txt">
		 * CCW Data Dictionary: PRVSTATE</a>.
		 */
		PRVDR_STATE_CD,

		/**
		 * NOT MAPPED
		 */
		DMERC_LINE_SUPPLR_TYPE_CD,

		/**
		 * NOT MAPPED
		 */
		HCPCS_3RD_MDFR_CD,

		/**
		 * NOT MAPPED
		 */
		HCPCS_4TH_MDFR_CD,

		/**
		 * NOT MAPPED
		 */
		DMERC_LINE_SCRN_SVGS_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/dme_unit.txt">
		 * CCW Data Dictionary: DME_UNIT</a>.
		 */
		DMERC_LINE_MTUS_CNT,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/unit_ind.txt">
		 * CCW Data Dictionary: UNIT_IND</a>.
		 */
		DMERC_LINE_MTUS_CD,

		/**
		 * Type: <code>NUM</code>, max chars: 4. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcthgbrs.txt">
		 * CCW Data Dictionary: HCTHGBRS</a>.
		 */
		LINE_HCT_HGB_RSLT_NUM,

		/**
		 * Type: <code>CHAR</code>, max chars: 2 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcthgbtp.txt">
		 * CCW Data Dictionary: HCTHGBTP</a>.
		 */
		LINE_HCT_HGB_TYPE_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 11 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/lnndccd.txt">
		 * CCW Data Dictionary: LNNDCCD</a>.
		 */
		LINE_NDC_CD;

		/**
		 * @return a {@link String} array containing all of the RIF column
		 *         names, in order
		 */
		public static String[] getColumnNames() {
			return Arrays.stream(values()).map(c -> c.name()).collect(Collectors.toList())
					.toArray(new String[values().length]);
		}
	}

	/**
	 * A simple helper app that will print out the {@link Column} entries as if
	 * they were a RIF header line, which can be used to help verify that the
	 * {@link Column} enum is correct.
	 * 
	 * @param args
	 *            (not used)
	 */
	public static void main(String[] args) {
		for (Column col : Column.values())
			System.out.print("|" + col);
	}
}
