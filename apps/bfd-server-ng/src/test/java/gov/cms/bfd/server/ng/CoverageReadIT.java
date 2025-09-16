package gov.cms.bfd.server.ng;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import io.restassured.RestAssured;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Coverage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public class CoverageReadIT extends IntegrationTestBase {

  private IReadTyped<Coverage> coverageRead() {
    return getFhirClient().read().resource(Coverage.class);
  }

  private String createCoverageId(String part, String beneId) {
    return String.format("%s-%s", part, beneId);
  }

  @Test
  void coverageReadValidPartACompositeId() {
    var validCoverageId = createCoverageId("part-a", BENE_ID_PART_A_ONLY);

    var coverage = coverageRead().withId(validCoverageId).execute();
    assertNotNull(coverage, "Coverage resource should not be null for a valid ID");
    assertFalse(coverage.isEmpty(), "Coverage resource should not be empty for a valid ID");
    expectFhir().scenario("validPartA").toMatchSnapshot(coverage);

    var missingCoverageId = createCoverageId("part-b", BENE_ID_PART_A_ONLY);
    var missingCoverage = coverageRead().withId(missingCoverageId).execute();
    // Response for a missing coverage part should only contain the ID
    assertTrue(missingCoverage.getIdentifier().isEmpty());
    assertNotNull(missingCoverage, "Coverage resource should not be null for a valid ID");
    assertFalse(missingCoverage.isEmpty(), "Coverage resource should not be empty for a valid ID");
    expectFhir().scenario("missingPartB").toMatchSnapshot(missingCoverage);
  }

  @Test
  void coverageReadValidPartBCompositeId() {
    var validCoverageId = createCoverageId("part-b", BENE_ID_PART_B_ONLY);

    var coverage = coverageRead().withId(validCoverageId).execute();
    assertNotNull(coverage, "Coverage resource should not be null for a valid ID");
    assertFalse(coverage.isEmpty(), "Coverage resource should not be empty for a valid ID");
    expectFhir().scenario("validPartB").toMatchSnapshot(coverage);

    var missingCoverageId = createCoverageId("part-a", BENE_ID_PART_B_ONLY);
    var missingCoverage = coverageRead().withId(missingCoverageId).execute();
    // Response for a missing coverage part should only contain the ID
    assertTrue(missingCoverage.getIdentifier().isEmpty());
    assertNotNull(missingCoverage, "Coverage resource should not be null for a valid ID");
    assertFalse(missingCoverage.isEmpty(), "Coverage resource should not be empty for a valid ID");
    expectFhir().scenario("missingPartA").toMatchSnapshot(missingCoverage);
  }

  @Test
  void coverageReadForNonCurrentEffectiveBeneficiaryIdShouldBeNotFound() {
    var nonCurrentEffectiveBeneId = "part-a-" + BENE_ID_NON_CURRENT;
    var readWithId = coverageRead().withId(nonCurrentEffectiveBeneId);
    assertThrows(
        ResourceNotFoundException.class,
        readWithId::execute,
        String.format(
            "Should throw ResourceNotFoundException for Coverage ID 'part-a-%s 'because the beneficiary record is not the current effective version.",
            BENE_ID_NON_CURRENT));
  }

  @ParameterizedTest
  @ValueSource(strings = {"a", "-12345", "part-a-abc", "foo-12345", "part-e-12345"})
  void coverageReadPartAWithAllCoverage() {
    var validCoverageId = createCoverageId("part-a", BENE_ID_ALL_PARTS_WITH_XREF);

    var coverage = coverageRead().withId(validCoverageId).execute();
    assertNotNull(coverage, "Coverage resource should not be null for a valid ID");
    assertFalse(coverage.isEmpty(), "Coverage resource should not be empty for a valid ID");
  }

  @Test
  void coverageReadBeneExistsButPartOrVersionNotFound() {
    var idForNonExistentCoverage = "part-e-" + BENE_ID_ALL_PARTS_WITH_XREF;

    var readWithId = coverageRead().withId(idForNonExistentCoverage);
    assertThrows(
        InvalidRequestException.class,
        readWithId::execute,
        "Should throw ResourceNotFoundException for a validly formatted ID that doesn't map to a resource.");
  }

  @Test
  void coverageReadBeneSkNotFound() {
    var nonExistentBeneSkId = "part-a-9999999";

    var readWithId = coverageRead().withId(nonExistentBeneSkId);
    assertThrows(
        ResourceNotFoundException.class,
        readWithId::execute,
        "Should throw ResourceNotFoundException if the beneficiary part of the ID does not exist.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"part-a", "-12345", "part-a-abc", "foo-12345", "part-e-12345"})
  void coverageReadInvalidIdFormatBadRequest(String invalidId) {
    var readWithId = coverageRead().withId(invalidId);
    assertThrows(
        InvalidRequestException.class,
        readWithId::execute,
        "Should throw InvalidRequestException from server for ID: " + invalidId);
  }

  @Test
  void coverageReadUnsupportedValidPartBadRequest() {
    var unsupportedPartId = "part-e-1";
    var readWithId = coverageRead().withId(unsupportedPartId);
    assertThrows(
        InvalidRequestException.class,
        readWithId::execute,
        "Should throw InvalidRequestException for an unsupported part.");
  }

  @Test
  void coverageReadMergedBeneReturnsNotFound() {
    var expiredCoverageId = createCoverageId("part-a", HISTORICAL_MERGED_BENE_SK);

    var readWithId = coverageRead().withId(expiredCoverageId);
    assertThrows(
        ResourceNotFoundException.class,
        readWithId::execute,
        "Should throw ResourceNotFoundException.");
  }

  @ParameterizedTest
  @ValueSource(strings = {"part-A", "part-B", "dual"})
  void coverageReadBeneWithNoCoverageReturnsEmpty(String part) {
    final String partId = createCoverageId(part, BENE_ID_NO_COVERAGE);
    var coverage = coverageRead().withId(partId).execute();
    assertEquals(partId.toLowerCase(), coverage.getIdPart());
    assertTrue(coverage.getIdentifier().isEmpty());
    expectFhir().scenario("missingAll" + part).toMatchSnapshot(coverage);
  }

  @ParameterizedTest
  @ValueSource(strings = {"part-A", "part-B", "dual"})
  void coverageReadBeneWithAllCoverage(String part) {
    final String partId = createCoverageId(part, BENE_ID_ALL_PARTS_WITH_XREF);
    var coverage = coverageRead().withId(partId).execute();
    assertEquals(partId.toLowerCase(), coverage.getIdPart());
    expectFhir().scenario("allCoverage" + part).toMatchSnapshot(coverage);
  }

  @Test
  void coverageReadForBeneWithOnlyPastEntitlementPeriodsShouldBeCancelled() {
    final var partBId = createCoverageId("part-b", BENE_ID_EXPIRED_COVERAGE);

    var coverage = coverageRead().withId(partBId).execute();
    assertEquals(partBId, coverage.getIdPart());
    assertEquals(Coverage.CoverageStatus.CANCELLED, coverage.getStatus());
    expectFhir().toMatchSnapshot(coverage);
  }

  @Test
  void coverageReadForBeneWithOnlyFutureEntitlementPeriodsShouldBeEmpty() {
    final String partBId = createCoverageId("part-b", BENE_ID_FUTURE_COVERAGE);

    var coverage = coverageRead().withId(partBId).execute();
    assertEquals(partBId, coverage.getIdPart());
    assertTrue(coverage.getIdentifier().isEmpty());
    expectFhir().toMatchSnapshot(coverage);
  }

  @Test
  void coverageReadForBeneWithMissingTpDataShouldStillReturnResource() {
    var partAId = createCoverageId("part-a", BENE_ID_NO_TP);
    var coverage = coverageRead().withId(partAId).execute();

    assertNotNull(coverage, "Coverage resource should not be null even without TPL data.");
    assertFalse(coverage.isEmpty(), "Coverage resource should not be empty.");

    expectFhir().toMatchSnapshot(coverage);
  }

  @Test
  void coverageReadDualOnly() {
    var dualId = createCoverageId("dual", BENE_ID_DUAL_ONLY);
    var coverage = coverageRead().withId(dualId).execute();

    assertEquals(dualId, coverage.getIdPart());
    assertEquals(Coverage.CoverageStatus.ACTIVE, coverage.getStatus());
    var dualStatusCodeExtension =
        getExtensionByUrl(coverage, SystemUrls.BLUE_BUTTON_STRUCTURE_DEFINITION_DUAL_STATUS_CODE);
    assertEquals(1, dualStatusCodeExtension.size());
    assertEquals(
        DUAL_ONLY_BENE_COVERAGE_STATUS_CODE,
        ((Coding) dualStatusCodeExtension.getFirst().getValue()).getCode());

    expectFhir().toMatchSnapshot(coverage);
  }

  @Test
  void coverageReadDualOnlyExpired() {
    var dualId = createCoverageId("dual", BENE_ID_DUAL_ONLY_EXPIRED);
    var coverage = coverageRead().withId(dualId).execute();

    assertEquals(dualId, coverage.getIdPart());
    assertEquals(Coverage.CoverageStatus.CANCELLED, coverage.getStatus());

    expectFhir().toMatchSnapshot(coverage);
  }

  @ParameterizedTest
  @EmptySource
  void coverageReadEmptyIdSegmentResultsInServerError(String id) {
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
