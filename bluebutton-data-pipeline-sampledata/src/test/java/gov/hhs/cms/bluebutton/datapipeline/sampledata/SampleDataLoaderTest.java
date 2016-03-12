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
	@Inject
	public CcwTestHelper ccwHelper;

	@Parameters
	public static Iterable<Object> createTestParameters() {
		return Arrays.asList(new HsqlProvisioningRequest("tests"));
	}

	@Parameter(0)
	public IProvisioningRequest provisioningRequest;

	/**
	 * Verifies that {@link SampleDataLoader} works correctly under normal
	 * circumstances.
	 */
	@Test
	public void normalUsage() {
		JDOPersistenceManagerFactory pmf = ccwHelper.provisionMockCcwDatabase(provisioningRequest);

		try (PersistenceManager pm = pmf.getPersistenceManager();) {
			// Run the loader and verify the results.
			SampleDataLoader loader = new SampleDataLoader(pm);
			loader.loadSampleData(Paths.get(".", "target"));

			Assert.assertEquals(SynpufArchive.SAMPLE_1.getBeneficiaryCount(),
					pm.newJDOQLTypedQuery(CurrentBeneficiary.class)
							.result(false, QCurrentBeneficiary.candidate().count()).executeResultUnique());
			Assert.assertEquals(779815L, pm.newJDOQLTypedQuery(PartAClaimFact.class)
					.result(false, QPartAClaimFact.candidate().count()).executeResultUnique());
		}
	}
}
