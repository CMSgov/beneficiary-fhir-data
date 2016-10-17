package gov.hhs.cms.bluebutton.datapipeline.fhir;

import org.hl7.fhir.dstu21.model.Bundle;
import org.hl7.fhir.dstu21.model.Bundle.BundleType;
import org.hl7.fhir.dstu21.model.Organization;
import org.hl7.fhir.dstu21.model.Reference;
import org.hl7.fhir.instance.model.api.IAnyResource;

import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirLoader;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.DataTransformer;

/**
 * Manages the data shared between this application's beneficiary/claim/event
 * records. This is FHIR data with a specific {@link IAnyResource#getId()}
 * value, which must only exist once in the FHIR database, and must be present
 * before any other records are transformed, as those records may reference it.
 */
public final class SharedDataManager {
	/**
	 * The {@link Organization#getName()} value for CMS.
	 */
	public static final String COVERAGE_ISSUER = "Centers for Medicare and Medicaid Services";

	/**
	 * @return a Reference to the {@link Organization} for CMS, which will only
	 *         be valid if {@link #upsertSharedData()} has been run
	 */
	public static Reference createReferenceToCms() {
		return new Reference(
				String.format("Organization?name=" + DataTransformer.urlEncode(SharedDataManager.COVERAGE_ISSUER)));
	}

	private final FhirLoader fhirLoader;

	/**
	 * Constructs a new {@link SharedDataManager} instance
	 * 
	 * @param fhirLoader
	 *            the {@link FhirLoader} to use
	 */
	public SharedDataManager(FhirLoader fhirLoader) {
		this.fhirLoader = fhirLoader;
	}

	/**
	 * Creates/updates the shared data in the FHIR server.
	 */
	public void upsertSharedData() {
		Bundle sharedDataBundle = new Bundle();
		sharedDataBundle.setType(BundleType.TRANSACTION);

		Organization cms = new Organization();
		cms.setName(SharedDataManager.COVERAGE_ISSUER);
		DataTransformer.upsert(sharedDataBundle, cms, createReferenceToCms().getReference());

		fhirLoader.process(new SharedDataFhirBundle(sharedDataBundle));
	}
}
