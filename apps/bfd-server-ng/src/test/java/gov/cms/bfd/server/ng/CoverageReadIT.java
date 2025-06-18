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
  void coverageReadValidPartACompositeId() {
    String validCoverageId = "part-a-405764107";

    var coverage = coverageRead().withId(validCoverageId).execute();
    assertNotNull(coverage, "Coverage resource should not be null for a valid ID");
    assertFalse(coverage.isEmpty(), "Coverage resource should not be empty for a valid ID");
    expect.serializer("fhir+json").toMatchSnapshot(coverage);
  }

  @Test
  void coverageReadValidPartBCompositeId() {
    String validCoverageId = "part-b-405764107";

    var coverage = coverageRead().withId(validCoverageId).execute();
    assertNotNull(coverage, "Coverage resource should not be null for a valid ID");
    assertFalse(coverage.isEmpty(), "Coverage resource should not be empty for a valid ID");
    expect.serializer("fhir+json").toMatchSnapshot(coverage);
  }

  @Test
  void coverageReadValidCompositeId() {
    String validCoverageId = "part-a-181968400";

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

  @Test
  void coverageReadBeneSkNotFound() {
    String nonExistentBeneSkId = "part-a-9999999";

    assertThrows(
        ResourceNotFoundException.class,
        () -> coverageRead().withId(nonExistentBeneSkId).execute(),
        "Should throw ResourceNotFoundException if the beneficiary part of the ID does not exist.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"part-a", "-12345", "part-a-abc", "foo-12345", "part-e-12345"})
  void coverageReadInvalidIdFormatBadRequest_UsingHapiClient(String invalidId) {
    assertThrows(
        InvalidRequestException.class,
        () -> coverageRead().withId(invalidId).execute(),
        "Should throw InvalidRequestException from server for ID: " + invalidId);
  }

  @Test
  void coverageReadUnsupportedValidPartBadRequest() {
    String unsupportedPartId = "part-c-1";
    assertThrows(
        InvalidRequestException.class,
        () -> coverageRead().withId(unsupportedPartId).execute(),
        "Should throw InvalidRequestException for an unsupported part like Part C/D.");
  }

  @ParameterizedTest
  @EmptySource
  void coverageReadEmptyIdSegmentResultsInServerError_WithRestAssured(String id) {
    RestAssured.given()
        .when()
        .get(getServerUrl() + "/Coverage/" + id)
        .then()
        .statusCode(400)
        .body(
            "issue[0].diagnostics",
            containsString(
                "The FHIR endpoint on this server does not know how to handle GET operation[Coverage/] with parameters [[]]"));
  }
}
