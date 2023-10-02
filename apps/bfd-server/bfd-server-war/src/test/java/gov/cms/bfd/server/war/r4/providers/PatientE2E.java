package gov.cms.bfd.server.war.r4.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
import gov.cms.bfd.server.war.ServerRequiredTest;
import java.util.List;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Endpoint end-to-end test for the V2 Patient endpoint. */
public class PatientE2E extends ServerRequiredTest {

  /** The base patient endpoint. */
  private static String patientEndpoint;

  /**
   * A list of expected historical mbis for adding to the sample A loaded data (as data coming back
   * from the endpoint will have this added in the resource provider).
   */
  private static final List<String> historicalMbis = List.of("9AB2WW3GR44", "543217066", "3456689");

  /** The current Mbi as found in the SAMPLE A data. */
  private static final String currentMbi = "3456789";

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    // Set this up once, after the server has run
    if (patientEndpoint == null) {
      patientEndpoint = baseServerUrl + "/v2/fhir/Patient/";
    }
  }

  /** Verifies patient read with an existing bene returns a 200 and response. */
  @Test
  public void testReadWhenExistingPatientExpect200() {
    String patientId = testUtils.getPatientId(testUtils.loadSampleAData());
    String requestString = patientEndpoint + patientId;

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Patient"))
        .body("id", equalTo(patientId))
        // Check current MBI is returned
        .body("identifier.value", hasItem(currentMbi))
        // Check historical MBIs are returned too
        .body("identifier.value", hasItems(historicalMbis.toArray()))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that Patient searchByLogicalId returns a 200 and response when the beneficiary exists
   * in the DB but has no {@link BeneficiaryHistory} or MedicareBeneficiaryIdHistory records.
   * Primarily this checks that the table joins do not cause any issue retrieving the patient when
   * there is nothing found in the history table.
   */
  @Test
  public void testReadWhenNoHistoricalMbisExpect200() {
    
  }

  /**
   * Verifies that Patient searchByLogicalId returns a 200 and response for a {@link Patient} that
   * exists in the DB.
   */
  @Test
  public void testPatientByLogicalIdUsingBeneIdExpect200() {}

  /**
   * Verifies that Patient searchByLogicalId returns a 200 and response for a {@link Patient} that
   * exists in the DB.
   */
  @Test
  public void testPatientByLogicalIdUsingCurrentMbiHashExpect200() {}

  /**
   * Verifies that Patient searchByLogicalId returns a 200 and response for a {@link Patient} that
   * exists in the DB and an MBI points to more than one bene id in either the Beneficiaries and/or
   * BeneficiariesHistory table.
   */
  @Test
  public void testPatientByLogicalIdUsingMbiHashWithBeneDupesExpect200() {}

  /**
   * Verifies that Patient searchByLogicalId returns a 200 and response when searching by a
   * historical MBI hash.
   */
  @Test
  public void testPatientByLogicalIdUsingHistoricalMbiHashExpect200() {}

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByIdentifier} works as expected for MBIs
   * associated with {@link Beneficiary}s that have <strong>no</strong> {@link BeneficiaryHistory}
   * records.
   */
  @Test
  public void testPatientByLogicalIdWhenNoHistoricalMbisExpect200() {}

  /**
   * Verifies that searching by a known existing part D contract number returns a result as
   * expected. Also ensures the unhashed MBI values are returned by default.
   */
  @Test
  public void testPatientByPartDContractExpectUnhashedMbis() {}

  /**
   * Regression test for part of BFD-525, which verifies that duplicate entries are not returned
   * when 1) plain-text identifiers are requested, 2) a beneficiary has multiple historical
   * identifiers, and 3) paging is requested. (This oddly specific combo had been bugged earlier and
   * was quite tricky to resolve).
   */
  @Test
  public void testPatientByPartDContractWithPagingAndMultipleMbisExpectNoDupes() {}

  /**
   * Regression test for part of BFD-525, which verifies that duplicate entries are not returned
   * when 1) plain-text identifiers are requested, 2) a beneficiary has multiple historical
   * identifiers, and 3) paging is not requested. (This oddly specific combo had been bugged earlier
   * and was quite tricky to resolve).
   */
  @Test
  public void testPatientByPartDContractWithMultipleMbisExpectNoDupes() {}

  /**
   * Verifies that {@link R4PatientResourceProvider#searchByCoverageContract} works as expected,
   * when an invalid year is specified.
   */
  @Test
  public void testPatientByPartDContractWithInvalidYearExpect400() {}

  /**
   * Verifies that searching by lastUpdated with its various supported prefixes returns results as
   * expected.
   */
  @Test
  public void testPatientByLogicalIdWithLastUpdatedExpectFilteredResults() {}

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
