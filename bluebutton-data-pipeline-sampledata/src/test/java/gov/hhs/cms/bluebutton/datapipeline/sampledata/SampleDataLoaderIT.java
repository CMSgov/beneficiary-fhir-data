package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.nio.file.Paths;
import java.util.Arrays;

import javax.inject.Inject;
import javax.jdo.PersistenceManager;

import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.junit.ClassRule;
import org.junit.Ignore;
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
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.justdavis.karl.misc.datasources.provisioners.IProvisioningRequest;
import com.justdavis.karl.misc.datasources.provisioners.hsql.HsqlProvisioningRequest;
import com.justdavis.karl.misc.datasources.provisioners.postgresql.PostgreSqlProvisioningRequest;

import gov.hhs.cms.bluebutton.datapipeline.ccw.test.CcwTestHelper;
import gov.hhs.cms.bluebutton.datapipeline.ccw.test.TearDownAcceptor;
import gov.hhs.cms.bluebutton.datapipeline.desynpuf.SynpufArchive;

/**
 * Integration tests for {@link SampleDataLoader}.
 */
@ContextConfiguration(classes = { SpringConfigForTests.class })
@RunWith(Parameterized.class)
public final class SampleDataLoaderIT {
	private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataLoaderIT.class);

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
		return Arrays.asList(new HsqlProvisioningRequest("tests"), new PostgreSqlProvisioningRequest("cms_ccw_tests"));
	}

	@Parameter(0)
	public IProvisioningRequest provisioningRequest;

	/**
	 * Runs {@link SampleDataLoader} against {@link SynpufArchive#SAMPLE_TEST_B}
	 * to verify that no errors are thrown.
	 */
	@Test
	@Ignore("slow (takes 190sec+), so skipped by default")
	public void noErrorsFromSampleTestB() {
		/*
		 * If I run this test by itself using Eclipse Mar's new feature that
		 * allows that, this seems to complete in 20-30 seconds. WTF?! Why so
		 * much faster?
		 */
		JDOPersistenceManagerFactory pmf = ccwHelper.provisionMockCcwDatabase(provisioningRequest, tearDown);

		try (PersistenceManager pm = pmf.getPersistenceManager();) {
			MetricRegistry metrics = new MetricRegistry();
			metrics.registerAll(new MemoryUsageGaugeSet());
			metrics.registerAll(new GarbageCollectorMetricSet());

			SampleDataLoader loader = new SampleDataLoader(metrics, pm);
			loader.loadSampleData(Paths.get(".", "target"), SynpufArchive.SAMPLE_TEST_B);

			// Collect and print out the metrics from the run, just cause.
			Slf4jReporter metricsReporter = Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build();
			metricsReporter.report();
		}
	}

	/**
	 * Runs {@link SampleDataLoader} against {@link SynpufArchive#SAMPLE_TEST_B}
	 * to verify that no errors are thrown.
	 */
	@Test
	@Ignore("slow (takes 600sec), so skipped by default")
	public void noErrorsFromSampleTestD() {
		JDOPersistenceManagerFactory pmf = ccwHelper.provisionMockCcwDatabase(provisioningRequest, tearDown);

		try (PersistenceManager pm = pmf.getPersistenceManager();) {
			MetricRegistry metrics = new MetricRegistry();
			metrics.registerAll(new MemoryUsageGaugeSet());
			metrics.registerAll(new GarbageCollectorMetricSet());

			SampleDataLoader loader = new SampleDataLoader(metrics, pm);
			loader.loadSampleData(Paths.get(".", "target"), SynpufArchive.SAMPLE_TEST_D);

			// Collect and print out the metrics from the run, just cause.
			Slf4jReporter metricsReporter = Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build();
			metricsReporter.report();
		}
	}

	/**
	 * Runs {@link SampleDataLoader} against {@link SynpufArchive#SAMPLE_1} to
	 * verify that no errors are thrown.
	 */
	@Test
	@Ignore("slow (takes TODO), so skipped by default")
	public void noErrorsFromSample1() {
		// TODO fill in the time, above
		/*
		 * FIXME can't I move these into categories or ITs, instead of disabling
		 * the slow test cases?
		 */

		JDOPersistenceManagerFactory pmf = ccwHelper.provisionMockCcwDatabase(provisioningRequest, tearDown);

		try (PersistenceManager pm = pmf.getPersistenceManager();) {
			MetricRegistry metrics = new MetricRegistry();
			metrics.registerAll(new MemoryUsageGaugeSet());
			metrics.registerAll(new GarbageCollectorMetricSet());

			SampleDataLoader loader = new SampleDataLoader(metrics, pm);
			loader.loadSampleData(Paths.get(".", "target"), SynpufArchive.SAMPLE_1);

			// Collect and print out the metrics from the run, just cause.
			Slf4jReporter metricsReporter = Slf4jReporter.forRegistry(metrics).outputTo(LOGGER).build();
			metricsReporter.report();
		}
	}
}
