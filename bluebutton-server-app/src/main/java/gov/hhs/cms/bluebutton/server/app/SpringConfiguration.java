package gov.hhs.cms.bluebutton.server.app;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.sql.DataSource;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.tool.schema.Action;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.zaxxer.hikari.HikariDataSource;

import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.ExplanationOfBenefitResourceProvider;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.PatientResourceProvider;

/**
 * The main Spring {@link Configuration} for the Blue Button API Backend
 * application.
 */
@Configuration
@ComponentScan(basePackageClasses = { BlueButtonServerInitializer.class })
public class SpringConfiguration {
	public static final String PROP_DB_URL = "bbfhir.db.url";
	public static final String PROP_DB_USERNAME = "bbfhir.db.username";
	public static final String PROP_DB_PASSWORD = "bbfhir.db.password";
	public static final String PROP_DB_CONNECTIONS_MAX = "bbfhir.db.connections.max";

	/**
	 * The {@link Bean#name()} for the {@link List} of STU3
	 * {@link IResourceProvider} beans for the application.
	 */
	static final String BLUEBUTTON_STU3_RESOURCE_PROVIDERS = "bluebuttonStu3ResourceProviders";

	/**
	 * Set this to <code>true</code> to have Hibernate log a ton of info on the
	 * SQL statements being run and each session's performance. Be sure to also
	 * adjust the related logging levels in Wildfly or whatever (see
	 * <code>server-config.sh</code> for details).
	 */
	private static final boolean HIBERNATE_DETAILED_LOGGING = false;

	/**
	 * @param url
	 *            the JDBC URL of the database for the application
	 * @param username
	 *            the database username to use
	 * @param password
	 *            the database password to use
	 * @param connectionsMaxText
	 *            the maximum number of database connections to use
	 * @param metricRegistry
	 *            the {@link MetricRegistry} for the application
	 * @return the {@link DataSource} that provides the application's database
	 *         connection
	 */
	@Bean(destroyMethod = "close")
	public DataSource dataSource(@Value("${" + PROP_DB_URL + "}") String url,
			@Value("${" + PROP_DB_USERNAME + "}") String username,
			@Value("${" + PROP_DB_PASSWORD + "}") String password,
			@Value("${" + PROP_DB_CONNECTIONS_MAX + ":-1}") String connectionsMaxText, MetricRegistry metricRegistry) {
		HikariDataSource poolingDataSource = new HikariDataSource();

		poolingDataSource.setJdbcUrl(url);
		poolingDataSource.setUsername(username);
		poolingDataSource.setPassword(password);

		int connectionsMax;
		try {
			connectionsMax = Integer.parseInt(connectionsMaxText);
		} catch (NumberFormatException e) {
			connectionsMax = -1;
		}
		if (connectionsMax < 1) {
			// Assign a reasonable default value, if none was specified.
			connectionsMax = Runtime.getRuntime().availableProcessors() * 5;
		}

		poolingDataSource.setMaximumPoolSize(connectionsMax);

		poolingDataSource.setRegisterMbeans(true);
		poolingDataSource.setMetricRegistry(metricRegistry);

		return poolingDataSource;
	}

	/**
	 * @param entityManagerFactory
	 *            the {@link EntityManagerFactory} to use
	 * @return the {@link JpaTransactionManager} for the application
	 */
	@Bean
	public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager retVal = new JpaTransactionManager();
		retVal.setEntityManagerFactory(entityManagerFactory);
		return retVal;
	}

	/**
	 * @param dataSource
	 *            the {@link DataSource} for the application
	 * @return the {@link LocalContainerEntityManagerFactoryBean}, which ensures
	 *         that other beans can safely request injection of
	 *         {@link EntityManager} instances
	 */
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
		LocalContainerEntityManagerFactoryBean retVal = new LocalContainerEntityManagerFactoryBean();
		retVal.setPersistenceUnitName("gov.hhs.cms.bluebutton.data");
		retVal.setDataSource(dataSource);
		retVal.setPackagesToScan("gov.hhs.cms.bluebutton");
		retVal.setPersistenceProvider(new HibernatePersistenceProvider());
		retVal.setJpaProperties(jpaProperties());
		retVal.afterPropertiesSet();
		return retVal;
	}

	/**
	 * @return the {@link Properties} to configure Hibernate and JPA with
	 */
	private Properties jpaProperties() {
		Properties extraProperties = new Properties();
		extraProperties.put(AvailableSettings.HBM2DDL_AUTO, Action.VALIDATE);
		extraProperties.put("hibernate.cache.use_query_cache", "false");
		extraProperties.put("hibernate.cache.use_second_level_cache", "false");
		extraProperties.put("hibernate.cache.use_structured_entries", "false");
		extraProperties.put("hibernate.cache.use_minimal_puts", "false");

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

		return extraProperties;
	}

	/**
	 * @return a Spring {@link BeanPostProcessor} that enables the use of the
	 *         JPA {@link PersistenceUnit} and {@link PersistenceContext}
	 *         annotations for injection of {@link EntityManagerFactory} and
	 *         {@link EntityManager} instances, respectively, into beans
	 */
	@Bean
	public PersistenceAnnotationBeanPostProcessor persistenceAnnotationProcessor() {
		return new PersistenceAnnotationBeanPostProcessor();
	}

	/**
	 * @param patientResourceProvider
	 *            the application's {@link PatientResourceProvider} bean
	 * @param eobResourceProvider
	 *            the application's {@link ExplanationOfBenefitResourceProvider}
	 *            bean
	 * @return the {@link List} of STU3 {@link IResourceProvider} beans for the
	 *         application
	 */
	@Bean(name = BLUEBUTTON_STU3_RESOURCE_PROVIDERS)
	public List<IResourceProvider> stu3ResourceProviders(PatientResourceProvider patientResourceProvider,
			ExplanationOfBenefitResourceProvider eobResourceProvider) {
		List<IResourceProvider> stu3ResourceProviders = new ArrayList<IResourceProvider>();
		stu3ResourceProviders.add(patientResourceProvider);
		stu3ResourceProviders.add(eobResourceProvider);
		return stu3ResourceProviders;
	}

	/**
	 * @return the application's {@link ExplanationOfBenefitResourceProvider}
	 *         bean
	 */
	@Bean
	public ExplanationOfBenefitResourceProvider eobResourceProvider() {
		ExplanationOfBenefitResourceProvider eobResourceProvider = new ExplanationOfBenefitResourceProvider();
		return eobResourceProvider;
	}

	/**
	 * @return the {@link MetricRegistry} for the application, which can be used
	 *         to collect statistics on the application's performance
	 */
	@Bean
	public MetricRegistry metricRegistry() {
		MetricRegistry metricRegistry = new MetricRegistry();
		metricRegistry.registerAll(new MemoryUsageGaugeSet());
		metricRegistry.registerAll(new GarbageCollectorMetricSet());

		final JmxReporter reporter = JmxReporter.forRegistry(metricRegistry).build();
		reporter.start();

		return metricRegistry;
	}

	/**
	 * @return an {@link IServerInterceptor} that will do some fancy logging to
	 *         create a nice access log that has details about each incoming
	 *         request
	 */
	@Bean
	public IServerInterceptor loggingInterceptor() {
		LoggingInterceptor retVal = new LoggingInterceptor();
		retVal.setLoggerName("bluebutton.web.access");
		retVal.setMessageFormat(
				"Path[${servletPath}] Source[${requestHeader.x-forwarded-for}] Operation[${operationType} ${operationName} ${idOrResourceName}] UA[${requestHeader.user-agent}] Params[${requestParameters}] ResponseEncoding[${responseEncodingNoDefault}]");
		retVal.setLogExceptions(true);
		retVal.setErrorMessageFormat("ERROR - ${requestVerb} ${requestUrl}");
		return retVal;
	}

	/**
	 * @return an {@link IServerInterceptor} that will add some pretty syntax
	 *         highlighting in responses when a browser is detected
	 */
	@Bean
	public IServerInterceptor responseHighlighterInterceptor() {
		ResponseHighlighterInterceptor retVal = new ResponseHighlighterInterceptor();
		return retVal;
	}
}
