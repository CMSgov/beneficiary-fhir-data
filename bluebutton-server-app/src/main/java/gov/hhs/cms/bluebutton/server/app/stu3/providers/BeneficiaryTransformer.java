package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Objects;

import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;

/**
 * Transforms CCW {@link Beneficiary} instances into FHIR {@link Patient}
 * resources.
 */
final class BeneficiaryTransformer {

	private static final Logger LOGGER = LoggerFactory.getLogger(BeneficiaryTransformer.class);

	/**
	 * @param metricRegistry the {@link MetricRegistry} to use
	 * @param beneficiary    the CCW {@link Beneficiary} to transform
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
	 * @param beneficiary the CCW {@link Beneficiary} to transform
	 * @return a FHIR {@link Patient} resource that represents the specified
	 *         {@link Beneficiary}
	 */
	private static Patient transform(Beneficiary beneficiary) {
		Objects.requireNonNull(beneficiary);

		Patient patient = new Patient();

		patient.setId(beneficiary.getBeneficiaryId());
		patient.addIdentifier(
				TransformerUtils.createIdentifier(CcwCodebookVariable.BENE_ID, beneficiary.getBeneficiaryId()));
		patient.addIdentifier().setSystem(TransformerConstants.CODING_BBAPI_BENE_HICN_HASH)
				.setValue(beneficiary.getHicn());

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
		else {
			LOGGER.warn(String.format("Unexpected value encountered - expected '0', '1', or '2': %s", sex));
			patient.setGender((AdministrativeGender.UNKNOWN));
		}

		if (beneficiary.getRace().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.RACE,
					beneficiary.getRace().get()));
		}

		HumanName name = patient.addName().addGiven(beneficiary.getNameGiven()).setFamily(beneficiary.getNameSurname())
				.setUse(HumanName.NameUse.USUAL);
		if (beneficiary.getNameMiddleInitial().isPresent())
			name.addGiven(String.valueOf(beneficiary.getNameMiddleInitial().get()));

		// The reference year of the enrollment data
		if (beneficiary.getBeneEnrollmentReferenceYear().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionDate(CcwCodebookVariable.RFRNC_YR,
					beneficiary.getBeneEnrollmentReferenceYear()));
		}

		// Monthly Medicare-Medicaid dual eligibility codes
		if (beneficiary.getMedicaidDualEligibilityJanCode().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.DUAL_01,
					beneficiary.getMedicaidDualEligibilityJanCode()));
		}
		if (beneficiary.getMedicaidDualEligibilityFebCode().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.DUAL_02,
					beneficiary.getMedicaidDualEligibilityFebCode()));
		}
		if (beneficiary.getMedicaidDualEligibilityMarCode().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.DUAL_03,
					beneficiary.getMedicaidDualEligibilityMarCode()));
		}
		if (beneficiary.getMedicaidDualEligibilityAprCode().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.DUAL_04,
					beneficiary.getMedicaidDualEligibilityAprCode()));
		}
		if (beneficiary.getMedicaidDualEligibilityMayCode().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.DUAL_05,
					beneficiary.getMedicaidDualEligibilityMayCode()));
		}
		if (beneficiary.getMedicaidDualEligibilityJunCode().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.DUAL_06,
					beneficiary.getMedicaidDualEligibilityJunCode()));
		}
		if (beneficiary.getMedicaidDualEligibilityJulCode().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.DUAL_07,
					beneficiary.getMedicaidDualEligibilityJulCode()));
		}
		if (beneficiary.getMedicaidDualEligibilityAugCode().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.DUAL_08,
					beneficiary.getMedicaidDualEligibilityAugCode()));
		}
		if (beneficiary.getMedicaidDualEligibilitySeptCode().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.DUAL_09,
					beneficiary.getMedicaidDualEligibilitySeptCode()));
		}
		if (beneficiary.getMedicaidDualEligibilityOctCode().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.DUAL_10,
					beneficiary.getMedicaidDualEligibilityOctCode()));
		}
		if (beneficiary.getMedicaidDualEligibilityNovCode().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.DUAL_11,
					beneficiary.getMedicaidDualEligibilityNovCode()));
		}
		if (beneficiary.getMedicaidDualEligibilityDecCode().isPresent()) {
			patient.addExtension(TransformerUtils.createExtensionCoding(patient, CcwCodebookVariable.DUAL_12,
					beneficiary.getMedicaidDualEligibilityDecCode()));
		}

		return patient;
	}
}
