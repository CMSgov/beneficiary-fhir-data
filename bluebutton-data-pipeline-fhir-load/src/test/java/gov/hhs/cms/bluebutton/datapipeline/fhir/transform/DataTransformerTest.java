package gov.hhs.cms.bluebutton.datapipeline.fhir.transform;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
import gov.hhs.cms.bluebutton.datapipeline.fhir.SpringConfigForTests;

/**
 * Unit tests for {@link DataTransformer}.
 */
@ContextConfiguration(classes = { SpringConfigForTests.class })
public final class DataTransformerTest {
	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed an
	 * empty stream.
	 */
	@Test
	public void transformEmptyStream() {
		DataTransformer transformer = new DataTransformer();

		Stream<CurrentBeneficiary> emptySourceStream = new ArrayList<CurrentBeneficiary>().stream();
		Stream<BeneficiaryBundle> transformedFhirStream = transformer.transformSourceData(emptySourceStream);
		Assert.assertNotNull(transformedFhirStream);
		Assert.assertEquals(0, transformedFhirStream.count());
	}

	/**
	 * Verifies that {@link DataTransformer} works correctly when when passed a
	 * small hand-crafted data set.
	 */
	@Test
	public void transformSmallDataset() {
		// Create some mock data.
		CurrentBeneficiary beneA = new CurrentBeneficiary().setId(0).setBirthDate(LocalDate.now());
		PartAClaimFact factA = new PartAClaimFact().setId(0L).setBeneficiary(beneA).setAdmittingDiagnosisCode("foo");
		beneA.getPartAClaimFacts().add(factA);
		CurrentBeneficiary beneB = new CurrentBeneficiary().setId(1).setBirthDate(LocalDate.now());
		PartAClaimFact factB = new PartAClaimFact().setId(1L).setBeneficiary(beneB).setAdmittingDiagnosisCode("foo");
		beneB.getPartAClaimFacts().add(factB);

		// Run the transformer against the mock data.
		DataTransformer transformer = new DataTransformer();
		Stream<CurrentBeneficiary> emptySourceStream = Arrays.asList(beneA, beneB).stream();
		Stream<BeneficiaryBundle> transformedFhirStream = transformer.transformSourceData(emptySourceStream);
		List<BeneficiaryBundle> transformedBundles = transformedFhirStream.collect(Collectors.toList());

		// Verify the transformation results.
		Assert.assertEquals(2, transformedBundles.size());
		Assert.assertEquals(1, transformedBundles.get(0).getPatient().getIdentifier().size());
		Assert.assertEquals("" + beneA.getId(),
				transformedBundles.get(0).getPatient().getIdentifier().get(0).getValue());
		Assert.assertEquals(Date.valueOf(beneA.getBirthDate()), transformedBundles.get(0).getPatient().getBirthDate());
		Assert.assertEquals(1, transformedBundles.get(0).getClaim().getDiagnosis().size());
		Assert.assertEquals(factA.getAdmittingDiagnosisCode(),
				transformedBundles.get(0).getClaim().getDiagnosis().get(0).getDiagnosis().getCode());
	}
}
