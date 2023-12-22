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
import java.util.Set;
import org.postgresql.ds.PGSimpleDataSource;

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

  /**
   * Constructs an instance of OpenApiDocs for a given project version and destination directory.
   *
   * @param projectVersion @see #projectVersion
   * @param destinationDirectory @see #destinationDirectory
   */
  private OpenApiDocs(String projectVersion, String destinationDirectory) {
    this.projectVersion = projectVersion;
    this.destinationDirectory = destinationDirectory;
  }

  /**
   * Downloads the OpenAPI yaml from the api-docs V1 endpoint using a local E2E test server
   * instance.
   *
   * @param apiVersion the bfd api version, either V1 or V2.
   * @throws IOException when IO operations fail
   */
  void downloadApiDocs(String apiVersion) throws IOException {
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

    writeFile(apiVersion, response);
  }

  /**
   * Write the OpenAPI yaml content to a file.
   *
   * @param apiVersion either V1 or V2
   * @param contents the OpenAPI yaml contents to write to the file
   * @throws IOException when file operations fail
   */
  private void writeFile(String apiVersion, String contents) throws IOException {
    var fileName =
        String.format("%s/%s-OpenAPI-%s.yaml", destinationDirectory, apiVersion, projectVersion);
    try (var fileWriter = new FileWriter(fileName)) {
      fileWriter.write(contents);
    }
  }

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

      // Download and save OpenAPI yaml for V1 and V2.
      for (var apiVersion : Set.of("V1", "V2")) {
        openApiDocs.downloadApiDocs(apiVersion);
      }

    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Stop E2E server instance.
    System.exit(0);
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
}
