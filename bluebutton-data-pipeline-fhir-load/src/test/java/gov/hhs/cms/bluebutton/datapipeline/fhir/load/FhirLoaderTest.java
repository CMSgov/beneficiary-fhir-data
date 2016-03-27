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

import com.codahale.metrics.MetricRegistry;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.AllClaimsProfile;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.ClaimType;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimRevLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.Procedure;
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
	 * Verifies that {@link FhirLoader} works correctly when passed a small,
	 * hand-crafted data set.
	 * 
	 * @throws URISyntaxException
	 *             (won't happen: URI is hardcoded)
	 */
	@Test
	public void loadSmallSample() throws URISyntaxException {
		// Use the DataTransformer to create some sample FHIR resources.
		CurrentBeneficiary beneA = new CurrentBeneficiary().setId(0).setBirthDate(LocalDate.now());
		PartAClaimFact outpatientClaimForBeneA = new PartAClaimFact().setId(0L).setBeneficiary(beneA)
				.setClaimProfile(new AllClaimsProfile().setId(1L).setClaimType(ClaimType.OUTPATIENT_CLAIM))
				.setAdmittingDiagnosisCode("foo");
		beneA.getPartAClaimFacts().add(outpatientClaimForBeneA);
		PartAClaimRevLineFact outpatientRevLineForBeneA = new PartAClaimRevLineFact().setClaim(outpatientClaimForBeneA)
				.setLineNumber(1).setRevenueCenter(new Procedure().setCode("foo"));
		outpatientClaimForBeneA.getClaimLines().add(outpatientRevLineForBeneA);
		CurrentBeneficiary beneB = new CurrentBeneficiary().setId(1).setBirthDate(LocalDate.now());
		PartAClaimFact outpatientClaimForBeneB = new PartAClaimFact().setId(1L).setBeneficiary(beneB)
				.setClaimProfile(outpatientClaimForBeneA.getClaimProfile()).setAdmittingDiagnosisCode("foo");
		beneB.getPartAClaimFacts().add(outpatientClaimForBeneB);
		PartAClaimRevLineFact outpatientRevLineForBeneB = new PartAClaimRevLineFact().setClaim(outpatientClaimForBeneB)
				.setLineNumber(1).setRevenueCenter(new Procedure().setCode("foo"));
		outpatientClaimForBeneB.getClaimLines().add(outpatientRevLineForBeneB);
		Stream<BeneficiaryBundle> fhirStream = new DataTransformer()
				.transformSourceData(Arrays.asList(beneA, beneB).stream());
		// TODO need to expand the test data here

		// Push the data to FHIR.
		URI fhirServer = new URI("http://localhost:8080/hapi-fhir/baseDstu2");
		LoadAppOptions options = new LoadAppOptions(fhirServer);
		FhirLoader loader = new FhirLoader(new MetricRegistry(), options);
		List<FhirResult> results = loader.insertFhirRecords(fhirStream);

		// Verify the results.
		Assert.assertNotNull(results);
		Assert.assertEquals(1, results.size());
		Assert.assertEquals(12, results.get(0).getResourcesPushedCount());

		// TODO verify results by actually querying the server.
	}
}
