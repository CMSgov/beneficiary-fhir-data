package gov.cms.bfd.server.war.r4.providers.pac;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import gov.cms.bfd.server.war.ServerRequiredTest;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Endpoint end-to-end test for the ClaimResponse endpoint. */
public class ClaimResponseE2E extends ServerRequiredTest {

  /** Test utils. */
  private static final RDATestUtils rdaTestUtils = new RDATestUtils();

  /** The base claim response endpoint. */
  private static String claimResponseEndpoint;

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
}