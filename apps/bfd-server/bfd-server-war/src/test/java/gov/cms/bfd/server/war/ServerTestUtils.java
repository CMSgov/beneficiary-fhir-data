package gov.cms.bfd.server.war;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileRecords;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.ccw.rif.load.CcwRifLoadTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.ccw.rif.load.RifLoader;
import gov.cms.bfd.pipeline.sharedutils.PipelineTestUtils;
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
import javax.persistence.EntityTransaction;
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

  /** The singleton {@link ServerTestUtils} instance to use everywhere. */
  private static ServerTestUtils SINGLETON;

  private final String serverBaseUrl;

  /**
   * Constructs a new {@link ServerTestUtils} instance. Marked <code>private</code>; use {@link
   * #get()}, instead.
   */
  private ServerTestUtils() {
    this.serverBaseUrl = initServerBaseUrl();
  }

  /** @return the singleton {@link ServerTestUtils} instance to use everywhere */
  public static synchronized ServerTestUtils get() {
    /*
     * Why are we using a singleton and caching all of these fields? Because creating some of the
     * fields stored in the PipelineApplicationState is EXPENSIVE (it maintains a DB connection
     * pool), so we don't want to have to re-create it for every test.
     */

    if (SINGLETON == null) {
      SINGLETON = new ServerTestUtils();
    }

    return SINGLETON;
  }

  /** @return the value to use for {@link #getServerBaseUrl()} */
  private static String initServerBaseUrl() {
    Properties testServerPorts = initTestServerPortsProperties();
    int httpsPort = Integer.parseInt(testServerPorts.getProperty("server.port.https"));
    String serverBaseUrl = String.format("https://localhost:%d", httpsPort);
    return serverBaseUrl;
  }

  /**
   * @return the {@link Properties} from the <code>server-ports.properties</code> that should have
   *     been written out by the integration tests' <code>server-start.sh</code> script
   */
  private static Properties initTestServerPortsProperties() {
    /*
     * The working directory for tests will either be the module directory or their parent
     * directory. With that knowledge, we're searching for the target/server-work directory.
     */
    Path serverRunDir = Paths.get("target", "server-work");
    if (!Files.isDirectory(serverRunDir))
      serverRunDir = Paths.get("bfd-server-war", "target", "server-work");
    if (!Files.isDirectory(serverRunDir))
      throw new IllegalStateException(
          "Unable to find server-work directory from current working directory: "
              + Paths.get(".").toAbsolutePath());

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
   * @return a new FHIR {@link IGenericClient} for use, configured to use the {@link
   *     ClientSslIdentity#TRUSTED} login
   */
  public IGenericClient createFhirClient() {
    return createFhirClient(Optional.of(ClientSslIdentity.TRUSTED));
  }

  /**
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the FHIR server
   * @return a new FHIR {@link IGenericClient} for use
   */
  public IGenericClient createFhirClient(Optional<ClientSslIdentity> clientSslIdentity) {
    return createFhirClient("v1", clientSslIdentity);
  }

  /**
   * @return a new FHIR {@link IGenericClient} for use, configured to use the {@link
   *     ClientSslIdentity#TRUSTED} login for FIHR v2 server
   */
  public IGenericClient createFhirClientV2() {
    return createFhirClientV2(Optional.of(ClientSslIdentity.TRUSTED));
  }

  /**
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the FV2 HIR server
   * @return a new FHIR {@link IGenericClient} for use
   */
  public IGenericClient createFhirClientV2(Optional<ClientSslIdentity> clientSslIdentity) {
    return createFhirClient("v2", clientSslIdentity, FhirContext.forR4());
  }

  /**
   * @param versionId the {@link v1 or v2 identifier to use as a part of the URL for the FHIR server
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the FHIR server
   * @return a new FHIR {@link IGenericClient} for use
   */
  private IGenericClient createFhirClient(
      String versionId, Optional<ClientSslIdentity> clientSslIdentity) {
    // Default behavor before was to spawn a DSTU3 context, so retain that
    return createFhirClient(versionId, clientSslIdentity, FhirContext.forDstu3());
  }

  /**
   * @param versionId the {@link v1 or v2 identifier to use as a part of the URL for the FHIR server
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the FHIR server
   * @return a new FHIR {@link IGenericClient} for use
   */
  private IGenericClient createFhirClient(
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
  public SSLContext createSslContext(Optional<ClientSslIdentity> clientSslIdentity) {
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
  public String getServerBaseUrl() {
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
  public List<Object> loadData(List<StaticRifResource> sampleResources) {
    LoadAppOptions loadOptions = CcwRifLoadTestUtils.getLoadOptions();
    RifFilesEvent rifFilesEvent =
        new RifFilesEvent(
            Instant.now(),
            sampleResources.stream().map(r -> r.toRifFile()).collect(Collectors.toList()));

    // Create the processors that will handle each stage of the pipeline.
    RifFilesProcessor processor = new RifFilesProcessor();

    // Link up the pipeline and run it.
    RifLoader loader =
        new RifLoader(loadOptions, PipelineTestUtils.get().getPipelineApplicationState());
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

  /**
   * A wrapper for the entity manager logic and action. The executor is called within a transaction
   *
   * @param executor to call with an entity manager.
   */
  public void doTransaction(Consumer<EntityManager> executor) {
    EntityManager em = null;
    EntityTransaction tx = null;
    try {
      em =
          PipelineTestUtils.get()
              .getPipelineApplicationState()
              .getEntityManagerFactory()
              .createEntityManager();
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
  public IGenericClient createFhirClientWithHeaders(RequestHeaders requestHeader) {
    IGenericClient fhirClient = createFhirClient();
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
  public IGenericClient createFhirClientWithHeadersV2(RequestHeaders requestHeader) {
    IGenericClient fhirClient = createFhirClientV2();
    if (requestHeader != null) {
      ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
      extraParamsInterceptor.setHeaders(requestHeader);
      fhirClient.registerInterceptor(extraParamsInterceptor);
    }
    return fhirClient;
  }
}
