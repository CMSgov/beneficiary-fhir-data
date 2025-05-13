package gov.cms.bfd.server.war.r4.providers.pac;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import gov.cms.bfd.server.war.EndpointJsonComparatorBase;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Claim;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * This set of tests compare the application's current responses to a set of previously-recorded
 * responses. This achieves several goals:
 *
 * <ul>
 *   <li>It helps us to ensure that we're not accidentally changing the application's responses
 *   <li>It helps us to maintain backwards compatibility.
 *   <li>As any changes in an operation's output will have to include a change to the recorded
 *       response, it makes it much easier to tell what our PRs are actually doing.
 * </ul>
 *
 * <p>There SHALL be a 1:1 relationship between test cases here and the application's operations;
 * every supported operation should have a test case.
 *
 * <p>Note that our responses include timestamps and have other differences from request to request
 * (e.g. element ordering). Each test case must ignore or otherwise work around such differences so
 * that tests work reliably.
 *
 * <p>To re-generate the recorded responses, build the application with -DgenerateTestData=true
 * which will run the test that creates the endpoint responses.
 */
public class EndpointJsonResponseComparatorV2ClaimE2E extends EndpointJsonComparatorBase {
  /** Test utils. */
  private static final RDATestUtils rdaTestUtils = new RDATestUtils();

  /** The base claim endpoint. */
  private static String claimEndpoint;

  /** A base ignore pattern for testing the read endpoint responses against an expected file. */
  private static final Set<String> READ_IGNORE_PATTERNS =
      Set.of("/link/[0-9]+/url", "/created", "/meta/lastUpdated");

  /** A base ignore pattern for testing the search by mbi responses against an expected file. */
  private static final Set<String> MBI_IGNORE_PATTERNS =
      Set.of(
          "/link/[0-9]+/url",
          "/created",
          "/meta/lastUpdated",
          "/id",
          "/entry/[0-9]+/resource/created");

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    // Set this up once, after the server has run
    if (claimEndpoint == null) {
      rdaTestUtils.init();
      rdaTestUtils.seedData(true);
      claimEndpoint = baseServerUrl + "/v2/fhir/Claim/";
    }
  }

  /** Cleans up the test data. */
  @AfterAll
  public static void tearDown() {
    rdaTestUtils.truncateTables();
    rdaTestUtils.destroy();
  }

  /**
   * Returns data for parameterized tests.
   *
   * @return the data
   */
  public static Stream<Arguments> data() {

    return Stream.of(
        arguments(
            "claimFissRead",
            (Supplier<String>)
                EndpointJsonResponseComparatorV2ClaimE2E::shouldGetCorrectFissClaimResourceById),
        arguments(
            "claimFissReadWithTaxNumbers",
            (Supplier<String>)
                EndpointJsonResponseComparatorV2ClaimE2E
                    ::shouldGetCorrectFissClaimResourceByIdWithTaxNumbers),
        arguments(
            "claimMcsRead",
            (Supplier<String>)
                EndpointJsonResponseComparatorV2ClaimE2E::shouldGetCorrectMcsClaimResourceById),
        arguments(
            "claimMcsReadWithTaxNumbers",
            (Supplier<String>)
                EndpointJsonResponseComparatorV2ClaimE2E
                    ::shouldGetCorrectMcsClaimResourceByIdWithTaxNumbers),
        arguments(
            "claimSearch",
            (Supplier<String>)
                EndpointJsonResponseComparatorV2ClaimE2E::shouldGetCorrectClaimResourcesByMbiHash),
        arguments(
            "claimSearchWithTaxNumbers",
            (Supplier<String>)
                EndpointJsonResponseComparatorV2ClaimE2E
                    ::shouldGetCorrectClaimResourcesByMbiHashWithTaxNumbers),
        arguments(
            "claimSearchPaginated",
            (Supplier<String>)
                EndpointJsonResponseComparatorV2ClaimE2E
                    ::shouldGetCorrectClaimResourcesByMbiHashWithPagination),
        arguments(
            "claimSearchPost",
            (Supplier<String>)
                EndpointJsonResponseComparatorV2ClaimE2E::shouldGetCorrectClaimResourcesByMbiPost));
  }

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
   * Tests to see if the correct response is given when a FISS {@link Claim} is looked up by a
   * specific ID.
   */
  private static String shouldGetCorrectFissClaimResourceById() {
    String requestString = claimEndpoint + "f-123456";

    return getPacRequest(requestString, false);
  }

  /**
   * Tests to see if the correct response is given when a FISS {@link Claim} is looked up by a
   * specific ID with tax numbers included.
   */
  public static String shouldGetCorrectFissClaimResourceByIdWithTaxNumbers() {
    String requestString = claimEndpoint + "f-123456";

    return getPacRequest(requestString, true);
  }

  /**
   * Tests to see if the correct response is given when an MCS {@link Claim} is looked up by a
   * specific ID.
   */
  public static String shouldGetCorrectMcsClaimResourceById() {
    String requestString = claimEndpoint + "m-654321";

    return getPacRequest(requestString, false);
  }

  /**
   * Tests to see if the correct response is given when an MCS {@link Claim} is looked up by a
   * specific ID with tax numbers included.
   */
  private static String shouldGetCorrectMcsClaimResourceByIdWithTaxNumbers() {
    String requestString = claimEndpoint + "m-654321";

    return getPacRequest(requestString, true);
  }

  /**
   * Tests to see if the correct response is given when a search is done for {@link Claim}s using
   * given mbi and service-date range. In this test case the query finds the matched claims because
   * their to dates are within the date range even though their from dates are not.
   */
  private static String shouldGetCorrectClaimResourcesByMbiHash() {
    String requestString =
        claimEndpoint
            + "?mbi="
            + RDATestUtils.MBI_HASH
            + "&service-date=gt1970-07-18&service-date=lt1970-07-25";

    return getPacRequest(requestString, false);
  }

  /**
   * Tests to see if the correct response is given when a search is done for {@link Claim}s using
   * given mbi and service-date range with tax numbers included. In this test case the query finds
   * the matched claims because their to dates are within the date range even though their from
   * dates are not.
   */
  private static String shouldGetCorrectClaimResourcesByMbiHashWithTaxNumbers() {
    String requestString =
        claimEndpoint
            + "?mbi="
            + RDATestUtils.MBI_HASH
            + "&service-date=gt1970-07-18&service-date=lt1970-07-25";

    return getPacRequest(requestString, true);
  }

  /**
   * Tests to see if the correct paginated response is given when a search is done for {@link
   * Claim}s using given mbi and service-date range. In this test case the query finds the matched
   * claims because their from dates are within the date range even though their to dates are not.
   */
  private static String shouldGetCorrectClaimResourcesByMbiHashWithPagination() {
    String requestString =
        claimEndpoint
            + "?mbi="
            + RDATestUtils.MBI_HASH
            + "&service-date=ge1970-07-10&service-date=le1970-07-18"
            + "&_count=5&startIndex=1";

    return getPacRequest(requestString, false);
  }

  /** Tests the search response when using a POST request. */
  private static String shouldGetCorrectClaimResourcesByMbiPost() {
    // Sending multiple entries with the same key doesn't appear to work in the POST body
    String requestString = claimEndpoint + "_search";

    // NOTE: Sending multiple values only works with List<String> as the map value, not String[]
    // because Rest Assured specifically looks for types that extend Collection
    return postPacRequest(
        requestString,
        Map.of(
            "mbi",
            List.of(RDATestUtils.MBI),
            "isHashed",
            List.of("false"),
            "service-date",
            List.of("gt1970-07-18", "lt1970-07-25")),
        false);
  }

  /**
   * Verifies the Claim response for the given requestString returns a 200 and the json response
   * matches the expected response file.
   *
   * @param requestString the request string to search with
   * @param formParams form POST params
   * @param includeTaxNumbers the value to use for IncludeTaxNumbers header
   */
  private static String postPacRequest(
      String requestString, Map<String, ?> formParams, boolean includeTaxNumbers) {

    return given()
        .spec(requestAuth)
        .formParams(formParams)
        .header(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, includeTaxNumbers)
        .expect()
        .statusCode(200)
        .when()
        .post(requestString)
        .then()
        .extract()
        .response()
        .asString();
  }

  /**
   * Verifies the Claim response for the given requestString returns a 200 and the json response
   * matches the expected response file.
   *
   * @param requestString the request string to search with
   * @param includeTaxNumbers the value to use for IncludeTaxNumbers header
   */
  private static String getPacRequest(String requestString, boolean includeTaxNumbers) {

    return given()
        .spec(requestAuth)
        .header(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, includeTaxNumbers)
        .expect()
        .statusCode(200)
        .when()
        .get(requestString)
        .then()
        .extract()
        .response()
        .asString();
  }

  /** {@inheritDoc} */
  @Override
  protected Path getExpectedJsonResponseDir() {
    Path approvedResponseDir =
        Paths.get("..", "src", "test", "resources", "endpoint-responses", "v2");
    if (!Files.isDirectory(approvedResponseDir)) {
      approvedResponseDir = Paths.get("src", "test", "resources", "endpoint-responses", "v2");
    }
    if (!Files.isDirectory(approvedResponseDir)) {
      throw new IllegalStateException();
    }

    return approvedResponseDir;
  }
}
