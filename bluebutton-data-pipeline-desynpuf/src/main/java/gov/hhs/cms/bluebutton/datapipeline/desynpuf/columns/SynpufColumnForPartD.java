package gov.hhs.cms.bluebutton.datapipeline.desynpuf.columns;

import java.util.Arrays;

import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufFile;

/**
 * Enumerates the columns in the {@link SynpufFile#PART_D_CLAIMS} file.
 */
public enum SynpufColumnForPartD {
	/**
	 * Beneficiary Code
	 */
	DESYNPUF_ID,

	/**
	 * CCW Part D Event Number
	 */
	PDE_ID,

	/**
	 * RX Service Date
	 */
	SRVC_DT,

	/**
	 * Product Service ID
	 */
	PROD_SRVC_ID,

	/**
	 * Quantity Dispensed
	 */
	QTY_DSPNSD_NUM,

	/**
	 * Days Supply
	 */
	DAYS_SUPLY_NUM,

	/**
	 * Patient Pay Amount
	 */
	PTNT_PAY_AMT,

	/**
	 * Gross Drug Cost
	 */
	TOT_RX_CST_AMT;

	/**
	 * @return a <code>String[]</code> containing all of the column names
	 */
	public static String[] getAllColumnNames() {
		return Arrays.stream(SynpufColumnForPartD.values()).map(v -> v.name()).toArray(size -> new String[size]);
	}
}
