package gov.hhs.cms.bluebutton.server.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import ca.uhn.fhir.jpa.config.BaseJavaConfigDstu3;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirResourceDao;
import ca.uhn.fhir.jpa.util.SubscriptionsRequireManualActivationInterceptorDstu3;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

/**
 * Provides the configuration for {@link BlueButtonServer} (and is activated via
 * Spring classpath scanning).
 */
@Configuration
@EnableTransactionManagement()
public class FhirServerConfig extends BaseJavaConfigDstu3 {
	public static final String PROP_DB_URL = "bbfhir.db.url";
	public static final String PROP_DB_USERNAME = "bbfhir.db.username";
	public static final String PROP_DB_PASSWORD = "bbfhir.db.password";

	/**
	 * @see ca.uhn.fhir.jpa.config.BaseJavaConfigDstu3#resourceProvidersDstu3()
	 */
	@Bean(name = "myResourceProvidersDstu3")
	@Override
	public List<IResourceProvider> resourceProvidersDstu3() {
		/*
		 * This is overridden to reduce the default "surface area" of our FHIR
		 * server: only support the resource types that are actually needed.
		 */

		List<IResourceProvider> retVal = new ArrayList<IResourceProvider>();
		retVal.add(rpBundleDstu3());
		retVal.add(rpConformanceDstu3());
		retVal.add(rpCoverageDstu3());
		retVal.add(rpExplanationOfBenefitDstu3());
		retVal.add(rpMedicationDstu3());
		retVal.add(rpMedicationOrderDstu3());
		retVal.add(rpOrganizationDstu3());
		retVal.add(rpPatientDstu3());
		retVal.add(rpPractitionerDstu3());
		retVal.add(rpReferralRequestDstu3());
		return retVal;
	}

	/**
	 * @see ca.uhn.fhir.jpa.config.BaseJavaConfigDstu3#resourceDaosDstu3()
	 */
	@Bean(name = "myResourceDaosDstu3")
	@Override
	public List<IFhirResourceDao<?>> resourceDaosDstu3() {
		/*
		 * This is overridden to reduce the default "surface area" of our FHIR
		 * server: only support the resource types that are actually needed.
		 */

		List<IFhirResourceDao<?>> retVal = new ArrayList<IFhirResourceDao<?>>();
		retVal.add(daoBundleDstu3());
		retVal.add(daoConformanceDstu3());
		retVal.add(daoCoverageDstu3());
		retVal.add(daoExplanationOfBenefitDstu3());
		retVal.add(daoMedicationDstu3());
		retVal.add(daoMedicationOrderDstu3());
		retVal.add(daoOrganizationDstu3());
		retVal.add(daoPatientDstu3());
		retVal.add(daoPractitionerDstu3());
		retVal.add(daoReferralRequestDstu3());
		return retVal;
	}

	/**
	 * Configure FHIR properties around the the JPA server via this bean
	 */
	@Bean()
	public DaoConfig daoConfig() {
		DaoConfig retVal = new DaoConfig();
		retVal.setSubscriptionEnabled(true);
		retVal.setSubscriptionPollDelay(5000);
		retVal.setSubscriptionPurgeInactiveAfterMillis(DateUtils.MILLIS_PER_HOUR);
		retVal.setAllowMultipleDelete(true);
		return retVal;
	}

	/**
	 * The following bean configures the database connection. The 'url' property
	 * value of "jdbc:derby:directory:jpaserver_derby_files;create=true"
	 * indicates that the server should save resources in a directory called
	 * "jpaserver_derby_files".
	 * 
	 * A URL to a remote database could also be placed here, along with login
	 * credentials and other properties supported by BasicDataSource.
	 */
	@Bean(destroyMethod = "close")
	public DataSource dataSource(@Value("${" + PROP_DB_URL + "}") String url,
			@Value("${" + PROP_DB_USERNAME + "}") String username,
			@Value("${" + PROP_DB_PASSWORD + "}") String password) {
		BasicDataSource retVal = new BasicDataSource();
		retVal.setUrl(url);
		retVal.setUsername(username);
		retVal.setPassword(password);
		return retVal;
	}

	@Bean()
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean retVal = new LocalContainerEntityManagerFactoryBean();
		retVal.setPersistenceUnitName("HAPI_PU");
		retVal.setDataSource(dataSource);
		retVal.setPackagesToScan("ca.uhn.fhir.jpa.entity");
		retVal.setPersistenceProvider(new HibernatePersistenceProvider());
		retVal.setJpaProperties(jpaProperties());
		retVal.afterPropertiesSet();
		return retVal;
	}

	private Properties jpaProperties() {
		Properties extraProperties = new Properties();
		extraProperties.put("hibernate.format_sql", "true");
		extraProperties.put("hibernate.show_sql", "false");
		extraProperties.put("hibernate.hbm2ddl.auto", "update");
		extraProperties.put("hibernate.jdbc.batch_size", "20");
		extraProperties.put("hibernate.cache.use_query_cache", "false");
		extraProperties.put("hibernate.cache.use_second_level_cache", "false");
		extraProperties.put("hibernate.cache.use_structured_entries", "false");
		extraProperties.put("hibernate.cache.use_minimal_puts", "false");
		extraProperties.put("hibernate.search.default.directory_provider", "filesystem");
		extraProperties.put("hibernate.search.default.indexBase", "target/lucenefiles");
		extraProperties.put("hibernate.search.lucene_version", "LUCENE_CURRENT");

		/*
		 * Disable Hibernate Search/Lucene indexing, as it slows writes down by
		 * about half. See this thread for some details on what the tradeoffs
		 * are: https://groups.google.com/forum/#!topic/hapi-fhir/lF9b_cLfgA4.
		 */
		extraProperties.put("hibernate.search.autoregister_listeners", "false");

		/*
		 * This property was used to disable Lucene in HAPI 1.4. Pretty sure
		 * it's not needed anymore, but seems worth keeping around, just in
		 * case.
		 */
		// extraProperties.put("hibernate.search.indexing_strategy", "manual");
		return extraProperties;
	}

	/**
	 * Do some fancy logging to create a nice access log that has details about
	 * each incoming request.
	 */
	public IServerInterceptor loggingInterceptor() {
		LoggingInterceptor retVal = new LoggingInterceptor();
		retVal.setLoggerName("hapi.access");
		retVal.setMessageFormat(
				"Path[${servletPath}] Source[${requestHeader.x-forwarded-for}] Operation[${operationType} ${operationName} ${idOrResourceName}] UA[${requestHeader.user-agent}] Params[${requestParameters}] ResponseEncoding[${responseEncodingNoDefault}]");
		retVal.setLogExceptions(true);
		retVal.setErrorMessageFormat("ERROR - ${requestVerb} ${requestUrl}");
		return retVal;
	}

	/**
	 * This interceptor adds some pretty syntax highlighting in responses when a
	 * browser is detected
	 */
	@Bean(autowire = Autowire.BY_TYPE)
	public IServerInterceptor responseHighlighterInterceptor() {
		ResponseHighlighterInterceptor retVal = new ResponseHighlighterInterceptor();
		return retVal;
	}

	@Bean(autowire = Autowire.BY_TYPE)
	public IServerInterceptor subscriptionSecurityInterceptor() {
		SubscriptionsRequireManualActivationInterceptorDstu3 retVal = new SubscriptionsRequireManualActivationInterceptorDstu3();
		return retVal;
	}

	@Bean()
	public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager retVal = new JpaTransactionManager();
		retVal.setEntityManagerFactory(entityManagerFactory);
		return retVal;
	}
}
