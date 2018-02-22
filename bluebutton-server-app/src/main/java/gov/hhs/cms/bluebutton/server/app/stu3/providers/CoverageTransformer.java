package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.CoverageStatus;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;

/**
 * Transforms CCW {@link Beneficiary} instances into FHIR {@link Coverage}
 * resources.
 */
final class CoverageTransformer {
	/**
	 * @param medicareSegment
	 *            the {@link MedicareSegment} to generate a {@link Coverage}
	 *            resource for
	 * @param beneficiary
	 *            the {@link Beneficiary} to generate a {@link Coverage}
	 *            resource for
	 * @return the {@link Coverage} resource that was generated
	 */
	public static Coverage transform(MedicareSegment medicareSegment, Beneficiary beneficiary) {
		Objects.requireNonNull(medicareSegment);

		if (medicareSegment == MedicareSegment.PART_A)
			return transformPartA(beneficiary);
		else if (medicareSegment == MedicareSegment.PART_B)
			return transformPartB(beneficiary);
		else if (medicareSegment == MedicareSegment.PART_D)
			return transformPartD(beneficiary);
		else
			throw new BadCodeMonkeyException();
	}

	/**
	 * @param beneficiary
	 *            the CCW {@link Beneficiary} to generate the {@link Coverage}s
	 *            for
	 * @return the FHIR {@link Coverage} resources that can be generated from
	 *         the specified {@link Beneficiary}
	 */
	public static List<Coverage> transform(Beneficiary beneficiary) {
		return Arrays.stream(MedicareSegment.values()).map(s -> transform(s, beneficiary)).collect(Collectors.toList());
	}

	/**
	 * @param beneficiary
	 *            the {@link Beneficiary} to generate a
	 *            {@link MedicareSegment#PART_A} {@link Coverage} resource for
	 * @return {@link MedicareSegment#PART_A} {@link Coverage} resource for the
	 *         specified {@link Beneficiary}
	 */
	private static Coverage transformPartA(Beneficiary beneficiary) {
		Objects.requireNonNull(beneficiary);

		Coverage coverage = new Coverage();
		coverage.setId(TransformerUtils.buildCoverageId(MedicareSegment.PART_A, beneficiary));
		if (beneficiary.getPartATerminationCode().isPresent() && beneficiary.getPartATerminationCode().get().equals('0'))
			coverage.setStatus(CoverageStatus.ACTIVE);
		else
			coverage.setStatus(CoverageStatus.CANCELLED);
		coverage.getGrouping().setSubGroup(TransformerConstants.COVERAGE_PLAN)
				.setSubPlan(TransformerConstants.COVERAGE_PLAN_PART_A);
		coverage.setType(
				TransformerUtils.createCodeableConcept(TransformerConstants.COVERAGE_PLAN,
						TransformerConstants.COVERAGE_PLAN_PART_A));
		coverage.setBeneficiary(TransformerUtils.referencePatient(beneficiary));
		if (beneficiary.getMedicareEnrollmentStatusCode().isPresent()) {
			coverage.addExtension(TransformerUtils.createExtensionCoding(coverage, CcwCodebookVariable.MS_CD,
					beneficiary.getMedicareEnrollmentStatusCode()));
		}
		if (beneficiary.getEntitlementCodeOriginal().isPresent()) {
			coverage.addExtension(TransformerUtils.createExtensionCoding(coverage, CcwCodebookVariable.OREC,
					beneficiary.getEntitlementCodeOriginal()));
		}
		if (beneficiary.getEntitlementCodeCurrent().isPresent()) {
			coverage.addExtension(TransformerUtils.createExtensionCoding(coverage, CcwCodebookVariable.CREC,
					beneficiary.getEntitlementCodeCurrent()));
		}
		if (beneficiary.getEndStageRenalDiseaseCode().isPresent()) {
			coverage.addExtension(TransformerUtils.createExtensionCoding(coverage, CcwCodebookVariable.ESRD_IND,
					beneficiary.getEndStageRenalDiseaseCode()));
		}
		if (beneficiary.getPartATerminationCode().isPresent()) {
			coverage.addExtension(TransformerUtils.createExtensionCoding(coverage, CcwCodebookVariable.A_TRM_CD,
					beneficiary.getPartATerminationCode()));
		}

		return coverage;
	}

	/**
	 * @param beneficiary
	 *            the {@link Beneficiary} to generate a
	 *            {@link MedicareSegment#PART_B} {@link Coverage} resource for
	 * @return {@link MedicareSegment#PART_B} {@link Coverage} resource for the
	 *         specified {@link Beneficiary}
	 */
	private static Coverage transformPartB(Beneficiary beneficiary) {
		Objects.requireNonNull(beneficiary);

		Coverage coverage = new Coverage();
		coverage.setId(TransformerUtils.buildCoverageId(MedicareSegment.PART_B, beneficiary));
		if (beneficiary.getPartBTerminationCode().isPresent()
				&& beneficiary.getPartBTerminationCode().get().equals('0'))
			coverage.setStatus(CoverageStatus.ACTIVE);
		else
			coverage.setStatus(CoverageStatus.CANCELLED);
		coverage.getGrouping().setSubGroup(TransformerConstants.COVERAGE_PLAN)
				.setSubPlan(TransformerConstants.COVERAGE_PLAN_PART_B);
		coverage.setType(TransformerUtils.createCodeableConcept(TransformerConstants.COVERAGE_PLAN,
				TransformerConstants.COVERAGE_PLAN_PART_B));
		coverage.setBeneficiary(TransformerUtils.referencePatient(beneficiary));
		if (beneficiary.getMedicareEnrollmentStatusCode().isPresent()) {
			coverage.addExtension(TransformerUtils.createExtensionCoding(coverage, CcwCodebookVariable.MS_CD,
					beneficiary.getMedicareEnrollmentStatusCode()));
		}
		if (beneficiary.getPartBTerminationCode().isPresent()) {
			coverage.addExtension(TransformerUtils.createExtensionCoding(coverage, CcwCodebookVariable.B_TRM_CD,
					beneficiary.getPartBTerminationCode()));
		}

		return coverage;
	}

	/**
	 * @param beneficiary
	 *            the {@link Beneficiary} to generate a
	 *            {@link MedicareSegment#PART_D} {@link Coverage} resource for
	 * @return {@link MedicareSegment#PART_D} {@link Coverage} resource for the
	 *         specified {@link Beneficiary}
	 */
	private static Coverage transformPartD(Beneficiary beneficiary) {
		Objects.requireNonNull(beneficiary);

		Coverage coverage = new Coverage();
		coverage.setId(TransformerUtils.buildCoverageId(MedicareSegment.PART_D, beneficiary));
		coverage.getGrouping().setSubGroup(TransformerConstants.COVERAGE_PLAN)
				.setSubPlan(TransformerConstants.COVERAGE_PLAN_PART_D);
		coverage.setType(TransformerUtils.createCodeableConcept(TransformerConstants.COVERAGE_PLAN,
				TransformerConstants.COVERAGE_PLAN_PART_D));
		coverage.setStatus(CoverageStatus.ACTIVE);
		coverage.setBeneficiary(TransformerUtils.referencePatient(beneficiary));
		if (beneficiary.getMedicareEnrollmentStatusCode().isPresent()) {
			coverage.addExtension(TransformerUtils.createExtensionCoding(coverage, CcwCodebookVariable.MS_CD,
					beneficiary.getMedicareEnrollmentStatusCode()));
		}

		return coverage;
	}
}
