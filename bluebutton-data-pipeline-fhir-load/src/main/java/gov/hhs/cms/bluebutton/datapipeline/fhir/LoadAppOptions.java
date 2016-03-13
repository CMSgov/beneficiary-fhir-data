package gov.hhs.cms.bluebutton.datapipeline.fhir;

import java.net.URI;

/**
 * Models the user-configurable application options.
 */
public final class LoadAppOptions {
	private final URI fhirServer;

	/**
	 * Constructs a new {@link LoadAppOptions} instance.
	 * 
	 * @param fhirServer
	 *            the value to use for {@link #getFhirServer()}
	 */
	public LoadAppOptions(URI fhirServer) {
		this.fhirServer = fhirServer;
	}

	/**
	 * @return the {@link URI} for the FHIR server that the application should
	 *         push data to
	 */
	public URI getFhirServer() {
		return fhirServer;
	}
}
