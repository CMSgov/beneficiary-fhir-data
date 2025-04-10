package gov.cms.bfd.server.war;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.model.rif.LoadedBatch;
import gov.cms.bfd.model.rif.LoadedFile;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.RifRecordBase;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
import gov.cms.bfd.model.rif.entities.BeneficiaryMonthly;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaimLine;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HHAClaimLine;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaimLine;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaimLine;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaimLine;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.entities.SNFClaimLine;
import gov.cms.bfd.model.rif.npi_fda.FDAData;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.pipeline.PipelineTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFileRecords;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFilesProcessor;
import gov.cms.bfd.pipeline.ccw.rif.load.CcwRifLoadTestUtils;
import gov.cms.bfd.pipeline.ccw.rif.load.LoadAppOptions;
import gov.cms.bfd.pipeline.ccw.rif.load.RifLoader;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.sharedutils.database.DatabaseUtils;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Table;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.management.MBeanServer;
import javax.net.ssl.SSLContext;
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
import org.testcontainers.shaded.org.awaitility.Awaitility;

/** Contains test utilities. */
public final class ServerTestUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerTestUtils.class);

  /** The list of base mdc keys that should be on every write to access.json. */
  private static final List<String> DEFAULT_MDC_KEYS =
      Arrays.asList(
          BfdMDC.BENE_ID,
          BfdMDC.HAPI_RESPONSE_TIMESTAMP_MILLI,
          BfdMDC.HAPI_POST_PROCESS_TIMESTAMP_MILLI,
          BfdMDC.HAPI_PRE_HANDLE_TIMESTAMP_MILLI,
          BfdMDC.HAPI_PRE_PROCESS_TIMESTAMP_MILLI,
          BfdMDC.HAPI_PROCESSING_COMPLETED_TIMESTAMP_MILLI,
          BfdMDC.HAPI_PROCESSING_COMPLETED_NORM_TIMESTAMP_MILLI,
          BfdMDC.HTTP_ACCESS_REQUEST_CLIENTSSL_DN,
          BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ACCEPT,
          BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ACCEPT_ENCODING,
          BfdMDC.HTTP_ACCESS_REQUEST_HEADER_CONN_ENCODING,
          BfdMDC.HTTP_ACCESS_REQUEST_HEADER_HOST_ENCODING,
          BfdMDC.HTTP_ACCESS_REQUEST_HEADER_USER_AGENT,
          BfdMDC.HTTP_ACCESS_REQUEST_HTTP_METHOD,
          BfdMDC.HTTP_ACCESS_REQUEST_OPERATION,
          BfdMDC.HTTP_ACCESS_REQUEST_QUERY_STR,
          BfdMDC.HTTP_ACCESS_REQUEST_TYPE,
          BfdMDC.HTTP_ACCESS_REQUEST_URI,
          BfdMDC.HTTP_ACCESS_REQUEST_URL,
          BfdMDC.HTTP_ACCESS_REQUEST_HEADER_CONTENT_TYPE,
          BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_MILLISECONDS,
          BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_ENCODING,
          BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_CONTENT_TYPE,
          BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_DATE,
          BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_LAST_MODIFIED,
          BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_POWERED_BY,
          BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_REQUEST_ID,
          BfdMDC.HTTP_ACCESS_RESPONSE_OUTPUT_SIZE_IN_BYTES,
          BfdMDC.HTTP_ACCESS_RESPONSE_STATUS,
          BfdMDC.HTTP_ACCESS_RESPONSE_CONTENT_LENGTH,
          BfdMDC.RESOURCES_RETURNED);

  /** The singleton {@link ServerTestUtils} instance to use everywhere. */
  private static ServerTestUtils SINGLETON;

  /** The server's base url. */
  private final String serverBaseUrl;

  /**
   * Constructs a new {@link ServerTestUtils} instance. Marked <code>private</code>; use {@link
   * #get()}, instead.
   */
  private ServerTestUtils() {
    this.serverBaseUrl = initServerBaseUrl();
  }

  /** File name for NPI enrichment data. */
  static final String FAKE_ORG_DATA = "npi_e2e_it.json";

  /** File name for Drug enrichment data. */
  static final String FAKE_DRUG_ORG_DATA = "fakeDrugOrg.json";

  /**
   * Get server test utils.
   *
   * <p>We use a singleton and cache all the fields because creating some of the fields stored in
   * the PipelineApplicationState is EXPENSIVE (it maintains a DB connection pool), so we don't want
   * to have to re-create it for every test.
   *
   * @return the singleton {@link ServerTestUtils} instance to use everywhere
   */
  public static synchronized ServerTestUtils get() {
    if (SINGLETON == null) {
      SINGLETON = new ServerTestUtils();
    }

    return SINGLETON;
  }

  /**
   * Init server base url string.
   *
   * @return the value to use for {@link #getServerBaseUrl()}
   */
  private static String initServerBaseUrl() {
    int httpsPort = Integer.parseInt(ServerExecutor.getServerPort());
    String serverBaseUrl = String.format("https://localhost:%d", httpsPort);
    return serverBaseUrl;
  }

  /**
   * Creates a new FHIR {@link IGenericClient} for use, configured to use the {@link
   * ClientSslIdentity#TRUSTED} login.
   *
   * @return a FHIR client
   */
  public IGenericClient createFhirClient() {
    return createFhirClient(Optional.of(ClientSslIdentity.TRUSTED));
  }

  /**
   * Create fhir client with the specified ssl identity.
   *
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the FHIR server
   * @return a new FHIR {@link IGenericClient} for use
   */
  public IGenericClient createFhirClient(Optional<ClientSslIdentity> clientSslIdentity) {
    return createFhirClient("v1", clientSslIdentity);
  }

  /**
   * Creates a new FHIR {@link IGenericClient} for use, configured to use the {@link
   * ClientSslIdentity#TRUSTED} login for FIHR v2 server.
   *
   * @return a FHIR client
   */
  public IGenericClient createFhirClientV2() {
    return createFhirClientV2(Optional.of(ClientSslIdentity.TRUSTED));
  }

  /**
   * Create V2 fhir client with the specified ssl identity.
   *
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the FV2 HIR server
   * @return a new FHIR {@link IGenericClient} for use
   */
  public IGenericClient createFhirClientV2(Optional<ClientSslIdentity> clientSslIdentity) {
    return createFhirClient("v2", clientSslIdentity, FhirContext.forR4());
  }

  /**
   * Creates a FHIR client.
   *
   * @param versionId the v1 or v2 identifier to use as a part of the URL for the FHIR server
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the FHIR server
   * @return a new FHIR {@link IGenericClient} for use
   */
  private IGenericClient createFhirClient(
      String versionId, Optional<ClientSslIdentity> clientSslIdentity) {
    // Default behavor before was to spawn a DSTU3 context, so retain that
    return createFhirClient(versionId, clientSslIdentity, FhirContext.forDstu3());
  }

  /**
   * Creates a FHIR client.
   *
   * @param versionId the v1 or v2 identifier to use as a part of the URL for the FHIR server
   * @param clientSslIdentity the {@link ClientSslIdentity} to use as a login for the FHIR server
   * @param ctx the fhir context
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
   * Creates the ssl context.
   *
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

  /**
   * Gets the {@link #serverBaseUrl}.
   *
   * @return the base URL for the server (not for the FHIR servlet, but just the server itself)
   */
  public String getServerBaseUrl() {
    return serverBaseUrl;
  }

  /**
   * Gets the local {@link Path} that the project can be found in.
   *
   * @return the local {@link Path}
   */
  public static Path getWarProjectDirectory() {
    try {
      /*
       * The working directory for tests will either be the module directory or their parent
       * directory. With that knowledge, we're searching for the project directory.
       */
      Path projectDir = Paths.get(".");
      if (!Files.isDirectory(projectDir) && projectDir.toRealPath().endsWith("bfd-server-war"))
        throw new IllegalStateException();
      return projectDir;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Gets the local {@link Path} that development/test key and trust stores can be found in.
   *
   * @return the ssl stores directory
   */
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

  /**
   * Gets the client trust store path.
   *
   * @return the local {@link Path} to the trust store that FHIR clients should use
   */
  private static Path getClientTrustStorePath() {
    Path trustStorePath = getSslStoresDirectory().resolve("client-truststore.pfx");
    return trustStorePath;
  }

  /**
   * Parses a list of sample sources.
   *
   * @param sampleResources the sample RIF resources to parse
   * @return the {@link List} of RIF records that were parsed (e.g. {@link Beneficiary}s, etc.)
   */
  public static List<Object> parseData(List<StaticRifResource> sampleResources) {
    RifFilesEvent rifFilesEvent =
        new RifFilesEvent(
            Instant.now(),
            false,
            sampleResources.stream().map(r -> r.toRifFile()).collect(Collectors.toList()));
    RifFilesProcessor processor = new RifFilesProcessor();
    List<Object> recordsParsed = new ArrayList<>();
    for (RifFileEvent rifFileEvent : rifFilesEvent.getFileEvents()) {
      RifFileRecords rifFileRecords = processor.produceRecords(rifFileEvent);
      rifFileRecords
          .getRecords()
          .map(r -> r.getRecord())
          .toIterable()
          .forEach(r -> recordsParsed.add(r));
    }

    return recordsParsed;
  }

  /**
   * Loads the sample A test data.
   *
   * @return the loaded records
   */
  public List<Object> loadSampleAData() {
    return loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
  }

  /**
   * Loads the sample A data with various samhsa codes to use in tests.
   *
   * @return the loaded records
   */
  public List<Object> loadSampleASamhsaData() {
    return loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A_SAMHSA.getResources()));
  }

  /** Loads enrichment data into the database. */
  public void loadEnrichmentData() {
    loadEnrichmentData(FAKE_DRUG_ORG_DATA, FDAData.class);
    loadEnrichmentData(FAKE_ORG_DATA, NPIData.class);
  }

  /**
   * Loads enrichment data into the database.
   *
   * @param filename File containing the enrichment data.
   * @param dataClass The class of the data entity to be loaded.
   * @param <TData> The type of the data entity to be loaded.
   */
  private static <TData> void loadEnrichmentData(String filename, Class<TData> dataClass) {
    InputStream dataStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
    String line;
    try (final InputStream stream = dataStream;
        EntityManager entityManager =
            PipelineTestUtils.get()
                .getPipelineApplicationState()
                .getEntityManagerFactory()
                .createEntityManager();
        final BufferedReader reader =
            new BufferedReader(new InputStreamReader(Objects.requireNonNull(stream)))) {
      ObjectMapper objectMapper = new ObjectMapper();
      while ((line = reader.readLine()) != null) {
        TData data = objectMapper.readValue(line, dataClass);
        entityManager.getTransaction().begin();
        entityManager.merge(data);
        entityManager.getTransaction().commit();
        entityManager.clear();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Gets the pagination link by name from the response. Throws an exception if the given link name
   * does not exist in the response.
   *
   * <p>Should be one of: next, previous, self, last, first
   *
   * @param response the response
   * @param paginationLinkName the pagination link name
   * @return the pagination link
   */
  public String getPaginationLink(Response response, String paginationLinkName) {

    List<Map<String, ?>> links = response.jsonPath().getList("link");
    return links.stream()
        .filter(m -> m.get("relation").equals(paginationLinkName))
        .findFirst()
        .orElseThrow()
        .get("url")
        .toString();
  }

  /**
   * Loads a list of sample resources.
   *
   * @param sampleResources the sample RIF resources to load
   * @return the {@link List} of RIF records that were loaded (e.g. {@link Beneficiary}s, etc.)
   */
  public List<Object> loadData(List<StaticRifResource> sampleResources) {
    LoadAppOptions loadOptions = CcwRifLoadTestUtils.getLoadOptions();
    RifFilesEvent rifFilesEvent =
        new RifFilesEvent(
            Instant.now(),
            false,
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
      loader
          .processAsync(rifFileRecords, new AtomicBoolean())
          .doOnError(error -> LOGGER.warn("Record(s) failed to load.", error))
          .doOnNext(result -> recordsLoaded.add(result.getRifRecordEvent().getRecord()))
          .blockLast();
    }
    LOGGER.info("Loaded RIF records: '{}'.", recordsLoaded.size());
    return recordsLoaded;
  }

  /**
   * Gets the first beneficiary in the provided data.
   *
   * @param loadedRecords the loaded records
   * @return the first beneficiary
   */
  public Beneficiary getFirstBeneficiary(List<Object> loadedRecords) {
    return loadedRecords.stream()
        .filter(r -> r instanceof Beneficiary)
        .map(Beneficiary.class::cast)
        .findFirst()
        .get();
  }

  /**
   * Gets the patient id.
   *
   * @param loadedRecords the loaded records
   * @return the patient id from the first beneficiary
   */
  public String getPatientId(List<Object> loadedRecords) {
    return CommonTransformerUtils.buildPatientId(getFirstBeneficiary(loadedRecords)).getIdPart();
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
   * Runs a <code>TRUNCATE</code> for all tables in the supplied database. Obviously, only use this
   * on test/local databases.
   *
   * @param dataSource the data source
   */
  public void truncateNonRdaTablesInDataSource(DataSource dataSource) {
    List<Class<?>> entityTypes =
        Arrays.asList(
            PartDEvent.class,
            SNFClaimLine.class,
            SNFClaim.class,
            OutpatientClaimLine.class,
            OutpatientClaim.class,
            InpatientClaimLine.class,
            InpatientClaim.class,
            HospiceClaimLine.class,
            HospiceClaim.class,
            HHAClaimLine.class,
            HHAClaim.class,
            DMEClaimLine.class,
            DMEClaim.class,
            CarrierClaimLine.class,
            CarrierClaim.class,
            BeneficiaryHistory.class,
            BeneficiaryMonthly.class,
            Beneficiary.class,
            LoadedBatch.class,
            LoadedFile.class);

    try (Connection connection = dataSource.getConnection(); ) {
      // Disable auto-commit and remember the default schema name.
      connection.setAutoCommit(false);
      Optional<String> defaultSchemaName = Optional.ofNullable(connection.getSchema());
      if (defaultSchemaName.isEmpty()) {
        throw new BadCodeMonkeyException("Unable to determine default schema name.");
      }

      // Loop over every @Entity type.
      for (Class<?> entityType : entityTypes) {
        Optional<Table> entityTableAnnotation =
            Optional.ofNullable(entityType.getAnnotation(Table.class));

        // First, make sure we found an @Table annotation.
        if (entityTableAnnotation.isEmpty()) {
          throw new BadCodeMonkeyException(
              "Unable to determine table metadata for entity: " + entityType.getCanonicalName());
        }

        // Then, make sure we have a table name specified.
        if (entityTableAnnotation.get().name() == null
            || entityTableAnnotation.get().name().isEmpty()) {
          throw new BadCodeMonkeyException(
              "Unable to determine table name for entity: " + entityType.getCanonicalName());
        }
        String tableNameSpecifier = normalizeTableName(entityTableAnnotation.get().name());

        // Then, switch to the appropriate schema.
        if (entityTableAnnotation.get().schema() != null
            && !entityTableAnnotation.get().schema().isEmpty()) {
          String schemaNameSpecifier = normalizeSchemaName(entityTableAnnotation.get().schema());
          connection.setSchema(schemaNameSpecifier);
        } else {
          connection.setSchema(defaultSchemaName.get());
        }

        /*
         * Finally, run the TRUNCATE. On Postgres the cascade option is required due to the
         * presence of FK constraints.
         */
        String truncateTableSql = String.format("TRUNCATE TABLE %s", tableNameSpecifier);
        if (DatabaseUtils.isPostgresConnection(connection)) {
          truncateTableSql = truncateTableSql + " CASCADE";
        }
        connection.createStatement().execute(truncateTableSql);

        connection.setSchema(defaultSchemaName.get());
      }

      connection.commit();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Normalizes the schema names by preserving case and removing quotes.
   *
   * @param schemaNameSpecifier name of a schema from a hibernate annotation
   * @return value compatible with call to {@link Connection#setSchema(String)}
   * @throws SQLException the sql exception
   */
  private String normalizeSchemaName(String schemaNameSpecifier) throws SQLException {
    return schemaNameSpecifier.replaceAll("`", "");
  }

  /**
   * Normalizes the table names by upper-casing table names without quotes and preserving case for
   * other table names.
   *
   * @param tableNameSpecifier name of a table from a hibernate annotation
   * @return value compatible with call to {@link java.sql.Statement#execute(String)}
   */
  private String normalizeTableName(String tableNameSpecifier) {
    if (tableNameSpecifier.startsWith("`")) {
      tableNameSpecifier = tableNameSpecifier.replaceAll("`", "");
    } else {
      tableNameSpecifier = tableNameSpecifier.toUpperCase();
    }
    return tableNameSpecifier;
  }

  /**
   * Validates if the test's db url is valid for testing; i.e. is this a local database we can
   * properly purge between tests. This is primarily to avoid calling truncate on a database you
   * shouldn't and ruining your entire career.
   *
   * @param dbUrl the db url to validate
   * @return if the db url is a local database that can be safely truncated and is not empty
   */
  public static boolean isValidServerDatabase(String dbUrl) {
    boolean isTestContainer = dbUrl.endsWith("tc");
    boolean isLocalDb = dbUrl.contains("localhost") || dbUrl.contains("127.0.0.1");
    return !dbUrl.isBlank() && (isTestContainer || isLocalDb);
  }

  /**
   * Generates a path to use to get or store endpoint json files.
   *
   * @param directory the path to where the file should be written
   * @param endpoint the string to identify which endpoint's response the file contents contain
   * @return a path to use as a filename
   */
  public static Path generatePathForEndpointJsonFile(Path directory, String endpoint) {
    return Paths.get(directory.toString(), endpoint + ".json");
  }

  /**
   * Reads a file at the specified location.
   *
   * @param path the path to the file
   * @return the contents of the file as a string.
   */
  public static String readFile(Path path) {
    try {
      return Files.readString(path, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Can't read file at " + path, e);
    }
  }

  /**
   * Gets the claim id for the specified record type in the loaded records.
   *
   * @param loadedRecords the loaded records
   * @param claimType the claim type
   * @return the claim id for
   */
  public static String getClaimIdFor(List<Object> loadedRecords, ClaimType claimType) {
    return switch (claimType) {
      case CARRIER:
        CarrierClaim carrier = getClaim(loadedRecords, CarrierClaim.class);
        yield String.valueOf(carrier.getClaimId());
      case DME:
        DMEClaim dme = getClaim(loadedRecords, DMEClaim.class);
        yield String.valueOf(dme.getClaimId());
      case HHA:
        HHAClaim hha = getClaim(loadedRecords, HHAClaim.class);
        yield String.valueOf(hha.getClaimId());
      case HOSPICE:
        HospiceClaim hospiceClaim = getClaim(loadedRecords, HospiceClaim.class);
        yield String.valueOf(hospiceClaim.getClaimId());
      case INPATIENT:
        InpatientClaim inpatientClaim = getClaim(loadedRecords, InpatientClaim.class);
        yield String.valueOf(inpatientClaim.getClaimId());
      case OUTPATIENT:
        OutpatientClaim outpatientClaim = getClaim(loadedRecords, OutpatientClaim.class);
        yield String.valueOf(outpatientClaim.getClaimId());
      case PDE:
        PartDEvent pde = getClaim(loadedRecords, PartDEvent.class);
        yield String.valueOf(pde.getEventId());
      case SNF:
        SNFClaim snfClaim = getClaim(loadedRecords, SNFClaim.class);
        yield String.valueOf(snfClaim.getClaimId());
    };
  }

  /**
   * Instantiates a new Get claim.
   *
   * @param loadedRecords the loaded records
   * @param clazz the rif record type
   * @param <T> the rif record type (must match clazz)
   * @return the claim of the given type from the sample data
   */
  public static <T extends RifRecordBase> T getClaim(List<Object> loadedRecords, Class<T> clazz) {

    return loadedRecords.stream()
        .filter(clazz::isInstance)
        .map(clazz::cast)
        .findFirst()
        .orElseThrow();
  }

  /**
   * Writes a file to the specified path.
   *
   * @param contents the string to be written to a file
   * @param filePath the path+name to save the file as
   */
  public static void writeFile(String contents, Path filePath) {
    try {
      Files.writeString(filePath, contents, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("Could not write file at " + filePath, e);
    }
  }

  /**
   * Assert mdc keys (not values) are written to the access.json log when calling the given REST
   * query string. Will check a set of common mdc keys that should be on every call, and optionally
   * takes a list of additional keys to check for specific to the request made.
   *
   * @param requestAuth the request auth for the restAssured call
   * @param requestString the request string for the restAssured call
   * @param additionalMdcKeysToCheck any additional mdc keys to check that arent in the base list of
   *     mdc keys to check for in the access.json
   * @throws IOException if there is an issue with the IO around the access.json file
   */
  public static void assertDefaultAndAdditionalMdcKeys(
      RequestSpecification requestAuth, String requestString, List<String> additionalMdcKeysToCheck)
      throws IOException {
    assertDefaultAndAdditionalMdcKeys(
        requestAuth, requestString, additionalMdcKeysToCheck, new HashMap<>());
  }

  /**
   * Assert mdc keys (not values) are written to the access.json log when calling the given REST
   * query string. Will check a set of common mdc keys that should be on every call, and optionally
   * takes a list of additional keys to check for specific to the request made.
   *
   * @param requestAuth the request auth for the restAssured call
   * @param requestString the request string for the restAssured call
   * @param additionalMdcKeysToCheck any additional mdc keys to check that arent in the base list of
   *     mdc keys to check for in the access.json
   * @param headers the headers needed for the restAssured request
   * @throws IOException if there is an issue with the IO around the access.json file
   */
  public static void assertDefaultAndAdditionalMdcKeys(
      RequestSpecification requestAuth,
      String requestString,
      List<String> additionalMdcKeysToCheck,
      Map<String, String> headers)
      throws IOException {
    assertMdcEntries(
        requestAuth,
        requestString,
        // Generate a Map that is the combination of the default keys and any additional keys to
        // check where each key has a corresponding empty value indicating that only the key's
        // existence should be verified; basically, this method doesn't check any MDC values
        Stream.concat(DEFAULT_MDC_KEYS.stream(), additionalMdcKeysToCheck.stream())
            .distinct()
            .collect(Collectors.toMap(key -> key, key -> Optional.empty())),
        headers);
  }

  /**
   * Assert that MDC entries provided exist in the access.jon log, and optionally check the values
   * of the entries if the value provided for a given MDC key is not an empty {@link Optional}
   * value.
   *
   * @param requestAuth the request auth for the restAssured call
   * @param requestString the request string for the restAssured call
   * @param expectedMdcKeysToValues a {@link Map} of MDC keys to values that are asserted upon. Keys
   *     are always checked for existence, whereas values are only checked if a non-empty value is
   *     provided
   * @throws IOException if there is an issue with the IO around the access.json file
   */
  public static void assertMdcEntries(
      RequestSpecification requestAuth,
      String requestString,
      Map<String, Optional<String>> expectedMdcKeysToValues)
      throws IOException {
    assertMdcEntries(requestAuth, requestString, expectedMdcKeysToValues, new HashMap<>());
  }

  /**
   * Assert that MDC entries provided exist in the access.jon log, and optionally check the values
   * of the entries if the value provided for a given MDC key is not an empty {@link Optional}
   * value.
   *
   * @param requestAuth the request auth for the restAssured call
   * @param requestString the request string for the restAssured call
   * @param expectedMdcKeysToValues a {@link Map} of MDC keys to values that are asserted upon. Keys
   *     are always checked for existence, whereas values are only checked if a non-empty value is
   *     provided
   * @param headers the headers needed for the restAssured request
   * @throws IOException if there is an issue with the IO around the access.json file
   */
  public static void assertMdcEntries(
      RequestSpecification requestAuth,
      String requestString,
      Map<String, Optional<String>> expectedMdcKeysToValues,
      Map<String, String> headers)
      throws IOException {
    Path accessLogJson =
        ServerTestUtils.getWarProjectDirectory()
            .resolve("target")
            .resolve("server-work")
            .resolve("access.json");
    // Empty the access json to avoid pollution from other tests
    try (BufferedWriter writer = Files.newBufferedWriter(accessLogJson)) {
      Files.writeString(accessLogJson, "");
    }

    given()
        .spec(requestAuth)
        .headers(headers)
        .contentType("application/json")
        .expect()
        .statusCode(200)
        .when()
        .get(requestString);

    // Wait for access log to be written
    Awaitility.await().atMost(5, TimeUnit.SECONDS).until(() -> Files.isReadable(accessLogJson));

    assertTrue(Files.isReadable(accessLogJson));
    assertTrue(Files.size(accessLogJson) > 0);

    String content = Files.readString(accessLogJson);

    content = content.substring(content.indexOf("mdc\":{") + 6, content.indexOf("}"));
    List<String> headerList = List.of(content.split("\","));
    Map<String, String> mdcKeyValueMap = new HashMap<>();
    for (String header : headerList) {
      String[] headerVal = header.split("\":");
      // Capture both the key and value, in case we want to test any of the values
      String key = headerVal[0].replace("\"", "");
      String val = headerVal[1].replace("\"", "");
      mdcKeyValueMap.put(key, val);
    }

    // Compile a list of mdc keys that are missing from the expected list
    List<String> missingMdcKeys = new ArrayList<>();
    for (String mdcKey : expectedMdcKeysToValues.keySet()) {
      if (!mdcKeyValueMap.containsKey(mdcKey)) {
        LOGGER.info("Missing header: " + mdcKey);
        missingMdcKeys.add(mdcKey);
      }
    }

    /* Check for any extra new mdc keys that may have suddenly appeared
     * Ignore JPA and database entries unless/until we care about comprehensively
     * testing them on all endpoints */
    List<String> extraMdcKeys = new ArrayList<>();
    for (String mdcKey : mdcKeyValueMap.keySet()) {
      if (!expectedMdcKeysToValues.containsKey(mdcKey)
          && !DEFAULT_MDC_KEYS.contains(mdcKey)
          && !mdcKey.startsWith("jpa")
          && !mdcKey.startsWith("database")) {
        LOGGER.info("Extra header: " + mdcKey);
        extraMdcKeys.add(mdcKey);
      }
    }

    // Reduce the expected MDC entries to a subset of entries with values we'd like to check; that
    // is, any entry with a value that isn't an empty Optional
    Map<String, String> mdcEntriesWithExpectedVals =
        expectedMdcKeysToValues.entrySet().stream()
            .filter(es -> es.getValue().isPresent())
            .map(es -> Map.entry(es.getKey(), es.getValue().get()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Reduce the actual values map to one with keys matching that of the expected, checked map
    // above. This is necessary as we only want to check the value of a particular entry if a value
    // was provided
    Map<String, String> actualMdcEntriesToCheck =
        mdcKeyValueMap.entrySet().stream()
            .filter(es -> mdcEntriesWithExpectedVals.containsKey(es.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // Check we have no missing mdc keys in a way that makes a useful assertion error if not
    assertEquals(missingMdcKeys, new ArrayList<>(), "Missing expected MDC keys in access.json");
    assertEquals(
        extraMdcKeys, new ArrayList<>(), "Found unexpected additional MDC keys in access.json");

    // Check the MDC entries with an expected value against the real value assertMdcEntries from
    // access.json
    assertEquals(
        mdcEntriesWithExpectedVals,
        actualMdcEntriesToCheck,
        "Found MDC entries with unexpected values in access.json");
  }
}
