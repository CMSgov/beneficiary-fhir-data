package gov.cms.bfd.server.war.r4.providers.pac;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.ServerRequiredTest;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.utils.AssertUtils;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Endpoint end-to-end test for the ClaimResponse endpoint. */
public class ClaimResponseE2E extends ServerRequiredTest {

  /** Test utils. */
  private static final RDATestUtils rdaTestUtils = new RDATestUtils();

  /** The base claim response endpoint. */
  private static String claimResponseEndpoint;

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
    if (claimResponseEndpoint == null) {
      rdaTestUtils.init();
      rdaTestUtils.seedData(true);
      claimResponseEndpoint = baseServerUrl + "/v2/fhir/ClaimResponse/";
    }
  }

  /** Cleans up the test data. */
  @AfterAll
  public static void tearDown() {
    rdaTestUtils.truncateTables();
    rdaTestUtils.destroy();
  }

  /**
   * Tests to see if the correct response is given when a FISS {@link ClaimResponse} is looked up by
   * a specific ID.
   */
  @Test
  void shouldGetCorrectFissClaimResponseResourceById() {
    String requestString = claimResponseEndpoint + "f-123456";

    verifyResponseMatchesFor(requestString, "claimResponseFissRead", READ_IGNORE_PATTERNS);
  }

  /**
   * Tests to see if the correct response is given when an MCS {@link ClaimResponse} is looked up by
   * a specific ID.
   */
  @Test
  void shouldGetCorrectMcsClaimResponseResourceById() {
    String requestString = claimResponseEndpoint + "m-654321";

    verifyResponseMatchesFor(requestString, "claimResponseMcsRead", READ_IGNORE_PATTERNS);
  }

  /**
   * Tests to see if nonsensitive MBI identifiers (hash and ID) are logged to the MDC when a FISS
   * {@link ClaimResponse} with an associated {@link Mbi} is looked up by a specific ID.
   */
  @Test
  void testClaimResponseReadLogsMbiIdentifiersForFissClaimWithMbiRecord() throws IOException {
    String requestString = claimResponseEndpoint + "f-123456";

    Mbi testingMbi = rdaTestUtils.lookupTestMbiRecord(rdaTestUtils.getEntityManager());
    ServerTestUtils.assertMdcEntries(
        requestAuth,
        requestString,
        Map.of(
            BfdMDC.MBI_HASH,
            Optional.of(testingMbi.getHash()),
            BfdMDC.MBI_ID,
            Optional.of(testingMbi.getMbiId().toString()),
            // This isn't a default key, but we need to include it without checking its value to
            // satisfy the assertion for extra, unexpected MDC keys
            BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_CONTENT_LOCATION,
            Optional.empty()));
  }

  /**
   * Tests to see if nonsensitive MBI identifiers (hash and ID) are logged to the MDC when an MCS
   * {@link ClaimResponse} with an associated {@link Mbi} is looked up by a specific ID.
   */
  @Test
  void testClaimResponseReadLogsMbiIdentifiersForMcsClaimWithMbiRecord() throws IOException {
    String requestString = claimResponseEndpoint + "m-654321";

    Mbi testingMbi = rdaTestUtils.lookupTestMbiRecord(rdaTestUtils.getEntityManager());
    ServerTestUtils.assertMdcEntries(
        requestAuth,
        requestString,
        Map.of(
            BfdMDC.MBI_HASH,
            Optional.of(testingMbi.getHash()),
            BfdMDC.MBI_ID,
            Optional.of(testingMbi.getMbiId().toString()),
            // This isn't a default key, but we need to include it without checking its value to
            // satisfy the assertion for extra, unexpected MDC keys
            BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_CONTENT_LOCATION,
            Optional.empty()));
  }

  /**
   * Tests to see if the correct response is given when a search is done for {@link ClaimResponse}s
   * using given mbi and service-date range. In this test case the query finds the matched claims
   * because their to dates are within the date range even though their from dates are not.
   */
  @Test
  void shouldGetCorrectClaimResponseResourcesByMbiHash() {
    String requestString =
        claimResponseEndpoint
            + "?mbi="
            + RDATestUtils.MBI_OLD_HASH
            + "&service-date=gt1970-07-18&service-date=lt1970-07-25";

    verifyResponseMatchesFor(requestString, "claimResponseSearch", MBI_IGNORE_PATTERNS);
  }

  /** Tests the search endpoint using a POST request. */
  @Test
  void shouldGetCorrectClaimResponseResourcesByMbiPost() {
    String requestString = claimResponseEndpoint + "_search";

    verifyPostResponseMatchesFor(
        requestString,
        Map.of(
            "mbi",
            List.of(RDATestUtils.MBI),
            "isHashed",
            "false",
            "service-date",
            List.of("gt1970-07-18", "lt1970-07-25")),
        "claimResponseSearch",
        MBI_IGNORE_PATTERNS);
  }

  /**
   * Tests to see if a valid response is given when a search is done for {@link ClaimResponse}s
   * using given mbi and excludeSAMHSA=true, since this does an extra check for samhsa data.
   */
  @Test
  void shouldGetClaimResponseResourcesByMbiHashWithExcludeSamhsaTrue() {
    String requestString =
        claimResponseEndpoint
            + "?mbi="
            + RDATestUtils.MBI_OLD_HASH
            + "&service-date=gt1970-07-18&service-date=lt1970-07-25"
            + "&excludeSAMHSA=true";

    // Test passes as long as we get a 200 with an entry and not an error
    given()
        .spec(requestAuth)
        .expect()
        .statusCode(200)
        .body("entry.size()", equalTo(2))
        .when()
        .get(requestString);
  }

  /**
   * Tests to see if the response size is accurate when SAMHSA is not excluded and the client can
   * see SAMHSA data.
   */
  @Test
  void shouldGetClaimResponseResourcesByMbiHashWithSamhsaAllowed() {
    String requestString =
        claimResponseEndpoint
            + "?mbi="
            + RDATestUtils.MBI_OLD_HASH
            + "&service-date=gt1970-07-18&service-date=lt1970-07-25";

    // Test passes as long as we get a 200 with an entry and not an error
    given()
        .spec(getRequestAuth(SAMHSA_KEYSTORE))
        .expect()
        .statusCode(200)
        .body("entry.size()", equalTo(3))
        .when()
        .get(requestString);
  }

  /**
   * Tests to see if the response size is accurate when SAMHSA is excluded and the client can see
   * SAMHSA data.
   */
  @Test
  void shouldGetClaimResponseResourcesByMbiHashWithSamhsaAllowedAndExcludeSamhsaTrue() {
    String requestString =
        claimResponseEndpoint
            + "?mbi="
            + RDATestUtils.MBI_OLD_HASH
            + "&service-date=gt1970-07-18&service-date=lt1970-07-25"
            + "&excludeSAMHSA=true";

    // Test passes as long as we get a 200 with an entry and not an error
    given()
        .spec(getRequestAuth(SAMHSA_KEYSTORE))
        .expect()
        .statusCode(200)
        .body("entry.size()", equalTo(2))
        .when()
        .get(requestString);
  }

  /**
   * Tests to see if the correct paginated response is given when a search is done for {@link
   * ClaimResponse}s using given mbi and service-date range. In this test case the query finds the
   * matched claims because their from dates are within the date range even though their to dates
   * are not.
   */
  @Test
  void shouldGetCorrectClaimResponseResourcesByMbiHashWithPagination() {
    String requestString =
        claimResponseEndpoint
            + "?mbi="
            + RDATestUtils.MBI_OLD_HASH
            + "&service-date=ge1970-07-10&service-date=le1970-07-18"
            + "&_count=5&startIndex=1";

    verifyResponseMatchesFor(requestString, "claimResponseSearchPaginated", MBI_IGNORE_PATTERNS);
  }

  /**
   * Verify that ClaimResponse throws a 400 error when the paging start (startIndex) is set higher
   * than the maximum number of results.
   */
  @Test
  public void testClaimResponseFindByPatientWithPagingStartBeyondMaxExpect400() {
    String requestString =
        claimResponseEndpoint + "?mbi=" + RDATestUtils.MBI_OLD_HASH + "&_count=2&startIndex=12";

    given()
        .spec(requestAuth)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem("Value for startIndex (12) must be less than than result size (4)"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verify that ClaimResponse throws a 400 error when the paging start (startIndex) is set to the
   * maximum number of results, since the highest index must be less than the number of results as a
   * 0-based index.
   */
  @Test
  public void testClaimResponseFindByPatientWithPagingStartSetToMaxResultsExpect400() {
    String requestString =
        claimResponseEndpoint + "?mbi=" + RDATestUtils.MBI_OLD_HASH + "&_count=2&startIndex=4";

    given()
        .spec(requestAuth)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem("Value for startIndex (4) must be less than than result size (4)"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verify that ClaimResponse does not error when the paging start (startIndex) is set to be equal
   * to one less than the maximum number of results.
   */
  @Test
  public void testClaimResponseFindByPatientWithPagingStartOneLessThanMaxExpect200() {
    String requestString =
        claimResponseEndpoint + "?mbi=" + RDATestUtils.MBI_OLD_HASH + "&_count=2&startIndex=3";

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .body()
        .body("resourceType", equalTo("Bundle"))
        // sine we start on the last item's index with 2 items per page, 1 item returned
        .body("entry.size()", equalTo(1))
        // 4 items total reported on all pages
        .body("total", equalTo(4))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verify that ClaimResponse logs nonsensitive MBI identifiers (hash and ID) to the MDC log when
   * receiving a valid ClaimResponse request.
   */
  @Test
  public void testClaimResponseFindByPatientLogsMbiIdentifiers() throws IOException {
    String requestString = claimResponseEndpoint + "?mbi=" + RDATestUtils.MBI_HASH;

    Mbi testingMbi = rdaTestUtils.lookupTestMbiRecord(rdaTestUtils.getEntityManager());
    ServerTestUtils.assertMdcEntries(
        requestAuth,
        requestString,
        Map.of(
            BfdMDC.MBI_HASH,
            Optional.of(testingMbi.getHash()),
            BfdMDC.MBI_ID,
            Optional.of(testingMbi.getMbiId().toString()),
            // This isn't a default key, but we need to include it without checking its value to
            // satisfy the assertion for extra, unexpected MDC keys
            BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_PER_KB,
            Optional.empty()));
  }

  /**
   * Verify that an empty bundle is returned when pagination is requested but no results are
   * returned. Normally this would return a 400 since the default startIndex is equal to the number
   * of results, but we make a special exception for empty returns since there's nothing to paginate
   * anyway.
   */
  @Test
  public void testClaimResponseFindByPatientWithNoResultsAndPaginationRequestedExpect200() {
    String requestString = claimResponseEndpoint + "?mbi=1111111111111&_count=50";

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .ifError()
        .body("total", equalTo(0))
        .statusCode(200)
        .when()
        .get(requestString);

    // check with startIndex as well
    requestString = claimResponseEndpoint + "?mbi=1111111111111&startIndex=2";

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .ifError()
        .body("total", equalTo(0))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies the ClaimResponse response for the given requestString returns a 200 and the json
   * response matches the expected response file.
   *
   * @param requestString the request string to search with
   * @param expectedResponseFileName the name of the response file to compare against
   * @param ignorePatterns the ignore patterns to use when comparing the result file to the response
   */
  private void verifyResponseMatchesFor(
      String requestString, String expectedResponseFileName, Set<String> ignorePatterns) {

    String response =
        given()
            .spec(requestAuth)
            .expect()
            .statusCode(200)
            .when()
            .get(requestString)
            .then()
            .extract()
            .response()
            .asString();

    String expected = rdaTestUtils.expectedResponseFor(expectedResponseFileName);

    AssertUtils.assertJsonEquals(expected, response, ignorePatterns);
  }

  /**
   * Verifies the ClaimResponse response for the given requestString returns a 200 and the json
   * response matches the expected response file.
   *
   * @param requestString the request string to search with
   * @param expectedResponseFileName the name of the response file to compare against
   * @param ignorePatterns the ignore patterns to use when comparing the result file to the response
   */
  private void verifyPostResponseMatchesFor(
      String requestString,
      Map<String, ?> formParams,
      String expectedResponseFileName,
      Set<String> ignorePatterns) {

    String response =
        given()
            .spec(requestAuth)
            .formParams(formParams)
            .expect()
            .statusCode(200)
            .when()
            .post(requestString)
            .then()
            .extract()
            .response()
            .asString();

    String expected = rdaTestUtils.expectedResponseFor(expectedResponseFileName);

    AssertUtils.assertJsonEquals(expected, response, ignorePatterns);
  }
}
