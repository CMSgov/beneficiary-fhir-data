package gov.cms.bfd.server.war;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.LoadedFile;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileRecords;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.schema.DatabaseTestHelper;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.ccw.rif.load.RifLoader;
import gov.cms.bfd.pipeline.ccw.rif.load.RifLoaderTestUtils;
import gov.cms.bfd.pipeline.sharedutils.DatabaseUtils;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import gov.cms.bfd.server.war.stu3.providers.ExtraParamsInterceptor;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.management.MBeanServer;
import javax.net.ssl.SSLContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.sql.DataSource;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Contains test utilities. */
public final class ServerTestUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerTestUtils.class);

  /**
   * @return a new FHIR {@link IGenericClient} for use, configured to use the {@link
   *     ClientSslIdentity#TRUSTED} login
   */
  public static IGenericClient createFhirClient() {
    return createFhirClient(Optional.of(ClientSslIdentity.TRUSTED));
  }

  /**
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the FHIR server
   * @return a new FHIR {@link IGenericClient} for use
   */
  public static IGenericClient createFhirClient(Optional<ClientSslIdentity> clientSslIdentity) {
    return createFhirClient("v1", clientSslIdentity);
  }

  /**
   * @return a new FHIR {@link IGenericClient} for use, configured to use the {@link
   *     ClientSslIdentity#TRUSTED} login for FIHR v2 server
   */
  public static IGenericClient createFhirClientV2() {
    return createFhirClientV2(Optional.of(ClientSslIdentity.TRUSTED));
  }

  /**
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the FV2 HIR server
   * @return a new FHIR {@link IGenericClient} for use
   */
  public static IGenericClient createFhirClientV2(Optional<ClientSslIdentity> clientSslIdentity) {
    return createFhirClient("v2", clientSslIdentity, FhirContext.forR4());
  }

  /**
   * @param versionId the {@link v1 or v2 identifier to use as a part of the URL for the FHIR server
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the FHIR server
   * @return a new FHIR {@link IGenericClient} for use
   */
  private static IGenericClient createFhirClient(
      String versionId, Optional<ClientSslIdentity> clientSslIdentity) {
    // Default behavor before was to spawn a DSTU3 context, so retain that
    return createFhirClient(versionId, clientSslIdentity, FhirContext.forDstu3());
  }

  /**
   * @param versionId the {@link v1 or v2 identifier to use as a part of the URL for the FHIR server
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the FHIR server
   * @return a new FHIR {@link IGenericClient} for use
   */
  private static IGenericClient createFhirClient(
      String versionId, Optional<ClientSslIdentity> clientSslIdentity, FhirContext ctx) {
    // Figure out where the test server is running.
    String fhirBaseUrl = String.format("%s/%s/fhir", getServerBaseUrl(), versionId);

    /*
     * We need to override the FHIR client's SSLContext. Unfortunately, that
     * requires overriding the entire HttpClient that it uses. Otherwise,
     * the settings used here mirror those that the default FHIR HttpClient
     * would use.
     */
    SSLContext sslContext = createSslContext(clientSslIdentity);

    /*
     * The default timeout is 10s, which was failing for batches of 100. A
     * 300s timeout was failing for batches of 100 once Part B claims were
     * mostly mapped, so batches were cut to 10, which ran at 12s or so,
     * each.
     */
    ctx.getRestfulClientFactory().setSocketTimeout((int) TimeUnit.MINUTES.toMillis(5));
    PoolingHttpClientConnectionManager connectionManager =
        new PoolingHttpClientConnectionManager(
            RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", new SSLConnectionSocketFactory(sslContext))
                .build(),
            null,
            null,
            null,
            5000,
            TimeUnit.MILLISECONDS);
    @SuppressWarnings("deprecation")
    RequestConfig defaultRequestConfig =
        RequestConfig.custom()
            .setSocketTimeout(ctx.getRestfulClientFactory().getSocketTimeout())
            .setConnectTimeout(ctx.getRestfulClientFactory().getConnectTimeout())
            .setConnectionRequestTimeout(
                ctx.getRestfulClientFactory().getConnectionRequestTimeout())
            .setStaleConnectionCheckEnabled(true)
            .build();
    HttpClient httpClient =
        HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(defaultRequestConfig)
            .disableCookieManagement()
            .build();
    ctx.getRestfulClientFactory().setHttpClient(httpClient);

    IGenericClient client = ctx.newRestfulGenericClient(fhirBaseUrl);

    /*
     * The FHIR client logging (for tests) can be managed via the
     * `src/test/resources/logback-test.xml` file.
     */
    LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
    loggingInterceptor.setLogRequestSummary(LOGGER.isDebugEnabled());
    loggingInterceptor.setLogResponseSummary(LOGGER.isDebugEnabled());
    loggingInterceptor.setLogRequestHeaders(LOGGER.isTraceEnabled());
    loggingInterceptor.setLogResponseHeaders(LOGGER.isTraceEnabled());
    loggingInterceptor.setLogRequestBody(LOGGER.isTraceEnabled());
    loggingInterceptor.setLogResponseBody(LOGGER.isTraceEnabled());
    client.registerInterceptor(loggingInterceptor);

    return client;
  }

  /**
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the server
   * @return a new {@link SSLContext} for HTTP clients connecting to the server to use
   */
  public static SSLContext createSslContext(Optional<ClientSslIdentity> clientSslIdentity) {
    SSLContext sslContext;
    try {
      SSLContextBuilder sslContextBuilder = SSLContexts.custom();

      // If a client key is desired, configure the key store with it.
      if (clientSslIdentity.isPresent())
        sslContextBuilder.loadKeyMaterial(
            clientSslIdentity.get().getKeyStore(),
            clientSslIdentity.get().getStorePassword(),
            clientSslIdentity.get().getKeyPass());

      // Configure the trust store.
      sslContextBuilder.loadTrustMaterial(
          getClientTrustStorePath().toFile(), "changeit".toCharArray());

      sslContext = sslContextBuilder.build();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (KeyManagementException
        | UnrecoverableKeyException
        | NoSuchAlgorithmException
        | KeyStoreException
        | CertificateException e) {
      throw new IllegalStateException(e);
    }
    return sslContext;
  }

  /** @return the base URL for the server (not for the FHIR servlet, but just the server itself) */
  public static String getServerBaseUrl() {
    Properties testServerPorts = readTestServerPortsProperties();
    int httpsPort = Integer.parseInt(testServerPorts.getProperty("server.port.https"));
    String serverBaseUrl = String.format("https://localhost:%d", httpsPort);
    return serverBaseUrl;
  }

  /** @return the local {@link Path} that development/test key and trust stores can be found in */
  static Path getSslStoresDirectory() {
    /*
     * The working directory for tests will either be the module directory
     * or their parent directory. With that knowledge, we're searching for
     * the ssl-stores directory.
     */
    Path sslStoresDir = Paths.get("..", "dev", "ssl-stores");
    if (!Files.isDirectory(sslStoresDir)) sslStoresDir = Paths.get("dev", "ssl-stores");
    if (!Files.isDirectory(sslStoresDir)) throw new IllegalStateException();
    return sslStoresDir;
  }

  /** @return the local {@link Path} to the trust store that FHIR clients should use */
  private static Path getClientTrustStorePath() {
    Path trustStorePath = getSslStoresDirectory().resolve("client-truststore.jks");
    return trustStorePath;
  }

  /**
   * @param sampleResources the sample RIF resources to parse
   * @return the {@link List} of RIF records that were parsed (e.g. {@link Beneficiary}s, etc.)
   */
  public static List<Object> parseData(List<StaticRifResource> sampleResources) {
    RifFilesEvent rifFilesEvent =
        new RifFilesEvent(
            Instant.now(),
            sampleResources.stream().map(r -> r.toRifFile()).collect(Collectors.toList()));
    RifFilesProcessor processor = new RifFilesProcessor();
    List<Object> recordsParsed = new ArrayList<>();
    for (RifFileEvent rifFileEvent : rifFilesEvent.getFileEvents()) {
      RifFileRecords rifFileRecords = processor.produceRecords(rifFileEvent);
      rifFileRecords.getRecords().map(r -> r.getRecord()).forEach(r -> recordsParsed.add(r));
    }

    return recordsParsed;
  }

  /**
   * @param sampleResources the sample RIF resources to load
   * @return the {@link List} of RIF records that were loaded (e.g. {@link Beneficiary}s, etc.)
   */
  public static List<Object> loadData(List<StaticRifResource> sampleResources) {
    DataSource dataSource = createDataSource();
    LoadAppOptions loadOptions = createRifLoaderOptions();
    RifFilesEvent rifFilesEvent =
        new RifFilesEvent(
            Instant.now(),
            sampleResources.stream().map(r -> r.toRifFile()).collect(Collectors.toList()));

    // Create the processors that will handle each stage of the pipeline.
    MetricRegistry loadAppMetrics = new MetricRegistry();
    RifFilesProcessor processor = new RifFilesProcessor();

    try (RifLoader loader = new RifLoader(loadAppMetrics, loadOptions, dataSource); ) {
      // Link up the pipeline and run it.
      LOGGER.info("Loading RIF records...");
      List<Object> recordsLoaded = new ArrayList<>();
      for (RifFileEvent rifFileEvent : rifFilesEvent.getFileEvents()) {
        RifFileRecords rifFileRecords = processor.produceRecords(rifFileEvent);
        loader.process(
            rifFileRecords,
            error -> {
              LOGGER.warn("Record(s) failed to load.", error);
            },
            result -> {
              recordsLoaded.add(result.getRifRecordEvent().getRecord());
            });
      }
      LOGGER.info("Loaded RIF records: '{}'.", recordsLoaded.size());
      return recordsLoaded;
    }
  }

  /**
   * A wrapper for the entity manager logic and action. The executor is called within a transaction
   *
   * @param executor to call with an entity manager.
   */
  public static void doTransaction(Consumer<EntityManager> executor) {
    final EntityManagerFactory entityManagerFactory = createEntityManagerFactory();
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em = entityManagerFactory.createEntityManager();
      tx = em.getTransaction();
      tx.begin();
      executor.accept(em);
      tx.commit();
    } finally {
      if (tx != null && tx.isActive()) {
        tx.rollback();
        LOGGER.info("Rolling back a transaction");
      }
      if (em != null && em.isOpen()) em.close();
      if (entityManagerFactory != null) entityManagerFactory.close();
    }
  }

  /** Cleans the test DB by running a bunch of <cod. */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static void cleanDatabaseServer() {
    EntityManagerFactory entityManagerFactory = null;
    EntityManager entityManager = null;
    EntityTransaction transaction = null;
    try {
      entityManagerFactory = createEntityManagerFactory();
      entityManager = entityManagerFactory.createEntityManager();

      // Determine the entity types to delete, and the order to do so in.
      Comparator<Class<?>> entityDeletionSorter =
          (t1, t2) -> {
            if (t1.equals(Beneficiary.class)) return 1;
            if (t2.equals(Beneficiary.class)) return -1;
            if (t1.getSimpleName().endsWith("Line")) return -1;
            if (t2.getSimpleName().endsWith("Line")) return 1;
            if (t1.equals(LoadedFile.class)) return 1;
            if (t2.equals(LoadedFile.class)) return -1;
            return 0;
          };
      List<Class<?>> entityTypesInDeletionOrder =
          entityManagerFactory.getMetamodel().getEntities().stream()
              .map(t -> t.getJavaType())
              .sorted(entityDeletionSorter)
              .collect(Collectors.toList());

      LOGGER.info("Deleting all resources...");
      transaction = entityManager.getTransaction();
      transaction.begin();
      for (Class<?> entityClass : entityTypesInDeletionOrder) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaDelete query = builder.createCriteriaDelete(entityClass);
        query.from(entityClass);
        entityManager.createQuery(query).executeUpdate();
      }

      /*
       * To be complete, we should also be resetting our sequences here. However, there isn't a
       * simple way to do that without hardcoding the sequence names, so I'm going to lean into my
       * laziness and not implement it: it's unlikely to cause issues with our tests.
       */

      transaction.commit();
      LOGGER.info("Deleted all resources.");
    } finally {
      if (transaction != null && transaction.isActive()) transaction.rollback();
      if (entityManager != null) entityManager.close();
      if (entityManagerFactory != null) entityManagerFactory.close();
    }
  }

  /** @return a {@link DataSource} for the test DB */
  public static final DataSource createDataSource() {
    String jdbcUrl, jdbcUsername, jdbcPassword;

    /*
     * In our tests, we either get the DB connection details from the system properties (for a
     * "normal" DB that was created outside of the tests), or from the test Properties file that was
     * created by the WAR when it launched (for HSQL DBs).
     */

    Properties testDbProps = readTestDatabaseProperties();
    if (testDbProps != null) {
      jdbcUrl = testDbProps.getProperty(SpringConfiguration.PROP_DB_URL);
      jdbcUsername = testDbProps.getProperty(SpringConfiguration.PROP_DB_USERNAME, null);
      jdbcPassword = testDbProps.getProperty(SpringConfiguration.PROP_DB_PASSWORD, null);
    } else {
      jdbcUrl = System.getProperty("its.db.url", null);
      jdbcUsername = System.getProperty("its.db.username", null);
      jdbcPassword = System.getProperty("its.db.password", null);
    }

    if (jdbcUsername != null && jdbcUsername.isEmpty()) jdbcUsername = null;
    if (jdbcPassword != null && jdbcPassword.isEmpty()) jdbcPassword = null;

    DataSource dataSource = DatabaseTestHelper.getTestDatabase(jdbcUrl, jdbcUsername, jdbcPassword);

    return dataSource;
  }

  /** @return an {@link EntityManagerFactory} for the test DB */
  private static EntityManagerFactory createEntityManagerFactory() {
    DataSource dataSource = createDataSource();
    return DatabaseUtils.createEntityManagerFactory(dataSource);
  }

  /** @return the {@link LoadAppOptions} to use with {@link RifLoader} in integration tests */
  public static LoadAppOptions createRifLoaderOptions() {
    return new LoadAppOptions(
        new IdHasher.Config(
            RifLoaderTestUtils.HICN_HASH_ITERATIONS, RifLoaderTestUtils.HICN_HASH_PEPPER),
        LoadAppOptions.DEFAULT_LOADER_THREADS,
        RifLoaderTestUtils.IDEMPOTENCY_REQUIRED);
  }

  /**
   * @return the {@link Properties} from the <code>server-ports.properties</code> that should have
   *     been written out by the integration tests' <code>server-start.sh</code> script
   */
  private static Properties readTestServerPortsProperties() {
    /*
     * The working directory for tests will either be the module directory
     * or their parent directory. With that knowledge, we're searching for
     * the target/server-work directory.
     */
    Path serverRunDir = Paths.get("target", "server-work");
    if (!Files.isDirectory(serverRunDir))
      serverRunDir = Paths.get("bfd-server-war", "target", "server-work");
    if (!Files.isDirectory(serverRunDir)) throw new IllegalStateException();

    Properties serverPortsProps = new Properties();
    try {
      serverPortsProps.load(
          new FileReader(serverRunDir.resolve("server-ports.properties").toFile()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return serverPortsProps;
  }

  /**
   * @return the {@link Properties} file created by {@link
   *     gov.cms.bfd.server.war.SpringConfiguration#findTestDatabaseProperties()}, or <code>null
   *     </code> if it's not present (indicating that just a regular DB connection is being used)
   */
  private static Properties readTestDatabaseProperties() {
    Path testDatabasePropertiesPath = SpringConfiguration.findTestDatabaseProperties();
    if (!Files.isRegularFile(testDatabasePropertiesPath)) return null;

    try {
      Properties testDbProps = new Properties();
      testDbProps.load(new FileReader(testDatabasePropertiesPath.toFile()));
      return testDbProps;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Starts a background thread that periodically generates a heap dump at <code>./heap-dumps/
   * </code>. Useful when trying to debug memory pressure issues in our test code.
   *
   * @param period how often to generate a heap dump
   */
  @SuppressWarnings("restriction")
  public static void startHeapDumpCollector(Duration period) {
    Runnable collector =
        () -> {
          MBeanServer server = ManagementFactory.getPlatformMBeanServer();
          try {
            Path heapDumpsDir = Paths.get(".", "heap-dumps");
            Files.createDirectories(heapDumpsDir);
            String heapDumpFileName =
                String.format(
                        "bfd-server-tests-%s.hprof",
                        DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
                    .replaceAll(":", "-");
            Path heapDump = heapDumpsDir.resolve(heapDumpFileName);
            LOGGER.info("Generating heap dump at: {}", heapDump.toAbsolutePath().toString());

            com.sun.management.HotSpotDiagnosticMXBean mxBean =
                java.lang.management.ManagementFactory.newPlatformMXBeanProxy(
                    server,
                    "com.sun.management:type=HotSpotDiagnostic",
                    com.sun.management.HotSpotDiagnosticMXBean.class);
            mxBean.dumpHeap(heapDump.toString(), true);
            LOGGER.info("Generated heap dump at: {}", heapDump.toAbsolutePath().toString());
          } catch (IOException e) {
            LOGGER.warn("Unable to generate heap dump.", e);
            throw new UncheckedIOException(e);
          }
        };
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(
        collector, period.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
  }

  /**
   * helper
   *
   * @return the client with extra params registered
   */
  public static IGenericClient createFhirClientWithHeaders(RequestHeaders requestHeader) {
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    if (requestHeader != null) {
      ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
      extraParamsInterceptor.setHeaders(requestHeader);
      fhirClient.registerInterceptor(extraParamsInterceptor);
    }
    return fhirClient;
  }

  /**
   * helper
   *
   * @return the client with extra params registered
   */
  public static IGenericClient createFhirClientWithHeadersV2(RequestHeaders requestHeader) {
    IGenericClient fhirClient = ServerTestUtils.createFhirClientV2();
    if (requestHeader != null) {
      ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
      extraParamsInterceptor.setHeaders(requestHeader);
      fhirClient.registerInterceptor(extraParamsInterceptor);
    }
    return fhirClient;
  }
}
