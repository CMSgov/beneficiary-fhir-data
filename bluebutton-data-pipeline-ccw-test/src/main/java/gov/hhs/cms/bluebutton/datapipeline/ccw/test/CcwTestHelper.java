package gov.hhs.cms.bluebutton.datapipeline.ccw.test;

import java.util.Properties;

import javax.inject.Inject;
import javax.jdo.JDOHelper;

import org.datanucleus.api.jdo.JDOPersistenceManagerFactory;
import org.junit.Rule;
import org.springframework.stereotype.Component;

import com.justdavis.karl.misc.datasources.DataSourceConnectorsManager;
import com.justdavis.karl.misc.datasources.provisioners.DataSourceProvisionersManager;
import com.justdavis.karl.misc.datasources.provisioners.DataSourceProvisionersManager.ProvisioningResult;
import com.justdavis.karl.misc.datasources.provisioners.IProvisioningRequest;
import com.justdavis.karl.misc.datasources.provisioners.IProvisioningTargetsProvider;

import gov.hhs.cms.bluebutton.datapipeline.ccw.schema.CcwSchemaInitializer;

/**
 * <p>
 * This test utility/helper can be used to provision a new database with a
 * CCW-compatible schema and provide a {@link JDOPersistenceManagerFactory} for
 * that database. This is intended for use as a JUnit {@link Rule}, as follows:
 * </p>
 * 
 * <pre><code>
 * {@literal @}ContextConfiguration(classes = { MySpringConfig.class })
 * public final class MyTest {
 * 	{@literal @}ClassRule
 * 	public static final SpringClassRule springClassRule = new SpringClassRule();
 * 
 * 	{@literal @}Rule
 * 	public final SpringMethodRule springMethodRule = new SpringMethodRule();
 * 	
 * 	{@literal @}Rule
 * 	public final TearDownAcceptor tearDown = new TearDownAcceptor();
 * 
 * 	{@literal @}Rule
 * 	{@literal @}Inject
 * 	public CcwTestHelper ccwHelper;
 * 
 * 	{@literal @}Test
 * 	public void normalUsage() {
 * 		JDOPersistenceManagerFactory pmf = ccwHelper.provisionMockCcwDatabase(
 * 			new HsqlProvisioningRequest("tests"), tearDown);
 * 		// Do stuff with the PMF...
 * 		
 * 		// The PMF and DB will be automatically cleaned up at the end of the 
 * 		// test. 
 * 	}
 * }
 * </code></pre>
 */
@Component
public final class CcwTestHelper implements AutoCloseable {
	private final DataSourceProvisionersManager provisioner;
	private final IProvisioningTargetsProvider provisioningTargetsProvider;
	private final CcwSchemaInitializer schemaInitializer;
	private final DataSourceConnectorsManager connector;

	private ProvisioningResult provisionedDb;
	private JDOPersistenceManagerFactory pmf;

	/**
	 * Constructs a new {@link CcwTestHelper} instance.
	 * 
	 * @param provisioner
	 *            the (injected) {@link DataSourceProvisionersManager} to use
	 * @param provisioningTargetsProvider
	 *            the (injected) {@link IProvisioningTargetsProvider} to use
	 * @param schemaInitializer
	 *            the (injected) {@link CcwSchemaInitializer} to use
	 * @param connector
	 *            the (injected) {@link DataSourceConnectorsManager} to use
	 */
	@Inject
	public CcwTestHelper(DataSourceProvisionersManager provisioner,
			IProvisioningTargetsProvider provisioningTargetsProvider, CcwSchemaInitializer schemaInitializer,
			DataSourceConnectorsManager connector) {
		this.provisioner = provisioner;
		this.provisioningTargetsProvider = provisioningTargetsProvider;
		this.schemaInitializer = schemaInitializer;
		this.connector = connector;
	}

	/**
	 * Provisions a new database, shoves in a schema compatible with the CCW,
	 * and returns a {@link JDOPersistenceManagerFactory} connected to that
	 * database. After the current test case completes, the
	 * {@link JDOPersistenceManagerFactory} and the database will automatically
	 * be cleaned up/removed.
	 * 
	 * @param provisioningRequest
	 *            an {@link IProvisioningRequest} that specifies what type of
	 *            database to stand up
	 * @return a {@link JDOPersistenceManagerFactory} connected to the
	 *         newly-provisioned database
	 */
	public JDOPersistenceManagerFactory provisionMockCcwDatabase(IProvisioningRequest provisioningRequest) {
		/*
		 * First, provision the database that we're going to use and then
		 * register it for cleanup after the test case.
		 */
		ProvisioningResult provisionedDb = provisioner.provision(provisioningTargetsProvider, provisioningRequest);
		this.provisionedDb = provisionedDb;

		/*
		 * Next, initialize the schema of the provisioned DB.
		 */
		schemaInitializer.initializeSchema(provisionedDb.getCoords());

		/*
		 * Finally, create a JDO PMF, save it for cleanup after the test case,
		 * and return it back out to the test case, as it's most likely what
		 * they'll directly use.
		 */
		JDOPersistenceManagerFactory pmf = createJdoPmf(provisionedDb);
		this.pmf = pmf;
		return pmf;
	}

	/**
	 * Convenience method. Works exactly as
	 * {@link #provisionMockCcwDatabase(IProvisioningRequest)}, but also
	 * registers this {@link CcwTestHelper} with the specified
	 * {@link TearDownAcceptor}.
	 * 
	 * @param provisioningRequest
	 *            (see {@link #provisionMockCcwDatabase(IProvisioningRequest)})
	 * @param tearDownAcceptor
	 *            the {@link TearDownAcceptor} to register this
	 *            {@link CcwTestHelper} with
	 * @return (see {@link #provisionMockCcwDatabase(IProvisioningRequest)})
	 */
	public JDOPersistenceManagerFactory provisionMockCcwDatabase(IProvisioningRequest provisioningRequest,
			TearDownAcceptor tearDownAcceptor) {
		JDOPersistenceManagerFactory pmf = provisionMockCcwDatabase(provisioningRequest);
		tearDownAcceptor.register(this);
		return pmf;
	}

	/**
	 * @param provisionedDb
	 *            the DB that the {@link JDOPersistenceManagerFactory} will
	 *            connect to
	 * @return a new, properly configured {@link JDOPersistenceManagerFactory}
	 */
	private JDOPersistenceManagerFactory createJdoPmf(ProvisioningResult provisionedDb) {
		/*
		 * Lastly, create a properly-configured JDO PMF.
		 */
		Properties dnProps = new Properties();
		dnProps.put("datanucleus.PersistenceUnitName", "CCW");
		dnProps.put("datanucleus.identifier.case", "MixedCase");
		dnProps.put("datanucleus.DetachAllOnCommit", "true");
		dnProps.put("datanucleus.CopyOnAttach", "false");
		dnProps.put("datanucleus.schema.validateTables", "true");
		dnProps.put("datanucleus.schema.validateConstraints", "false");
		dnProps.put("datanucleus.ConnectionFactory", connector.createDataSource(provisionedDb.getCoords()));
		JDOPersistenceManagerFactory pmf = (JDOPersistenceManagerFactory) JDOHelper
				.getPersistenceManagerFactory(dnProps);

		return pmf;
	}

	/**
	 * Closes the {@link JDOPersistenceManagerFactory}, deletes the provisioned
	 * database, and readies this {@link CcwTestHelper} for a new call to
	 * {@link #createJdoPmf(ProvisioningResult)}
	 */
	public void cleanupAndReset() {
		if (pmf != null)
			pmf.close();
		if (provisionedDb != null)
			provisioner.delete(provisionedDb);
	}

	/**
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() throws Exception {
		cleanupAndReset();
	}
}
