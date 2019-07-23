package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Enumerations.AdministrativeGender;
import org.hl7.fhir.dstu3.model.Extension;
import org.hl7.fhir.dstu3.model.HumanName;
import org.hl7.fhir.dstu3.model.Identifier;
import org.hl7.fhir.dstu3.model.Patient;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryHistory;
import gov.hhs.cms.bluebutton.data.model.rif.MedicareBeneficiaryIdHistory;
import gov.hhs.cms.bluebutton.data.model.rif.parse.InvalidRifValueException;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.PatientResourceProvider.IncludeIdentifiersMode;

/**
 * Transforms CCW {@link Beneficiary} instances into FHIR {@link Patient}
 * resources.
 */
final class BeneficiaryTransformer {
	/**
	 * @param metricRegistry         the {@link MetricRegistry} to use
	 * @param beneficiary            the CCW {@link Beneficiary} to transform
	 * @param includeIdentifiersMode the {@link IncludeIdentifiersMode} to use
	 * @return a FHIR {@link Patient} resource that represents the specified
	 *         {@link Beneficiary}
	 */
	public static Patient transform(MetricRegistry metricRegistry, Beneficiary beneficiary,
			IncludeIdentifiersMode includeIdentifiersMode) {
		Timer.Context timer = metricRegistry
				.timer(MetricRegistry.name(BeneficiaryTransformer.class.getSimpleName(), "transform")).time();
		Patient patient = transform(beneficiary, includeIdentifiersMode);
		timer.stop();

		return patient;
	}

	/**
	 * @param beneficiary            the CCW {@link Beneficiary} to transform
	 * @param includeIdentifiersMode the {@link IncludeIdentifiersMode} to use
	 * @return a FHIR {@link Patient} resource that represents the specified
	 *         {@link Beneficiary}
	 */
	private static Patient transform(Beneficiary beneficiary, IncludeIdentifiersMode includeIdentifiersMode) {
		Objects.requireNonNull(beneficiary);

		Patient patient = new Patient();

		patient.setId(beneficiary.getBeneficiaryId());
		patient.addIdentifier(
				TransformerUtils.createIdentifier(CcwCodebookVariable.BENE_ID, beneficiary.getBeneficiaryId()));
		patient.addIdentifier().setSystem(TransformerConstants.CODING_BBAPI_BENE_HICN_HASH)
				.setValue(beneficiary.getHicn());

		if (includeIdentifiersMode == IncludeIdentifiersMode.INCLUDE_HICNS_AND_MBIS) {
			Extension currentIdentifier = TransformerUtils
					.createIdentifierCurrencyExtension(CurrencyIdentifier.CURRENT);
			
			addUnhashedIdentifier(patient, beneficiary.getHicnUnhashed().get(),
					TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED, currentIdentifier);

			addUnhashedIdentifier(patient, beneficiary.getMedicareBeneficiaryId().get(),
					TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED, currentIdentifier);

			Extension historicalIdentifier = TransformerUtils
					.createIdentifierCurrencyExtension(CurrencyIdentifier.HISTORIC);

			List<String> unhashedHicns = new ArrayList<String>();
			for (BeneficiaryHistory beneHistory : beneficiary.getBeneficiaryHistories()) {
				unhashedHicns.add(beneHistory.getHicnUnhashed().get());
			}
			List<String> unhashedHicnsNoDupes = unhashedHicns.stream().distinct().collect(Collectors.toList());
			for (String hicn : unhashedHicnsNoDupes) {
				addUnhashedIdentifier(patient, hicn, TransformerConstants.CODING_BBAPI_BENE_HICN_UNHASHED,
						historicalIdentifier);
			}

			List<String> unhashedMbis = new ArrayList<String>();
			for (MedicareBeneficiaryIdHistory mbiHistory : beneficiary.getMedicareBeneficiaryIdHistories()) {
				unhashedMbis.add(mbiHistory.getMedicareBeneficiaryId().get());
			}
			List<String> unhashedMbisNoDupes = unhashedMbis.stream().distinct().collect(Collectors.toList());
			for (String mbi : unhashedMbisNoDupes) {
				addUnhashedIdentifier(patient, mbi, TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED,
						historicalIdentifier);
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

	/**
	 * @param patient
	 *            the FHIR {@link Patient} resource to add the {@link Identifier} to
	 * @param value
	 *            the value for {@link Identifier#getValue()}
	 * @param system
	 *            the value for {@link Identifier#getSystem()}
	 * @param identifierCurrencyExtension
	 *            the {@link Extension} to add to the {@link Identifier}
	 */
	private static void addUnhashedIdentifier(Patient patient, String value, String system,
			Extension identifierCurrencyExtension) {
		patient.addIdentifier().setSystem(system).setValue(value).addExtension(identifierCurrencyExtension);
	}

	/**
	 * Enumerates the options for the currency of an {@link Identifier}.
	 */
	public static enum CurrencyIdentifier {
		CURRENT,

		HISTORIC;
	}
}
