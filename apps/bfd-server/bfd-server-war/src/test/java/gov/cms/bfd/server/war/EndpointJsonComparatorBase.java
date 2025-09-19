package gov.cms.bfd.server.war;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.specification.RequestSpecification;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Holds shared functionality between endpoint comparator tests in v1/v2. */
public abstract class EndpointJsonComparatorBase extends ServerRequiredTest {

  /** Test to use for an ignored field. */
  protected static final String IGNORED_FIELD_TEXT = "IGNORED_FIELD";

  /**
   * Generates the "golden" files, i.e. the approved responses to compare to. Purpose of this
   * testing is to perform regression testing against the "Golden Beneficiary Data" at a specific
   * point in time. It is important to note that this testing focuses on checking for regressions
   * against the data at that particular moment, and not necessarily against data artifacts. To run
   * this test, execute the following Maven Command: mvn clean install -DgenerateTestData=true.
   *
   * @param endpointId the endpoint id
   * @param endpointOperation the endpoint operation
   */
  @EnabledIfSystemProperty(named = "generateTestData", matches = "true")
  @ParameterizedTest(name = "endpointId = {0}")
  @MethodSource("data")
  public void generateApprovedResponseFiles(String endpointId, Supplier<String> endpointOperation) {
    Path approvedResponseDir = getExpectedJsonResponseDir();

    // Call the server endpoint and save its result out to a file corresponding to
    // the endpoint Id.
    String endpointResponse = endpointOperation.get();
    String jsonResponse = replaceIgnoredFieldsWithFillerText(endpointId, endpointResponse);

    ServerTestUtils.writeFile(
        jsonResponse,
        ServerTestUtils.generatePathForEndpointJsonFile(approvedResponseDir, endpointId));
  }

  /**
   * Generates current endpoint response files, comparing them to the corresponding approved
   * responses.
   *
   * <p>If you've recently made mapping changes, and this test fails, you may need to re-generate
   * the expected response files by building the project with the maven parameter
   * -DgenerateTestData=true which will run the test that generates new response files. This should
   * only be done if you're sure your mapping changes are correct, as these files are used for
   * regression testing future changes. Once generated, these newly minted files should be checked
   * in with your mapping changes.
   *
   * @param endpointId the endpoint id
   * @param endpointOperation the endpoint operation
   */
  @ParameterizedTest(name = "endpointId = {0}")
  @MethodSource("data")
  public void verifyCorrectEndpointResponse(String endpointId, Supplier<String> endpointOperation) {
    String endpointResponse = endpointOperation.get();
    assertJsonDiffIsEmpty(endpointId, endpointResponse);
  }

  /**
   * Compares the approved and current responses for an endpoint.
   *
   * @param endpointId the name of the operation being tested, used to determine which files to
   *     compare
   * @param endpointResponse the endpoint response
   */
  public void assertJsonDiffIsEmpty(String endpointId, String endpointResponse) {
    String expectedJson =
        ServerTestUtils.readFile(
            ServerTestUtils.generatePathForEndpointJsonFile(
                getExpectedJsonResponseDir(), endpointId));
    String replacedResponse = replaceIgnoredFieldsWithFillerText(endpointId, endpointResponse);
    assertEquals(expectedJson, replacedResponse);
  }

  /**
   * Gets the json response for the specified endpoint request string and default content type json
   * header.
   *
   * @param requestString the request string
   * @return the json response for
   */
  protected static String getJsonResponseFor(String requestString) {
    return getJsonResponseFor(
        requestString, new Headers(new Header("Content-Type", "application/json+fhir")));
  }

  /**
   * Gets the json response for the specified endpoint request string and default content type json
   * header.
   *
   * @param requestString the request string
   * @param requestSpec the request Spec
   * @return the json response for
   */
  protected static String getJsonResponseFor(
      String requestString, RequestSpecification requestSpec) {
    return getJsonResponseFor(
        requestString,
        new Headers(new Header("Content-Type", "application/json+fhir")),
        requestSpec);
  }

  /**
   * Gets the json response for the specified endpoint request string and specified headers.
   *
   * @param requestString the request string
   * @param headers the headers for the request
   * @param requestSpec the request Specification
   * @return the json response for
   */
  protected static String getJsonResponseFor(
      String requestString, Headers headers, RequestSpecification requestSpec) {
    return given()
        .spec(requestSpec)
        .headers(headers)
        .expect()
        .log()
        .ifError()
        .statusCode(200)
        .when()
        .get(requestString)
        .then()
        .extract()
        .response()
        .asString();
  }

  /**
   * Gets the json response for the specified endpoint request string and specified headers.
   *
   * @param requestString the request string
   * @param headers the headers for the request
   * @return the json response for
   */
  protected static String getJsonResponseFor(String requestString, Headers headers) {
    return getJsonResponseFor(requestString, headers, requestAuth);
  }

  /**
   * Gets the expected json response directory. These are generated as a point in time comparison
   * when we know the responses are in a good state so that the json response tests can regression
   * test against them.
   *
   * <p>Should be overridden in the version specific class.
   *
   * @return the expected json response dir
   */
  protected abstract Path getExpectedJsonResponseDir();

  /**
   * Replaces ignored fields with filler text.
   *
   * @param endpointId the {@link String} endpoint
   * @param endpointResponse the {@link String} endpoint response JSON
   * @return the replaced string
   */
  protected static String replaceIgnoredFieldsWithFillerText(
      String endpointId, String endpointResponse) {

    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonNode;
    try {
      jsonNode = mapper.readTree(endpointResponse);
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + endpointResponse, e);
    }

    replaceIgnoredFieldsWithFillerText(
        jsonNode,
        "id",
        Optional.of(
            Pattern.compile(
                "[A-Za-z0-9]{8}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{12}")));
    replaceIgnoredFieldsWithFillerText(
        jsonNode,
        "reference",
        Optional.of(
            Pattern.compile(
                "#[A-Za-z0-9]{8}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{12}")));
    replaceIgnoredFieldsWithFillerText(
        jsonNode, "url", Optional.of(Pattern.compile("(https://localhost:)(\\d+(?=/))(.*)")));
    replaceIgnoredFieldsWithFillerText(jsonNode, "lastUpdated", Optional.empty());
    replaceIgnoredFieldsWithFillerText(jsonNode, "created", Optional.empty());

    if (endpointId.equals("metadata")) {
      replaceIgnoredFieldsWithFillerText(jsonNode, "date", Optional.empty());
      replaceIgnoredFieldsWithFillerText(
          jsonNode, "version", Optional.of(Pattern.compile("\\d+\\.\\d+\\.\\d+(?:-SNAPSHOT)?")));
    }

    String jsonResponse;
    try {
      jsonResponse = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNode);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(
          "Unable to deserialize the following JSON content as tree: " + endpointResponse, e);
    }
    return jsonResponse;
  }

  /**
   * Replaces ignored fields with filler text.
   *
   * @param parent the {@link JsonNode} on which to perform the replacement
   * @param fieldName the {@link String} name of the field that is being replaced
   * @param pattern an optional {@link Pattern} pattern to correctly identify fields needing to be
   *     replaced
   */
  private static void replaceIgnoredFieldsWithFillerText(
      JsonNode parent, String fieldName, Optional<Pattern> pattern) {
    if (parent.has(fieldName)) {
      if (pattern.isPresent()) {
        Pattern p = pattern.get();
        Matcher m = p.matcher(parent.get(fieldName).toString());
        if (m.find()) {
          if (fieldName.equals("url")) {
            // Only replace the port numbers (m.group(2)) on urls
            String replacementUrl = m.group(1) + IGNORED_FIELD_TEXT + m.group(3);
            ((ObjectNode) parent)
                .put(fieldName, replacementUrl.substring(0, replacementUrl.length() - 1));
          } else {
            ((ObjectNode) parent).put(fieldName, IGNORED_FIELD_TEXT);
          }
        }
      } else ((ObjectNode) parent).put(fieldName, IGNORED_FIELD_TEXT);
    }

    // Now, recursively invoke this method on all properties
    for (JsonNode child : parent) {
      replaceIgnoredFieldsWithFillerText(child, fieldName, pattern);
    }
  }

  /**
   * Gets the first sample A beneficiary, and loads enrichment data.
   *
   * @return the first sample A beneficiary
   */
  protected static Beneficiary getSampleABene() {
    List<Object> loadedRecords = ServerTestUtils.get().loadSampleAData();
    ServerTestUtils.get().loadEnrichmentData();
    return ServerTestUtils.get().getFirstBeneficiary(loadedRecords);
  }

  /**
   * Gets the first sample A beneficiary, and loads enrichment data.
   *
   * @return the first sample A beneficiary
   */
  protected static Beneficiary getSampleABeneSamhsa() {
    List<Object> loadedRecords = ServerTestUtils.get().loadSampleASamhsaData();
    ServerTestUtils.get().loadEnrichmentData();
    return ServerTestUtils.get().getFirstBeneficiary(loadedRecords);
  }

  /**
   * Gets the first sample A beneficiary.
   *
   * @return the first sample A beneficiary
   */
  protected static Beneficiary getSampleABeneWithoutRefYear() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(
                Arrays.asList(
                    StaticRifResourceGroup.SAMPLE_A_WITHOUT_REFERENCE_YEAR.getResources()));
    return ServerTestUtils.get().getFirstBeneficiary(loadedRecords);
  }
}
