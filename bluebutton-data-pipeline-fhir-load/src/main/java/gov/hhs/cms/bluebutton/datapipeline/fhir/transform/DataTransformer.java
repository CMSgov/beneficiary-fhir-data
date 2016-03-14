package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.sql.Date;
import java.util.Arrays;
import java.util.stream.Stream;

import org.hl7.fhir.dstu21.model.IdType;
import org.hl7.fhir.dstu21.model.Patient;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;

/**
 * Handles the translation from source/CCW {@link CurrentBeneficiary} data into
 * FHIR {@link BeneficiaryBundle}s.
 */
public final class DataTransformer {
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
		Patient patient = new Patient();
		patient.setId(IdType.newRandomUuid());

		patient.addIdentifier().setValue("" + sourceBeneficiary.getId());
		patient.setBirthDate(Date.valueOf(sourceBeneficiary.getBirthDate()));

		return new BeneficiaryBundle(Arrays.asList(patient));
	}
}
