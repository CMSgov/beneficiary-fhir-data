package gov.hhs.cms.bluebutton.datapipeline.fhir;

import org.hl7.fhir.dstu21.model.Bundle;

/**
 * This {@link LoadableFhirBundle} implementation represents FHIR {@link Bundle}
 * transactions that are created for the system's shared data.
 * 
 * @see SharedDataManager
 */
public final class SharedDataFhirBundle implements LoadableFhirBundle {
	private final Bundle fhirBundle;

	/**
	 * Constructs a new {@link SharedDataFhirBundle} instance.
	 * 
	 * @param fhirBundle
	 *            the value to use for {@link #getResult()}
	 */
	public SharedDataFhirBundle(Bundle fhirBundle) {
		this.fhirBundle = fhirBundle;
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.fhir.LoadableFhirBundle#getSourceType()
	 */
	@Override
	public String getSourceType() {
		return SharedDataManager.class.getSimpleName();
	}

	/**
	 * @see gov.hhs.cms.bluebutton.datapipeline.fhir.LoadableFhirBundle#getResult()
	 */
	@Override
	public Bundle getResult() {
		return fhirBundle;
	}
}
