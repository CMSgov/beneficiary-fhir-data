package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.nio.file.Paths;
import java.util.Arrays;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;

import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import com.justdavis.karl.misc.datasources.provisioners.IProvisioningRequest;
import com.justdavis.karl.misc.datasources.provisioners.hsql.HsqlProvisioningRequest;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QCurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.test.CcwTestHelper;
import gov.hhs.cms.bluebutton.datapipeline.ccw.test.TearDownAcceptor;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufArchive;

/**
 * Unit tests for {@link SampleDataLoader}.
 */
@ContextConfiguration(classes = { SpringConfigForTests.class })
@RunWith(Parameterized.class)
public final class SampleDataLoaderTest {
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
	 * Verifies that {@link SampleDataLoader} loads the expected number of
	 * records when run against {@link SynpufArchive#SAMPLE_TEST_A}.
	 */
	@Test
	public void verifyCountsForSampleTestA() {
		JDOPersistenceManagerFactory pmf = ccwHelper.provisionMockCcwDatabase(provisioningRequest, tearDown);

		try (PersistenceManager pm = pmf.getPersistenceManager();) {
			// Run the loader and verify the results.
			SampleDataLoader loader = new SampleDataLoader(pm);
			SynpufArchive archive = SynpufArchive.SAMPLE_TEST_A;
			loader.loadSampleData(Paths.get(".", "target"), archive);

			Assert.assertEquals(archive.getBeneficiaryCount(), pm.newJDOQLTypedQuery(CurrentBeneficiary.class)
					.result(false, QCurrentBeneficiary.candidate().count()).executeResultUnique());
			long partAFactCount = (long) pm.newJDOQLTypedQuery(PartAClaimFact.class)
					.result(false, QPartAClaimFact.candidate().count()).executeResultUnique();
			Assert.assertTrue(partAFactCount > 0L);
		}
	}

	/**
	 * Spot-checks a single loaded beneficiary from
	 * {@link SynpufArchive#SAMPLE_TEST_A} to verify that
	 * {@link SampleDataLoader} is handling fields as expected.
	 */
	@Test
	public void spotCheckBeneficiaryFromSampleTestA() {
		JDOPersistenceManagerFactory pmf = ccwHelper.provisionMockCcwDatabase(provisioningRequest, tearDown);

		try (PersistenceManager pm = pmf.getPersistenceManager();) {
			// Run the loader and verify the results.
			SampleDataLoader loader = new SampleDataLoader(pm);
			SynpufArchive archive = SynpufArchive.SAMPLE_TEST_A;
			loader.loadSampleData(Paths.get(".", "target"), archive);

			// Grab the beneficiary to spot-check.
			CurrentBeneficiary loadedBene = pm.newJDOQLTypedQuery(CurrentBeneficiary.class)
					.filter(QCurrentBeneficiary.candidate().id.eq(0)).executeUnique();

			// Spot check the CurrentBeneificiary itself.
			Assert.assertEquals(1923, loadedBene.getBirthDate().getYear());
			Assert.assertTrue(loadedBene.getGivenName() != null && loadedBene.getGivenName().length() > 0);
			Assert.assertTrue(loadedBene.getSurname() != null && loadedBene.getSurname().length() > 0);

			// Spot check one of the beneficiary's PartAClaimFacts.
			Assert.assertEquals(1, loadedBene.getPartAClaimFacts().size());
			PartAClaimFact partAClaim = loadedBene.getPartAClaimFacts().get(0);
			Assert.assertEquals(542192281063886L, (long) partAClaim.getId());
			Assert.assertEquals("V5883", partAClaim.getAdmittingDiagnosisCode());
		}
	}
}
