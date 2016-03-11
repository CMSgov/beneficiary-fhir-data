package gov.hhs.cms.bluebutton.datapipeline.ccw.test;

import java.net.URL;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.justdavis.karl.misc.datasources.DataSourceConnectorsManager;
import com.justdavis.karl.misc.datasources.provisioners.DataSourceProvisionersManager;
import com.justdavis.karl.misc.datasources.provisioners.IProvisioningTargetsProvider;
import com.justdavis.karl.misc.datasources.provisioners.XmlProvisioningTargetsProvider;

import gov.hhs.cms.bluebutton.datapipeline.ccw.SpringConfigForBlueButtonPipelineCcw;

/**
 * Spring {@link Configuration} for this project.
 */
@Configuration
@Import(value = { SpringConfigForBlueButtonPipelineCcw.class })
@ComponentScan(basePackageClasses = { SpringConfigForBlueButtonPipelineCcwTest.class })
public class SpringConfigForBlueButtonPipelineCcwTest {
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
}
