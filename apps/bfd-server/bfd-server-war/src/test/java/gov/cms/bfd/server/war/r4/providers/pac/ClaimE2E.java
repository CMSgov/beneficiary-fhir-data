package gov.cms.bfd.server.war.r4.providers.pac;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import gov.cms.bfd.model.rda.Mbi;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.ServerRequiredTest;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Claim;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Endpoint end-to-end test for the Claim endpoint. */
public class ClaimE2E extends ServerRequiredTest {

  /** Test utils. */
  private static final RDATestUtils rdaTestUtils = new RDATestUtils();

  /** The base claim endpoint. */
  private static String claimEndpoint;

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
   * Tests invalid claim IDs, for FISS / MCS claim {@link Claim} is looked up with and got 400
   * response with FHIR OperationOutcome-> diagnostics match ID validation error message.
   */
  @Test
  public void testGetClaimResourceById400Error() {
    List<String> invalid_claim_ids =
        Arrays.asList(
            "F-LTA0M2E0NWY5ZGU3ZjI5MzJmYWFiYmI", // upper case resource type
            "f-MWkrjfrkejfk-98000", // dash in id value
            "f123456", // no separator '-' between resource type and id value
            "fLTA0M2E0NWY5ZGU3ZjI5MzJmYWFiYmI", // no separator
            "-m-XYZ001", // malformed mcs id
            "m----123456", // extra dash mcs id value part
            "M-123456", // upper case resource type
            "K-H123456", // invalid resource type 'K'
            "f-1*23456", // star char in id value
            "m-1*23456"); // invalid chars in mcs id value
    for (String idStr : invalid_claim_ids) {
      String requestString = claimEndpoint + idStr;
      String resp = getResponseByIDWith400Error(requestString);
      Assertions.assertTrue(resp.contains("OperationOutcome"));
      Assertions.assertTrue(resp.contains("error"));
      Assertions.assertTrue(resp.contains("diagnostics"));
      Assertions.assertTrue(
          resp.contains(
              "ID pattern: '"
                  + idStr
                  + "' does not match expected pattern: {singleCharacter}-{claimIdNumber}"));
    }
  }

  /**
   * Tests wel-formed but non-exist claim IDs, for FISS / MCS claim {@link Claim} is looked up using
   * a well-formed claim ID, expect 404 with FHIR OperationOutcome.
   */
  @Test
  public void testGetClaimResourceById404Error() {
    List<String> invalid_claim_ids = Arrays.asList("f-123456nonexist", "m-654321nonexist");
    for (String idStr : invalid_claim_ids) {
      String requestString = claimEndpoint + idStr;
      String resp = getResponseByIDWith404Error(requestString);
      Assertions.assertTrue(resp.contains("OperationOutcome"));
      Assertions.assertTrue(resp.contains("error"));
      Assertions.assertTrue(resp.contains("diagnostics"));
      Assertions.assertTrue(resp.contains(idStr + " is not known"));
    }
  }

  /**
   * Tests to see if nonsensitive MBI identifiers (hash and ID) are logged to the MDC when a FISS
   * {@link Claim} with an associated {@link Mbi} is looked up by a specific ID.
   */
  @Test
  void testClaimReadLogsMbiIdentifiersForFissClaimWithMbiRecord() throws IOException {
    String requestString = claimEndpoint + "f-123456";

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
   * {@link Claim} with an associated {@link Mbi} is looked up by a specific ID.
   */
  @Test
  void testClaimReadLogsMbiIdentifiersForMcsClaimWithMbiRecord() throws IOException {
    String requestString = claimEndpoint + "m-654321";

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
   * Verify that Claim throws a 400 error when the paging start (startIndex) is set higher than the
   * maximum number of results.
   */
  @Test
  public void testClaimFindByPatientWithPagingStartBeyondMaxExpect400() {
    String requestString =
        claimEndpoint + "?mbi=" + RDATestUtils.MBI_OLD_HASH + "&_count=2&startIndex=12";

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
   * Verify that Claim throws a 400 error when the paging start (startIndex) is set to the maximum
   * number of results, since the highest index must be less than the number of results as a 0-based
   * index.
   */
  @Test
  public void testClaimFindByPatientWithPagingStartSetToMaxResultsExpect400() {
    String requestString =
        claimEndpoint + "?mbi=" + RDATestUtils.MBI_OLD_HASH + "&_count=2&startIndex=4";

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
   * Verify that Claim does not error when the paging start (startIndex) is set to be equal to one
   * less than the maximum number of results.
   */
  @Test
  public void testClaimFindByPatientWithPagingStartOneLessThanMaxExpect200() {
    String requestString =
        claimEndpoint + "?mbi=" + RDATestUtils.MBI_OLD_HASH + "&_count=2&startIndex=3";

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // since we start on the last item's index with 2 items per page, 1 item returned
        .body("entry.size()", equalTo(1))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verify the response totals are accurate when SAMHSA data is excluded and the client is not
   * allowed to see SAMHSA data.
   */
  @Test
  public void testClaimFindByPatientWithExcludeSamhsaTrue() {
    String requestString =
        claimEndpoint + "?mbi=" + RDATestUtils.MBI_OLD_HASH + "&excludeSAMHSA=true";

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // since we start on the last item's index with 2 items per page, 1 item returned
        .body("entry.size()", equalTo(4))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verify the response totals are accurate when SAMHSA data is not excluded and the client is
   * allowed to see SAMHSA data.
   */
  @Test
  public void testClaimFindByPatientWithSamhsaAllowed() {
    String requestString = claimEndpoint + "?mbi=" + RDATestUtils.MBI_OLD_HASH;

    given()
        .spec(getRequestAuth(SAMHSA_KEYSTORE))
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // since we start on the last item's index with 2 items per page, 1 item returned
        .body("entry.size()", equalTo(6))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verify the response totals are accurate when SAMHSA data is excluded and the client is allowed
   * to see SAMHSA data.
   */
  @Test
  public void testClaimFindByPatientWithSamhsaAllowedAndExcludeSamhsaTrue() {
    String requestString =
        claimEndpoint + "?mbi=" + RDATestUtils.MBI_OLD_HASH + "&excludeSAMHSA=true";

    given()
        .spec(getRequestAuth(SAMHSA_KEYSTORE))
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // since we start on the last item's index with 2 items per page, 1 item returned
        .body("entry.size()", equalTo(4))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verify that Claim logs nonsensitive MBI identifiers (hash and ID) to the MDC log when receiving
   * a valid Claim request.
   */
  @Test
  public void testClaimFindByPatientLogsMbiIdentifiers() throws IOException {
    String requestString = claimEndpoint + "?mbi=" + RDATestUtils.MBI_HASH;

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
  public void testClaimFindByPatientWithNoResultsAndPaginationRequestedExpect200() {
    String requestString = claimEndpoint + "?mbi=1111111111111&_count=50";

    given()
        .spec(requestAuth)
        .expect()
        .body("entry", equalTo(null))
        .statusCode(200)
        .when()
        .get(requestString);

    // check with startIndex as well
    requestString = claimEndpoint + "?mbi=1111111111111&startIndex=2";

    given()
        .spec(requestAuth)
        .expect()
        .body("entry", equalTo(null))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies the Claim response for the given requestString returns a 400 and the json response of
   * FHIR OperationOutcome.
   *
   * @param requestString the request string to search with.
   * @return the string of the response.
   */
  private String getResponseByIDWith400Error(String requestString) {
    return getResponseByIDWith4XX(requestString, 400);
  }

  /**
   * Verifies the Claim response for the given requestString returns a 404 and the json response of
   * FHIR OperationOutcome.
   *
   * @param requestString the request string to search with.
   * @return the string of the response.
   */
  private String getResponseByIDWith404Error(String requestString) {
    return getResponseByIDWith4XX(requestString, 404);
  }

  /**
   * Helper common func for 4XX get response.
   *
   * @param requestString - request url
   * @param expected4XXCode - the expected error code
   * @return string of the response json.
   */
  private String getResponseByIDWith4XX(String requestString, int expected4XXCode) {
    return given()
        .spec(requestAuth)
        .expect()
        .statusCode(expected4XXCode)
        .when()
        .get(requestString)
        .then()
        .extract()
        .response()
        .asString();
  }
}
