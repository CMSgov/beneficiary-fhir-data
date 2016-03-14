package gov.hhs.cms.bluebutton.datapipeline.fhir.load;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.fhir.LoadAppOptions;
import gov.hhs.cms.bluebutton.datapipeline.fhir.SpringConfigForTests;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.BeneficiaryBundle;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.DataTransformer;

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
	 * Verifies that {@link DataTransformer} works correctly when passed an
	 * empty stream.
	 * 
	 * @throws URISyntaxException
	 *             (won't happen: URI is hardcoded)
	 */
	@Test
	public void loadEmptyStream() throws URISyntaxException {
		URI fhirServer = new URI("https://example.com/foo");
		LoadAppOptions options = new LoadAppOptions(fhirServer);
		FhirLoader loader = new FhirLoader(options);

		Stream<BeneficiaryBundle> fhirStream = new ArrayList<BeneficiaryBundle>().stream();
		List<FhirResult> results = loader.insertFhirRecords(fhirStream);
		Assert.assertNotNull(results);
		Assert.assertEquals(0, results.size());
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when passed a
	 * small, hand-crafted data set.
	 * 
	 * @throws URISyntaxException
	 *             (won't happen: URI is hardcoded)
	 */
	@Test
	public void loadSmallSample() throws URISyntaxException {
		// Use the DataTransformer to create some sample FHIR resources.
		CurrentBeneficiary beneA = new CurrentBeneficiary().setId(0).setBirthDate(LocalDate.now());
		PartAClaimFact factA = new PartAClaimFact().setId(0L).setBeneficiary(beneA).setAdmittingDiagnosisCode("foo");
		beneA.getPartAClaimFacts().add(factA);
		CurrentBeneficiary beneB = new CurrentBeneficiary().setId(1).setBirthDate(LocalDate.now());
		PartAClaimFact factB = new PartAClaimFact().setId(1L).setBeneficiary(beneB).setAdmittingDiagnosisCode("foo");
		beneB.getPartAClaimFacts().add(factB);
		Stream<BeneficiaryBundle> fhirStream = new DataTransformer()
				.transformSourceData(Arrays.asList(beneA, beneB).stream());

		// Push the data to FHIR.
		URI fhirServer = new URI("http://localhost:8080/hapi-fhir/baseDstu2");
		LoadAppOptions options = new LoadAppOptions(fhirServer);
		FhirLoader loader = new FhirLoader(options);
		List<FhirResult> results = loader.insertFhirRecords(fhirStream);

		// Verify the results.
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.size());
		Assert.assertEquals(6, results.get(0).getResourcesPushedCount());

		// TODO verify results by actually querying the server.
	}
}
