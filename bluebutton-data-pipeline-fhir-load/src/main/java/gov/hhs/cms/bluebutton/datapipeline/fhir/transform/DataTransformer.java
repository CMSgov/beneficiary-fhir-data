package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.util.Objects;
import java.util.stream.Stream;

import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;

/**
 * Handles the translation from source/CCW {@link CurrentBeneficiary} data into
 * FHIR {@link ExplanationOfBenefit} data.
 */
public final class DataTransformer {
	/**
	 * @param sourceBeneficiaries
	 *            the source/CCW {@link CurrentBeneficiary} records to be
	 *            transformed
	 * @return a {@link Stream} of FHIR {@link ExplanationOfBenefit} records
	 */
	public Stream<ExplanationOfBenefit> transformSourceData(Stream<CurrentBeneficiary> sourceBeneficiaries) {
		// FIXME remove filter once this is less fake
		Stream<ExplanationOfBenefit> transformedRecords = sourceBeneficiaries.map(b -> convertToFhir(b))
				.filter(Objects::nonNull);
		return transformedRecords;
	}

	/**
	 * @param sourceBeneficiary
	 *            a source {@link CurrentBeneficiary} record, along with its
	 *            associated claims data
	 * @return an {@link ExplanationOfBenefit} that contains the specified
	 *         beneficiary and its associated claims data
	 */
	static ExplanationOfBenefit convertToFhir(CurrentBeneficiary sourceBeneficiary) {
		// TODO
		return null;
	}
}
