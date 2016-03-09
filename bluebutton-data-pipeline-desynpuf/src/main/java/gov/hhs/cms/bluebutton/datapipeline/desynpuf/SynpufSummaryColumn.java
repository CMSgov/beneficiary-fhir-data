package gov.hhs.cms.bluebutton.datapipeline.desynpuf;

import java.util.Arrays;

/**
 * Enumerates the columns in the CMS DE-SynPUF Beneficiary Summary files.
 */
public enum SynpufSummaryColumn {
	/**
	 * Beneficiary Code
	 */
	DESYNPUF_ID,

	/**
	 * Date of birth
	 */
	BENE_BIRTH_DT,

	/**
	 * Date of death
	 */
	BENE_DEATH_DT,

	/**
	 * Sex
	 */
	BENE_SEX_IDENT_CD,

	/**
	 * Beneficiary Race Code
	 */
	BENE_RACE_CD,

	/**
	 * End stage renal disease Indicator
	 */
	BENE_ESRD_IND,

	/**
	 * State Code
	 */
	SP_STATE_CODE,

	/**
	 * County Code
	 */
	BENE_COUNTY_CD,

	/**
	 * Total number of months of part A coverage for the beneficiary.
	 */
	BENE_HI_CVRAGE_TOT_MONS,

	/**
	 * Total number of months of part B coverage for the beneficiary.
	 */
	BENE_SMI_CVRAGE_TOT_MONS,

	/**
	 * Total number of months of HMO coverage for the beneficiary.
	 */
	BENE_HMO_CVRAGE_TOT_MONS,

	/**
	 * Total number of months of part D plan coverage for the beneficiary.
	 */
	PLAN_CVRG_MOS_NUM,

	/**
	 * Chronic Condition: Alzheimer or related disorders or senile
	 */
	SP_ALZHDMTA,

	/**
	 * Chronic Condition: Heart Failure
	 */
	SP_CHF,

	/**
	 * Chronic Condition: Chronic Kidney Disease
	 */
	SP_CHRNKIDN,

	/**
	 * Chronic Condition: Cancer
	 */
	SP_CNCR,

	/**
	 * Chronic Condition: Chronic Obstructive Pulmonary Disease
	 */
	SP_COPD,

	/**
	 * Chronic Condition: Depression
	 */
	SP_DEPRESSN,

	/**
	 * Chronic Condition: Diabetes
	 */
	SP_DIABETES,

	/**
	 * Chronic Condition: Ischemic Heart Disease
	 */
	SP_ISCHMCHT,

	/**
	 * Chronic Condition: Osteoporosis
	 */
	SP_OSTEOPRS,

	/**
	 * Chronic Condition: rheumatoid arthritis and osteoarthritis (RA/OA)
	 */
	SP_RA_OA,

	/**
	 * Chronic Condition: Stroke/transient Ischemic Attack
	 */
	SP_STRKETIA,

	/**
	 * Inpatient annual Medicare reimbursement amount
	 */
	MEDREIMB_IP,

	/**
	 * Inpatient annual beneficiary responsibility amount
	 */
	BENRES_IP,

	/**
	 * Inpatient annual primary payer reimbursement amount
	 */
	PPPYMT_IP,

	/**
	 * Outpatient Institutional annual Medicare reimbursement amount
	 */
	MEDREIMB_OP,

	/**
	 * Outpatient Institutional annual beneficiary responsibility amount
	 */
	BENRES_OP,

	/**
	 * Outpatient Institutional annual primary payer reimbursement amount
	 */
	PPPYMT_OP,

	/**
	 * Carrier annual Medicare reimbursement amount
	 */
	MEDREIMB_CAR,

	/**
	 * Carrier annual beneficiary responsibility amount
	 */
	BENRES_CAR,

	/**
	 * Carrier annual primary payer reimbursement amount
	 */
	PPPYMT_CAR;

	/**
	 * @return a <code>String[]</code> containing all of the column names
	 */
	public static String[] getAllColumnNames() {
		return Arrays.stream(SynpufSummaryColumn.values()).map(v -> v.name()).toArray(size -> new String[size]);
	}
}
