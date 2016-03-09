package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.net.URL;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.justdavis.karl.misc.SpringConfigForJEMisc;
import com.justdavis.karl.misc.datasources.DataSourceConnectorsManager;
import com.justdavis.karl.misc.datasources.provisioners.DataSourceProvisionersManager;
import com.justdavis.karl.misc.datasources.provisioners.IProvisioningTargetsProvider;
import com.justdavis.karl.misc.datasources.provisioners.XmlProvisioningTargetsProvider;
import com.justdavis.karl.misc.datasources.schema.IDataSourceSchemaManager;
import com.justdavis.karl.misc.datasources.schema.LiquibaseSchemaManager;

/**
 * Spring {@link Configuration} for this project's unit tests.
 */
@Configuration
@Import(value = { SpringConfigForJEMisc.class })
@ComponentScan(basePackageClasses = { SampleDataLoader.class })
public class SpringConfigForTests {
	/**
	 * @param provisionersManager
	 *            the injected {@link DataSourceConnectorsManager} for the
	 *            application
	 * @return the {@link IProvisioningTargetsProvider} for the application
	 */
	@Bean
	IProvisioningTargetsProvider targetsProvider(DataSourceProvisionersManager provisionersManager) {
		/*
		 * The src/test/resources/datasource-provisioning-targets.xml file
		 * contains the location of the database servers that we can provision
		 * databases onto as part of our tests.
		 */
		URL availableTargetsUrl = Thread.currentThread().getContextClassLoader()
				.getResource("datasource-provisioning-targets.xml");
		IProvisioningTargetsProvider targetsProvider = new XmlProvisioningTargetsProvider(provisionersManager,
				availableTargetsUrl);

		return targetsProvider;
	}

	/**
	 * @param connectorsManager
	 *            the (injected) {@link DataSourceConnectorsManager} to use
	 * @return the injectable {@link IDataSourceSchemaManager} to use
	 */
	@Bean
	public IDataSourceSchemaManager schemaManager(DataSourceConnectorsManager connectorsManager) {
		String liquibaseChangeLogPath = "ccw-schema-liquibase.xml";
		return new LiquibaseSchemaManager(connectorsManager, liquibaseChangeLogPath);
	}
}
