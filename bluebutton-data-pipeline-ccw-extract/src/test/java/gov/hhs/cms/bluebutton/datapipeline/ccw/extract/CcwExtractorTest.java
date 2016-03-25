package gov.hhs.cms.bluebutton.datapipeline.ccw.extract;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;
import javax.jdo.Transaction;

import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import com.codahale.metrics.MetricRegistry;
import com.justdavis.karl.misc.datasources.provisioners.IProvisioningRequest;
import com.justdavis.karl.misc.datasources.provisioners.hsql.HsqlProvisioningRequest;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.AllClaimsProfile;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.ClaimType;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.test.CcwTestHelper;
import gov.hhs.cms.bluebutton.datapipeline.ccw.test.TearDownAcceptor;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufArchive;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader;

/**
 * Unit tests for {@link CcwExtractor}.
 */
@ContextConfiguration(classes = { SpringConfigForTests.class })
@RunWith(Parameterized.class)
public final class CcwExtractorTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(CcwExtractorTest.class);

	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@Rule
	public final TearDownAcceptor tearDown = new TearDownAcceptor();

	@Inject
	public CcwTestHelper ccwHelper;

	@Parameters
	public static Iterable<Object> createTestParameters() {
		return Arrays.asList(new HsqlProvisioningRequest("tests"));
	}

	@Parameter(0)
	public IProvisioningRequest provisioningRequest;

	/**
	 * Verifies that {@link CcwExtractor} works correctly when extracting a
	 * small amount of sample data.
	 */
	@Test
	public void extractSmallSample() {
		JDOPersistenceManagerFactory pmf = ccwHelper.provisionMockCcwDatabase(provisioningRequest, tearDown);

		try (PersistenceManager pm = pmf.getPersistenceManager();) {
			// Create some model objects and persist them.
			Transaction tx = pm.currentTransaction();
			try {
				tx.begin();
				CurrentBeneficiary beneA = new CurrentBeneficiary().setId(0).setBirthDate(LocalDate.now());
				PartAClaimFact factA = new PartAClaimFact().setId(0L).setBeneficiary(beneA)
						.setClaimProfile(new AllClaimsProfile().setId(1L).setClaimType(ClaimType.OUTPATIENT_CLAIM))
						.setAdmittingDiagnosisCode("foo");
				beneA.getPartAClaimFacts().add(factA);
				pm.makePersistent(beneA);
				CurrentBeneficiary beneB = new CurrentBeneficiary().setId(1).setBirthDate(LocalDate.now());
				PartAClaimFact factB = new PartAClaimFact().setId(1L).setBeneficiary(beneB)
						.setClaimProfile(factA.getClaimProfile()).setAdmittingDiagnosisCode("foo");
				beneB.getPartAClaimFacts().add(factB);
				pm.makePersistent(beneB);
				tx.commit();
			} finally {
				if (tx != null && tx.isActive())
					tx.rollback();
			}

			/*
			 * Run the extractor and verify the results.
			 */
			CcwExtractor extractor = new CcwExtractor(pm);
			Stream<CurrentBeneficiary> beneficiariesStream = extractor.extractAllBeneficiaries();

			Assert.assertNotNull(beneficiariesStream);
			Assert.assertFalse(beneficiariesStream.isParallel());
			List<CurrentBeneficiary> beneficiariesList = beneficiariesStream.collect(Collectors.toList());
			Assert.assertEquals(2, beneficiariesList.size());
		}
	}

	/**
	 * Verifies that {@link CcwExtractor} works correctly when extracting the
	 * sample DE-SynPUF data from {@link SampleDataLoader}.
	 */
	@Test
	public void extractSynpuf() {
		JDOPersistenceManagerFactory pmf = ccwHelper.provisionMockCcwDatabase(provisioningRequest, tearDown);

		try (PersistenceManager pm = pmf.getPersistenceManager();) {
			// Load the DE-SynPUF sample data.
			SampleDataLoader sampleLoader = new SampleDataLoader(new MetricRegistry(), pm);
			SynpufArchive archive = SynpufArchive.SAMPLE_TEST_A;
			sampleLoader.loadSampleData(Paths.get(".", "target"), archive);

			/*
			 * Run the extractor and verify the results. The first thing to
			 * verify will be that just getting the Stream didn't take a lot of
			 * time. If it did, that indicates that all of the data was loaded
			 * up-front, which we don't want.
			 */
			LOGGER.info("Creating stream...");
			long streamStart = System.currentTimeMillis();
			CcwExtractor extractor = new CcwExtractor(pm);
			Stream<CurrentBeneficiary> beneficiariesStream = extractor.extractAllBeneficiaries();
			long streamEnd = System.currentTimeMillis();
			LOGGER.info("Created stream.");
			Assert.assertTrue((streamEnd - streamStart) <= 5L * 1000L);

			Assert.assertNotNull(beneficiariesStream);
			Assert.assertEquals(archive.getBeneficiaryCount(), beneficiariesStream.count());
		}
	}
}
