package gov.hhs.cms.bluebutton.server.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.apache.commons.lang3.time.DateUtils;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.zaxxer.hikari.HikariDataSource;

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
	 * Set this to <code>true</code> to have Hibernate log a ton of info on the
	 * SQL statements being run and each session's performance. Be sure to also
	 * adjust the related logging levels in Wildfly or whatever (see
	 * <code>server-config.sh</code> for details).
	 */
	private static final boolean HIBERNATE_DETAILED_LOGGING = false;

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
	 * @return the {@link MetricRegistry} for the application, which can be used
	 *         to collect statistics on the application's performance
	 */
	@Bean()
	public MetricRegistry metricRegistry() {
		MetricRegistry metricRegistry = new MetricRegistry();
		metricRegistry.registerAll(new MemoryUsageGaugeSet());
		metricRegistry.registerAll(new GarbageCollectorMetricSet());

		final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
		reporter.start();

		return metricRegistry;
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
			@Value("${" + PROP_DB_PASSWORD + "}") String password, MetricRegistry metricRegistry) {
		HikariDataSource poolingDataSource = new HikariDataSource();

		poolingDataSource.setJdbcUrl(url);
		poolingDataSource.setUsername(username);
		poolingDataSource.setPassword(password);

		poolingDataSource.setMaximumPoolSize(Runtime.getRuntime().availableProcessors() * 5);
		poolingDataSource.setRegisterMbeans(true);
		poolingDataSource.setMetricRegistry(metricRegistry);

		return poolingDataSource;
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
		extraProperties.put("hibernate.hbm2ddl.auto", "update");
		extraProperties.put("hibernate.jdbc.batch_size", "200");
		extraProperties.put("hibernate.cache.use_query_cache", "false");
		extraProperties.put("hibernate.cache.use_second_level_cache", "false");
		extraProperties.put("hibernate.cache.use_structured_entries", "false");
		extraProperties.put("hibernate.cache.use_minimal_puts", "false");
		extraProperties.put("hibernate.search.default.directory_provider", "filesystem");
		extraProperties.put("hibernate.search.default.indexBase", "target/lucenefiles");
		extraProperties.put("hibernate.search.lucene_version", "LUCENE_CURRENT");

		/*
		 * These configuration settings will set Hibernate to log all SQL
		 * statements and collect statistics, logging them out at the end of
		 * each session. They will cause a ton of logging, which will REALLY
		 * slow things down, so this should generally be disabled in production.
		 */
		if (HIBERNATE_DETAILED_LOGGING) {
			extraProperties.put(AvailableSettings.FORMAT_SQL, "true");
			extraProperties.put(AvailableSettings.USE_SQL_COMMENTS, "true");
			extraProperties.put(AvailableSettings.SHOW_SQL, "true");
			extraProperties.put(AvailableSettings.GENERATE_STATISTICS, "true");
		}

		/*
		 * Couldn't get these settings to work. Might need to read
		 * http://www.codesenior.com/en/tutorial/How-to-Show-Hibernate-
		 * Statistics-via-JMX-in-Spring-Framework-And-Jetty-Server more closely.
		 * (But I suspect the reason is that Hibernate's JMX support is just
		 * poorly tested and flat-out broken.)
		 */
		// extraProperties.put(AvailableSettings.JMX_ENABLED, "true");
		// extraProperties.put(AvailableSettings.JMX_DOMAIN_NAME, "hibernate");

		/*
		 * Disable Hibernate Search/Lucene indexing, as it slows writes down by
		 * about half. See this thread for some details on what the tradeoffs
		 * are: https://groups.google.com/forum/#!topic/hapi-fhir/lF9b_cLfgA4.
		 */
		extraProperties.put("hibernate.search.autoregister_listeners", "false");
		extraProperties.put("hibernate.search.indexing_strategy", "manual");
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
