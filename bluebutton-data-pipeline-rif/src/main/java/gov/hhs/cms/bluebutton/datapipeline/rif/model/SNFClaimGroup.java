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
 * Models rows from {@link RifFileType#SNF} RIF files. Rows in this file are
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
public final class SNFClaimGroup {

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
	 * @see Column#CLAIM_QUERY_CODE
	 */
	public Character claimQueryCode;

	/**
	 * @see Column#PRVDR_NUM
	 */
	public String providerNumber;

	/**
	 * @see Column#CLM_FAC_TYPE_CD
	 */
	public Character claimFacilityTypeCode;

	/**
	 * @see Column#CLM_SRVC_CLSFCTN_TYPE_CD
	 */
	public Character claimServiceClassificationTypeCode;

	/**
	 * @see Column#CLM_FREQ_CD
	 */
	public Character claimFrequencyCode;

	/**
	 * @see Column#CLM_MDCR_NON_PMT_RSN_CD
	 */
	public Optional<String> claimNonPaymentReasonCode;

	/**
	 * @see Column#CLM_PMT_AMT
	 */
	public BigDecimal paymentAmount;

	/**
	 * @see Column#NCH_PRMRY_PYR_CLM_PD_AMT
	 */
	public BigDecimal primaryPayerPaidAmount;

	/**
	 * @see Column#NCH_PRMRY_PYR_CD
	 */
	public Optional<Character> claimPrimaryPayerCode;

	/**
	 * @see Column#PRVDR_STATE_CD
	 */
	public String providerStateCode;

	/**
	 * @see Column#ORG_NPI_NUM
	 */
	public Optional<String> organizationNpi;

	/**
	 * @see Column#AT_PHYSN_NPI
	 */
	public Optional<String> attendingPhysicianNpi;

	/**
	 * @see Column#OP_PHYSN_NPI
	 */
	public Optional<String> operatingPhysicianNpi;

	/**
	 * @see Column#OT_PHYSN_NPI
	 */
	public Optional<String> otherPhysicianNpi;

	/**
	 * @see Column#CLM_MCO_PD_SW
	 */
	public Optional<Character> mcoPaidSw;

	/**
	 * @see Column#PTNT_DSCHRG_STUS_CD
	 */
	public String patientDischargeStatusCode;

	/**
	 * @see Column#CLM_TOT_CHRG_AMT
	 */
	public BigDecimal totalChargeAmount;

	/**
	 * @see Column#CLM_ADMSN_DT
	 */
	public Optional<LocalDate> claimAdmissionDate;

	/**
	 * @see Column#CLM_IP_ADMSN_TYPE_CD
	 */
	public Character admissionTypeCd;

	/**
	 * @see Column#CLM_SRC_IP_ADMSN_CD
	 */
	public Optional<Character> sourceAdmissionCd;

	/**
	 * @see Column#NCH_PTNT_STATUS_IND_CD
	 */
	public Optional<Character> patientStatusCd;

	/**
	 * @see Column#NCH_BENE_IP_DDCTBL_AMT
	 */
	public BigDecimal deductibleAmount;

	/**
	 * @see Column#NCH_BENE_PTA_COINSRNC_LBLTY_AM
	 */
	public BigDecimal partACoinsuranceLiabilityAmount;

	/**
	 * @see Column#NCH_BENE_BLOOD_DDCTBL_LBLTY_AM
	 */
	public BigDecimal bloodDeductibleLiabilityAmount;

	/**
	 * @see Column#NCH_IP_NCVRD_CHRG_AMT
	 */
	public BigDecimal noncoveredCharge;

	/**
	 * @see Column#NCH_IP_TOT_DDCTN_AMT
	 */
	public BigDecimal totalDeductionAmount;

	/**
	 * @see Column#CLM_PPS_CPTL_FSP_AMT
	 */
	public Optional<BigDecimal> claimPPSCapitalFSPAmount;

	/**
	 * @see Column#CLM_PPS_CPTL_OUTLIER_AMT
	 */
	public Optional<BigDecimal> claimPPSCapitalOutlierAmount;

	/**
	 * @see Column#CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT
	 */
	public Optional<BigDecimal> claimPPSCapitalDisproportionateShareAmt;

	/**
	 * @see Column#CLM_PPS_CPTL_IME_AMT
	 */
	public Optional<BigDecimal> claimPPSCapitalIMEAmount;

	/**
	 * @see Column#CLM_PPS_CPTL_EXCPTN_AMT
	 */
	public Optional<BigDecimal> claimPPSCapitalExceptionAmount;

	/**
	 * @see Column#CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT
	 */
	public Optional<BigDecimal> claimPPSOldCapitalHoldHarmlessAmount;

	/**
	 * @see Column#CLM_UTLZTN_DAY_CNT
	 */
	public Integer utilizationDayCount;

	/**
	 * @see Column#BENE_TOT_COINSRNC_DAYS_CNT
	 */
	public Integer coinsuranceDayCount;

	/**
	 * @see Column#CLM_NON_UTLZTN_DAYS_CNT
	 */
	public Integer nonUtilizationDayCount;

	/**
	 * @see Column#NCH_BLOOD_PNTS_FRNSHD_QTY
	 */
	public Integer bloodPintsFurnishedQty;

	/**
	 * @see Column#NCH_QLFYD_STAY_FROM_DT
	 */
	public Optional<LocalDate> qualifiedStayFromDate;

	/**
	 * @see Column#NCH_QLFYD_STAY_THRU_DT
	 */
	public Optional<LocalDate> qualifiedStayThroughDate;

	/**
	 * @see Column#NCH_VRFD_NCVRD_STAY_FROM_DT
	 */
	public Optional<LocalDate> noncoveredStayFromDate;

	/**
	 * @see Column#NCH_VRFD_NCVRD_STAY_THRU_DT
	 */
	public Optional<LocalDate> noncoveredStayThroughDate;

	/**
	 * @see Column#NCH_ACTV_OR_CVRD_LVL_CARE_THRU
	 */
	public Optional<LocalDate> coveredCareThoughDate;

	/**
	 * @see Column#NCH_BENE_MDCR_BNFTS_EXHTD_DT_I
	 */
	public Optional<LocalDate> medicareBenefitsExhaustedDate;

	/**
	 * @see Column#NCH_BENE_DSCHRG_DT
	 */
	public Optional<LocalDate> beneficiaryDischargeDate;

	/**
	 * @see Column#CLM_DRG_CD
	 */
	public Optional<String> diagnosisRelatedGroupCd;

	/**
	 * @see Column#ADMTG_DGNS_CD
	 * @see Column#ADMTG_DGNS_VRSN_CD
	 */
	public Optional<IcdCode> diagnosisAdmitting;

	/**
	 * @see Column#PRNCPAL_DGNS_CD
	 * @see Column#PRNCPAL_DGNS_VRSN_CD
	 */
	public IcdCode diagnosisPrincipal;

	/**
	 * See {@link Column#ICD_DGNS_CD1} through {@link Column#ICD_DGNS_CD25} and
	 * {@link Column#ICD_DGNS_VRSN_CD1} through
	 * {@link Column#ICD_DGNS_VRSN_CD25}.
	 */
	public List<IcdCode> diagnosesAdditional = new LinkedList<>();

	/**
	 * @see Column#FST_DGNS_E_CD
	 * @see Column#FST_DGNS_E_VRSN_CD
	 */
	public Optional<IcdCode> diagnosisFirstClaimExternal;

	/**
	 * See {@link Column#ICD_DGNS_E_CD1} through {@link Column#ICD_DGNS_E_CD12}
	 * and {@link Column#ICD_DGNS_E_VRSN_CD1} through
	 * {@link Column#ICD_DGNS_E_VRSN_CD12}.
	 */
	public List<IcdCode> diagnosesExternal = new LinkedList<>();

	/**
	 * See {@link Column#ICD_PRCDR_CD1} through {@link Column#ICD_PRCDR_CD25}
	 * and {@link Column#ICD_PRCDR_VRSN_CD1} through
	 * {@link Column#ICD_PRCDR_VRSN_CD25} and {@link Column#PRCDR_DT1} through
	 * {@link Column#PRCDR_DT25}.
	 */
	public List<IcdCode> procedureCodes = new LinkedList<>();

	/**
	 * Represents the data contained in {@link Column#CLM_LINE_NUM} and
	 * subsequent columns: one entry for every "claim line" in the claim
	 * represented by this {@link SNFClaimGroup} instance.
	 */
	public List<SNFClaimLine> lines = new LinkedList<>();

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SNFClaimGroup [version=");
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
		builder.append(", claimQueryCode=");
		builder.append(claimQueryCode);
		builder.append(", providerNumber=");
		builder.append(providerNumber);
		builder.append(", claimFacilityTypeCode=");
		builder.append(claimFacilityTypeCode);
		builder.append(", claimServiceClassificationTypeCode=");
		builder.append(claimServiceClassificationTypeCode);
		builder.append(", claimFrequencyCode=");
		builder.append(claimFrequencyCode);
		builder.append(", claimNonPaymentReasonCode=");
		builder.append(claimNonPaymentReasonCode);
		builder.append(", paymentAmount=");
		builder.append(paymentAmount);
		builder.append(", primaryPayerPaidAmount=");
		builder.append(primaryPayerPaidAmount);
		builder.append(", claimPrimaryPayerCode=");
		builder.append(claimPrimaryPayerCode);
		builder.append(", providerStateCode=");
		builder.append(providerStateCode);
		builder.append(", organizationNpi=");
		builder.append(organizationNpi);
		builder.append(", attendingPhysicianNpi=");
		builder.append(attendingPhysicianNpi);
		builder.append(", operatingPhysicianNpi=");
		builder.append(operatingPhysicianNpi);
		builder.append(", otherPhysicianNpi=");
		builder.append(otherPhysicianNpi);
		builder.append(", mcoPaidSw=");
		builder.append(mcoPaidSw);
		builder.append(", patientDischargeStatusCode=");
		builder.append(patientDischargeStatusCode);
		builder.append(", totalChargeAmount=");
		builder.append(totalChargeAmount);
		builder.append(", claimAdmissionDate=");
		builder.append(claimAdmissionDate);
		builder.append(", admissionTypeCd=");
		builder.append(admissionTypeCd);
		builder.append(", sourceAdmissionCd=");
		builder.append(sourceAdmissionCd);
		builder.append(", patientStatusCd=");
		builder.append(patientStatusCd);
		builder.append(", deductibleAmount=");
		builder.append(deductibleAmount);
		builder.append(", partACoinsuranceLiabilityAmount=");
		builder.append(partACoinsuranceLiabilityAmount);
		builder.append(", bloodDeductibleLiabilityAmount=");
		builder.append(bloodDeductibleLiabilityAmount);
		builder.append(", noncoveredCharge=");
		builder.append(noncoveredCharge);
		builder.append(", totalDeductionAmount=");
		builder.append(totalDeductionAmount);
		builder.append(", claimPPSCapitalFSPAmount=");
		builder.append(claimPPSCapitalFSPAmount);
		builder.append(", claimPPSCapitalOutlierAmount=");
		builder.append(claimPPSCapitalOutlierAmount);
		builder.append(", claimPPSCapitalDisproportionateShareAmt=");
		builder.append(claimPPSCapitalDisproportionateShareAmt);
		builder.append(", claimPPSCapitalIMEAmount=");
		builder.append(claimPPSCapitalIMEAmount);
		builder.append(", claimPPSCapitalExceptionAmount=");
		builder.append(claimPPSCapitalExceptionAmount);
		builder.append(", claimPPSOldCapitalHoldHarmlessAmount=");
		builder.append(claimPPSOldCapitalHoldHarmlessAmount);
		builder.append(", utilizationDayCount=");
		builder.append(utilizationDayCount);
		builder.append(", coinsuranceDayCount=");
		builder.append(coinsuranceDayCount);
		builder.append(", nonUtilizationDayCount=");
		builder.append(nonUtilizationDayCount);
		builder.append(", bloodPintsFurnishedQty=");
		builder.append(bloodPintsFurnishedQty);
		builder.append(", qualifiedStayFromDate=");
		builder.append(qualifiedStayFromDate);
		builder.append(", qualifiedStayThroughDate=");
		builder.append(qualifiedStayThroughDate);
		builder.append(", noncoveredStayFromDate=");
		builder.append(noncoveredStayFromDate);
		builder.append(", noncoveredStayThroughDate=");
		builder.append(noncoveredStayThroughDate);
		builder.append(", coveredCareThoughDate=");
		builder.append(coveredCareThoughDate);
		builder.append(", medicareBenefitsExhaustedDate=");
		builder.append(medicareBenefitsExhaustedDate);
		builder.append(", beneficiaryDischargeDate=");
		builder.append(beneficiaryDischargeDate);
		builder.append(", diagnosisRelatedGroupCd=");
		builder.append(diagnosisRelatedGroupCd);
		builder.append(", diagnosisAdmitting=");
		builder.append(diagnosisAdmitting);
		builder.append(", diagnosisPrincipal=");
		builder.append(diagnosisPrincipal);
		builder.append(", diagnosesAdditional=");
		builder.append(diagnosesAdditional);
		builder.append(", diagnosisFirstClaimExternal=");
		builder.append(diagnosisFirstClaimExternal);
		builder.append(", diagnosesExternal=");
		builder.append(diagnosesExternal);
		builder.append(", procedureCodes=");
		builder.append(procedureCodes);
		builder.append(", lines=");
		builder.append(lines);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Models individual claim lines within a {@link SNFClaimGroup} instance.
	 */
	public static final class SNFClaimLine {

		/**
		 * @see Column#CLM_LINE_NUM
		 */
		public Integer lineNumber;

		/**
		 * @see Column#REV_CNTR
		 */
		public String revenueCenter;

		/**
		 * @see Column#HCPCS_CD
		 */
		public Optional<String> hcpcsCode;

		/**
		 * @see Column#REV_CNTR_UNIT_CNT
		 */
		public BigDecimal unitCount;

		/**
		 * @see Column#REV_CNTR_RATE_AMT
		 */
		public BigDecimal rateAmount;

		/**
		 * @see Column#REV_CNTR_TOT_CHRG_AMT
		 */
		public BigDecimal totalChargeAmount;

		/**
		 * @see Column#REV_CNTR_NCVRD_CHRG_AMT
		 */
		public BigDecimal nonCoveredChargeAmount;

		/**
		 * @see Column#REV_CNTR_DDCTBL_COINSRNC_CD
		 */
		public Optional<Character> deductibleCoinsuranceCd;

		/**
		 * @see Column#REV_CNTR_NDC_QTY
		 */
		public Optional<BigDecimal> nationalDrugCodeQuantity;

		/**
		 * @see Column#REV_CNTR_NDC_QTY_QLFR_CD
		 */
		public Optional<String> nationalDrugCodeQualifierCode;

		/**
		 * @see Column#RNDRNG_PHYSN_NPI
		 */
		public Optional<String> revenueCenterRenderingPhysicianNPI;

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("SNFClaimLine [lineNumber=");
			builder.append(lineNumber);
			builder.append(", revenueCenter=");
			builder.append(revenueCenter);
			builder.append(", hcpcsCode=");
			builder.append(hcpcsCode);
			builder.append(", unitCount=");
			builder.append(unitCount);
			builder.append(", rateAmount=");
			builder.append(rateAmount);
			builder.append(", totalChargeAmount=");
			builder.append(totalChargeAmount);
			builder.append(", nonCoveredChargeAmount=");
			builder.append(nonCoveredChargeAmount);
			builder.append(", deductibleCoinsuranceCd=");
			builder.append(deductibleCoinsuranceCd);
			builder.append(", nationalDrugCodeQuantity=");
			builder.append(nationalDrugCodeQuantity);
			builder.append(", nationalDrugCodeQualifierCode=");
			builder.append(nationalDrugCodeQualifierCode);
			builder.append(", revenueCenterRenderingPhysicianNPI=");
			builder.append(revenueCenterRenderingPhysicianNPI);
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * Enumerates the raw RIF columns represented in {@link SNFClaimGroup},
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
		 * NOT MAPPED
		 */
		FI_CLM_PROC_DT,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/query_cd.txt">
		 * CCW Data Dictionary: QUERY_CD</a>.
		 */
		CLAIM_QUERY_CODE,

		/**
		 * Type: <code>CHAR</code>, max chars: 6. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/provider.txt">
		 * CCW Data Dictionary: PRVDR_NUM</a>.
		 */
		PRVDR_NUM,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/fac_type.txt">
		 * CCW Data Dictionary: FAC_TYPE</a>.
		 */
		CLM_FAC_TYPE_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/typesrvc.txt">
		 * CCW Data Dictionary: TYPESRVC</a>.
		 */
		CLM_SRVC_CLSFCTN_TYPE_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/freq_cd.txt">
		 * CCW Data Dictionary: FREQ_CD</a>.
		 */
		CLM_FREQ_CD,

		/**
		 * NOT MAPPED
		 */
		FI_NUM,

		/**
		 * Type: <code>CHAR</code>, max chars: 2 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/nopay_cd.txt">
		 * CCW Data Dictionary: NOPAY_CD</a>.
		 */
		CLM_MDCR_NON_PMT_RSN_CD,

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
		NCH_PRMRY_PYR_CLM_PD_AMT,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prpay_cd.txt">
		 * CCW Data Dictionary: PRPAY_CD</a>.
		 */
		NCH_PRMRY_PYR_CD,

		/**
		 * NOT MAPPED
		 */
		FI_CLM_ACTN_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 2. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prstate.txt">
		 * CCW Data Dictionary: PRSTATE</a>.
		 */
		PRVDR_STATE_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 10 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/orgnpinm.txt">
		 * CCW Data Dictionary: ORGNPINM</a>.
		 */
		ORG_NPI_NUM,

		/**
		 * NOT MAPPED
		 */
		AT_PHYSN_UPIN,

		/**
		 * Type: <code>CHAR</code>, max chars: 12 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/at_npi.txt">
		 * CCW Data Dictionary: AT_NPI</a>.
		 */
		AT_PHYSN_NPI,

		/**
		 * NOT MAPPED
		 */
		OP_PHYSN_UPIN,

		/**
		 * Type: <code>CHAR</code>, max chars: 12 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/op_npi.txt">
		 * CCW Data Dictionary: OP_NPI</a>.
		 */
		OP_PHYSN_NPI,

		/**
		 * NOT MAPPED
		 */
		OT_PHYSN_UPIN,

		/**
		 * Type: <code>CHAR</code>, max chars: 12 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ot_npi.txt">
		 * CCW Data Dictionary: OT_NPI</a>.
		 */
		OT_PHYSN_NPI,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/mcopdsw.txt">
		 * CCW Data Dictionary: MCOPDSW</a>.
		 */
		CLM_MCO_PD_SW,

		/**
		 * Type: <code>CHAR</code>, max chars: 2. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/stus_cd.txt">
		 * CCW Data Dictionary: STUS_CD</a>.
		 */
		PTNT_DSCHRG_STUS_CD,

		/**
		 * NOT MAPPED
		 */
		CLM_PPS_IND_CD,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/tot_chrg.txt">
		 * CCW Data Dictionary: TOT_CHRG</a>.
		 */
		CLM_TOT_CHRG_AMT,

		/**
		 * Type: <code>DATE</code>, max chars: 8 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/admsn_dt.txt">
		 * CCW Data Dictionary: ADMSN_DT</a>.
		 */
		CLM_ADMSN_DT,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/type_adm.txt">
		 * CCW Data Dictionary: TYPE_ADM</a>.
		 */
		CLM_IP_ADMSN_TYPE_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/src_adms.txt">
		 * CCW Data Dictionary: SRC_ADMS</a>.
		 */
		CLM_SRC_IP_ADMSN_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ptntstus.txt">
		 * CCW Data Dictionary: PTNTSTUS</a>.
		 */
		NCH_PTNT_STATUS_IND_CD,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ded_amt.txt">
		 * CCW Data Dictionary: DED_AMT</a>.
		 */
		NCH_BENE_IP_DDCTBL_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/coin_amt.txt">
		 * CCW Data Dictionary: COIN_AMT</a>.
		 */
		NCH_BENE_PTA_COINSRNC_LBLTY_AM,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/blddedam.txt">
		 * CCW Data Dictionary: BLDDEDAM</a>.
		 */
		NCH_BENE_BLOOD_DDCTBL_LBLTY_AM,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ncchgamt.txt">
		 * CCW Data Dictionary: NCCHGAMT </a>.
		 */
		NCH_IP_NCVRD_CHRG_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/tdedamt.txt">
		 * CCW Data Dictionary: TDEDAMT </a>.
		 */
		NCH_IP_TOT_DDCTN_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cptl_fsp.txt">
		 * CCW Data Dictionary: CPTLOUTL </a>.
		 */
		CLM_PPS_CPTL_FSP_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cptloutl.txt">
		 * CCW Data Dictionary: CPTL_FSP </a>.
		 */
		CLM_PPS_CPTL_OUTLIER_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/disp_shr.txt">
		 * CCW Data Dictionary: DISP_SHR </a>.
		 */
		CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ime_amt.txt">
		 * CCW Data Dictionary: IME_AMT</a>.
		 */
		CLM_PPS_CPTL_IME_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cptl_exp.txt">
		 * CCW Data Dictionary: CPTL_EXP</a>.
		 */
		CLM_PPS_CPTL_EXCPTN_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hldhrmls.txt">
		 * CCW Data Dictionary: HLDHRMLS</a>.
		 */
		CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 3. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/util_day.txt">
		 * CCW Data Dictionary: UTIL_DAY</a>.
		 */
		CLM_UTLZTN_DAY_CNT,

		/**
		 * Type: <code>NUM</code>, max chars: 3. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/coin_day.txt">
		 * CCW Data Dictionary: COIN_DAY</a>.
		 */
		BENE_TOT_COINSRNC_DAYS_CNT,

		/**
		 * Type: <code>NUM</code>, max chars: 5. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/nutilday.txt">
		 * CCW Data Dictionary: NUTILDAY</a>.
		 */
		CLM_NON_UTLZTN_DAYS_CNT,

		/**
		 * Type: <code>NUM</code>, max chars: 3. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/bldfrnsh.txt">
		 * CCW Data Dictionary: BLDFRNSH</a>.
		 */
		NCH_BLOOD_PNTS_FRNSHD_QTY,

		/**
		 * Type: <code>DATE</code>, max chars: 8 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/qlfyfrom.txt">
		 * CCW Data Dictionary: QLFYFROM</a>.
		 */
		NCH_QLFYD_STAY_FROM_DT,

		/**
		 * Type: <code>DATE</code>, max chars: 8 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/qlfythru.txt">
		 * CCW Data Dictionary: QLFYTHRU</a>.
		 */
		NCH_QLFYD_STAY_THRU_DT,

		/**
		 * Type: <code>DATE</code>, max chars: 8 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ncovfrom.txt">
		 * CCW Data Dictionary: NCOVFROM</a>.
		 */
		NCH_VRFD_NCVRD_STAY_FROM_DT,

		/**
		 * Type: <code>DATE</code>, max chars: 8 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ncovthru.txt">
		 * CCW Data Dictionary: NCOVTHRU</a>.
		 */
		NCH_VRFD_NCVRD_STAY_THRU_DT,

		/**
		 * Type: <code>DATE</code>, max chars: 8 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/carethru.txt">
		 * CCW Data Dictionary: CARETHRU</a>.
		 */
		NCH_ACTV_OR_CVRD_LVL_CARE_THRU,

		/**
		 * Type: <code>DATE</code>, max chars: 8 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/exhst_.txt">
		 * CCW Data Dictionary: EXHST_DT</a>.
		 */
		NCH_BENE_MDCR_BNFTS_EXHTD_DT_I,

		/**
		 * Type: <code>DATE</code>, max chars: 8 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/dschrgdt.txt">
		 * CCW Data Dictionary: DSCHRGDT</a>.
		 */
		NCH_BENE_DSCHRG_DT,

		/**
		 * Type: <code>CHAR</code>, max chars: 3 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/drg_cd.txt">
		 * CCW Data Dictionary: DRG_CD</a>.
		 */
		CLM_DRG_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/admtg_dgns_cd.txt">
		 * CCW Data Dictionary: ADMTG_DGNS_CD</a>.
		 */
		ADMTG_DGNS_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/admtg_dgns_vrsn_cd.txt">
		 * CCW Data Dictionary: ADMTG_DGNS_VRSN_CD</a>.
		 */
		ADMTG_DGNS_VRSN_CD,

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
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd13.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD13</a>.
		 */
		ICD_DGNS_CD13,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd13.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD13</a>.
		 */
		ICD_DGNS_VRSN_CD13,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd14.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD14</a>.
		 */
		ICD_DGNS_CD14,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd14.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD14</a>.
		 */
		ICD_DGNS_VRSN_CD14,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd15.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD15</a>.
		 */
		ICD_DGNS_CD15,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd15.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD15</a>.
		 */
		ICD_DGNS_VRSN_CD15,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd16.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD16</a>.
		 */
		ICD_DGNS_CD16,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd16.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD16</a>.
		 */
		ICD_DGNS_VRSN_CD16,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd17.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD17</a>.
		 */
		ICD_DGNS_CD17,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd17.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD17</a>.
		 */
		ICD_DGNS_VRSN_CD17,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd18.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD18</a>.
		 */
		ICD_DGNS_CD18,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd18.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD18</a>.
		 */
		ICD_DGNS_VRSN_CD18,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd19.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD19</a>.
		 */
		ICD_DGNS_CD19,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd19.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD19</a>.
		 */
		ICD_DGNS_VRSN_CD19,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd20.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD20</a>.
		 */
		ICD_DGNS_CD20,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd20.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD20</a>.
		 */
		ICD_DGNS_VRSN_CD20,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd21.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD21</a>.
		 */
		ICD_DGNS_CD21,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd21.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD21</a>.
		 */
		ICD_DGNS_VRSN_CD21,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd22.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD22</a>.
		 */
		ICD_DGNS_CD22,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd22.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD22</a>.
		 */
		ICD_DGNS_VRSN_CD22,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd23.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD23</a>.
		 */
		ICD_DGNS_CD23,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd23.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD23</a>.
		 */
		ICD_DGNS_VRSN_CD23,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd24.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD24</a>.
		 */
		ICD_DGNS_CD24,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd24.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD24</a>.
		 */
		ICD_DGNS_VRSN_CD24,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_cd25.txt">
		 * CCW Data Dictionary: ICD_DGNS_CD25</a>.
		 */
		ICD_DGNS_CD25,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_cd25.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_CD25</a>.
		 */
		ICD_DGNS_VRSN_CD25,

		/**
		 * Type: <code>CHAR</code>, max chars: 7 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/fst_dgns_e_cd.txt">
		 * CCW Data Dictionary: FST_DGNS_E_CD</a>.
		 */
		FST_DGNS_E_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/fst_dgns_e_vrsn_cd.txt">
		 * CCW Data Dictionary: FST_DGNS_E_VRSN_CD</a>.
		 */
		FST_DGNS_E_VRSN_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 7 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_cd1.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_CD1</a>.
		 */
		ICD_DGNS_E_CD1,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_vrsn_cd1.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_VRSN_CD1</a>.
		 */
		ICD_DGNS_E_VRSN_CD1,

		/**
		 * Type: <code>CHAR</code>, max chars: 7 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_cd2.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_CD2</a>.
		 */
		ICD_DGNS_E_CD2,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_vrsn_cd2.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_VRSN_CD2</a>.
		 */
		ICD_DGNS_E_VRSN_CD2,

		/**
		 * Type: <code>CHAR</code>, max chars: 7 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_cd3.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_CD3</a>.
		 */
		ICD_DGNS_E_CD3,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_vrsn_cd3.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_VRSN_CD3</a>.
		 */
		ICD_DGNS_E_VRSN_CD3,

		/**
		 * Type: <code>CHAR</code>, max chars: 7 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_cd4.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_CD4</a>.
		 */
		ICD_DGNS_E_CD4,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_vrsn_cd4.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_VRSN_CD4</a>.
		 */
		ICD_DGNS_E_VRSN_CD4,

		/**
		 * Type: <code>CHAR</code>, max chars: 7 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_cd5.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_CD5</a>.
		 */
		ICD_DGNS_E_CD5,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_vrsn_cd5.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_VRSN_CD5</a>.
		 */
		ICD_DGNS_E_VRSN_CD5,

		/**
		 * Type: <code>CHAR</code>, max chars: 7 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_cd6.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_CD6</a>.
		 */
		ICD_DGNS_E_CD6,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_vrsn_cd6.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_VRSN_CD6</a>.
		 */
		ICD_DGNS_E_VRSN_CD6,

		/**
		 * Type: <code>CHAR</code>, max chars: 7 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_cd7.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_CD7</a>.
		 */
		ICD_DGNS_E_CD7,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_vrsn_cd7.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_VRSN_CD7</a>.
		 */
		ICD_DGNS_E_VRSN_CD7,

		/**
		 * Type: <code>CHAR</code>, max chars: 7 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_cd8.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_CD8</a>.
		 */
		ICD_DGNS_E_CD8,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_vrsn_cd8.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_VRSN_CD8</a>.
		 */
		ICD_DGNS_E_VRSN_CD8,

		/**
		 * Type: <code>CHAR</code>, max chars: 7 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_cd9.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_CD9</a>.
		 */
		ICD_DGNS_E_CD9,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_vrsn_cd9.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_VRSN_CD9</a>.
		 */
		ICD_DGNS_E_VRSN_CD9,

		/**
		 * Type: <code>CHAR</code>, max chars: 7 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_cd10.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_CD10</a>.
		 */
		ICD_DGNS_E_CD10,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_vrsn_cd10.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_VRSN_CD10</a>.
		 */
		ICD_DGNS_E_VRSN_CD10,

		/**
		 * Type: <code>CHAR</code>, max chars: 7 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_cd11.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_CD11</a>.
		 */
		ICD_DGNS_E_CD11,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_vrsn_cd11.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_VRSN_CD11</a>.
		 */
		ICD_DGNS_E_VRSN_CD11,

		/**
		 * Type: <code>CHAR</code>, max chars: 7 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_cd12.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_CD12</a>.
		 */
		ICD_DGNS_E_CD12,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_e_vrsn_cd12.txt">
		 * CCW Data Dictionary: ICD_DGNS_E_VRSN_CD12</a>.
		 */
		ICD_DGNS_E_VRSN_CD12,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd1.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD1</a>.
		 */
		ICD_PRCDR_CD1,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd1.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD1</a>.
		 */
		ICD_PRCDR_VRSN_CD1,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt1.txt">
		 * CCW Data Dictionary: PRCDR_DT1</a>.
		 */
		PRCDR_DT1,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd2.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD2</a>.
		 */
		ICD_PRCDR_CD2,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd2.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD2</a>.
		 */
		ICD_PRCDR_VRSN_CD2,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt2.txt">
		 * CCW Data Dictionary: PRCDR_DT2</a>.
		 */
		PRCDR_DT2,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd3.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD3</a>.
		 */
		ICD_PRCDR_CD3,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd3.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD3</a>.
		 */
		ICD_PRCDR_VRSN_CD3,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt3.txt">
		 * CCW Data Dictionary: PRCDR_DT3</a>.
		 */
		PRCDR_DT3,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd4.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD4</a>.
		 */
		ICD_PRCDR_CD4,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd4.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD4</a>.
		 */
		ICD_PRCDR_VRSN_CD4,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt4.txt">
		 * CCW Data Dictionary: PRCDR_DT4</a>.
		 */
		PRCDR_DT4,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd5.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD5</a>.
		 */
		ICD_PRCDR_CD5,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd5.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD5</a>.
		 */
		ICD_PRCDR_VRSN_CD5,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt5.txt">
		 * CCW Data Dictionary: PRCDR_DT5</a>.
		 */
		PRCDR_DT5,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd6.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD6</a>.
		 */
		ICD_PRCDR_CD6,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd6.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD6</a>.
		 */
		ICD_PRCDR_VRSN_CD6,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt6.txt">
		 * CCW Data Dictionary: PRCDR_DT6</a>.
		 */
		PRCDR_DT6,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd7.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD7</a>.
		 */
		ICD_PRCDR_CD7,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd7.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD7</a>.
		 */
		ICD_PRCDR_VRSN_CD7,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt7.txt">
		 * CCW Data Dictionary: PRCDR_DT7</a>.
		 */
		PRCDR_DT7,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd8.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD8</a>.
		 */
		ICD_PRCDR_CD8,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd8.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD8</a>.
		 */
		ICD_PRCDR_VRSN_CD8,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt8.txt">
		 * CCW Data Dictionary: PRCDR_DT8</a>.
		 */
		PRCDR_DT8,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd9.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD9</a>.
		 */
		ICD_PRCDR_CD9,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd9.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD9</a>.
		 */
		ICD_PRCDR_VRSN_CD9,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt9.txt">
		 * CCW Data Dictionary: PRCDR_DT9</a>.
		 */
		PRCDR_DT9,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd10.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD10</a>.
		 */
		ICD_PRCDR_CD10,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd10.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD10</a>.
		 */
		ICD_PRCDR_VRSN_CD10,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt10.txt">
		 * CCW Data Dictionary: PRCDR_DT10</a>.
		 */
		PRCDR_DT10,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd11.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD11</a>.
		 */
		ICD_PRCDR_CD11,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd11.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD11</a>.
		 */
		ICD_PRCDR_VRSN_CD11,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt11.txt">
		 * CCW Data Dictionary: PRCDR_DT11</a>.
		 */
		PRCDR_DT11,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd12.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD12</a>.
		 */
		ICD_PRCDR_CD12,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd12.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD12</a>.
		 */
		ICD_PRCDR_VRSN_CD12,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt12.txt">
		 * CCW Data Dictionary: PRCDR_DT12</a>.
		 */
		PRCDR_DT12,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd13.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD13</a>.
		 */
		ICD_PRCDR_CD13,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd13.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD13</a>.
		 */
		ICD_PRCDR_VRSN_CD13,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt13.txt">
		 * CCW Data Dictionary: PRCDR_DT13</a>.
		 */
		PRCDR_DT13,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd14.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD14</a>.
		 */
		ICD_PRCDR_CD14,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd14.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD14</a>.
		 */
		ICD_PRCDR_VRSN_CD14,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt14.txt">
		 * CCW Data Dictionary: PRCDR_DT14</a>.
		 */
		PRCDR_DT14,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd15.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD15</a>.
		 */
		ICD_PRCDR_CD15,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd15.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD15</a>.
		 */
		ICD_PRCDR_VRSN_CD15,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt15.txt">
		 * CCW Data Diction ary: PRCDR_DT15</a>.
		 */
		PRCDR_DT15,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd16.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD16</a>.
		 */
		ICD_PRCDR_CD16,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd16.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD16</a>.
		 */
		ICD_PRCDR_VRSN_CD16,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt16.txt">
		 * CCW Data Dictionary: PRCDR_DT16</a>.
		 */
		PRCDR_DT16,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd17.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD17</a>.
		 */
		ICD_PRCDR_CD17,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd17.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD17</a>.
		 */
		ICD_PRCDR_VRSN_CD17,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt17.txt">
		 * CCW Data Dictionary: PRCDR_DT17</a>.
		 */
		PRCDR_DT17,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd18.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD18</a>.
		 */
		ICD_PRCDR_CD18,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd18.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD18</a>.
		 */
		ICD_PRCDR_VRSN_CD18,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt18.txt">
		 * CCW Data Dictionary: PRCDR_DT18</a>.
		 */
		PRCDR_DT18,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd19.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD19</a>.
		 */
		ICD_PRCDR_CD19,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd19.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD19</a>.
		 */
		ICD_PRCDR_VRSN_CD19,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt19.txt">
		 * CCW Data Dictionary: PRCDR_DT19</a>.
		 */
		PRCDR_DT19,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd20.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD20</a>.
		 */
		ICD_PRCDR_CD20,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd20.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD20</a>.
		 */
		ICD_PRCDR_VRSN_CD20,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt20.txt">
		 * CCW Data Dictionary: PRCDR_DT20</a>.
		 */
		PRCDR_DT20,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd21.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD21</a>.
		 */
		ICD_PRCDR_CD21,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd21.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD21</a>.
		 */
		ICD_PRCDR_VRSN_CD21,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt21.txt">
		 * CCW Data Dictionary: PRCDR_DT21</a>.
		 */
		PRCDR_DT21,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd22.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD22</a>.
		 */
		ICD_PRCDR_CD22,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd22.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD22</a>.
		 */
		ICD_PRCDR_VRSN_CD22,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt22.txt">
		 * CCW Data Dictionary: PRCDR_DT22</a>.
		 */
		PRCDR_DT22,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd23.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD23</a>.
		 */
		ICD_PRCDR_CD23,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd23.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD23</a>.
		 */
		ICD_PRCDR_VRSN_CD23,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt23.txt">
		 * CCW Data Dictionary: PRCDR_DT23</a>.
		 */
		PRCDR_DT23,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd24.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD24</a>.
		 */
		ICD_PRCDR_CD24,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd24.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD24</a>.
		 */
		ICD_PRCDR_VRSN_CD24,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt24.txt">
		 * CCW Data Dictionary: PRCDR_DT24</a>.
		 */
		PRCDR_DT24,

		/**
		 * Type: <code>CHAR</code>, max chars: 7. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_cd25.txt">
		 * CCW Data Dictionary: ICD_PRCDR_CD25</a>.
		 */
		ICD_PRCDR_CD25,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_prcdr_vrsn_cd25.txt">
		 * CCW Data Dictionary: ICD_PRCDR_VRSN_CD25</a>.
		 */
		ICD_PRCDR_VRSN_CD25,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prcdr_dt25.txt">
		 * CCW Data Dictionary: PRCDR_DT25</a>.
		 */
		PRCDR_DT25,


		/**
		 * Type: <code>NUM</code>, max chars: 13. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_ln.txt">
		 * CCW Data Dictionary: CLM_LN</a>.
		 */
		CLM_LINE_NUM,

		/**
		 * Type: <code>CHAR</code>, max chars: 4 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_cntr.txt">
		 * CCW Data Dictionary: REV_CNTR</a>.
		 */
		REV_CNTR,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcpcs_cd.txt">
		 * CCW Data Dictionary: HCPCS_CD</a>.
		 */
		HCPCS_CD,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_unit.txt">
		 * CCW Data Dictionary: REV_UNIT </a>.
		 */
		REV_CNTR_UNIT_CNT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_rate.txt">
		 * CCW Data Dictionary: REV_RATE </a>.
		 */
		REV_CNTR_RATE_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_chrg.txt">
		 * CCW Data Dictionary: REV_CHRG </a>.
		 */
		REV_CNTR_TOT_CHRG_AMT,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_ncvr.txt">
		 * CCW Data Dictionary: REV_NCVR </a>.
		 */
		REV_CNTR_NCVRD_CHRG_AMT,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/revdedcd.txt">
		 * CCW Data Dictionary: REVDEDCD</a>.
		 */
		REV_CNTR_DDCTBL_COINSRNC_CD,

		/**
		 * Type: <code>NUM</code>, max chars: 10 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_cntr_ndc_qty.txt">
		 * CCW Data Dictionary: REV_CNTR_NDC_QTY</a>.
		 */
		REV_CNTR_NDC_QTY,

		/**
		 * Type: <code>CHAR</code>, max chars: 2 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rev_cntr_ndc_qty_qlfr_cd.txt">
		 * CCW Data Dictionary: REV_CNTR_NDC_QTY_QLFR_CD</a>.
		 */
		REV_CNTR_NDC_QTY_QLFR_CD,

		/**
		 * NOT MAPPED
		 */
		RNDRNG_PHYSN_UPIN,

		/**
		 * Type: <code>String</code>, max chars: NA. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rndrng_physn_npi.txt">
		 * CCW Data Dictionary: RNDRNG_PHYSN_NPI</a>.
		 */
		RNDRNG_PHYSN_NPI;


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
