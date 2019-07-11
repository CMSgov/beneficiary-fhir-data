package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Objects;

import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryHistory;
import gov.hhs.cms.bluebutton.data.model.rif.MedicareBeneficiaryIdHistory;
import gov.hhs.cms.bluebutton.data.model.rif.parse.InvalidRifValueException;

/**
 * Transforms CCW {@link Beneficiary} instances into FHIR {@link Patient}
 * resources.
 */
final class BeneficiaryTransformer {
	/**
	 * @param metricRegistry
	 *            the {@link MetricRegistry} to use
	 * @param beneficiary
	 *            the CCW {@link Beneficiary} to transform
	 * @param includeIdentifiers
	 * @return a FHIR {@link Patient} resource that represents the specified
	 *         {@link Beneficiary}
	 */
	public static Patient transform(MetricRegistry metricRegistry, Beneficiary beneficiary, String includeIdentifiers) {
		Timer.Context timer = metricRegistry
				.timer(MetricRegistry.name(BeneficiaryTransformer.class.getSimpleName(), "transform")).time();
		Patient patient = transform(beneficiary, includeIdentifiers);
		timer.stop();

		return patient;
	}

	/**
	 * @param beneficiary
	 *            the CCW {@link Beneficiary} to transform
	 * @param includeIdentifiers
	 * @return a FHIR {@link Patient} resource that represents the specified
	 *         {@link Beneficiary}
	 */
	private static Patient transform(Beneficiary beneficiary, String includeIdentifiers) {
		Objects.requireNonNull(beneficiary);

		Patient patient = new Patient();

		patient.setId(beneficiary.getBeneficiaryId());
		patient.addIdentifier(
				TransformerUtils.createIdentifier(CcwCodebookVariable.BENE_ID, beneficiary.getBeneficiaryId()));
		patient.addIdentifier().setSystem(TransformerConstants.CODING_BBAPI_BENE_HICN_HASH)
				.setValue(beneficiary.getHicn());

		if (Boolean.parseBoolean(includeIdentifiers) == true) {
			patient.addIdentifier().setSystem(TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED)
					.setValue(beneficiary.getHicnUnhashed().get());
			for (BeneficiaryHistory beneHistory : beneficiary.getBeneficiaryHistories()) {
				patient.addIdentifier().setSystem(TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED)
						.setValue(beneHistory.getHicnUnhashed().get());
			}

			patient.addIdentifier().setSystem(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED)
					.setValue(beneficiary.getMedicareBeneficiaryId().get());
			for (MedicareBeneficiaryIdHistory mbiHistory : beneficiary.getMedicareBeneficiaryIdHistories()) {
				patient.addIdentifier().setSystem(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED)
						.setValue(mbiHistory.getMedicareBeneficiaryId().get());
			}
		}

		patient.addAddress().setState(beneficiary.getStateCode()).setDistrict(beneficiary.getCountyCode())
				.setPostalCode(beneficiary.getPostalCode());

		if (beneficiary.getBirthDate() != null) {
			patient.setBirthDate(TransformerUtils.convertToDate(beneficiary.getBirthDate()));
		}

		char sex = beneficiary.getSex();
		if (sex == Sex.MALE.getCode())
			patient.setGender((AdministrativeGender.MALE));
		else if (sex == Sex.FEMALE.getCode())
			patient.setGender((AdministrativeGender.FEMALE));
		else if (sex == Sex.UNKNOWN.getCode())
			patient.setGender((AdministrativeGender.UNKNOWN));
		else
			throw new InvalidRifValueException(
					String.format("Unexpected value encountered - expected '0', '1', or '2': %s", sex));

		if (beneficiary.getRace().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.RACE,
					beneficiary.getRace().get()));
		}

		HumanName name = patient.addName().addGiven(beneficiary.getNameGiven()).setFamily(beneficiary.getNameSurname())
				.setUse(HumanName.NameUse.USUAL);
		if (beneficiary.getNameMiddleInitial().isPresent())
			name.addGiven(String.valueOf(beneficiary.getNameMiddleInitial().get()));

		return patient;
	}
}
