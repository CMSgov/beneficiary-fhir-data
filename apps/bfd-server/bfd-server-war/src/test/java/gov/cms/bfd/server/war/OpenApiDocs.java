package gov.cms.bfd.server.war;

import static gov.cms.bfd.DatabaseTestUtils.TEST_CONTAINER_DATABASE_PASSWORD;
import static gov.cms.bfd.DatabaseTestUtils.TEST_CONTAINER_DATABASE_USERNAME;
import static io.restassured.RestAssured.certificate;
import static io.restassured.RestAssured.given;

import gov.cms.bfd.DatabaseTestUtils;
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.CertificateAuthSettings;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

/** Program to gather the OpenAPI yaml content for V1 and V2 and store. */
public class OpenApiDocs {

  /** BFD API version 1 moniker. */
  private static final String API_VERSION_1 = "v1";

  /** BFD API version 2 moniker. */
  private static final String API_VERSION_2 = "v2";

  /** The version from the project pom. */
  private final String projectVersion;

  /** The destination directory for the output of the OpenAPI yaml file documents. */
  private final String destinationDirectory;

  /** The base server url. */
  private String baseServerUrl;

  /** The request specification to use with the api-docs requests. */
  private RequestSpecification requestAuth;

  /** YAML reading/writing utility. */
  private final Yaml yaml;

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiDocs.class);

  /** V2 deprecated endpoints. */
  public static final String[] V2_DEPRECATED_ENDPOINTS =
      new String[] {
        "/ExplanationOfBenefit",
        "/Patient",
        "/Claim",
        "/ClaimResponse",
        "/ExplanationOfBenefit/_search",
        "/Patient/_search",
        "/Claim/_search",
        "/ClaimResponse/_search"
      };

  /** V1 deprecated endpoints. */
  public static final String[] V1_DEPRECATED_ENDPOINTS =
      new String[] {"/Patient", "/Patient/_search"};

  /**
   * Entry point, starts an E2E test server instance and downloads OpenAPI yaml.
   *
   * @param args The project version, working directory, and destination directory.
   * @throws IOException for file operation or server startup errors.
   */
  public static void main(String[] args) throws IOException {

    // Validate arguments.
    validateArgs(args);

    var projectVersion = args[0];
    var workingDirectory = args[1];
    var destinationDirectory = args[2];

    // Update working directory so E2E test server instance can find properties.
    System.setProperty("user.dir", workingDirectory);

    var openApiDocs = new OpenApiDocs(projectVersion, destinationDirectory);

    try {
      // Start E2E test server instance.
      openApiDocs.setup();

      List<Map<String, Object>> specs = new ArrayList<>(2);

      for (String apiVersion : List.of(API_VERSION_1, API_VERSION_2)) {

        // Download OpenAPI specs
        var spec = openApiDocs.downloadApiDocs(apiVersion);

        // Update paths and tags
        openApiDocs.updatePathsAndTags(spec, apiVersion);

        // Add deprecation flag to deprecated endpoints
        openApiDocs.addDeprecatedFlag(spec, apiVersion);
        specs.add(spec);
      }

      // Merge spec versions into a combined spec
      var combinedSpec = openApiDocs.merge(specs);

      // Add header information to combined spec
      openApiDocs.addHeaderParameters(combinedSpec);

      // Write out combined spec
      openApiDocs.save(combinedSpec);
      System.exit(0);
    } catch (Exception ex) {
      LOGGER.error("Error creating OpenAPI document", ex);
      System.exit(1);
    }
  }

  /**
   * Validates the program arguments.
   *
   * @param args the project version, working directory, and destination directory.
   */
  private static void validateArgs(String[] args) {
    if (args.length != 3) {
      throw new RuntimeException(
          "Project version, working directory, and destination directory are required.");
    }

    var workingDirectory = args[1];
    var destinationDirectory = args[2];

    var workingDirectoryFile = new File(workingDirectory);
    if (!workingDirectoryFile.isDirectory()) {
      throw new RuntimeException(
          String.format("Working directory (%s) is not valid.", workingDirectory));
    }

    var destinationDirectoryFile = new File(destinationDirectory);
    if (!destinationDirectoryFile.isDirectory()) {
      throw new RuntimeException(
          String.format("Destination directory (%s) is not valid.", destinationDirectory));
    }
  }

  /**
   * Constructs an instance of OpenApiDocs for a given project version and destination directory.
   *
   * @param projectVersion @see #projectVersion
   * @param destinationDirectory @see #destinationDirectory
   */
  private OpenApiDocs(String projectVersion, String destinationDirectory) {
    this.projectVersion = projectVersion;
    this.destinationDirectory = destinationDirectory;
    this.yaml = new Yaml();
  }

  /**
   * Initializes and starts a test database and bfd server.
   *
   * @throws IOException when an error occurs starting the bfd server.
   */
  private void setup() throws IOException {
    var dataSource = DatabaseTestUtils.get().getUnpooledDataSource();
    String resolvedDbUrl = ((PGSimpleDataSource) dataSource).getUrl();

    boolean startedServer =
        ServerExecutor.startServer(
            resolvedDbUrl, TEST_CONTAINER_DATABASE_USERNAME, TEST_CONTAINER_DATABASE_PASSWORD);
    if (startedServer) {
      baseServerUrl = "https://localhost:" + ServerExecutor.getServerPort();
      setRequestAuth();
      Runtime.getRuntime().addShutdownHook(new Thread(ServerExecutor::stopServer));
    } else {
      throw new RuntimeException("Could not start server instance.");
    }
  }

  /**
   * Downloads the OpenAPI yaml from the api-docs V1 endpoint using a local E2E test server
   * instance.
   *
   * @param apiVersion the bfd api version, either V1 or V2.
   * @return a Map with the OpenAPI spec components.
   * @throws IOException when file operations fail
   */
  private Map<String, Object> downloadApiDocs(String apiVersion) throws IOException {
    var apiDocsUrl = String.format("%s/%s/fhir/api-docs", baseServerUrl, apiVersion.toLowerCase());
    var response =
        given()
            .spec(requestAuth)
            .when()
            .get(apiDocsUrl)
            .then()
            .contentType("text/yaml")
            .extract()
            .asString();

    return yaml.load(response);
  }

  /**
   * Add the api version to endpoint paths and add version tags.
   *
   * @param spec the spec Map to update.
   * @param apiVersion the api version to apply to the paths and tags.
   */
  private void updatePathsAndTags(Map<String, Object> spec, String apiVersion) {
    var paths = (Map<String, Object>) spec.get("paths");
    var newPaths = new LinkedHashMap<String, Object>(paths.size());

    // iterate over paths and update the url to add the version,
    // and add an api version tag.

    Set<String> pathKeys = paths.keySet();
    for (String path : pathKeys) {
      var pathMap = (Map<String, Object>) paths.get(path);
      for (var operation : List.of("get", "post")) {
        var pathOperation = (Map<String, Object>) pathMap.get(operation);
        if (pathOperation != null) {
          var tags = (List<String>) pathOperation.get("tags");
          tags.addFirst(apiVersion);
          newPaths.put(String.format("/%s/fhir%s", apiVersion, path), pathMap);
        }
      }
    }

    spec.put("paths", newPaths);
  }

  /**
   * Adds deprecated flag to certain HAPI FHIR generated get endpoints. Since we are passed the
   * entire YAML tree, we can traverse it until we get to the needed get node to add the flag.
   *
   * @param spec the OpenAPI spec Map to update with deprecated info.
   * @param apiVersion either V1 or V2
   */
  private void addDeprecatedFlag(Map<String, Object> spec, String apiVersion) {
    // iterate over paths and add deprecated to search endpoints
    var paths = (Map<String, Object>) spec.get("paths");
    for (String path : paths.keySet()) {
      if ((apiVersion.equals(API_VERSION_2)
              && (Arrays.stream(V2_DEPRECATED_ENDPOINTS).anyMatch(path::endsWith))
          || (apiVersion.equals(API_VERSION_1)
              && Arrays.stream(V1_DEPRECATED_ENDPOINTS).anyMatch(path::endsWith)))) {
        var getSpec = findGetSpecification(path, spec);
        getSpec.put("deprecated", true);
      }
    }
  }

  /**
   * Merge the spec Maps for BFD API V1 and V2, adding version tags and servers to the combined
   * definition.
   *
   * @param specs the list of spec Maps to combine, first Map is V1, second is V2.
   * @return a new spec Map combining the two separate spec Maps.
   * @throws IOException for input spec errors.
   */
  private Map<String, Object> merge(List<Map<String, Object>> specs) throws IOException {

    // start combined spec with v1
    var combinedSpec = new LinkedHashMap<>(specs.getFirst());

    // update servers, tags
    try (InputStream serversYaml =
        this.getClass().getClassLoader().getResourceAsStream("openapi/servers.yaml")) {
      Map<String, Object> serversAndTags = yaml.load(serversYaml);

      // replace servers with values from the servers.yaml
      var newServers = serversAndTags.get("servers");
      combinedSpec.put("servers", newServers);

      // prepend api version tags from servers.yaml
      var newTags = (List<Object>) serversAndTags.get("tags");
      var tags = (List<Object>) combinedSpec.get("tags");
      newTags.addAll(tags);
      combinedSpec.put("tags", newTags);
    }

    // add v2 paths
    var combinedPaths = (Map<String, Object>) combinedSpec.get("paths");
    var v2Paths = (Map<String, Object>) specs.get(1).get("paths");
    combinedPaths.putAll(v2Paths);

    return combinedSpec;
  }

  /**
   * Adds header parameters to all paths except '/metadata'.
   *
   * @param spec the OpenAPI spec Map
   * @throws IOException for input stream errors.
   */
  private void addHeaderParameters(Map<String, Object> spec) throws IOException {

    try (InputStream headersYaml =
        this.getClass().getClassLoader().getResourceAsStream("openapi/headers.yaml")) {

      Map<String, Object> headers = yaml.load(headersYaml);

      // add header params to be used in refs
      var components = (Map<String, Object>) spec.get("components");
      var headerComponents = (Map<String, Object>) headers.get("components");
      var headerParameters = (Map<String, Object>) headerComponents.get("parameters");
      components.put("parameters", headerParameters);

      // add parameter refs to each path
      var paths = (Map<String, Object>) spec.get("paths");
      for (String path : paths.keySet()) {
        if (!path.endsWith("/metadata")) {
          var pathMap = (Map<String, Object>) paths.get(path);
          var headerRefs = new ArrayList<>();
          headerRefs.add(createRef("#/components/parameters/BULK-CLIENTID-header"));
          headerRefs.add(createRef("#/components/parameters/BULK-CLIENTNAME-header"));
          headerRefs.add(createRef("#/components/parameters/BULK-JOBID-header"));
          headerRefs.add(createRef("#/components/parameters/BlueButton-OriginalQueryId-header"));
          headerRefs.add(
              createRef("#/components/parameters/BlueButton-OriginalQueryCounter-header"));
          headerRefs.add(
              createRef("#/components/parameters/BlueButton-OriginalQueryTimestamp-header"));
          headerRefs.add(createRef("#/components/parameters/BlueButton-DeveloperId-header"));
          headerRefs.add(createRef("#/components/parameters/BlueButton-Developer-header"));
          headerRefs.add(createRef("#/components/parameters/BlueButton-ApplicationId-header"));
          headerRefs.add(createRef("#/components/parameters/BlueButton-Application-header"));
          headerRefs.add(createRef("#/components/parameters/BlueButton-UserId-header"));
          headerRefs.add(createRef("#/components/parameters/BlueButton-User-header"));
          headerRefs.add(createRef("#/components/parameters/BlueButton-BeneficiaryId-header"));

          // add ref for IncludeAddressFields and IncludeIdentifiers headers for Patient endpoints
          if (path.contains("/Patient")) {
            headerRefs.add(createRef("#/components/parameters/IncludeIdentifiers-header"));
            headerRefs.add(createRef("#/components/parameters/IncludeAddressFields-header"));
          }

          pathMap.put("parameters", headerRefs);
        }
      }
    }
  }

  /**
   * Write the OpenAPI yaml content to a file.
   *
   * @param spec the OpenAPI yaml contents to write to the file
   * @throws IOException when file operations fail
   */
  private void save(Map<String, Object> spec) throws IOException {
    var fileName = String.format("%s/OpenAPI-%s.yaml", destinationDirectory, projectVersion);
    var contents = yaml.dump(spec);
    try (var fileWriter = new FileWriter(fileName)) {
      fileWriter.write(contents);
    }
  }

  /** Initializes the request authorization. @see ServerRequiredTest#setRequestAuth. */
  private void setRequestAuth() {
    // Get the certs for the test
    String trustStorePath = "src/test/resources/certs/test-truststore.jks";
    String keyStorePath = "src/test/resources/certs/test-keystore.p12";
    String testPassword = "changeit";
    String keystoreType = "pkcs12";
    // Set up the cert for the calls
    AuthenticationScheme testCertificate =
        certificate(
            trustStorePath,
            testPassword,
            keyStorePath,
            testPassword,
            CertificateAuthSettings.certAuthSettings()
                .keyStoreType(keystoreType)
                .trustStoreType(keystoreType)
                .allowAllHostnames());
    requestAuth =
        new RequestSpecBuilder().setBaseUri(baseServerUrl).setAuth(testCertificate).build();
  }

  /**
   * Retrieve the get method spec for a given endpoint.
   *
   * @param endPoint the endpoint to search for.
   * @param getSpecs the collection of get specifications.
   * @return the get spec for the given endpoint.
   * @throws RuntimeException when get spec is not found.
   */
  private Map<String, Object> findGetSpecification(String endPoint, Map<String, Object> getSpecs)
      throws RuntimeException {
    var paths = (Map<String, Object>) getSpecs.get("paths");
    var path = (Map<String, Object>) paths.get(endPoint);
    if (path == null) {
      throw new RuntimeException("GET specification not found for endpoint " + endPoint);
    }
    return (Map<String, Object>) path.get("get");
  }

  /**
   * Helper function to create a new ref.
   *
   * @param link the target of the ref element.
   * @return a new Map of the ref element.
   */
  private Map<String, Object> createRef(String link) {
    var ref = new LinkedHashMap<String, Object>();
    ref.put("$ref", link);
    return ref;
  }
}
