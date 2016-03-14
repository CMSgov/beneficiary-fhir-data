package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.util.List;

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
}
