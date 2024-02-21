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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.shaded.org.yaml.snakeyaml.Yaml;

/** Program to gather the OpenAPI yaml content for V1 and V2 and store. */
public class OpenApiDocs {

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

  /**
   * Entry point, starts an E2E test server instance and downloads OpenAPI yaml.
   *
   * @param args The project version, working directory, and destination directory.
   */
  public static void main(String[] args) {

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

      for (var apiVersion : Set.of("V1", "V2")) {

        // Download OpenAPI spec
        var spec = openApiDocs.downloadApiDocs(apiVersion);

        // Add post specifications
        openApiDocs.addPostSpecifications(spec, apiVersion);

        // Augment with header parameters
        openApiDocs.addHeaderParameters(spec, apiVersion);

        // Write out spec
        openApiDocs.save(spec, apiVersion);
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Stop E2E server instance.
    System.exit(0);
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
   * Downloads the OpenAPI yaml from the api-docs V1 endpoint using a local E2E test server
   * instance.
   *
   * @param apiVersion the bfd api version, either V1 or V2.
   * @return a Map with the OpenAPI spec components.
   * @throws IOException when IO operations fail
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
   * Write the OpenAPI yaml content to a file.
   *
   * @param spec the OpenAPI yaml contents to write to the file
   * @param apiVersion either V1 or V2
   * @throws IOException when file operations fail
   */
  private void save(Map<String, Object> spec, String apiVersion) throws IOException {
    var fileName =
        String.format("%s/%s-OpenAPI-%s.yaml", destinationDirectory, apiVersion, projectVersion);
    var contents = yaml.dump(spec);
    try (var fileWriter = new FileWriter(fileName)) {
      fileWriter.write(contents);
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
   * Adds header parameters to all paths except '/metadata'.
   *
   * @param spec the OpenAPI spec Map
   * @param apiVersion the bfd api version, either V1 or V2.
   * @throws IOException for input stream errors.
   */
  private void addHeaderParameters(Map<String, Object> spec, String apiVersion) throws IOException {

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
        if (!"/metadata".equals(path)) {
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

          // add ref for IncludeIdentifiers header for V1 Patient endpoints
          if (path.contains("/Patient") && apiVersion.equals("V1")) {
            headerRefs.add(createRef("#/components/parameters/IncludeIdentifiers-header"));
          }

          // add ref for IncludeAddressFields for Patient endpoints
          if (path.contains("/Patient")) {
            headerRefs.add(createRef("#/components/parameters/IncludeAddressFields-header"));
          }

          // add ref for IncludeTaxNumbers for ExplanationOfBenefit, Claim, ClaimResponse endpoints
          if (path.contains("/ExplanationOfBenefit") || path.contains("/Claim")) {
            headerRefs.add(createRef("#/components/parameters/IncludeTaxNumbers-header"));
          }

          pathMap.put("parameters", headerRefs);
        }
      }
    }
  }

  /**
   * Add specification for POST endpoints which are not generated by hapi-fhir.
   *
   * @param spec the OpenAPI spec Map to update with POST info.
   * @param apiVersion either V1 or V2
   * @throws IOException for input stream errors.
   */
  private void addPostSpecifications(Map<String, Object> spec, String apiVersion)
      throws IOException {

    try (InputStream postsYaml =
        this.getClass().getClassLoader().getResourceAsStream("openapi/posts.yaml")) {
      Map<String, Object> postSpecs = yaml.load(postsYaml);

      // iterate over paths and add post specification to _search endpoints
      var paths = (Map<String, Object>) spec.get("paths");
      for (String path : paths.keySet()) {
        if (apiVersion.equals("V2")
            && (path.endsWith("ExplanationOfBenefit/_search")
                || path.endsWith("Patient/_search"))) {
          var pathMap = (Map<String, Object>) paths.get(path);
          var postSpec = findPostSpecification(path, postSpecs);

          // add resulting POST specification
          pathMap.put("post", postSpec);
        }
      }
    }
  }

  /**
   * Retrieve the post method spec for a given endpoint.
   *
   * @param endPoint the endpoint to search for.
   * @param postSpecs the collection of post specifications.
   * @return the post spec for the given endpoint.
   * @throws RuntimeException when post spec is not found.
   */
  private Map<String, Object> findPostSpecification(String endPoint, Map<String, Object> postSpecs)
      throws RuntimeException {
    var paths = (Map<String, Object>) postSpecs.get("paths");
    var path = (Map<String, Object>) paths.get(endPoint);
    if (path == null) {
      throw new RuntimeException("POST specification not found for endpoint " + endPoint);
    }
    return (Map<String, Object>) path.get("post");
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
