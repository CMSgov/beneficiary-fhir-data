package gov.hhs.cms.bluebutton.datapipeline.sampledata.persist;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.justdavis.karl.misc.datasources.provisioners.IProvisioningRequest;
import com.justdavis.karl.misc.datasources.provisioners.hsql.HsqlProvisioningRequest;

import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimRevLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartBClaimLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartDEventFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QCurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartAClaimRevLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartBClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartBClaimLineFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartDEventFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.test.CcwTestHelper;
import gov.hhs.cms.bluebutton.datapipeline.ccw.test.TearDownAcceptor;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufArchive;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.SampleDataLoader;
import gov.hhs.cms.bluebutton.datapipeline.sampledata.SpringConfigForTests;

/**
 * Unit tests for {@link SampleDataLoader}.
 */
@ContextConfiguration(classes = { SpringConfigForTests.class })
@RunWith(Parameterized.class)
public final class SampleDataPersisterTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataPersisterTest.class);

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
			MetricRegistry metrics = new MetricRegistry();

			// Generate some sample data to test with.
			SampleDataLoader loader = new SampleDataLoader(metrics);
			SynpufArchive archive = SynpufArchive.SAMPLE_TEST_A;
			List<CurrentBeneficiary> beneficiaries = loader.loadSampleData(Paths.get(".", "target"), archive);

			// Run the persister.
			SampleDataPersister persister = new SampleDataPersister(metrics, pm);
			persister.persist(beneficiaries.stream());

			/*
			 * Run queries against the DB to ensure that things were persisted,
			 * as expected.
			 */

			Assert.assertEquals(archive.getBeneficiaryCount(), pm.newJDOQLTypedQuery(CurrentBeneficiary.class)
					.result(false, QCurrentBeneficiary.candidate().count()).executeResultUnique());

			long partAFactCount = (long) pm.newJDOQLTypedQuery(PartAClaimFact.class)
					.result(false, QPartAClaimFact.candidate().count()).executeResultUnique();
			Assert.assertTrue(partAFactCount > 0L);
			long partARevLineFactCount = (long) pm.newJDOQLTypedQuery(PartAClaimRevLineFact.class)
					.result(false, QPartAClaimRevLineFact.candidate().count()).executeResultUnique();
			Assert.assertTrue(partARevLineFactCount > 0L);

			long partBFactCount = (long) pm.newJDOQLTypedQuery(PartBClaimFact.class)
					.result(false, QPartBClaimFact.candidate().count()).executeResultUnique();
			Assert.assertTrue(partBFactCount > 0L);
			long partBFactLineCount = (long) pm.newJDOQLTypedQuery(PartBClaimLineFact.class)
					.result(false, QPartBClaimLineFact.candidate().count()).executeResultUnique();
			Assert.assertTrue(partBFactLineCount > partBFactCount);

			long partDFactCount = (long) pm.newJDOQLTypedQuery(PartDEventFact.class)
					.result(false, QPartDEventFact.candidate().count()).executeResultUnique();
			Assert.assertTrue(partDFactCount > 0L);

			// Collect and print out the metrics from the run, just cause.
			Slf4jReporter metricsReporter = Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build();
			metricsReporter.report();
		}
	}
}
