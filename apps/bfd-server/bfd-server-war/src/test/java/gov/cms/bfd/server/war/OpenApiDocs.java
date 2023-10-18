package gov.cms.bfd.server.war;

import static io.restassured.RestAssured.given;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

/** Program to gather the OpenAPI yaml content for V1 and V2 and store. */
public class OpenApiDocs extends ServerRequiredTest {

  /** The version from the project pom. */
  private final String projectVersion;

  /** The destination directory for the output of the OpenAPI yaml file documents. */
  private final String destinationDir;

  /**
   * Constructs an instance of OpenApiDocs for a given project version and destination directory.
   *
   * @param projectVersion @see #projectVersion
   * @param destinationDir @see #destinationDir
   */
  private OpenApiDocs(String projectVersion, String destinationDir) {
    this.projectVersion = projectVersion;
    this.destinationDir = destinationDir;
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
        String.format("%s/%s-OpenAPI-%s.yaml", destinationDir, apiVersion, projectVersion);
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

    // Update working directory so E2E test server instance can find properties.
    System.setProperty("user.dir", args[1]);

    var openApiDocs = new OpenApiDocs(args[0], args[2]);
    try {
      // Start E2E test server instance.
      setup();

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

    var workingDirectory = new File(args[1]);
    if (!workingDirectory.isDirectory()) {
      throw new RuntimeException(String.format("Working directory (%s) is not valid.", args[1]));
    }

    var destinationDirectory = new File(args[2]);
    if (!destinationDirectory.isDirectory()) {
      throw new RuntimeException(
          String.format("Destination directory (%s) is not valid.", args[2]));
    }
  }
}
