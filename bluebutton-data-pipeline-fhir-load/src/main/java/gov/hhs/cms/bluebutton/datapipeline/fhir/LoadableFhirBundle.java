package gov.hhs.cms.bluebutton.datapipeline.fhir;

import org.hl7.fhir.dstu21.model.Bundle;

import ca.uhn.fhir.rest.client.IGenericClient;

/**
 * Implementations of this interface represent intermediate FHIR bundles that
 * can be pushed to a FHIR server via {@link IGenericClient#transaction()}.
 */
public interface LoadableFhirBundle {
	/**
	 * @return a human-readable {@link String} that identifies what type of
	 *         source this {@link LoadableFhirBundle} was created from, suitable
	 *         for use in debugging and log messages
	 */
	String getSourceType();

	/**
	 * @return the FHIR transaction {@link Bundle} that can be pushed to a FHIR
	 *         server via {@link IGenericClient#transaction()}
	 */
	Bundle getResult();
}
