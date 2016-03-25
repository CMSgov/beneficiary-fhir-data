package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu21.model.Coding;
import org.hl7.fhir.dstu21.model.Coverage;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu21.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.ClaimType;

/**
 * Groups together the related FHIR {@link IBaseResource}s that comprise a
 * beneficiary and their claims data.
 */
public final class BeneficiaryBundle {
	private final List<IBaseResource> fhirResources;

	/**
	 * Constructs a new {@link BeneficiaryBundle} instance.
	 * 
	 * @param fhirResources
	 *            the value to use for {@link #getFhirResources()}
	 */
	public BeneficiaryBundle(List<IBaseResource> fhirResources) {
		this.fhirResources = fhirResources;
	}

	/**
	 * @return the related FHIR {@link IBaseResource}s that comprise a
	 *         beneficiary and their claims data
	 */
	public List<IBaseResource> getFhirResources() {
		return fhirResources;
	}

	/**
	 * @return the {@link Patient} resource in {@link #getFhirResources()}
	 * @throws IllegalStateException
	 *             An {@link IllegalStateException} will be thrown if exactly
	 *             one {@link Patient} is not found.
	 */
	public Patient getPatient() {
		List<IBaseResource> patients = fhirResources.stream().filter(r -> r instanceof Patient)
				.collect(Collectors.toList());
		if (patients.size() != 1)
			throw new IllegalStateException();
		return (Patient) patients.get(0);
	}

	/**
	 * @return the {@link Coverage} resource for Part B in
	 *         {@link #getFhirResources()}
	 * @throws IllegalStateException
	 *             An {@link IllegalStateException} will be thrown if exactly
	 *             one matching {@link Coverage} is not found.
	 */
	public Coverage getPartBCoverage() {
		List<Coverage> coverages = fhirResources.stream().filter(r -> r instanceof Coverage).map(r -> (Coverage) r)
				.filter(c -> c.getPlan().equals(DataTransformer.COVERAGE_PLAN_PART_B)).collect(Collectors.toList());
		if (coverages.size() != 1)
			throw new IllegalStateException();
		return coverages.get(0);
	}

	/**
	 * @return the {@link Coverage} resource for Part D in
	 *         {@link #getFhirResources()}
	 * @throws IllegalStateException
	 *             An {@link IllegalStateException} will be thrown if exactly
	 *             one matching {@link Coverage} is not found.
	 */
	public Coverage getPartDCoverage() {
		List<Coverage> coverages = fhirResources.stream().filter(r -> r instanceof Coverage).map(r -> (Coverage) r)
				.filter(c -> c.getPlan().equals(DataTransformer.COVERAGE_PLAN_PART_D)).collect(Collectors.toList());
		if (coverages.size() != 1)
			throw new IllegalStateException();
		return coverages.get(0);
	}

	/**
	 * @return the {@link ExplanationOfBenefit}s resources for Part A inpatient
	 *         claims in {@link #getFhirResources()}
	 */
	public List<ExplanationOfBenefit> getExplanationOfBenefitsForInpatient() {
		List<ExplanationOfBenefit> eobs = fhirResources.stream().filter(r -> r instanceof ExplanationOfBenefit)
				.map(r -> (ExplanationOfBenefit) r)
				.filter(e -> e.getExtension().stream()
						.filter(x -> DataTransformer.EXTENSION_CMS_CLAIM_TYPE.equals(x.getUrl())
								&& ClaimType.INPATIENT_CLAIM.getCode().equals(((Coding) x.getValue()).getCode()))
						.findAny().isPresent())
				.collect(Collectors.toList());
		return eobs;
	}

	/**
	 * @return the {@link ExplanationOfBenefit}s resources for Part A outpatient
	 *         claims in {@link #getFhirResources()}
	 */
	public List<ExplanationOfBenefit> getExplanationOfBenefitsForOutpatient() {
		List<ExplanationOfBenefit> eobs = fhirResources.stream().filter(r -> r instanceof ExplanationOfBenefit)
				.map(r -> (ExplanationOfBenefit) r)
				.filter(e -> e.getExtension().stream()
						.filter(x -> DataTransformer.EXTENSION_CMS_CLAIM_TYPE.equals(x.getUrl())
								&& ClaimType.OUTPATIENT_CLAIM.getCode().equals(((Coding) x.getValue()).getCode()))
						.findAny().isPresent())
				.collect(Collectors.toList());
		return eobs;
	}

	/**
	 * @return the {@link ExplanationOfBenefit}s resources for Part B claims in
	 *         {@link #getFhirResources()}
	 */
	public List<ExplanationOfBenefit> getExplanationOfBenefitsForCarrier() {
		List<ExplanationOfBenefit> eobs = fhirResources.stream().filter(r -> r instanceof ExplanationOfBenefit)
				.map(r -> (ExplanationOfBenefit) r)
				.filter(e -> e.getExtension().stream()
						.filter(x -> DataTransformer.EXTENSION_CMS_CLAIM_TYPE.equals(x.getUrl())
								&& ClaimType.CARRIER_NON_DME_CLAIM.getCode().equals(((Coding) x.getValue()).getCode()))
						.findAny().isPresent())
				.collect(Collectors.toList());
		return eobs;
	}

	/**
	 * @return the {@link ExplanationOfBenefit}s resources for Part D claims in
	 *         {@link #getFhirResources()}
	 */
	public List<ExplanationOfBenefit> getExplanationOfBenefitsForPartD() {
		List<ExplanationOfBenefit> eobs = fhirResources.stream().filter(r -> r instanceof ExplanationOfBenefit)
				.map(r -> (ExplanationOfBenefit) r)
				.filter(eob -> eob.getCoverage().getCoverage().getReference().equals(getPartDCoverage().getId()))
				.collect(Collectors.toList());
		return eobs;
	}
}
