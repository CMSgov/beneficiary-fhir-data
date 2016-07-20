package gov.hhs.cms.bluebutton.datapipeline.fhir.load;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import com.codahale.metrics.MetricRegistry;

import gov.hhs.cms.bluebutton.datapipeline.fhir.LoadAppOptions;
import gov.hhs.cms.bluebutton.datapipeline.fhir.SpringConfigForTests;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.BeneficiaryBundle;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.TransformedBundle;

/**
 * Unit tests for {@link FhirLoader}.
 */
@ContextConfiguration(classes = { SpringConfigForTests.class })
public final class FhirLoaderTest {
	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	/**
	 * Verifies that {@link FhirLoader} works correctly when passed an empty
	 * stream.
	 * 
	 * @throws URISyntaxException
	 *             (won't happen: URI is hardcoded)
	 */
	@Test
	public void loadEmptyStream() throws URISyntaxException {
		URI fhirServer = new URI("https://example.com/foo");
		LoadAppOptions options = new LoadAppOptions(fhirServer);
		FhirLoader loader = new FhirLoader(new MetricRegistry(), options);

		Stream<BeneficiaryBundle> fhirStream = new ArrayList<BeneficiaryBundle>().stream();
		List<FhirResult> results = loader.insertFhirRecords(fhirStream);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.size());
	}

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
		LoadAppOptions options = new LoadAppOptions(fhirServer);
		FhirLoader loader = new FhirLoader(new MetricRegistry(), options);

		Stream<TransformedBundle> fhirStream = new ArrayList<TransformedBundle>().stream();
		Stream<FhirBundleResult> results = loader.process(fhirStream);
		Assert.assertNotNull(results);
		Assert.assertEquals(0L, results.count());
	}
}
