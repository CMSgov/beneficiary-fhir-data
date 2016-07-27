package gov.hhs.cms.bluebutton.datapipeline.rif.model;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>
 * Models rows from {@link RifFileType#BENEFICIARY} RIF files.
 * </p>
 * <p>
 * Design Note: This class is too painful to maintain as a bean, so I've
 * stripped it down to just a struct. To be clear, this is <strong>not</strong>
 * immutable and thus <strong>not</strong> thread-safe (and it really shouldn't
 * need to be).
 * </p>
 */
public final class BeneficiaryRow {
	/**
	 * FIXME is this the schema version or the record version?
	 */
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
	 * @see Column#STATE_CODE
	 */
	public String stateCode;

	/**
	 * @see Column#BENE_COUNTY_CD
	 */
	public String countyCode;

	/**
	 * @see Column#BENE_ZIP_CD
	 */
	public String postalCode;

	/**
	 * @see Column#BENE_BIRTH_DT
	 */
	public LocalDate birthDate;

	/**
	 * @see Column#BENE_SEX_IDENT_CD
	 */
	public char sex;

	/**
	 * @see Column#BENE_RACE_CD
	 */
	public char race;

	/**
	 * @see Column#BENE_ENTLMT_RSN_ORIG
	 */
	public Optional<Character> entitlementCodeOriginal;

	/**
	 * @see Column#BENE_ENTLMT_RSN_CURR
	 */
	public Optional<Character> entitlementCodeCurrent;

	/**
	 * @see Column#BENE_ESRD_IND
	 */
	public Optional<Character> endStageRenalDiseaseCode;

	/**
	 * @see Column#BENE_MDCR_STATUS_CD
	 */
	public Optional<String> medicareEnrollmentStatusCode;

	/**
	 * @see Column#BENE_PTA_TRMNTN_CD
	 */
	public Optional<Character> partATerminationCode;

	/**
	 * @see Column#BENE_PTB_TRMNTN_CD
	 */
	public Optional<Character> partBTerminationCode;

	/**
	 * @see Column#BENE_CRNT_HIC_NUM
	 */
	public String hicn;

	/**
	 * @see Column#BENE_SRNM_NAME
	 */
	public String nameSurname;

	/**
	 * @see Column#BENE_GVN_NAME
	 */
	public String nameGiven;

	/**
	 * @see Column#BENE_MDL_NAME
	 */
	public Optional<Character> nameMiddleInitial;

	/**
	 * @see java.lang.Object#toString()
	 */

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BeneficiaryRow [version=");
		builder.append(version);
		builder.append(", recordAction=");
		builder.append(recordAction);
		builder.append(", beneficiaryId=");
		builder.append(beneficiaryId);
		builder.append(", stateCode=");
		builder.append(stateCode);
		builder.append(", countyCode=");
		builder.append(countyCode);
		builder.append(", postalCode=");
		builder.append(postalCode);
		builder.append(", birthDate=");
		builder.append(birthDate);
		builder.append(", sex=");
		builder.append(sex);
		builder.append(", race=");
		builder.append(race);
		builder.append(", entitlementCodeOriginal=");
		builder.append(entitlementCodeOriginal);
		builder.append(", entitlementCodeCurrent=");
		builder.append(entitlementCodeCurrent);
		builder.append(", endStageRenalDiseaseCode=");
		builder.append(endStageRenalDiseaseCode);
		builder.append(", medicareEnrollmentStatusCode=");
		builder.append(medicareEnrollmentStatusCode);
		builder.append(", partATerminationCode=");
		builder.append(partATerminationCode);
		builder.append(", partBTerminationCode=");
		builder.append(partBTerminationCode);
		builder.append(", hicn=");
		builder.append(hicn);
		builder.append(", nameSurname=");
		builder.append(nameSurname);
		builder.append(", nameGiven=");
		builder.append(nameGiven);
		builder.append(", nameMiddleInitial=");
		builder.append(nameMiddleInitial);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Enumerates the raw RIF columns represented in {@link BeneficiaryRow},
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
		 * CCW Data Dictionary: BENE_ID</a>.
		 */
		BENE_ID,

		/**
		 * Type: <code>CHAR</code>, max chars: 2. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/state_cd.txt">
		 * CCW Data Dictionary: STATE_CODE</a>.
		 */
		STATE_CODE,

		/**
		 * Type: <code>CHAR</code>, max chars: 3. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/cnty_cd.txt">
		 * CCW Data Dictionary: BENE_COUNTY_CD</a>.
		 */
		BENE_COUNTY_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 9. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/bene_zip.txt">
		 * CCW Data Dictionary: BENE_ZIP_CD</a>.
		 */
		BENE_ZIP_CD,

		/**
		 * Type: <code>DATE</code>, max chars: 8. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/bene_dob.txt">
		 * CCW Data Dictionary: BENE_BIRTH_DT</a>.
		 */
		BENE_BIRTH_DT,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/sex.txt">
		 * CCW Data Dictionary: BENE_SEX_IDENT_CD</a>.
		 */
		BENE_SEX_IDENT_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/race.txt">
		 * CCW Data Dictionary: BENE_RACE_CD</a>.
		 */
		BENE_RACE_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/orec.txt">
		 * CCW Data Dictionary: BENE_ENTLMT_RSN_ORIG</a>.
		 */
		BENE_ENTLMT_RSN_ORIG,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/crec.txt">
		 * CCW Data Dictionary: BENE_ENTLMT_RSN_CURR</a>.
		 */
		BENE_ENTLMT_RSN_CURR,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/esrd_ind.txt">
		 * CCW Data Dictionary: BENE_ESRD_IND</a>.
		 */
		BENE_ESRD_IND,

		/**
		 * Type: <code>CHAR</code>, max chars: 2 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/ms_cd.txt">
		 * CCW Data Dictionary: BENE_MDCR_STATUS_CD</a>.
		 */
		BENE_MDCR_STATUS_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/a_trm_cd.txt">
		 * CCW Data Dictionary: BENE_PTA_TRMNTN_CD</a>.
		 */
		BENE_PTA_TRMNTN_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/b_trm_cd.txt">
		 * CCW Data Dictionary: BENE_PTB_TRMNTN_CD</a>.
		 */
		BENE_PTB_TRMNTN_CD,

		/**
		 * TODO - need data dictionary link for BENE_CRNT_HIC_NUM Type:
		 * <code>CHAR</code>, max chars: 12. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/???.txt">
		 * CCW Data Dictionary: BENE_CRNT_HIC_NUM</a>.
		 */
		BENE_CRNT_HIC_NUM,

		/**
		 * TODO - need data dictionary link for BENE_SRNM_NAME Type:
		 * <code>CHAR</code>, max chars: 24. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/???.txt">
		 * CCW Data Dictionary: BENE_SRNM_NAME</a>.
		 */
		BENE_SRNM_NAME,

		/**
		 * TODO - need data dictionary link for BENE_GVN_NAME Type:
		 * <code>CHAR</code>, max chars: 5. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/???.txt">
		 * CCW Data Dictionary: BENE_GVN_NAME</a>.
		 */
		BENE_GVN_NAME,

		/**
		 * TODO - need data dictionary link for BENE_MDL_NAME Type:
		 * <code>CHAR</code>, max chars: 1 <code>Optional</code>. See <a href=
		 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/???.txt">
		 * CCW Data Dictionary: BENE_MDL_NAME</a>.
		 */
		BENE_MDL_NAME;

		/*
		 * @return a {@link String} array containing all of the RIF column
		 * names, in order
		 */
		public static String[] getColumnNames() {
			return Arrays.stream(values()).map(c -> c.name()).collect(Collectors.toList())
					.toArray(new String[values().length]);
		}
	}
}
