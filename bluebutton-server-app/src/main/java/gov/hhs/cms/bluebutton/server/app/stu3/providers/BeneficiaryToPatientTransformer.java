package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Objects;

import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;

/**
 * Transforms CCW {@link Beneficiary} instances into FHIR {@link Patient}
 * resources.
 */
public final class BeneficiaryToPatientTransformer {
	/**
	 * See <a href=
	 * "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/bene_id.txt">
	 * CCW Data Dictionary: BENE_ID</a>.
	 */
	public static final String CODING_SYSTEM_CCW_BENE_ID = "http://bluebutton.cms.hhs.gov/identifier#bene_id";

	/**
	 * The {@link Identifier#getSystem()} used in {@link Patient} resources to
	 * store a one-way cryptographic hash of each Medicare beneficiaries' HICN.
	 * Note that, with the SSNRI initiative, CMS is planning to move away from
	 * HICNs. However, HICNs are still the primary/only Medicare identifier for
	 * now.
	 */
	public static final String CODING_SYSTEM_CCW_BENE_HICN_HASH = "http://bluebutton.cms.hhs.gov/identifier#hicnHash";

	static final String EXTENSION_URL_US_CORE_RACE = "http://hl7.org/fhir/StructureDefinition/us-core-race";

	static final String CODING_SYSTEM_CCW_RACE = "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/race.txt";

	/**
	 * @param beneficiary
	 *            the CCW {@link Beneficiary} to transform
	 * @return a FHIR {@link Patient} resource that represents the specified
	 *         {@link Beneficiary}
	 */
	public static Patient transform(Beneficiary beneficiary) {
		Objects.requireNonNull(beneficiary);

		Patient patient = new Patient();

		patient.setId(beneficiary.getBeneficiaryId());
		patient.addIdentifier().setSystem(CODING_SYSTEM_CCW_BENE_ID).setValue(beneficiary.getBeneficiaryId());
		patient.addIdentifier().setSystem(CODING_SYSTEM_CCW_BENE_HICN_HASH).setValue(beneficiary.getHicn());

		patient.addAddress().setState(beneficiary.getStateCode()).setDistrict(beneficiary.getCountyCode())
				.setPostalCode(beneficiary.getPostalCode());

		if (beneficiary.getBirthDate() != null) {
			patient.setBirthDate(TransformerUtils.convertToDate(beneficiary.getBirthDate()));
		}

		switch (beneficiary.getSex()) {
		case ('M'):
			patient.setGender((AdministrativeGender.MALE));
			break;
		case ('F'):
			patient.setGender((AdministrativeGender.FEMALE));
			break;
		default:
			patient.setGender((AdministrativeGender.UNKNOWN));
			break;
		}

		if (beneficiary.getRace().isPresent()) {
			TransformerUtils.addExtensionCoding(patient, EXTENSION_URL_US_CORE_RACE, CODING_SYSTEM_CCW_RACE,
					"" + beneficiary.getRace().get());
		}

		HumanName name = patient.addName().addGiven(beneficiary.getNameGiven()).setFamily(beneficiary.getNameSurname())
				.setUse(HumanName.NameUse.USUAL);
		if (beneficiary.getNameMiddleInitial().isPresent())
			name.addGiven(String.valueOf(beneficiary.getNameMiddleInitial().get()));

		return patient;
	}
}
