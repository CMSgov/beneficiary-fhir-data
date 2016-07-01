package gov.hhs.cms.bluebutton.datapipeline.rif.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * <p>
 * Models rows from {@link RifFileType#PDE} RIF Files.
 * </p>
 * <p>
 * The RIF file layout used here is specific to the Blue Button API's ETL
 * process. The layouts of these files is detailed in the
 * <code>bluebutton-data-pipeline-rif/dev/rif-record-layout.xlsx</code> file.
 * The columns contained in the files are largely similar to those detailed in
 * <a href="https://www.ccwdata.org/web/guest/data-dictionaries">CCW: Data
 * Dictionaries</a>.
 * </p>
 * 
 * <p>
 * Design Note: This class is too painful to maintain as a bean, so I've
 * stripped it down to just a struct. To be clear, this is <strong>not</strong>
 * immutable and thus <strong>not</strong> thread-safe (and it really shouldn't
 * need to be).
 * </p>
 */
public class PartDEventRow {
	public static final int COMPOUND_CODE_NOT_COMPOUNDED = 1;
	public static final int COMPOUND_CODE_COMPOUNDED = 2;

	public static final String SVC_PRVDR_ID_QLFYR_CD_NPI = "01";
	public static final String SVC_PRVDR_ID_QLFYR_CD_NCPDP = "07";
	public static final String SVC_PRVDR_ID_QLFYR_CD_STLICENSE = "08";
	public static final String SVC_PRVDR_ID_QLFYR_CD_FEDTAX = "11";

	public static final String PRSCRBR_ID_QLFYR_CD_NPI = "01";

	/**
	 * @see Column#VERSION
	 */
	public int version;

	/**
	 * @see Column#DML_IND
	 */
	public RecordAction recordAction;

	/**
	 * @see Column#PDE_ID
	 */
	public String partDEventId;

	/**
	 * @see Column#BENE_ID
	 */
	public String beneficiaryId;

	/**
	 * @see Column#SRVC_DT
	 */
	public LocalDate prescriptionFillDate;

	/**
	 * @see Column#PD_DT
	 */
	public Optional<LocalDate> paymentDate;

	/**
	 * @see Column#SRVC_PRVDR_ID_QLFYR_CD
	 */
	public String serviceProviderIdQualiferCode;

	/**
	 * @see Column#SRVC_PRVDR_ID
	 */
	public String serviceProviderId;

	/**
	 * @see Column#PRSCRBR_ID_QLFYR_CD
	 */
	public String prescriberIdQualifierCode;

	/**
	 * @see Column#PRSCRBR_ID
	 */
	public String prescriberId;

	/**
	 * @see Column#RX_SRVC_RFRNC_NUM
	 */
	public Long prescriptionReferenceNumber;

	/**
	 * @see Column#PROD_SRVC_ID
	 */
	public String nationalDrugCode;

	/**
	 * @see Column#PLAN_CNTRCT_REC_ID
	 */
	public String planContractId;

	/**
	 * @see Column#PLAN_PBP_REC_NUM
	 */
	public String planBenefitPackageId;

	/**
	 * @see Column#CMPND_CD
	 */
	public Integer compoundCode;

	/**
	 * @see Column#DAW_PROD_SLCTN_CD
	 */
	public String dispenseAsWrittenProductSelectionCode;

	/**
	 * @see Column#QTY_DPSNSD_NUM
	 */
	public BigDecimal quantityDispensed;

	/**
	 * @see Column#DAYS_SUPLY_NUM
	 */
	public Integer daysSupply;

	/**
	 * @see Column#FILL_NUM
	 */
	public Integer fillNumber;

	/**
	 * @see Column#DSPNSNG_STUS_CD
	 */
	public Optional<Character> dispensingStatuscode;

	// TODO FIll in rest of fields

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PartDEventRow [version=");
		builder.append(version);
		builder.append(", recordAction=");
		builder.append(recordAction);
		builder.append(", partDEventId=");
		builder.append(partDEventId);
		builder.append(", beneficiaryId=");
		builder.append(beneficiaryId);
		builder.append(", prescriptionFillDate=");
		builder.append(prescriptionFillDate);
		builder.append(", paymentDate=");
		builder.append(paymentDate);
		builder.append(", serviceProviderIdQualiferCode=");
		builder.append(serviceProviderIdQualiferCode);
		builder.append(", serviceProviderId=");
		builder.append(serviceProviderId);
		builder.append(", prescriberIdQualifierCode=");
		builder.append(prescriberIdQualifierCode);
		builder.append(", prescriberId=");
		builder.append(prescriberId);
		builder.append(", prescriptionReferenceNumber=");
		builder.append(prescriptionReferenceNumber);
		builder.append(", nationalDrugCode=");
		builder.append(nationalDrugCode);
		builder.append(", planContractId=");
		builder.append(planContractId);
		builder.append(", planBenefitPackageId=");
		builder.append(planBenefitPackageId);
		builder.append(", compoundCode=");
		builder.append(compoundCode);
		builder.append(", dispenseAsWrittenProductSelectionCode=");
		builder.append(dispenseAsWrittenProductSelectionCode);
		builder.append(", quantityDispensed=");
		builder.append(quantityDispensed);
		builder.append(", daysSupply=");
		builder.append(daysSupply);
		builder.append(", fillNumber=");
		builder.append(fillNumber);
		builder.append(", dispensingStatuscode=");
		builder.append(dispensingStatuscode);
		builder.append("]");
		return builder.toString();
		// TODO FIll in rest of fields
	}

	/**
	 * Enumerates the raw RIF columns represented in {@link PartDEventRow},
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
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pde_id.txt">
		 * CCW Data Dictionary: PDE_ID</a>, though note that this instance of +
		 * * the field is unencrypted.
		 */
		PDE_ID,

		/**
		 * Type: <code>CHAR</code>, max chars: 15. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/bene_id.txt">
		 * CCW Data Dictionary: BENE_ID</a>, though note that this instance of +
		 * * the field is unencrypted.
		 */
		BENE_ID,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/srvc_dt.txt">
		 * CCW Data Dictionary: SRVC_DT</a>.
		 */
		SRVC_DT,

		/**
		 * Type: <code>DATE</code>, max chars: 8, <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/pd_dt.txt">
		 * CCW Data Dictionary: PD_DT</a>.
		 */
		PD_DT,

		/**
		 * Type: <code>CHAR</code>, max chars: 2. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/srvc_prvdr_id_qlfyr_cd.txt">
		 * CCW Data Dictionary: SRVC_PRVDR_ID_QLFYR_CD</a>.
		 */
		SRVC_PRVDR_ID_QLFYR_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 15. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/srvc_prvdr_id.txt">
		 * CCW Data Dictionary: SRVC_PRVDR_ID</a>.
		 */
		SRVC_PRVDR_ID,

		/**
		 * Type: <code>CHAR</code>, max chars: 2. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prscrbr_id_qlfyr_cd.txt">
		 * CCW Data Dictionary: PRSCRBR_ID_QLFYR_CD</a>.
		 */
		PRSCRBR_ID_QLFYR_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 15. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prscrbr_id.txt">
		 * CCW Data Dictionary: PRSCRBR_ID</a>.
		 */
		PRSCRBR_ID,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/rx_srvc_rfrnc_num.txt">
		 * CCW Data Dictionary: RX_SRVC_RFRNC_NUM</a>.
		 */
		RX_SRVC_RFRNC_NUM,

		/**
		 * Type: <code>CHAR</code>, max chars: 19. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prod_srvc_id.txt">
		 * CCW Data Dictionary: PROD_SRVC_ID</a>.
		 */
		PROD_SRVC_ID,

		/**
		 * Type: <code>CHAR</code>, max chars: 5. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plan_cntrct_rec_id.txt">
		 * CCW Data Dictionary: PLAN_CNTRCT_REC_ID</a>.
		 */
		PLAN_CNTRCT_REC_ID,

		/**
		 * Type: <code>CHAR</code>, max chars: 3. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/plan_pbp_rec_num.txt">
		 * CCW Data Dictionary: PLAN_PBP_REC_NUM</a>.
		 */
		PLAN_PBP_REC_NUM,

		/**
		 * Type: <code>NUM</code>, max chars: 2. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cmpnd_cd.txt">
		 * CCW Data Dictionary: CMPND_CD</a>.
		 */
		CMPND_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/daw_prod_slctn_cd.txt">
		 * CCW Data Dictionary: DAW_PROD_SLCTN_CD</a>.
		 */
		DAW_PROD_SLCTN_CD,

		/**
		 * Type: <code>NUM</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/qty_dspnsd_num.txt">
		 * CCW Data Dictionary: QTY_DPSNSD_NUM</a>.
		 */
		QTY_DPSNSD_NUM,

		/**
		 * Type: <code>NUM</code>, max chars: 3. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/days_suply_num.txt">
		 * CCW Data Dictionary: DAYS_SUPLY_NUM</a>.
		 */
		DAYS_SUPLY_NUM,

		/**
		 * Type: <code>NUM</code>, max chars: 3. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/fill_num.txt">
		 * CCW Data Dictionary: FILL_NUM</a>.
		 */
		FILL_NUM,

		/**
		 * Type: <code>CHAR</code>, max chars: 1, <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/dspnsng_stus_cd.txt">
		 * CCW Data Dictionary: DSPNSNG_STUS_CD</a>.
		 */
		DSPNSNG_STUS_CD
		// TODO FIll in rest of fields
	}
}
