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

  private static final String BENE_ID_PART_A_ONLY = "178083966";
  private static final String BENE_ID_PART_B_ONLY = "365359727";
  private static final String BENE_ID_HAS_A_AND_B = "405764107";
  private static final String BENE_ID_NO_TP = "451482106";
  private static final String BENE_ID_EXPIRED_COVERAGE = "421056595";
  private static final String BENE_ID_FUTURE_COVERAGE = "971050241";

  private IReadTyped<Coverage> coverageRead() {
    return getFhirClient().read().resource(Coverage.class);
  }

  private String createCoverageId(String part, String beneId) {
    return String.format("part-%s-%s", part, beneId);
  }

  @Test
  void coverageReadValidPartACompositeId() {
    var validCoverageId = createCoverageId("a", BENE_ID_PART_A_ONLY);

    var coverage = coverageRead().withId(validCoverageId).execute();
    assertNotNull(coverage, "Coverage resource should not be null for a valid ID");
    assertFalse(coverage.isEmpty(), "Coverage resource should not be empty for a valid ID");
    expect.serializer("fhir+json").toMatchSnapshot(coverage);
  }

  @Test
  void coverageReadValidPartBCompositeId() {
    var validCoverageId = createCoverageId("b", BENE_ID_PART_B_ONLY);

    var coverage = coverageRead().withId(validCoverageId).execute();
    assertNotNull(coverage, "Coverage resource should not be null for a valid ID");
    assertFalse(coverage.isEmpty(), "Coverage resource should not be empty for a valid ID");
    expect.serializer("fhir+json").toMatchSnapshot(coverage);
  }

  @Test
  void coverageReadForNonCurrentEffectiveBeneficiaryIdShouldBeNotFound() {

    String nonCurrentEffectiveBeneId = "part-a-181968400";
    assertThrows(
        ResourceNotFoundException.class,
        () -> coverageRead().withId(nonCurrentEffectiveBeneId).execute(),
        "Should throw ResourceNotFoundException for Coverage ID 'part-a-181968400' "
            + "because the beneficiary record is not the current effective version.");
  }

  @Test
  void coverageReadValidCompositeId() {
    var validCoverageId = createCoverageId("b", BENE_ID_HAS_A_AND_B);

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

  @Test
  void coverageReadPartACoverageNotFound() {

    var expiredCoverageId = createCoverageId("a", "848484848");

    assertThrows(
        ResourceNotFoundException.class,
        () -> coverageRead().withId(expiredCoverageId).execute(),
        "Should throw ResourceNotFoundException.");
  }

  @Test
  void coverageRead_beneWithNoCoverage_throwsNotFound() {
    // Check that Part A is not found
    final String partAId = createCoverageId("a", "289169129");
    assertThrows(
        ResourceNotFoundException.class,
        () -> coverageRead().withId(partAId).execute(),
        "Part A Coverage should not be found for a beneficiary with no coverage.");

    // Check that Part B is not found
    final String partBId = createCoverageId("a", "848484848");
    assertThrows(
        ResourceNotFoundException.class,
        () -> coverageRead().withId(partBId).execute(),
        "Part B Coverage should not be found for a beneficiary with no coverage.");
  }

  @Test
  void coverageRead_forBeneWithOnlyPastEntitlementPeriods_shouldBeNotFound() {
    final String partBId = createCoverageId("b", BENE_ID_EXPIRED_COVERAGE);

    assertThrows(
        ResourceNotFoundException.class,
        () -> coverageRead().withId(partBId).execute(),
        "Should throw ResourceNotFoundException as the beneficiary's Part B entitlement period is in the past.");
  }

  @Test
  void coverageRead_forBeneWithOnlyFutureEntitlementPeriods_shouldBeNotFound() {
    final String partBId = createCoverageId("b", BENE_ID_FUTURE_COVERAGE);

    assertThrows(
        ResourceNotFoundException.class,
        () -> coverageRead().withId(partBId).execute(),
        "Should throw ResourceNotFoundException as the beneficiary's Part B entitlement period is in the past.");
  }

  @Test
  void coverageRead_forBeneWithMissingTplData_shouldStillReturnResource() {

    var partAId = createCoverageId("a", BENE_ID_NO_TP);
    var coverage = coverageRead().withId(partAId).execute();

    assertNotNull(coverage, "Coverage resource should not be null even without TPL data.");
    assertFalse(coverage.isEmpty(), "Coverage resource should not be empty.");

    expect.serializer("fhir+json").toMatchSnapshot(coverage);
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
