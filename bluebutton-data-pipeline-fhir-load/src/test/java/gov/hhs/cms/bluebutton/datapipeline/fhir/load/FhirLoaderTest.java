package gov.hhs.cms.bluebutton.datapipeline.fhir.load;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.junit.Test;

import com.codahale.metrics.MetricRegistry;

import gov.hhs.cms.bluebutton.datapipeline.fhir.LoadAppOptions;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.TransformedBundle;

/**
 * Unit tests for {@link FhirLoader}.
 */
public final class FhirLoaderTest {
	/**
	 * Verifies that {@link FhirLoader} works correctly when passed an empty
	 * stream.
	 * 
	 * @throws URISyntaxException
	 *             (won't happen: URI is hardcoded)
	 */
	@Test
	public void processEmptyStream() throws URISyntaxException {
		URI fhirServer = new URI("https://example.com/foo");
		LoadAppOptions options = new LoadAppOptions(fhirServer, FhirTestUtilities.getClientKeyStorePath(),
				FhirTestUtilities.CLIENT_KEY_STORE_PASSWORD, FhirTestUtilities.getClientTrustStorePath(),
				FhirTestUtilities.CLIENT_TRUST_STORE_PASSWORD);
		FhirLoader loader = new FhirLoader(new MetricRegistry(), options);

		Stream<TransformedBundle> fhirStream = new ArrayList<TransformedBundle>().stream();
		loader.process(fhirStream, error -> {
			throw new AssertionError();
		}, error -> {
			throw new AssertionError();
		});
	}
}
