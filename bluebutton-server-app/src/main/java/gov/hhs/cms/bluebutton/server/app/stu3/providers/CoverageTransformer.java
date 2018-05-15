package gov.hhs.cms.bluebutton.server.app.stu3.providers;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.Coverage.CoverageStatus;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.codebook.data.CcwCodebookVariable;
import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;

/**
 * Transforms CCW {@link Beneficiary} instances into FHIR {@link Coverage}
 * resources.
 */
final class CoverageTransformer {
	/**
	 * @param metricRegistry
	 *            the {@link MetricRegistry} to use
	 * @param medicareSegment
	 *            the {@link MedicareSegment} to generate a {@link Coverage}
	 *            resource for
	 * @param beneficiary
	 *            the {@link Beneficiary} to generate a {@link Coverage} resource
	 *            for
	 * @return the {@link Coverage} resource that was generated
	 */
	public static Coverage transform(MetricRegistry metricRegistry, MedicareSegment medicareSegment,
			Beneficiary beneficiary) {
		Objects.requireNonNull(medicareSegment);

		if (medicareSegment == MedicareSegment.PART_A)
			return transformPartA(metricRegistry, beneficiary);
		else if (medicareSegment == MedicareSegment.PART_B)
			return transformPartB(metricRegistry, beneficiary);
		else if (medicareSegment == MedicareSegment.PART_D)
			return transformPartD(metricRegistry, beneficiary);
		else
			throw new BadCodeMonkeyException();
	}

	/**
	 * @param metricRegistry
	 *            the {@link MetricRegistry} to use
	 * @param beneficiary
	 *            the CCW {@link Beneficiary} to generate the {@link Coverage}s for
	 * @return the FHIR {@link Coverage} resources that can be generated from the
	 *         specified {@link Beneficiary}
	 */
	public static List<Coverage> transform(MetricRegistry metricRegistry, Beneficiary beneficiary) {
		return Arrays.stream(MedicareSegment.values()).map(s -> transform(metricRegistry, s, beneficiary))
				.collect(Collectors.toList());
	}

	/**
	 * @param metricRegistry
	 *            the {@link MetricRegistry} to use
	 * @param beneficiary
	 *            the {@link Beneficiary} to generate a
	 *            {@link MedicareSegment#PART_A} {@link Coverage} resource for
	 * @return {@link MedicareSegment#PART_A} {@link Coverage} resource for the
	 *         specified {@link Beneficiary}
	 */
	private static Coverage transformPartA(MetricRegistry metricRegistry, Beneficiary beneficiary) {
		Timer.Context timer = metricRegistry
				.timer(MetricRegistry.name(CoverageTransformer.class.getSimpleName(), "transform", "part_a")).time();

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

		timer.stop();
		return coverage;
	}

	/**
	 * @param metricRegistry
	 *            the {@link MetricRegistry} to use
	 * @param beneficiary
	 *            the {@link Beneficiary} to generate a
	 *            {@link MedicareSegment#PART_B} {@link Coverage} resource for
	 * @return {@link MedicareSegment#PART_B} {@link Coverage} resource for the
	 *         specified {@link Beneficiary}
	 */
	private static Coverage transformPartB(MetricRegistry metricRegistry, Beneficiary beneficiary) {
		Timer.Context timer = metricRegistry
				.timer(MetricRegistry.name(CoverageTransformer.class.getSimpleName(), "transform", "part_b")).time();

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

		timer.stop();
		return coverage;
	}

	/**
	 * @param metricRegistry
	 *            the {@link MetricRegistry} to use
	 * @param beneficiary
	 *            the {@link Beneficiary} to generate a
	 *            {@link MedicareSegment#PART_D} {@link Coverage} resource for
	 * @return {@link MedicareSegment#PART_D} {@link Coverage} resource for the
	 *         specified {@link Beneficiary}
	 */
	private static Coverage transformPartD(MetricRegistry metricRegistry, Beneficiary beneficiary) {
		Timer.Context timer = metricRegistry
				.timer(MetricRegistry.name(CoverageTransformer.class.getSimpleName(), "transform", "part_d")).time();

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

		timer.stop();
		return coverage;
	}
}
