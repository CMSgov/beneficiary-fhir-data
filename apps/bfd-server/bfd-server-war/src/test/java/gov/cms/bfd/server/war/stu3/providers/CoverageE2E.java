package gov.cms.bfd.server.war.stu3.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import gov.cms.bfd.server.war.ServerRequiredTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Endpoint end-to-end test for the V1 Coverage endpoint. */
public class CoverageE2E extends ServerRequiredTest {

  /** The base coverage endpoint. */
  private static String coverageEndpoint;

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    // Set this up once, after the server has run
    if (coverageEndpoint == null) {
      coverageEndpoint = baseServerUrl + "/v1/fhir/Coverage/";
    }
  }

  /**
   * Verify that Coverage throws a 400 error when the paging start (startIndex) is set higher than
   * the maximum number of results.
   */
  @Test
  public void testCoverageSearchByBeneWithPagingStartBeyondMaxExpect400() {
    String beneficiaryId =
        String.valueOf(
            testUtils.getFirstBeneficiary(testUtils.loadSampleAData()).getBeneficiaryId());
    String requestString =
        coverageEndpoint + "?beneficiary=" + beneficiaryId + "&_count=2&startIndex=12";

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
   * Verify that Coverage throws a 400 error when the paging start (startIndex) is set to the
   * maximum number of results, since the highest index must be less than the number of results as a
   * 0-based index.
   */
  @Test
  public void testCoverageSearchByBeneWithPagingStartSetToMaxResultsExpect400() {
    String beneficiaryId =
        String.valueOf(
            testUtils.getFirstBeneficiary(testUtils.loadSampleAData()).getBeneficiaryId());
    String requestString =
        coverageEndpoint + "?beneficiary=" + beneficiaryId + "&_count=2&startIndex=4";

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .ifError()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem("Value for startIndex (4) must be less than than result size (4)"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verify that Coverage does not error when the paging start (startIndex) is set to be equal to
   * one less than the maximum number of results.
   */
  @Test
  public void testCoverageSearchByBeneWithPagingStartOneLessThanMaxExpect200() {
    String beneficiaryId =
        String.valueOf(
            testUtils.getFirstBeneficiary(testUtils.loadSampleAData()).getBeneficiaryId());
    String requestString =
        coverageEndpoint + "?beneficiary=" + beneficiaryId + "&_count=2&startIndex=3";

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .ifError()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
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
  public void testCoverageSearchByBeneWithNoResultsAndPaginationRequestedExpect200() {
    String beneId = "0";
    String requestString = coverageEndpoint + "?beneficiary=" + beneId + "&_count=50";

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
    requestString = coverageEndpoint + "?beneficiary=" + beneId + "&startIndex=2";

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
