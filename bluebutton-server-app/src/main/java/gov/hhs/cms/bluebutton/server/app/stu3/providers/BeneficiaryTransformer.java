package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Objects;

import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;

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
	 * @return a FHIR {@link Patient} resource that represents the specified
	 *         {@link Beneficiary}
	 */
	public static Patient transform(MetricRegistry metricRegistry, Beneficiary beneficiary) {
		Timer.Context timer = metricRegistry
				.timer(MetricRegistry.name(BeneficiaryTransformer.class.getSimpleName(), "transform")).time();
		Patient patient = transform(beneficiary);
		timer.stop();

		return patient;
	}

	/**
	 * @param beneficiary
	 *            the CCW {@link Beneficiary} to transform
	 * @return a FHIR {@link Patient} resource that represents the specified
	 *         {@link Beneficiary}
	 */
	private static Patient transform(Beneficiary beneficiary) {
		Objects.requireNonNull(beneficiary);

		Patient patient = new Patient();

		patient.setId(beneficiary.getBeneficiaryId());
		patient.addIdentifier(
				TransformerUtils.createIdentifier(CcwCodebookVariable.BENE_ID, beneficiary.getBeneficiaryId()));

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
