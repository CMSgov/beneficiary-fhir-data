package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.dstu21.model.Address;
import org.hl7.fhir.dstu21.model.Claim;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu21.model.IdType;
import org.hl7.fhir.dstu21.model.Patient;
import org.hl7.fhir.dstu21.model.Reference;
import org.hl7.fhir.instance.model.api.IBaseResource;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;

/**
 * Handles the translation from source/CCW {@link CurrentBeneficiary} data into
 * FHIR {@link BeneficiaryBundle}s.
 */
public final class DataTransformer {
	static final String CODING_SYSTEM_ICD9_DIAG = "http://hl7.org/fhir/sid/icd-9-cm/diagnosis";

	/**
	 * @param sourceBeneficiaries
	 *            the source/CCW {@link CurrentBeneficiary} records to be
	 *            transformed
	 * @return a {@link Stream} of FHIR {@link BeneficiaryBundle}s
	 */
	public Stream<BeneficiaryBundle> transformSourceData(Stream<CurrentBeneficiary> sourceBeneficiaries) {
		Stream<BeneficiaryBundle> transformedRecords = sourceBeneficiaries.map(b -> convertToFhir(b));
		return transformedRecords;
	}

	/**
	 * @param sourceBeneficiary
	 *            a source {@link CurrentBeneficiary} record, along with its
	 *            associated claims data
	 * @return a {@link BeneficiaryBundle} that represents the specified
	 *         beneficiary and its associated claims data
	 */
	static BeneficiaryBundle convertToFhir(CurrentBeneficiary sourceBeneficiary) {
		List<IBaseResource> resources = new ArrayList<>();

		Patient patient = new Patient();
		resources.add(patient);
		patient.setId(IdType.newRandomUuid());
		patient.addIdentifier().setValue("" + sourceBeneficiary.getId());
		patient.setBirthDate(Date.valueOf(sourceBeneficiary.getBirthDate()));
		patient.addName().addFamily(sourceBeneficiary.getSurname()).addGiven(sourceBeneficiary.getGivenName());

		List<String> addressComponents = Arrays.asList(sourceBeneficiary.getContactAddress(),
				sourceBeneficiary.getContactAddressZip());
		addressComponents = addressComponents.stream().filter(c -> (c != null && c.trim().length() > 0))
				.collect(Collectors.toList());
		if (!addressComponents.isEmpty()) {
			Address address = patient.addAddress();
			for (String addressComponent : addressComponents)
				address.addLine(addressComponent);
		}

		for (PartAClaimFact sourcePartAClaim : sourceBeneficiary.getPartAClaimFacts()) {
			ExplanationOfBenefit eob = new ExplanationOfBenefit();
			resources.add(eob);
			eob.setId(IdType.newRandomUuid());
			eob.setPatient(new Reference().setReference(patient.getId()));
			eob.addIdentifier().setValue("" + sourcePartAClaim.getId());

			Claim claim = new Claim();
			resources.add(claim);
			claim.setId(IdType.newRandomUuid());
			eob.setClaim(new Reference().setReference(claim.getId()));
			claim.addIdentifier().setValue("" + sourcePartAClaim.getId());
			if (sourcePartAClaim.getAdmittingDiagnosisCode() != null)
				claim.addDiagnosis().getDiagnosis().setSystem(CODING_SYSTEM_ICD9_DIAG)
						.setCode(sourcePartAClaim.getAdmittingDiagnosisCode());
		}

		return new BeneficiaryBundle(resources);
	}
}
