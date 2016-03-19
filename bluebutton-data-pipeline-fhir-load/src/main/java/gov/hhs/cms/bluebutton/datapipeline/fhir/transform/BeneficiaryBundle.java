package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu21.model.Claim;
import org.hl7.fhir.dstu21.model.Coverage;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu21.model.Patient;
import org.hl7.fhir.instance.model.api.IBaseResource;

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
	 * @return the {@link Claim} resource in {@link #getFhirResources()}
	 * @throws IllegalStateException
	 *             An {@link IllegalStateException} will be thrown if exactly
	 *             one {@link Claim} is not found.
	 */
	public Claim getClaim() {
		List<IBaseResource> claims = fhirResources.stream().filter(r -> r instanceof Claim)
				.collect(Collectors.toList());
		if (claims.size() != 1)
			throw new IllegalStateException();
		return (Claim) claims.get(0);
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
	 * @return the {@link ExplanationOfBenefit}s resources for Part B claims in
	 *         {@link #getFhirResources()}
	 */
	public List<ExplanationOfBenefit> getExplanationOfBenefitsForPartB() {
		List<ExplanationOfBenefit> eobs = fhirResources.stream().filter(r -> r instanceof ExplanationOfBenefit)
				.map(r -> (ExplanationOfBenefit) r)
				.filter(eob -> eob.getCoverage().getCoverage().getReference().equals(getPartBCoverage().getId()))
				.collect(Collectors.toList());
		return eobs;
	}
}
