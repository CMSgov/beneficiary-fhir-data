package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import javax.inject.Inject;
import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;

import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
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

import com.justdavis.karl.misc.datasources.DataSourceConnectorsManager;
import com.justdavis.karl.misc.datasources.provisioners.DataSourceProvisionersManager;
import com.justdavis.karl.misc.datasources.provisioners.DataSourceProvisionersManager.ProvisioningResult;
import com.justdavis.karl.misc.datasources.provisioners.IProvisioningRequest;
import com.justdavis.karl.misc.datasources.provisioners.IProvisioningTargetsProvider;
import com.justdavis.karl.misc.datasources.provisioners.hsql.HsqlProvisioningRequest;

import gov.hhs.cms.bluebutton.datapipeline.ccw.CcwSchemaInitializer;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.CurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.PartAClaimFact;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QCurrentBeneficiary;
import gov.hhs.cms.bluebutton.datapipeline.ccw.jdo.QPartAClaimFact;

/**
 * Unit tests for {@link SampleDataLoader}.
 */
@ContextConfiguration(classes = { SpringConfigForTests.class })
@RunWith(Parameterized.class)
public final class SampleDataLoaderTest {
	/*
	 * FIXME This is a huge mess. Needs abstraction. Desperately.
	 */

	private static final Logger LOGGER = LoggerFactory.getLogger(SampleDataLoaderTest.class);

	@ClassRule
	public static final SpringClassRule springClassRule = new SpringClassRule();

	@Rule
	public final SpringMethodRule springMethodRule = new SpringMethodRule();

	@Parameters
	public static Iterable<Object> createTestParameters() {
		return Arrays.asList(new HsqlProvisioningRequest("tests"));
	}

	@Parameter(0)
	public IProvisioningRequest provisioningRequest;

	@Inject
	private CcwSchemaInitializer schemaIniter;

	@Inject
	private DataSourceProvisionersManager provisioner;

	@Inject
	private DataSourceConnectorsManager connector;

	@Inject
	private IProvisioningTargetsProvider provisioningTargetsProvider;

	private ProvisioningResult provisionedDb;

	private JDOPersistenceManagerFactory pmf;

	@Before
	public void provisionDb() {
		this.provisionedDb = provisioner.provision(provisioningTargetsProvider, provisioningRequest);
		schemaIniter.initializeSchema(provisionedDb.getCoords());

		// Create the DN service objects.
		Properties dnProps = new Properties();
		dnProps.put("datanucleus.PersistenceUnitName", "CCW");
		dnProps.put("datanucleus.identifier.case", "MixedCase");
		dnProps.put("datanucleus.DetachAllOnCommit", "true");
		dnProps.put("datanucleus.CopyOnAttach", "false");
		// dnProps.put("datanucleus.schema.autoCreateAll", true);
		dnProps.put("datanucleus.schema.validateTables", "true");
		dnProps.put("datanucleus.schema.validateConstraints", "false");
		dnProps.put("datanucleus.ConnectionFactory", connector.createDataSource(provisionedDb.getCoords()));
		this.pmf = (JDOPersistenceManagerFactory) JDOHelper.getPersistenceManagerFactory(dnProps);
	}

	@After
	public void deprovisionDb() {
		this.pmf.close();
		provisioner.delete(provisionedDb);
	}

	/**
	 * Verifies that {@link SampleDataLoader} works correctly under normal
	 * circumstances.
	 */
	@Test
	public void normalUsage() {
		try (PersistenceManager pm = pmf.getPersistenceManager();) {
			// Run the loader and verify the results.
			SampleDataLoader loader = new SampleDataLoader(pm);
			loader.loadSampleData(Paths.get(".", "target"));

			Assert.assertEquals(116352L, pm.newJDOQLTypedQuery(CurrentBeneficiary.class)
					.result(false, QCurrentBeneficiary.candidate().count()).executeResultUnique());
			Assert.assertEquals(779815L, pm.newJDOQLTypedQuery(PartAClaimFact.class)
					.result(false, QPartAClaimFact.candidate().count()).executeResultUnique());
		}
	}
}
