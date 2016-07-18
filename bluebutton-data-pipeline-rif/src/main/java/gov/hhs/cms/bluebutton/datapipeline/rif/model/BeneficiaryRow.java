package gov.hhs.cms.bluebutton.datapipeline.rif.model;

import java.time.LocalDate;
import java.util.Arrays;
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
	public int version;

	/**
	 * Indicates whether the record for the row is an insert, update, or delete.
	 */
	public RecordAction recordAction;

	/**
	 * The CCW's assigned ID for this beneficiary.
	 */
	public String beneficiaryId;

	/**
	 * The two-letter code that represents the US state that the beneficiary
	 * resides in. TODO: link to state codes dictionary
	 */
	public String stateCode;

	/**
	 * The three-letter code that represents the county that the beneficiary
	 * resides in. TODO: link to county codes dictionary
	 */
	public String countyCode;

	/**
	 * The five-or-nine-digit postal code that the beneficiary resides in.
	 * (Note: this is a {@link String} to preserve any leading zeros.)
	 */
	public String postalCode;

	/**
	 * The beneficiary's date of birth.
	 */
	public LocalDate birthDate;

	/**
	 * The one-letter code that represents the beneficiary's sex. TODO: link to
	 * sex codes dictionary
	 */
	public char sex;

	/**
	 * The one-letter code that represents the beneficiary's race. TODO: link to
	 * race codes dictionary
	 */
	public char race;

	/**
	 * The one-letter code that represents the beneficiary's original reason for
	 * Medicare entitlement. TODO: link to entitlement codes dictionary
	 */
	public char entitlementCodeOriginal;

	/**
	 * The one-letter code that represents the beneficiary's current reason for
	 * Medicare entitlement. TODO: link to entitlement codes dictionary
	 */
	public char entitlementCodeCurrent;

	/**
	 * The one-letter code that indicates whether or not the beneficiary is
	 * classified as being in end-stage renal disease. TODO: link to codes
	 * dictionary
	 */
	public char endStageRenalDiseaseCode;

	/**
	 * The two-letter code for the beneficiary's Medicare enrollment status.
	 * TODO: link to codes dictionary
	 */
	public String medicareEnrollmentStatusCode;

	/**
	 * The one-letter code that details why the beneficiary's Part A enrollment
	 * was terminated (if it was). TODO: link to codes dictionary
	 */
	public char partATerminationCode;

	/**
	 * The one-letter code that details why the beneficiary's Part B enrollment
	 * was terminated (if it was). TODO: link to codes dictionary
	 */
	public char partBTerminationCode;

	/**
	 * The beneficiary's twelve-character alphanumeric "HIC Number/HICN", which
	 * is their Medicare ID.
	 */
	public String hicn;

	/**
	 * The beneficiary's surname/last name.
	 */
	public String nameSurname;

	/**
	 * The beneficiary's given/first name.
	 */
	public String nameGiven;

	/**
	 * The beneficiary's middleInitial.
	 */
	public char nameMiddleInitial;

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
		 * Type: <code>CHAR</code>, max chars: 15.
		 */
		BENE_ID,

		/**
		 * Type: <code>CHAR</code>, max chars: 2.
		 */
		STATE_CODE,

		/**
		 * Type: <code>CHAR</code>, max chars: 3.
		 */
		BENE_COUNTY_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 9.
		 */
		BENE_ZIP_CD,

		/**
		 * Type: <code>DATE</code>, max chars: 8.
		 */
		BENE_BIRTH_DT,

		/**
		 * Type: <code>CHAR</code>, max chars: 1.
		 */
		BENE_SEX_IDENT_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1.
		 */
		BENE_RACE_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1.
		 */
		BENE_ENTLMT_RSN_ORIG,

		/**
		 * Type: <code>CHAR</code>, max chars: 1.
		 */
		BENE_ENTLMT_RSN_CURR,

		/**
		 * Type: <code>CHAR</code>, max chars: 1.
		 */
		BENE_ESRD_IND,

		/**
		 * Type: <code>CHAR</code>, max chars: 2.
		 */
		BENE_MDCR_STATUS_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1.
		 */
		BENE_PTA_TRMNTN_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 1.
		 */
		BENE_PTB_TRMNTN_CD,

		/**
		 * Type: <code>CHAR</code>, max chars: 12.
		 */
		BENE_CRNT_HIC_NUM,

		/**
		 * Type: <code>CHAR</code>, max chars: 24.
		 */
		BENE_SRNM_NAME,

		/**
		 * Type: <code>CHAR</code>, max chars: 5.
		 */
		BENE_GVN_NAME,

		/**
		 * Type: <code>CHAR</code>, max chars: 1.
		 */
		BENE_MDL_NAME;

		/**
		 * @return a {@link String} array containing all of the RIF column
		 *         names, in order
		 */
		public static String[] getColumnNames() {
			return Arrays.stream(values()).map(c -> c.name()).collect(Collectors.toList())
					.toArray(new String[values().length]);
		}
	}
}
