package gov.cms.bfd.server.war;

import ca.uhn.fhir.rest.server.IResourceProvider;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.server.war.r4.providers.R4CoverageResourceProvider;
import gov.cms.bfd.server.war.r4.providers.R4ExplanationOfBenefitResourceProvider;
import gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider;
import gov.cms.bfd.server.war.r4.providers.pac.R4ClaimResourceProvider;
import gov.cms.bfd.server.war.r4.providers.pac.R4ClaimResponseResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.CoverageResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider;
import gov.cms.bfd.server.war.stu3.providers.PatientResourceProvider;
import gov.cms.bfd.sharedutils.config.AwsClientConfig;
import gov.cms.bfd.sharedutils.config.BaseConfiguration;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.config.ConfigLoaderSource;
import gov.cms.bfd.sharedutils.config.LayeredConfiguration;
import gov.cms.bfd.sharedutils.database.DataSourceFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceUnit;
import jakarta.servlet.ServletContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.hibernate.tool.schema.Action;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.support.PersistenceAnnotationBeanPostProcessor;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/** The main Spring {@link Configuration} for the Blue Button API Backend application. */
@Configuration
@ComponentScan(basePackageClasses = {ServerInitializer.class})
@EnableScheduling
@EnableRetry(proxyTargetClass = true)
public class SpringConfiguration extends BaseConfiguration {
  /**
   * The {@link String } property that is used to hold drug code file name that is used for
   * integration testing.
   */
  public static final String PROP_DRUG_CODE_FILE_NAME = "bfdServer.drug.code.file.name";

  /**
   * The {@link String } property that represents org file name that is used for integration
   * testing.
   */
  public static final String PROP_ORG_FILE_NAME = "bfdServer.org.file.name";

  /**
   * The {@link String} property that lists the client certificates that are allowed to see SAMHSA
   * data.
   */
  public static final String SSM_PATH_SAMHSA_ALLOWED_CERTIFICATE_ALIASES_JSON =
      "samhsa_allowed_certificate_aliases_json";

  /**
   * The {@link String } Boolean property that is used to enable the partially adjudicated claims
   * data resources.
   */
  public static final String SSM_PATH_PAC_ENABLED = "pac/enabled";

  /**
   * The {@link String } Boolean property that is used to enable using old MBI hash values for
   * queries. Only needed for a limited time after rotating hashing parameters to allow clients to
   * query using old and new hashed values during a compatibility window.
   */
  public static final String PROP_PAC_OLD_MBI_HASH_ENABLED = "bfdServer.pac.oldMbiHash.enabled";

  /**
   * Comma separated list of claim source types to support when querying for partially adjudicated
   * claims data.
   */
  public static final String SSM_PATH_PAC_CLAIM_SOURCE_TYPES = "pac/claim_source_types";

  /** SSM Path for the server trust store. */
  private static final String SSM_PATH_TRUSTSTORE = "paths/files/truststore";

  /** The {@link String } Boolean property that is used to enable the C4DIC profile. */
  public static final String SSM_PATH_C4DIC_ENABLED = "c4dic/enabled";

  /** The {@link String } Boolean property that is used to enable the samhsa 2.0 profile. */
  public static final String SSM_PATH_SAMHSA_V2_ENABLED = "samhsa_v2/enabled";

  /** The {@link String } Boolean property that is used to enable the samhsa 2.0 profile. */
  public static final String SSM_PATH_SAMHSA_V2_SHADOW = "samhsa_v2/shadow";

  /** Maximum number of threads to use for executing EOB claim transformers in parallel. */
  public static final String PROP_EXECUTOR_SERVICE_THREADS = "bfdServer.executorService.threads";

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
        : createConfigLoader(ConfigLoaderSource.fromEnv());
  }

  /**
   * Creates an {@link AwsClientConfig} used for all AWS services.
   *
   * @param configLoader used to look up configuration values
   * @return the config
   */
  @Bean
  public AwsClientConfig awsClientConfig(ConfigLoader configLoader) {
    return loadAwsClientConfig(configLoader);
  }

  /**
   * Creates a {@link KeyStore} from the trust store path.
   *
   * @param configLoader config loader
   * @return the {@link KeyStore} object
   * @throws KeyStoreException if the key store can't be created
   * @throws IOException if there's a problem reading the file
   * @throws CertificateException if the certificates can't be loaded
   * @throws NoSuchAlgorithmException if the key store algorithm can't be found
   */
  @Bean(name = "serverTrustStore")
  public KeyStore serverTrustStore(ConfigLoader configLoader)
      throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
    String truststore = configLoader.readableFile(SSM_PATH_TRUSTSTORE).toString();
    KeyStore keyStore = KeyStore.getInstance("PKCS12");
    keyStore.load(new FileInputStream(truststore), "changeit".toCharArray());
    return keyStore;
  }

  /**
   * Creates a factory to create {@link DataSource}s.
   *
   * @param configLoader used to look up configuration values
   * @param awsClientConfig common AWS settings
   * @return the factory
   */
  @Bean
  public DataSourceFactory dataSourceFactory(
      ConfigLoader configLoader, AwsClientConfig awsClientConfig) {
    final var dbOptions = loadDatabaseOptions(configLoader);
    return createDataSourceFactory(dbOptions, awsClientConfig);
  }

  /**
   * Creates the application's database data source using a factory.
   *
   * @param dataSourceFactory factory used to create {@link DataSource}s
   * @param metricRegistry the {@link MetricRegistry} for the application
   * @return the {@link DataSource} that provides the application's database connection
   */
  @Bean(destroyMethod = "close")
  public DataSource dataSource(DataSourceFactory dataSourceFactory, MetricRegistry metricRegistry) {
    DataSource dataSource = dataSourceFactory.createDataSource(metricRegistry);

    // Wrap the pooled DataSource in a proxy that records performance data.
    return ProxyDataSourceBuilder.create(dataSource)
        .name("BFD-Data")
        .listener(new QueryLoggingListener())
        .proxyResultSet()
        .build();
  }

  /**
   * Creates the transaction manager for the application from a factory.
   *
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
   * Creates the entity manager factory from a datasource.
   *
   * @param dataSource the {@link DataSource} for the application
   * @return the {@link LocalContainerEntityManagerFactoryBean}, which ensures that other beans can
   *     safely request injection of {@link EntityManager} instances
   */
  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
    LocalContainerEntityManagerFactoryBean containerEmfBean =
        new LocalContainerEntityManagerFactoryBean();
    containerEmfBean.setDataSource(dataSource);
    containerEmfBean.setPackagesToScan("gov.cms.bfd.model");
    containerEmfBean.setPersistenceProvider(new HibernatePersistenceProvider());
    containerEmfBean.setJpaProperties(jpaProperties());
    containerEmfBean.afterPropertiesSet();
    return containerEmfBean;
  }

  /**
   * Creates the {@link Properties} to configure Hibernate and JPA with.
   *
   * @return the jpa properties
   */
  private Properties jpaProperties() {
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
   * Creates a Spring {@link BeanPostProcessor} that enables the use of the JPA {@link
   * PersistenceUnit} and {@link PersistenceContext} annotations for injection of {@link
   * EntityManagerFactory} and {@link EntityManager} instances, respectively, into beans.
   *
   * @return the post processor
   */
  @Bean
  public PersistenceAnnotationBeanPostProcessor persistenceAnnotationProcessor() {
    return new PersistenceAnnotationBeanPostProcessor();
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
      @Value("${" + PROP_PAC_OLD_MBI_HASH_ENABLED + ":false}") Boolean enabled) {
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
      @Value("${" + SSM_PATH_PAC_ENABLED + ":false}") Boolean pacEnabled) {

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
   * Build a {@link ConfigLoader} that accounts for all possible sources of configuration
   * information. The provided {@link ConfigLoaderSource} is used to look up environment variables
   * so that these can be simulated in tests without having to fork a process.
   *
   * <p>{@see LayeredConfiguration#createConfigLoader} for possible sources of configuration
   * variables.
   *
   * @param getenv {@link ConfigLoaderSource} used to access environment variables (provided
   *     explicitly for testing)
   * @return appropriately configured {@link ConfigLoader}
   */
  public static ConfigLoader createConfigLoader(ConfigLoaderSource getenv) {
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
      @Value("${" + PROP_EXECUTOR_SERVICE_THREADS + ":80}") Integer threadCount) {
    return Executors.newFixedThreadPool(
        threadCount,
        r -> {
          Thread t = new Thread(r);
          t.setName("eob_claims");
          return t;
        });
  }
}
