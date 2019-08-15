package gov.hhs.cms.bluebutton.server.app;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.hsqldb.persist.HsqlProperties;
import org.hsqldb.server.ServerAcl.AclFormatException;
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
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;
import com.zaxxer.hikari.HikariDataSource;

import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import gov.hhs.cms.bluebutton.data.model.rif.schema.DatabaseSchemaManager;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.CoverageResourceProvider;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.ExplanationOfBenefitResourceProvider;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.PatientResourceProvider;
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;

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
	public static final int TRANSACTION_TIMEOUT = 2;

	/**
	 * This fake JDBC URL prefix indicates to {@link SpringConfiguration} and
	 * <code>ServerTestUtils</code> that a database should be created for the
	 * integration tests being run.
	 */
	private static final String JDBC_URL_PREFIX_BLUEBUTTON_TEST = "jdbc:bluebutton-test:";

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
		HikariDataSource poolingDataSource;
		if (url.startsWith(JDBC_URL_PREFIX_BLUEBUTTON_TEST)) {
			poolingDataSource = createTestDatabaseIfNeeded(url, connectionsMaxText, metricRegistry);
		} else {
			poolingDataSource = new HikariDataSource();
			poolingDataSource.setJdbcUrl(url);
			poolingDataSource.setUsername(username);
			poolingDataSource.setPassword(password);
			configureDataSource(poolingDataSource, connectionsMaxText, metricRegistry);
		}

		// Wrap the pooled DataSource in a proxy that records performance data.
		ProxyDataSource proxyDataSource = ProxyDataSourceBuilder.create(poolingDataSource).name("BFD-Data")
				.listener(new QueryLoggingListener()).proxyResultSet().build();

		return proxyDataSource;
	}

	/**
	 * <p>
	 * When running this application for integration testing, this application
	 * should provision its own database. In addition, it must ensure that the
	 * database is accessible to other processes on this system, which allows
	 * the test code to connect directly to the database and load/remove data.
	 * </p>
	 * <p>
	 * To determine whether or not this is the case, the integration tests use
	 * special fake JDBC URLs that are special-cased here and in the
	 * <code>ServerTestUtils</code> class.
	 * </p>
	 * 
	 * @param url
	 *            the JDBC URL that the application was configured to use
	 * @param connectionsMaxText
	 *            the maximum number of database connections to use
	 * @param metricRegistry
	 *            the {@link MetricRegistry} for the application
	 */
	private static HikariDataSource createTestDatabaseIfNeeded(String url, String connectionsMaxText,
			MetricRegistry metricRegistry) {
		if (!url.startsWith(JDBC_URL_PREFIX_BLUEBUTTON_TEST)) {
			throw new IllegalArgumentException();
		}

		/*
		 * Note: Eventually, we may add support for other test DB types, but
		 * right now only in-memory HSQL DBs are supported.
		 */
		if (!url.endsWith(":hsqldb:mem")) {
			throw new BadCodeMonkeyException("Unsupported test URL: " + url);
		}

		/*
		 * Select a random local port to run the HSQL DB server on, so that one
		 * test run doesn't conflict with another.
		 */
		int hsqldbPort = findFreePort();

		HsqlProperties p = new HsqlProperties();
		p.setProperty("server.database.0", "mem:test-embedded;user=test;password=test");
		p.setProperty("server.dbname.0", "test-embedded");
		p.setProperty("server.port", "" + hsqldbPort);
		p.setProperty("hsqldb.tx", "mvcc");
		org.hsqldb.server.Server server = new org.hsqldb.server.Server();

		try {
			server.setProperties(p);
		} catch (IOException | AclFormatException e) {
			throw new BadCodeMonkeyException(e);
		}

		server.setLogWriter(null);
		server.setErrWriter(null);
		server.start();

		// Create the DataSource to connect to that shiny new DB.
		HikariDataSource poolingDataSource = new HikariDataSource();
		poolingDataSource.setJdbcUrl(String.format("jdbc:hsqldb:hsql://localhost:%d/test-embedded", hsqldbPort));
		poolingDataSource.setUsername("test");
		poolingDataSource.setPassword("test");
		configureDataSource(poolingDataSource, connectionsMaxText, metricRegistry);

		/*
		 * Ensure the DataSource DB's schema is ready to use, because once
		 * Spring starts, anything can try to use it.
		 */
		DatabaseSchemaManager.createOrUpdateSchema(poolingDataSource);

		/*
		 * Write out the DB properties for <code>ServerTestUtils</code> to use.
		 */
		Properties testDbProps = new Properties();
		testDbProps.setProperty(PROP_DB_URL, poolingDataSource.getJdbcUrl());
		testDbProps.setProperty(PROP_DB_USERNAME, poolingDataSource.getUsername());
		testDbProps.setProperty(PROP_DB_PASSWORD, poolingDataSource.getPassword());
		Path testDbPropsPath = findTestDatabaseProperties();
		try {
			testDbProps.store(new FileWriter(testDbPropsPath.toFile()), null);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return poolingDataSource;
	}

	/**
	 * @return the {@link Path} to the {@link Properties} file in
	 *         <code>target/bluebutton-server</code> that the test DB connection
	 *         properties will be written out to
	 */
	public static Path findTestDatabaseProperties() {
		Path serverRunDir = Paths.get("target", "bluebutton-server");
		if (!Files.isDirectory(serverRunDir))
			serverRunDir = Paths.get("bluebutton-data-server-app", "target", "bluebutton-server");
		if (!Files.isDirectory(serverRunDir))
			throw new IllegalStateException("Unable to find 'bluebutton-server' directory. Working directory: "
					+ Paths.get(".").toAbsolutePath());

		Path testDbPropertiesPath = serverRunDir.resolve("server-test-db.properties");
		return testDbPropertiesPath;
	}

	/**
	 * Note: It's possible for this to result in race conditions, if the random
	 * port selected enters use after this method returns and before whatever
	 * called this method gets a chance to grab it. It's pretty unlikely,
	 * though, and there's not much we can do about it, either. So.
	 *
	 * @return a free local port number
	 */
	private static int findFreePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			socket.setReuseAddress(true);
			return socket.getLocalPort();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * @param poolingDataSource
	 *            the {@link HikariDataSource} to be configured, which must
	 *            already have its basic connection properties (URL, username,
	 *            password) configured
	 * @param connectionsMaxText
	 *            the maximum number of database connections to use
	 * @param metricRegistry
	 *            the {@link MetricRegistry} for the application
	 */
	private static void configureDataSource(HikariDataSource poolingDataSource,
			String connectionsMaxText, MetricRegistry metricRegistry) {
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

		/*
		 * FIXME Temporary workaround for CBBI-357: send Postgres' query planner a
		 * strongly worded letter instructing it to avoid sequential scans whenever
		 * possible.
		 */
		if (poolingDataSource.getJdbcUrl().contains("postgre"))
			poolingDataSource.setConnectionInitSql("set enable_seqscan = false;");

		poolingDataSource.setRegisterMbeans(true);
		poolingDataSource.setMetricRegistry(metricRegistry);
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
		// retVal.setDefaultTimeout(TRANSACTION_TIMEOUT);
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
		LocalContainerEntityManagerFactoryBean containerEmfBean = new LocalContainerEntityManagerFactoryBean();
		containerEmfBean.setDataSource(dataSource);
		containerEmfBean.setPackagesToScan("gov.hhs.cms.bluebutton.data.model.rif");
		containerEmfBean.setPersistenceProvider(new HibernatePersistenceProvider());
		containerEmfBean.setJpaProperties(jpaProperties());
		containerEmfBean.afterPropertiesSet();
		return containerEmfBean;
	}

	/**
	 * @return the {@link Properties} to configure Hibernate and JPA with
	 */
	private Properties jpaProperties() {
		Properties extraProperties = new Properties();
		extraProperties.put(AvailableSettings.HBM2DDL_AUTO, Action.VALIDATE);
		
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
	 * @param coverageResourceProvider
	 *            the application's {@link CoverageResourceProvider} bean
	 * @param eobResourceProvider
	 *            the application's {@link ExplanationOfBenefitResourceProvider}
	 *            bean
	 * @return the {@link List} of STU3 {@link IResourceProvider} beans for the
	 *         application
	 */
	@Bean(name = BLUEBUTTON_STU3_RESOURCE_PROVIDERS)
	public List<IResourceProvider> stu3ResourceProviders(PatientResourceProvider patientResourceProvider,
			CoverageResourceProvider coverageResourceProvider,
			ExplanationOfBenefitResourceProvider eobResourceProvider) {
		List<IResourceProvider> stu3ResourceProviders = new ArrayList<IResourceProvider>();
		stu3ResourceProviders.add(patientResourceProvider);
		stu3ResourceProviders.add(coverageResourceProvider);
		stu3ResourceProviders.add(eobResourceProvider);
		return stu3ResourceProviders;
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
	 * @return the {@link HealthCheckRegistry} for the application, which collects
	 *         any/all health checks that it provides
	 */
	@Bean
	public HealthCheckRegistry healthCheckRegistry() {
		HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
		return healthCheckRegistry;
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
