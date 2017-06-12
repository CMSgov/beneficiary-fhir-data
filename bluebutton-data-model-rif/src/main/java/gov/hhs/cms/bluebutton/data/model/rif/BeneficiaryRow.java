package gov.hhs.cms.bluebutton.data.model.rif;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;


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
@Entity
@Table(name = "`Accounts`")
public class BeneficiaryRow {
	/**
	 * @see Column#BENE_ID
	 */
	@Id
	private String beneficiaryId;

	/**
	 * @see Column#STATE_CODE
	 */
	private String stateCode;

	/**
	 * @see Column#BENE_COUNTY_CD
	 */
	private String countyCode;

	/**
	 * @see Column#BENE_ZIP_CD
	 */
	private String postalCode;

	/**
	 * @see Column#BENE_BIRTH_DT
	 */
	private LocalDate birthDate;

	/**
	 * @see Column#BENE_SEX_IDENT_CD
	 */
	private char sex;

	/**
	 * @see Column#BENE_RACE_CD
	 */
	private Character race;

	/**
	 * @see Column#BENE_ENTLMT_RSN_ORIG
	 */
	private Character entitlementCodeOriginal;

	/**
	 * @see Column#BENE_ENTLMT_RSN_CURR
	 */
	private Character entitlementCodeCurrent;

	/**
	 * @see Column#BENE_ESRD_IND
	 */
	private Character endStageRenalDiseaseCode;

	/**
	 * @see Column#BENE_MDCR_STATUS_CD
	 */
	private String medicareEnrollmentStatusCode;

	/**
	 * @see Column#BENE_PTA_TRMNTN_CD
	 */
	private Character partATerminationCode;

	/**
	 * @see Column#BENE_PTB_TRMNTN_CD
	 */
	private Character partBTerminationCode;

	/**
	 * @see Column#BENE_CRNT_HIC_NUM
	 */
	private String hicn;

	/**
	 * @see Column#BENE_SRNM_NAME
	 */
	private String nameSurname;

	/**
	 * @see Column#BENE_GVN_NAME
	 */
	private String nameGiven;

	/**
	 * @see Column#BENE_MDL_NAME
	 */
	private Character nameMiddleInitial;

	public String getBeneficiaryId() {
		return beneficiaryId;
	}

	public void setBeneficiaryId(String beneficiaryId) {
		this.beneficiaryId = beneficiaryId;
	}

	public String getStateCode() {
		return stateCode;
	}

	public void setStateCode(String stateCode) {
		this.stateCode = stateCode;
	}

	public String getCountyCode() {
		return countyCode;
	}

	public void setCountyCode(String countyCode) {
		this.countyCode = countyCode;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public char getSex() {
		return sex;
	}

	public void setSex(char sex) {
		this.sex = sex;
	}

	public Optional<Character> getRace() {
		return Optional.ofNullable(race);
	}

	public void setRace(Optional<Character> race) {
		this.race = race.orElse(null);
	}

	public Optional<Character> getEntitlementCodeOriginal() {
		return Optional.ofNullable(entitlementCodeOriginal);
	}

	public void setEntitlementCodeOriginal(Optional<Character> entitlementCodeOriginal) {
		this.entitlementCodeOriginal = entitlementCodeOriginal.orElse(null);
	}

	public Optional<Character> getEntitlementCodeCurrent() {
		return Optional.ofNullable(entitlementCodeCurrent);
	}

	public void setEntitlementCodeCurrent(Optional<Character> entitlementCodeCurrent) {
		this.entitlementCodeCurrent = entitlementCodeCurrent.orElse(null);
	}

	public Optional<Character> getEndStageRenalDiseaseCode() {
		return Optional.ofNullable(endStageRenalDiseaseCode);
	}

	public void setEndStageRenalDiseaseCode(Optional<Character> endStageRenalDiseaseCode) {
		this.endStageRenalDiseaseCode = endStageRenalDiseaseCode.orElse(null);
	}

	public Optional<String> getMedicareEnrollmentStatusCode() {
		return Optional.ofNullable(medicareEnrollmentStatusCode);
	}

	public void setMedicareEnrollmentStatusCode(Optional<String> medicareEnrollmentStatusCode) {
		this.medicareEnrollmentStatusCode = medicareEnrollmentStatusCode.orElse(null);
	}

	public Optional<Character> getPartATerminationCode() {
		return Optional.ofNullable(partATerminationCode);
	}

	public void setPartATerminationCode(Optional<Character> partATerminationCode) {
		this.partATerminationCode = partATerminationCode.orElse(null);
	}

	public Optional<Character> getPartBTerminationCode() {
		return Optional.ofNullable(partBTerminationCode);
	}

	public void setPartBTerminationCode(Optional<Character> partBTerminationCode) {
		this.partBTerminationCode = partBTerminationCode.orElse(null);
	}

	public String getHicn() {
		return hicn;
	}

	public void setHicn(String hicn) {
		this.hicn = hicn;
	}

	public String getNameSurname() {
		return nameSurname;
	}

	public void setNameSurname(String nameSurname) {
		this.nameSurname = nameSurname;
	}

	public String getNameGiven() {
		return nameGiven;
	}

	public void setNameGiven(String nameGiven) {
		this.nameGiven = nameGiven;
	}

	public Optional<Character> getNameMiddleInitial() {
		return Optional.ofNullable(nameMiddleInitial);
	}

	public void setNameMiddleInitial(Optional<Character> nameMiddleInitial) {
		this.nameMiddleInitial = nameMiddleInitial.orElse(null);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BeneficiaryRow [beneficiaryId=");
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
		builder.append("***");
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
		 * CCW Data Dictionary: BENE_ID</a>, though note that this instance of
		 * the field is unencrypted.
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
		 * Type: <code>CHAR</code>, max chars: 1 <code>Optional</code>. See
		 * <a href=
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
		 * <code>CHAR</code>, max chars: 12. This is the beneficiary's current
		 * HIC number.
		 */
		BENE_CRNT_HIC_NUM,

		/**
		 * <code>CHAR</code>, max chars: 24. This is the beneficiary's last
		 * name.
		 */
		BENE_SRNM_NAME,

		/**
		 * <code>CHAR</code>, max chars: 5. This is the beneficiary's first
		 * name.
		 */
		BENE_GVN_NAME,

		/**
		 * <code>CHAR</code>, max chars: 1 <code>Optional</code>. This is the
		 * beneficiary's middle initial.
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