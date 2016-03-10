package gov.hhs.cms.bluebutton.datapipeline.ccw;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.justdavis.karl.misc.SpringConfigForJEMisc;
import com.justdavis.karl.misc.datasources.DataSourceConnectorsManager;
import com.justdavis.karl.misc.datasources.schema.IDataSourceSchemaManager;
import com.justdavis.karl.misc.datasources.schema.LiquibaseSchemaManager;

/**
 * Spring {@link Configuration} for this project.
 */
@Configuration
@Import(value = { SpringConfigForJEMisc.class })
@ComponentScan(basePackageClasses = { CcwSchemaInitializer.class })
public class SpringConfigForBlueButtonPipelineCcw {
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
