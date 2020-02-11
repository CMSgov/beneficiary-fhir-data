package gov.cms.bfd.server.war;

import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;
import gov.cms.bfd.model.rif.schema.DatabaseTestHelper;
import gov.cms.bfd.model.rif.schema.DatabaseTestHelper.DataSourceComponents;
import gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
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
import net.ttddyy.dsproxy.support.ProxyDataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
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
import org.springframework.scheduling.annotation.EnableScheduling;

/** The main Spring {@link Configuration} for the Blue Button API Backend application. */
@Configuration
@ComponentScan(basePackageClasses = {ServerInitializer.class})
@EnableScheduling
public class SpringConfiguration {
  public static final String PROP_DB_URL = "bfdServer.db.url";
  public static final String PROP_DB_USERNAME = "bfdServer.db.username";
  public static final String PROP_DB_PASSWORD = "bfdServer.db.password";
  public static final String PROP_DB_CONNECTIONS_MAX = "bfdServer.db.connections.max";
  public static final String PROP_DB_SCHEMA_APPLY = "bfdServer.db.schema.apply";
  public static final int TRANSACTION_TIMEOUT = 30;

  /**
   * The {@link Bean#name()} for the {@link List} of STU3 {@link IResourceProvider} beans for the
   * application.
   */
  static final String BLUEBUTTON_STU3_RESOURCE_PROVIDERS = "bluebuttonStu3ResourceProviders";

  /**
   * Set this to <code>true</code> to have Hibernate log a ton of info on the SQL statements being
   * run and each session's performance. Be sure to also adjust the related logging levels in
   * Wildfly or whatever (see <code>server-config.sh</code> for details).
   */
  private static final boolean HIBERNATE_DETAILED_LOGGING = false;

  /**
   * @param url the JDBC URL of the database for the application
   * @param username the database username to use
   * @param password the database password to use
   * @param connectionsMaxText the maximum number of database connections to use
   * @param schemaApplyText whether or not to create/update the DB schema
   * @param metricRegistry the {@link MetricRegistry} for the application
   * @return the {@link DataSource} that provides the application's database connection
   */
  @Bean(destroyMethod = "close")
  public DataSource dataSource(
      @Value("${" + PROP_DB_URL + "}") String url,
      @Value("${" + PROP_DB_USERNAME + "}") String username,
      @Value("${" + PROP_DB_PASSWORD + "}") String password,
      @Value("${" + PROP_DB_CONNECTIONS_MAX + ":-1}") String connectionsMaxText,
      @Value("${" + PROP_DB_SCHEMA_APPLY + ":false}") String schemaApplyText,
      MetricRegistry metricRegistry) {
    HikariDataSource poolingDataSource;
    if (url.startsWith(DatabaseTestHelper.JDBC_URL_PREFIX_BLUEBUTTON_TEST)) {
      poolingDataSource = createTestDatabaseIfNeeded(url, connectionsMaxText, metricRegistry);
    } else {
      poolingDataSource = new HikariDataSource();
      poolingDataSource.setJdbcUrl(url);
      if (username != null && !username.isEmpty()) poolingDataSource.setUsername(username);
      if (password != null && !password.isEmpty()) poolingDataSource.setPassword(password);
      configureDataSource(poolingDataSource, connectionsMaxText, metricRegistry);
    }

    // Wrap the pooled DataSource in a proxy that records performance data.
    ProxyDataSource proxyDataSource =
        ProxyDataSourceBuilder.create(poolingDataSource)
            .name("BFD-Data")
            .listener(new QueryLoggingListener())
            .proxyResultSet()
            .build();

    // Create/upgrade the DB schema, if specified.
    boolean schemaApply = Boolean.parseBoolean(schemaApplyText);
    if (schemaApply) {
      DatabaseSchemaManager.createOrUpdateSchema(proxyDataSource);
    }

    return proxyDataSource;
  }

  /**
   * Some of the DBs we support using in local development and testing require special handling.
   * This method takes care of that.
   *
   * @param url the JDBC URL that the application was configured to use
   * @param connectionsMaxText the maximum number of database connections to use
   * @param metricRegistry the {@link MetricRegistry} for the application
   */
  private static HikariDataSource createTestDatabaseIfNeeded(
      String url, String connectionsMaxText, MetricRegistry metricRegistry) {
    /*
     * Note: Eventually, we may add support for other test DB types, but
     * right now only in-memory HSQL DBs are supported.
     */
    if (url.endsWith(":hsqldb:mem")) {
      return createTestDatabaseIfNeededForHsql(url, connectionsMaxText, metricRegistry);
    } else {
      throw new BadCodeMonkeyException("Unsupported test URL: " + url);
    }
  }

  /**
   * Handles {@link #createTestDatabaseIfNeeded(String, String, MetricRegistry)} for HSQL. We need
   * to special-case the HSQL DBs that are supported by our tests, so that they get handled
   * correctly. Specifically, we need to ensure that the HSQL Server is started up, so that our test
   * code can access the DB directly. In addition, we need to ensure that connection details to that
   * HSQL server get written out somewhere that the test code can find it.
   *
   * @param url the JDBC URL that the application was configured to use
   * @param connectionsMaxText the maximum number of database connections to use
   * @param metricRegistry the {@link MetricRegistry} for the application
   */
  private static HikariDataSource createTestDatabaseIfNeededForHsql(
      String url, String connectionsMaxText, MetricRegistry metricRegistry) {
    DataSource dataSource = DatabaseTestHelper.getTestDatabaseAfterCleanAndSchema();
    DataSourceComponents dataSourceComponents = new DataSourceComponents(dataSource);

    // Create the DataSource to connect to that shiny new DB.
    HikariDataSource dataSourcePool = new HikariDataSource();
    dataSourcePool.setDataSource(dataSource);
    configureDataSource(dataSourcePool, connectionsMaxText, metricRegistry);

    /*
     * Write out the DB properties for <code>ServerTestUtils</code> to use.
     */
    Properties testDbProps = new Properties();
    testDbProps.setProperty(PROP_DB_URL, dataSourceComponents.getUrl());
    testDbProps.setProperty(PROP_DB_USERNAME, dataSourceComponents.getUsername());
    testDbProps.setProperty(PROP_DB_PASSWORD, dataSourceComponents.getPassword());
    Path testDbPropsPath = findTestDatabaseProperties();
    try {
      testDbProps.store(new FileWriter(testDbPropsPath.toFile()), null);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    return dataSourcePool;
  }

  /**
   * @return the {@link Path} to the {@link Properties} file in <code>target/server-work</code> that
   *     the test DB connection properties will be written out to
   */
  public static Path findTestDatabaseProperties() {
    Path serverRunDir = Paths.get("target", "server-work");
    if (!Files.isDirectory(serverRunDir))
      serverRunDir = Paths.get("bfd-server-war", "target", "server-work");
    if (!Files.isDirectory(serverRunDir))
      throw new IllegalStateException(
          "Unable to find 'server-work' directory. Working directory: "
              + Paths.get(".").toAbsolutePath());

    Path testDbPropertiesPath = serverRunDir.resolve("server-test-db.properties");
    return testDbPropertiesPath;
  }

  /**
   * @param poolingDataSource the {@link HikariDataSource} to be configured, which must already have
   *     its basic connection properties (URL, username, password) configured
   * @param connectionsMaxText the maximum number of database connections to use
   * @param metricRegistry the {@link MetricRegistry} for the application
   */
  private static void configureDataSource(
      HikariDataSource poolingDataSource,
      String connectionsMaxText,
      MetricRegistry metricRegistry) {
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
    if (poolingDataSource.getJdbcUrl() != null
        && poolingDataSource.getJdbcUrl().contains("postgre"))
      poolingDataSource.setConnectionInitSql(
          "set application_name = 'bfd-server'; set enable_seqscan = false;");

    poolingDataSource.setRegisterMbeans(true);
    poolingDataSource.setMetricRegistry(metricRegistry);

    /*
     * FIXME Temporary setting for BB-1233 to find the source of any possible leaks
     * (see: https://github.com/brettwooldridge/HikariCP/issues/1111)
     */
    poolingDataSource.setLeakDetectionThreshold(60 * 1000);
  }

  /**
   * @param entityManagerFactory the {@link EntityManagerFactory} to use
   * @return the {@link JpaTransactionManager} for the application
   */
  @Bean
  public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    JpaTransactionManager retVal = new JpaTransactionManager();
    retVal.setEntityManagerFactory(entityManagerFactory);
    return retVal;
  }

  /**
   * @param dataSource the {@link DataSource} for the application
   * @return the {@link LocalContainerEntityManagerFactoryBean}, which ensures that other beans can
   *     safely request injection of {@link EntityManager} instances
   */
  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean containerEmfBean =
        new LocalContainerEntityManagerFactoryBean();
    containerEmfBean.setDataSource(dataSource);
    containerEmfBean.setPackagesToScan("gov.cms.bfd.model.rif");
    containerEmfBean.setPersistenceProvider(new HibernatePersistenceProvider());
    containerEmfBean.setJpaProperties(jpaProperties());
    containerEmfBean.afterPropertiesSet();
    return containerEmfBean;
  }

  /** @return the {@link Properties} to configure Hibernate and JPA with */
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

    // This limits how long each query will run before being terminated. We've seen
    // long running queries cause the application to respond poorly to other
    // requests.
    extraProperties.put("javax.persistence.query.timeout", TRANSACTION_TIMEOUT * 1000);

    return extraProperties;
  }

  /**
   * @return a Spring {@link BeanPostProcessor} that enables the use of the JPA {@link
   *     PersistenceUnit} and {@link PersistenceContext} annotations for injection of {@link
   *     EntityManagerFactory} and {@link EntityManager} instances, respectively, into beans
   */
  @Bean
  public PersistenceAnnotationBeanPostProcessor persistenceAnnotationProcessor() {
    return new PersistenceAnnotationBeanPostProcessor();
  }

  /**
   * @param patientResourceProvider the application's {@link PatientResourceProvider} bean
   * @param coverageResourceProvider the application's {@link CoverageResourceProvider} bean
   * @param eobResourceProvider the application's {@link ExplanationOfBenefitResourceProvider} bean
   * @return the {@link List} of STU3 {@link IResourceProvider} beans for the application
   */
  @Bean(name = BLUEBUTTON_STU3_RESOURCE_PROVIDERS)
  public List<IResourceProvider> stu3ResourceProviders(
      PatientResourceProvider patientResourceProvider,
      CoverageResourceProvider coverageResourceProvider,
      ExplanationOfBenefitResourceProvider eobResourceProvider) {
    List<IResourceProvider> stu3ResourceProviders = new ArrayList<IResourceProvider>();
    stu3ResourceProviders.add(patientResourceProvider);
    stu3ResourceProviders.add(coverageResourceProvider);
    stu3ResourceProviders.add(eobResourceProvider);
    return stu3ResourceProviders;
  }

  /**
   * @return the {@link MetricRegistry} for the application, which can be used to collect statistics
   *     on the application's performance
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
   * @return the {@link HealthCheckRegistry} for the application, which collects any/all health
   *     checks that it provides
   */
  @Bean
  public HealthCheckRegistry healthCheckRegistry() {
    HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
    return healthCheckRegistry;
  }

  /**
   * @return an {@link IServerInterceptor} that will add some pretty syntax highlighting in
   *     responses when a browser is detected
   */
  @Bean
  public IServerInterceptor responseHighlighterInterceptor() {
    ResponseHighlighterInterceptor retVal = new ResponseHighlighterInterceptor();
    return retVal;
  }
}
