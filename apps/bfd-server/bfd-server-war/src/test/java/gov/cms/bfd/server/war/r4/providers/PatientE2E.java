package gov.cms.bfd.server.war.r4.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import gov.cms.bfd.server.war.ServerRequiredTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Endpoint end-to-end test for the V2 Patient endpoint. */
public class PatientE2E extends ServerRequiredTest {

  /** The base patient endpoint. */
  private static String patientEndpoint;

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    // Set this up once, after the server has run
    if (patientEndpoint == null) {
      patientEndpoint = baseServerUrl + "/v2/fhir/Patient/";
    }
  }

  /**
   * Verify that Patient throws a 400 error when the paging start (startIndex) is set higher than
   * the maximum number of results.
   */
  @Test
  public void testPatientByLogicalIdWithPagingStartBeyondMaxExpect400() {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + "?_id=" + patientId + "&_count=2&startIndex=12";

    given()
        .spec(requestAuth)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem("Value for startIndex (12) must be less than than result size (1)"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verify that Patient throws a 400 error when the paging start (startIndex) is set to the maximum
   * number of results, since the highest index must be less than the number of results as a 0-based
   * index.
   */
  @Test
  public void testPatientByLogicalIdWithPagingStartSetToMaxResultsExpect400() {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + "?_id=" + patientId + "&_count=1&startIndex=1";

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .ifError()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem("Value for startIndex (1) must be less than than result size (1)"))
        .statusCode(400)
        .when()
        .get(requestString);
  }

  /**
   * Verify that Patient does not error when the paging start (startIndex) is set to be equal to one
   * less than the maximum number of results.
   */
  @Test
  public void testPatientByLogicalIdWithPagingStartOneLessThanMaxExpect200() {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + "?_id=" + patientId + "&_count=1&startIndex=0";

    given()
        .spec(requestAuth)
        .expect()
        .log()
        .ifError()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(1))
        .body("total", equalTo(1))
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
  public void testPatientByLogicalIdWithNoResultsAndPaginationRequestedExpect200() {
    String patientId = "0";
    String requestString = patientEndpoint + "?_id=" + patientId + "&_count=50";

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
    requestString = patientEndpoint + "?_id=" + patientId + "&startIndex=2";

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
