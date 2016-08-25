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
 * Models rows from {@link RifFileType#INPATIENT} RIF files. Rows in this file
 * are grouped, such that there is one group per claim, with multiple rows: one
 * for each claim line.
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
public final class InpatientClaimGroup {

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
	 * @see Column#PRVDR_STATE_CD
	 */
	public String providerStateCode;

	/**
	 * @see Column#ORG_NPI_NUM
	 */
	public String organizationNpi;

	/**
	 * @see Column#AT_PHYSN_NPI
	 */
	public String attendingPhysicianNpi;

	/**
	 * @see Column#OP_PHYSN_NPI
	 */
	public String operatingPhysicianNpi;

	/**
	 * @see Column#OT_PHYSN_NPI
	 */
	public String otherPhysicianNpi;

	/**
	 * @see Column#PTNT_DSCHRG_STUS_CD
	 */
	public String patientDischargeStatusCode;

	/**
	 * @see Column#CLM_TOT_CHRG_AMT
	 */
	public BigDecimal totalChargeAmount;


	/**
	 * @see Column#CLM_PASS_THRU_PER_DIEM_AMT
	 */
	public BigDecimal passThruPerDiemAmount;

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
	 * @see Column#NCH_PROFNL_CMPNT_CHRG_AMT
	 */
	public BigDecimal professionalComponentCharge;

	/**
	 * @see Column#NCH_IP_NCVRD_CHRG_AMT
	 */
	public BigDecimal noncoveredCharge;

	/**
	 * @see Column#NCH_IP_TOT_DDCTN_AMT
	 */
	public BigDecimal totalDeductionAmount;

	/**
	 * @see Column#ADMTG_DGNS_CD
	 * @see Column#ADMTG_DGNS_VRSN_CD
	 */
	public IcdCode diagnosisAdmitting;

	/**
	 * @see Column#PRNCPAL_DGNS_CD
	 * @see Column#PRNCPAL_DGNS_VRSN_CD
	 */
	public IcdCode diagnosisPrincipal;

	/**
	 * See {@link Column#ICD_DGNS_CD1} through {@link Column#ICD_DGNS_CD25} and
	 * {@link Column#ICD_DGNS_VRSN_CD1} through
	 * {@link Column#ICD_DGNS_VRSN_CD25} and {@link Column#CLM_POA_IND_SW1}
	 * through {@link Column#CLM_POA_IND_SW25}.
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
	 * {@link Column#ICD_DGNS_E_VRSN_CD12} and {@link Column#CLM_E_POA_IND_SW1}
	 * through {@link Column#CLM_E_POA_IND_SW12}.
	 */
	public List<IcdCode> diagnosesExternal = new LinkedList<>();

	/**
	 * Represents the data contained in {@link Column#LINE_NUM} and subsequent
	 * columns: one entry for every "claim line" in the claim represented by
	 * this {@link InpatientClaimGroup} instance.
	 */
	public List<InpatientClaimLine> lines = new LinkedList<>();

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("InpatientClaimGroup [version=");
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
		builder.append(", providerNumber=");
		builder.append(providerNumber);
		builder.append(", claimFacilityTypeCode=");
		builder.append(claimFacilityTypeCode);
		builder.append(", claimServiceClassificationTypeCode=");
		builder.append(claimServiceClassificationTypeCode);
		builder.append(", claimNonPaymentReasonCode=");
		builder.append(claimNonPaymentReasonCode);
		builder.append(", paymentAmount=");
		builder.append(paymentAmount);
		builder.append(", primaryPayerPaidAmount=");
		builder.append(primaryPayerPaidAmount);
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
		builder.append(", patientDischargeStatusCode=");
		builder.append(patientDischargeStatusCode);
		builder.append(", totalChargeAmount=");
		builder.append(totalChargeAmount);
		builder.append(", passThruPerDiemAmount=");
		builder.append(passThruPerDiemAmount);
		builder.append(", deductibleAmount=");
		builder.append(deductibleAmount);
		builder.append(", partACoinsuranceLiabilityAmount=");
		builder.append(partACoinsuranceLiabilityAmount);
		builder.append(", bloodDeductibleLiabilityAmount=");
		builder.append(bloodDeductibleLiabilityAmount);
		builder.append(", professionalComponentCharge=");
		builder.append(professionalComponentCharge);
		builder.append(", noncoveredCharge=");
		builder.append(noncoveredCharge);
		builder.append(", totalDeductionAmount=");
		builder.append(totalDeductionAmount);
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
		builder.append(", lines=");
		builder.append(lines);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Models individual claim lines within a {@link InpatientClaimGroup}
	 * instance.
	 */
	public static final class InpatientClaimLine {

		/**
		 * @see Column#CLM_LINE_NUM
		 */
		public Integer lineNumber;

		/**
		 * @see Column#HCPCS_CD
		 */
		public String hcpcsCode;

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("InpatientClaimLine [lineNumber=");
			builder.append(lineNumber);
			builder.append(", hcpcsCode=");
			builder.append(hcpcsCode);
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * Enumerates the raw RIF columns represented in
	 * {@link InpatientClaimGroup}, where {@link Column#ordinal()} values
	 * represent each column's position in the actual data.
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
		 * NOT MAPPED
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
		 * NOT MAPPED
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
		 * NOT MAPPED
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
		 * Type: <code>CHAR</code>, max chars: 10. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/orgnpinm.txt">
		 * CCW Data Dictionary: ORGNPINM</a>.
		 */
		ORG_NPI_NUM,
		
		/**
		 * NOT MAPPED
		 */
		AT_PHYSN_UPIN,

		/**
		 * Type: <code>CHAR</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/at_npi.txt">
		 * CCW Data Dictionary: AT_NPI</a>.
		 */
		AT_PHYSN_NPI,
		
		/**
		 * NOT MAPPED
		 */
		OP_PHYSN_UPIN,

		/**
		 * Type: <code>CHAR</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/op_npi.txt">
		 * CCW Data Dictionary: OP_NPI</a>.
		 */
		OP_PHYSN_NPI,
		
		/**
		 * NOT MAPPED
		 */
		OT_PHYSN_UPIN,

		/**
		 * Type: <code>CHAR</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ot_npi.txt">
		 * CCW Data Dictionary: OT_NPI</a>.
		 */
		OT_PHYSN_NPI,
		
		/**
		 * NOT MAPPED
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
		 * NOT MAPPED
		 */
		CLM_ADMSN_DT,
		
		/**
		 * NOT MAPPED
		 */
		CLM_IP_ADMSN_TYPE_CD,

		/**
		 * NOT MAPPED
		 */
		CLM_SRC_IP_ADMSN_CD,

		/**
		 * NOT MAPPED
		 */
		NCH_PTNT_STATUS_IND_CD,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/per_diem.txt">
		 * CCW Data Dictionary: PER_DIEM</a>.
		 */
		CLM_PASS_THRU_PER_DIEM_AMT,

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
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pcchgamt.txt">
		 * CCW Data Dictionary: PCCHGAMT </a>.
		 */
		NCH_PROFNL_CMPNT_CHRG_AMT,

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
		 * NOT MAPPED
		 */
		CLM_TOT_PPS_CPTL_AMT,

		/**
		 * NOT MAPPED
		 */
		CLM_PPS_CPTL_FSP_AMT,

		/**
		 * NOT MAPPED
		 */
		CLM_PPS_CPTL_OUTLIER_AMT,

		/**
		 * NOT MAPPED
		 */
		CLM_PPS_CPTL_DSPRPRTNT_SHR_AMT,

		/**
		 * NOT MAPPED
		 */
		CLM_PPS_CPTL_IME_AMT,

		/**
		 * NOT MAPPED
		 */
		CLM_PPS_CPTL_EXCPTN_AMT,

		/**
		 * NOT MAPPED
		 */
		CLM_PPS_OLD_CPTL_HLD_HRMLS_AMT,

		/**
		 * NOT MAPPED
		 */
		CLM_PPS_CPTL_DRG_WT_NUM,

		/**
		 * NOT MAPPED
		 */
		CLM_UTLZTN_DAY_CNT,

		/**
		 * NOT MAPPED
		 */
		BENE_TOT_COINSRNC_DAYS_CNT,

		/**
		 * NOT MAPPED
		 */
		BENE_LRD_USED_CNT,

		/**
		 * NOT MAPPED
		 */
		CLM_NON_UTLZTN_DAYS_CNT,

		/**
		 * NOT MAPPED
		 */
		NCH_BLOOD_PNTS_FRNSHD_QTY,

		/**
		 * NOT MAPPED
		 */
		NCH_VRFD_NCVRD_STAY_FROM_DT,

		/**
		 * NOT MAPPED
		 */
		NCH_VRFD_NCVRD_STAY_THRU_DT,

		/**
		 * NOT MAPPED
		 */
		NCH_ACTV_OR_CVRD_LVL_CARE_THRU,

		/**
		 * NOT MAPPED
		 */
		NCH_BENE_MDCR_BNFTS_EXHTD_DT_I,

		/**
		 * NOT MAPPED
		 */
		NCH_BENE_DSCHRG_DT,

		/**
		 * NOT MAPPED
		 */
		CLM_DRG_CD,

		/**
		 * NOT MAPPED
		 */
		CLM_DRG_OUTLIER_STAY_CD,

		/**
		 * NOT MAPPED
		 */
		NCH_DRG_OUTLIER_APRVD_PMT_AMT,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw1.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW1</a>.
		 */
		CLM_POA_IND_SW1,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw2.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW2</a>.
		 */
		CLM_POA_IND_SW2,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw3.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW3</a>.
		 */
		CLM_POA_IND_SW3,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw4.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW4</a>.
		 */
		CLM_POA_IND_SW4,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw5.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW5</a>.
		 */
		CLM_POA_IND_SW5,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw6.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW6</a>.
		 */
		CLM_POA_IND_SW6,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw7.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW7</a>.
		 */
		CLM_POA_IND_SW7,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw8.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW8</a>.
		 */
		CLM_POA_IND_SW8,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw9.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW9</a>.
		 */
		CLM_POA_IND_SW9,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw10.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW10</a>.
		 */
		CLM_POA_IND_SW10,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw11.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW11</a>.
		 */
		CLM_POA_IND_SW11,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw12.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW12</a>.
		 */
		CLM_POA_IND_SW12,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw13.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW13</a>.
		 */
		CLM_POA_IND_SW13,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw14.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW14</a>.
		 */
		CLM_POA_IND_SW14,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_c15.txt">
		 * CCW Data Dictionary: ICD_DGNS_C15</a>.
		 */
		ICD_DGNS_C15,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_c15.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_C15</a>.
		 */
		ICD_DGNS_VRSN_C15,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw15.txt">
		 * CCW Data Diction ary: CLM_POA_IND_SW15</a>.
		 */
		CLM_POA_IND_SW15,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_c16.txt">
		 * CCW Data Dictionary: ICD_DGNS_C16</a>.
		 */
		ICD_DGNS_C16,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/icd_dgns_vrsn_c16.txt">
		 * CCW Data Dictionary: ICD_DGNS_VRSN_C16</a>.
		 */
		ICD_DGNS_VRSN_C16,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw16.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW16</a>.
		 */
		CLM_POA_IND_SW16,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw17.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW17</a>.
		 */
		CLM_POA_IND_SW17,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw18.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW18</a>.
		 */
		CLM_POA_IND_SW18,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw19.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW19</a>.
		 */
		CLM_POA_IND_SW19,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw20.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW20</a>.
		 */
		CLM_POA_IND_SW20,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw21.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW21</a>.
		 */
		CLM_POA_IND_SW21,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw22.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW22</a>.
		 */
		CLM_POA_IND_SW22,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw23.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW23</a>.
		 */
		CLM_POA_IND_SW23,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw24.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW24</a>.
		 */
		CLM_POA_IND_SW24,

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
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_poa_ind_sw25.txt">
		 * CCW Data Dictionary: CLM_POA_IND_SW25</a>.
		 */
		CLM_POA_IND_SW25,

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
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_e_poa_ind_sw1.txt">
		 * CCW Data Dictionary: CLM_E_POA_IND_SW1</a>.
		 */
		CLM_E_POA_IND_SW1,

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
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_e_poa_ind_sw2.txt">
		 * CCW Data Dictionary: CLM_E_POA_IND_SW2</a>.
		 */
		CLM_E_POA_IND_SW2,

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
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_e_poa_ind_sw3.txt">
		 * CCW Data Dictionary: CLM_E_POA_IND_SW3</a>.
		 */
		CLM_E_POA_IND_SW3,

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
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_e_poa_ind_sw4.txt">
		 * CCW Data Dictionary: CLM_E_POA_IND_SW4</a>.
		 */
		CLM_E_POA_IND_SW4,

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
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_e_poa_ind_sw5.txt">
		 * CCW Data Dictionary: CLM_E_POA_IND_SW5</a>.
		 */
		CLM_E_POA_IND_SW5,

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
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_e_poa_ind_sw6.txt">
		 * CCW Data Dictionary: CLM_E_POA_IND_SW6</a>.
		 */
		CLM_E_POA_IND_SW6,

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
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_e_poa_ind_sw7.txt">
		 * CCW Data Dictionary: CLM_E_POA_IND_SW7</a>.
		 */
		CLM_E_POA_IND_SW7,

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
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_e_poa_ind_sw8.txt">
		 * CCW Data Dictionary: CLM_E_POA_IND_SW8</a>.
		 */
		CLM_E_POA_IND_SW8,

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
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_e_poa_ind_sw9.txt">
		 * CCW Data Dictionary: CLM_E_POA_IND_SW9</a>.
		 */
		CLM_E_POA_IND_SW9,

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
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_e_poa_ind_sw10.txt">
		 * CCW Data Dictionary: CLM_E_POA_IND_SW10</a>.
		 */
		CLM_E_POA_IND_SW10,

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
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_e_poa_ind_sw11.txt">
		 * CCW Data Dictionary: CLM_E_POA_IND_SW11</a>.
		 */
		CLM_E_POA_IND_SW11,

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
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_e_poa_ind_sw12.txt">
		 * CCW Data Dictionary: CLM_E_POA_IND_SW12</a>.
		 */
		CLM_E_POA_IND_SW12,

		/**
		 * Type: <code>NUM</code>, max chars: 13. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/clm_ln.txt">
		 * CCW Data Dictionary: CLM_LN</a>.
		 */
		CLM_LINE_NUM,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/hcpcs_cd.txt">
		 * CCW Data Dictionary: HCPCS_CD</a>.
		 */
		HCPCS_CD;

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
