package gov.cms.bfd.server.ng;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import io.restassured.RestAssured;
import org.hl7.fhir.r4.model.Coverage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class CoverageReadIT extends IntegrationTestBase {

  private IReadTyped<Coverage> coverageRead() {
    return getFhirClient().read().resource(Coverage.class);
  }

  
  @Test
  void coverageReadValidCompositeId() {
    String validCoverageId = "part-a-1";

    var coverage = coverageRead().withId(validCoverageId).execute();
    assertNotNull(coverage, "Coverage resource should not be null for a valid ID");
    assertFalse(coverage.isEmpty(), "Coverage resource should not be empty for a valid ID");
  }

  @Test
  void coverageReadBeneExistsButPartOrVersionNotFound() {
    String idForNonExistentCoverage = "part-c-1";

    assertThrows(
        InvalidRequestException.class,
        () -> coverageRead().withId(idForNonExistentCoverage).execute(),
        "Should throw ResourceNotFoundException for a validly formatted ID that doesn't map to a resource.");
  }

  /**
   * Test reading a Coverage resource where the bene_sk part of the ID does not correspond to any
   * existing Beneficiary.
   */
  @Test
  void coverageReadBeneSkNotFound() {
    String nonExistentBeneSkId = "part-a-9999999"; // Assuming bene_sk 9999999 does not exist

    assertThrows(
        ResourceNotFoundException.class,
        () -> coverageRead().withId(nonExistentBeneSkId).execute(),
        "Should throw ResourceNotFoundException if the beneficiary part of the ID does not exist.");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "part-a", // Missing bene_sk
        "-12345", // Missing part identifier
        "part-a-abc", // Invalid bene_sk (not a number)
        "foo-12345", // Invalid part prefix "foo"
        "part-e-12345" // Invalid part "part-e" (if not in CoveragePart enum)
      })
  void coverageReadInvalidIdFormatBadRequest_UsingHapiClient(String invalidId) {
    // These formats should be rejected by your server-side CoverageCompositeId.parse()
    // The HAPI client might let some of these through if they are not blank.
    assertThrows(
        InvalidRequestException.class,
        () -> coverageRead().withId(invalidId).execute(),
        "Should throw InvalidRequestException from server for ID: " + invalidId);
  }

  /**
   * Test reading a Coverage resource with an ID that is syntactically valid (matches pattern) but
   * uses a CoveragePart that is not supported by the FFS endpoint (e.g., Part C or D, assuming your
   * FhirInputConverter or Handler throws InvalidRequestException for these).
   */
  @Test
  void coverageReadUnsupportedValidPartBadRequest() {
    String unsupportedPartId = "part-c-1";
    assertThrows(
        InvalidRequestException.class,
        () -> coverageRead().withId(unsupportedPartId).execute(),
        "Should throw InvalidRequestException for an unsupported part like Part C/D.");
  }

  @ParameterizedTest
  @EmptySource // Provides "" for the ID, resulting in GET /Coverage/
  void coverageReadEmptyIdSegmentResultsInServerError_WithRestAssured(String id) {
    RestAssured.given()
        .when()
        .get(getServerUrl() + "/Coverage/" + id) // Sends GET /Coverage/ (if id is "")
        .then()
        .statusCode(400)
        .body(
            "issue[0].diagnostics",
            containsString(
                "The FHIR endpoint on this server does not know how to handle GET operation[Coverage/] with parameters [[]]"));
  }
}
