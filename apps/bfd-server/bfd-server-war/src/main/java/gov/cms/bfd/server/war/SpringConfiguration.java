package gov.cms.bfd.server.war;

import static gov.cms.bfd.sharedutils.config.BaseAppConfiguration.ENV_VAR_KEY_AWS_REGION;

import ca.uhn.fhir.rest.server.IResourceProvider;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.newrelic.NewRelicReporter;
import com.google.common.base.Strings;
import com.newrelic.telemetry.Attributes;
import com.newrelic.telemetry.OkHttpPoster;
import com.newrelic.telemetry.SenderConfiguration;
import com.newrelic.telemetry.metrics.MetricBatchSender;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.data.fda.lookup.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.server.war.r4.providers.R4CoverageResourceProvider;
import gov.cms.bfd.server.war.r4.providers.R4ExplanationOfBenefitResourceProvider;
import gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider;
import gov.cms.bfd.server.war.r4.providers.pac.R4ClaimResourceProvider;
import gov.cms.bfd.server.war.r4.providers.pac.R4ClaimResponseResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider;
import gov.cms.bfd.sharedutils.TransactionManager;
import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.config.LayeredConfiguration;
import gov.cms.bfd.sharedutils.database.DataSourceFactory;
import gov.cms.bfd.sharedutils.database.DatabaseOptions;
import gov.cms.bfd.sharedutils.database.DatabaseUtils;
import gov.cms.bfd.sharedutils.database.HikariDataSourceFactory;
import gov.cms.bfd.sharedutils.database.RdsDataSourceFactory;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.tool.schema.Action;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.regions.Region;

/** The main Spring {@link Configuration} for the Blue Button API Backend application. */
@Configuration
@ComponentScan(basePackageClasses = {ServerInitializer.class})
@EnableScheduling
public class SpringConfiguration {
  /** The authentication type that BFD will use for all database connections. */
  public static final String PROP_DB_AUTH_TYPE = "bfdServer.db.authType";
  /** The database url that BFD will use for all database calls. */
  public static final String PROP_DB_URL = "bfdServer.db.url";
  /** The database username. */
  public static final String PROP_DB_USERNAME = "bfdServer.db.username";
  /** The database password. */
  public static final String PROP_DB_PASSWORD = "bfdServer.db.password";
  /** The max number of database connections to be used. */
  public static final String PROP_DB_CONNECTIONS_MAX = "bfdServer.db.connections.max";
  /** The schema apply text. */
  public static final String PROP_DB_SCHEMA_APPLY = "bfdServer.db.schema.apply";
  /**
   * The {@link String } Boolean property that is used to enable the fake drug code (00000-0000)
   * that is used for integration testing. When this property is set to the string 'true', this fake
   * drug code will be appended to the drug code lookup map to avoid test failures that result from
   * unexpected changes to the external drug code file in {@link
   * FdaDrugCodeDisplayLookup#retrieveFDADrugCodeDisplay}. This property defaults to false and
   * should only be set to true when the server is under test in a local environment.
   */
  public static final String PROP_INCLUDE_FAKE_DRUG_CODE = "bfdServer.include.fake.drug.code";
  /**
   * The {@link String } Boolean property that is used to enable the fake org name that is used for
   * integration testing.
   */
  public static final String PROP_INCLUDE_FAKE_ORG_NAME = "bfdServer.include.fake.org.name";

  /** The database transaction timeout value (seconds). */
  public static final int TRANSACTION_TIMEOUT = 30;

  /**
   * The {@link Bean#name()} for the {@link List} of STU3 {@link IResourceProvider} beans for the
   * application.
   */
  static final String BLUEBUTTON_STU3_RESOURCE_PROVIDERS = "bluebuttonStu3ResourceProviders";

  /**
   * The {@link Bean#name()} for the {@link List} of R4 {@link IResourceProvider} beans for the
   * application.
   */
  static final String BLUEBUTTON_R4_RESOURCE_PROVIDERS = "bluebuttonR4ResourceProviders";

  /**
   * Set this to {@code true} to have Hibernate log a ton of info on the SQL statements being run
   * and each session's performance. Be sure to also adjust the related logging levels in Wildfly or
   * whatever (see {@code server-config.sh} for details).
   */
  private static final boolean HIBERNATE_DETAILED_LOGGING = false;

  /**
   * Attribute name used to expose the source {@link ConfigLoader} for use by {@link
   * SpringConfiguration}. Avoids the need to recreate an instance there if one has already been
   * created for use here or define a static field to hold it.
   */
  static final String CONFIG_LOADER_CONTEXT_NAME = "ConfigLoaderInstance";

  /**
   * The {@link Bean#name()} for the {@link Boolean} indicating if PAC data should be queryable
   * using old MBI hash.
   */
  public static final String PAC_OLD_MBI_HASH_ENABLED = "PacOldMbiHashEnabled";

  /** The persistence unit name for the adjudicated pipeline. */
  public static final String PERSISTENCE_UNIT_NAME = "gov.cms.bfd";
  /** The persistence unit name for the RDA (pre-adjudicated) pipeline. */
  public static final String RDA_PERSISTENCE_UNIT_NAME = "gov.cms.bfd.rda";

  /**
   * Exposes our {@link ConfigLoader} instance as a singleton to components in the application. If
   * one has already been created for use in a {@link ConfigPropertySource} and added to the {@link
   * ServletContext} we simply return that one. Otherwise we create a new one.
   *
   * @param servletContext used to look for config loader attribute
   * @return the config object
   */
  @Bean
  public ConfigLoader configLoader(@Autowired ServletContext servletContext) {
    return servletContext.getAttribute(CONFIG_LOADER_CONTEXT_NAME) != null
        ? (ConfigLoader) servletContext.getAttribute(CONFIG_LOADER_CONTEXT_NAME)
        : createConfigLoader(System::getenv);
  }

  /**
   * Creates an {@link AwsClientConfig} used for all AWS services.
   *
   * @param awsRegionName optional region name
   * @return the config
   */
  @Bean
  public AwsClientConfig awsClientConfig(
      @Value("${" + ENV_VAR_KEY_AWS_REGION + ":}") String awsRegionName) {
    Region region = Strings.isNullOrEmpty(awsRegionName) ? null : Region.of(awsRegionName);
    return AwsClientConfig.awsBuilder().region(region).build();
  }

  /**
   * Creates a factory to create {@link DataSource}s.
   *
   * @param authTypeName whether to use RDS or JDBC authentication
   * @param url the JDBC URL of the database for the application
   * @param username the database username to use
   * @param password the database password to use
   * @param connectionsMaxText the maximum number of database connections to use
   * @param awsClientConfig common AWS settings
   * @return the factory
   */
  @Bean
  public DataSourceFactory dataSourceFactory(
      @Value("${" + PROP_DB_AUTH_TYPE + ":JDBC}") String authTypeName,
      @Value("${" + PROP_DB_URL + "}") String url,
      @Value("${" + PROP_DB_USERNAME + "}") String username,
      @Value("${" + PROP_DB_PASSWORD + ":}") String password,
      @Value("${" + PROP_DB_CONNECTIONS_MAX + ":-1}") String connectionsMaxText,
      AwsClientConfig awsClientConfig) {
    final var authType = DatabaseOptions.AuthenticationType.valueOf(authTypeName);
    final int maxPoolSize = DatabaseUtils.computeMaximumPoolSize(connectionsMaxText);
    final var databaseOptions =
        DatabaseOptions.builder()
            .authenticationType(authType)
            .databaseUrl(url)
            .databaseUsername(username)
            .databasePassword(password)
            .maxPoolSize(maxPoolSize)
            .build();
    if (databaseOptions.getAuthenticationType() == DatabaseOptions.AuthenticationType.RDS) {
      return RdsDataSourceFactory.builder()
          .awsClientConfig(awsClientConfig)
          .databaseOptions(databaseOptions)
          .build();
    } else {
      return new HikariDataSourceFactory(databaseOptions);
    }
  }

  /**
   * Creates the application's database connection pool using a factory.
   *
   * @param dataSourceFactory factory used to create {@link DataSource}s
   * @param metricRegistry the {@link MetricRegistry} for the application
   * @return the {@link DataSource} that provides the application's database connection
   */
  @Bean(destroyMethod = "close")
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public DataSource dataSource(DataSourceFactory dataSourceFactory, MetricRegistry metricRegistry) {

    HikariDataSource pooledDataSource = dataSourceFactory.createDataSource();
    DatabaseUtils.configureDataSource(pooledDataSource, metricRegistry);

    // Wrap the pooled DataSource in a proxy that records performance data.
    return ProxyDataSourceBuilder.create(pooledDataSource)
        .name("BFD-Data")
        .listener(new QueryLoggingListener())
        .proxyResultSet()
        .build();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public EntityManagerFactory entityManagerFactory(DataSource dataSource) {
    return createEntityManagerFactory(dataSource, PERSISTENCE_UNIT_NAME);
  }

  /**
   * Creates the transaction manager for the application from a factory.
   *
   * @param entityManagerFactory the {@link EntityManagerFactory} to use
   * @return the {@link TransactionManager} for the application
   */
  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public TransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    return new TransactionManager(entityManagerFactory);
  }

  /**
   * Creates an entity manager factory.
   *
   * @param pooledDataSource the JDBC {@link DataSource} for the application's database
   * @param persistenceUnitName the persistence unit name
   * @return a JPA {@link EntityManagerFactory} for the application's database
   */
  private static EntityManagerFactory createEntityManagerFactory(
      DataSource pooledDataSource, String persistenceUnitName) {
    /*
     * The number of JDBC statements that will be queued/batched within a single transaction. Most
     * recommendations suggest this should be 5-30. Paradoxically, setting it higher seems to
     * actually slow things down. Presumably, it's delaying work that could be done earlier in a
     * batch, and that starts to cost more than the extra network roundtrips.
     */
    final int jdbcBatchSize = 10;

    Map<String, Object> hibernateProperties = new HashMap<>();
    hibernateProperties.put(org.hibernate.cfg.AvailableSettings.DATASOURCE, pooledDataSource);
    /*
     * Hibernate validation is done in the validator app, so leave HBM2DDL_AUTO disabled here.
     * This defaults to NONE, but just explicitly noting this.
     */
    hibernateProperties.put(org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, Action.NONE);
    hibernateProperties.put(
        org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, jdbcBatchSize);

    EntityManagerFactory entityManagerFactory =
        Persistence.createEntityManagerFactory(persistenceUnitName, jpaProperties());
    return entityManagerFactory;
  }

  /**
   * Creates the {@link Properties} to configure Hibernate and JPA with.
   *
   * @return the jpa properties
   */
  private static Properties jpaProperties() {
    Properties extraProperties = new Properties();
    /*
     * Hibernate validation is being disabled in the applications so that
     * validation failures do not prevent the server from starting.
     * With the implementation of RFC-0011 this validation will be moved
     * to a more appropriate stage of the deployment.
     */
    extraProperties.put(AvailableSettings.HBM2DDL_AUTO, Action.NONE);

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
    extraProperties.put("jakarta.persistence.query.timeout", TRANSACTION_TIMEOUT * 1000);

    return extraProperties;
  }

  /**
   * Gets a {@link List} of STU3 {@link IResourceProvider} beans for the application.
   *
   * @param patientResourceProvider the application's {@link PatientResourceProvider} bean
   * @param coverageResourceProvider the application's {@link CoverageResourceProvider} bean
   * @param eobResourceProvider the application's {@link ExplanationOfBenefitResourceProvider} bean
   * @return the {@link List} of STU3 {@link IResourceProvider} beans
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
   * Determines if the fhir resources related to partially adjudicated claims data will accept
   * {@link Mbi#getOldHash()} values for queries. This is off by default but when enabled will
   * simplify rotation of hash values.
   *
   * @param enabled injected property indicating if feature is enabled
   * @return True if the resources should use oldHash values in queries, False otherwise.
   */
  @Bean(name = PAC_OLD_MBI_HASH_ENABLED)
  Boolean isPacOldMbiHashEnabled(
      @Value("${bfdServer.pac.oldMbiHash.enabled:false}") Boolean enabled) {
    return enabled;
  }

  /**
   * Creates a new r4 resource provider list.
   *
   * @param r4PatientResourceProvider the application's {@link R4PatientResourceProvider} bean
   * @param r4CoverageResourceProvider the r4 coverage resource provider
   * @param r4EOBResourceProvider the r4 eob resource provider
   * @param r4ClaimResourceProvider the r4 claim resource provider
   * @param r4ClaimResponseResourceProvider the r4 claim response resource provider
   * @param pacEnabled Determines if the fhir resources related to partially adjudicated claims data
   *     should be accessible via the fhir api service.
   * @return the {@link List} of R4 {@link IResourceProvider} beans for the application
   */
  @Bean(name = BLUEBUTTON_R4_RESOURCE_PROVIDERS)
  public List<IResourceProvider> r4ResourceProviders(
      R4PatientResourceProvider r4PatientResourceProvider,
      R4CoverageResourceProvider r4CoverageResourceProvider,
      R4ExplanationOfBenefitResourceProvider r4EOBResourceProvider,
      R4ClaimResourceProvider r4ClaimResourceProvider,
      R4ClaimResponseResourceProvider r4ClaimResponseResourceProvider,
      @Value("${bfdServer.pac.enabled:false}") Boolean pacEnabled) {

    List<IResourceProvider> r4ResourceProviders = new ArrayList<IResourceProvider>();
    r4ResourceProviders.add(r4PatientResourceProvider);
    r4ResourceProviders.add(r4CoverageResourceProvider);
    r4ResourceProviders.add(r4EOBResourceProvider);
    if (pacEnabled) {
      r4ResourceProviders.add(r4ClaimResourceProvider);
      r4ResourceProviders.add(r4ClaimResponseResourceProvider);
    }

    return r4ResourceProviders;
  }

  /**
   * Creates a {@link MetricRegistry} for the application, which can be used to collect statistics
   * on the application's performance.
   *
   * @param config used to look up configuration values
   * @return the metric registry
   */
  @Bean
  public MetricRegistry metricRegistry(ConfigLoader config) {
    MetricRegistry metricRegistry = new MetricRegistry();
    metricRegistry.registerAll(new MemoryUsageGaugeSet());
    metricRegistry.registerAll(new GarbageCollectorMetricSet());

    String newRelicMetricKey = config.stringValue("NEW_RELIC_METRIC_KEY", null);

    if (newRelicMetricKey != null) {
      String newRelicAppName = config.stringValue("NEW_RELIC_APP_NAME", null);
      String newRelicMetricHost = config.stringValue("NEW_RELIC_METRIC_HOST", null);
      String newRelicMetricPath = config.stringValue("NEW_RELIC_METRIC_PATH", null);
      String rawNewRelicPeriod = config.stringValue("NEW_RELIC_METRIC_PERIOD", null);

      int newRelicPeriod;
      try {
        newRelicPeriod = Integer.parseInt(rawNewRelicPeriod);
      } catch (NumberFormatException ex) {
        newRelicPeriod = 15;
      }

      String hostname;
      try {
        hostname = InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        hostname = "unknown";
      }

      SenderConfiguration configuration =
          SenderConfiguration.builder(newRelicMetricHost, newRelicMetricPath)
              .httpPoster(new OkHttpPoster())
              .apiKey(newRelicMetricKey)
              .build();

      MetricBatchSender metricBatchSender = MetricBatchSender.create(configuration);

      Attributes commonAttributes =
          new Attributes().put("host", hostname).put("appName", newRelicAppName);

      NewRelicReporter newRelicReporter =
          NewRelicReporter.build(metricRegistry, metricBatchSender)
              .commonAttributes(commonAttributes)
              .build();

      newRelicReporter.start(newRelicPeriod, TimeUnit.SECONDS);
    }

    return metricRegistry;
  }

  /**
   * Creates the {@link HealthCheckRegistry} for the application, which collects any/all health
   * checks that it provides.
   *
   * @return the {@link HealthCheckRegistry}
   */
  @Bean
  public HealthCheckRegistry healthCheckRegistry() {
    HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
    return healthCheckRegistry;
  }

  /**
   * This bean provides an {@link FdaDrugCodeDisplayLookup} for use in the transformers to look up
   * drug codes.
   *
   * @param includeFakeDrugCode if true, the {@link FdaDrugCodeDisplayLookup} will include a fake
   *     drug code for testing purposes.
   * @return the {@link FdaDrugCodeDisplayLookup} for the application.
   */
  @Bean
  public FdaDrugCodeDisplayLookup fdaDrugCodeDisplayLookup(
      @Value("${" + PROP_INCLUDE_FAKE_DRUG_CODE + ":false}") Boolean includeFakeDrugCode) {
    if (includeFakeDrugCode) {
      return FdaDrugCodeDisplayLookup.createDrugCodeLookupForTesting();
    } else {
      return FdaDrugCodeDisplayLookup.createDrugCodeLookupForProduction();
    }
  }

  /**
   * This bean provides an {@link NPIOrgLookup} for use in the transformers to look up org name.
   *
   * @param includeFakeOrgName if true, the {@link NPIOrgLookup} will include a fake org name for
   *     testing purposes.
   * @return the {@link NPIOrgLookup} for the application.
   * @throws IOException if there is an error accessing the resource
   */
  @Bean
  public NPIOrgLookup npiOrgLookup(
      @Value("${" + PROP_INCLUDE_FAKE_ORG_NAME + ":false}") Boolean includeFakeOrgName)
      throws IOException {
    if (includeFakeOrgName) {
      return new NPIOrgLookup();
    } else {
      return NPIOrgLookup.createNpiOrgLookup();
    }
  }

  /**
   * Build a {@link ConfigLoader} that accounts for all possible sources of configuration
   * information. The provided function is used to look up environment variables so that these can
   * be simulated in tests without having to fork a process.
   *
   * <p>{@see LayeredConfiguration#createConfigLoader} for possible sources of configuration
   * variables.
   *
   * @param getenv function used to access environment variables (provided explicitly for testing)
   * @return appropriately configured {@link ConfigLoader}
   */
  public static ConfigLoader createConfigLoader(Function<String, String> getenv) {
    return LayeredConfiguration.createConfigLoader(Map.of(), getenv);
  }

  /**
   * This bean provides an {@link ExecutorService} to enable EOB claim transformers to run in
   * parallel (threads).
   *
   * <p>Using a fixed thread pool as ExplanationOfBenefit processing is broken into thread tasks,
   * one per claim type; threads run concurrently, with each running in generally less than a
   * second. So, while a fixed thread pool might represent wasted resources (memory allocated per
   * thread at time of thread pool creation), retrieving EOB claims represents a high-volume service
   * that will make good use of allocated threads.
   *
   * @param threadCount system parameter for the number of threads in the fixed thread pool.
   * @return {@link ExecutorService} for the application.
   */
  @Bean
  public ExecutorService executorService(
      @Value("${bfdServer.executorService.threads:80}") Integer threadCount) {
    return Executors.newFixedThreadPool(
        threadCount,
        r -> {
          Thread t = new Thread(r);
          t.setName("eob_claims");
          return t;
        });
  }
}
