package gov.cms.bfd.server.ng;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import io.restassured.RestAssured;
import java.sql.SQLException;
import org.hl7.fhir.r4.model.Coverage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.TestTransaction;

public class CoverageReadIT extends IntegrationTestBase {

  @Autowired protected JdbcTemplate jdbcTemplate;

  private static final String BENE_ID_HAS_A_AND_B = "405764107";

  private IReadTyped<Coverage> coverageRead() {
    return getFhirClient().read().resource(Coverage.class);
  }

  protected void runSql(String sql) throws SQLException {
    jdbcTemplate.execute(sql);

    if (TestTransaction.isActive()) {
      TestTransaction.flagForCommit();
      TestTransaction.end();
      TestTransaction.start();
    }
  }

  private String createCoverageId(String part, String beneId) {
    return String.format("part-%s-%s", part, beneId);
  }

  @Test
  void coverageReadValidPartACompositeId() {
    var validCoverageId = "part-a-405764107";

    var coverage = coverageRead().withId(validCoverageId).execute();
    assertNotNull(coverage, "Coverage resource should not be null for a valid ID");
    assertFalse(coverage.isEmpty(), "Coverage resource should not be empty for a valid ID");
    expect.serializer("fhir+json").toMatchSnapshot(coverage);
  }

  @Test
  void coverageReadValidPartBCompositeId() {
    String validCoverageId = "part-a-405764107";

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
        "Should throw ResourceNotFoundException for Coverage ID 'part-a-405764107' "
            + "because the beneficiary record is not the current effective version.");
  }

  @Test
  void coverageReadValidCompositeId() {
    String validCoverageId = "part-a-405764107";

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
  void read_beneWithOnlyExpiredCoverage_isNotFound() throws SQLException {

    final String updateSql =
        String.format(
            "UPDATE \"idr\".\"beneficiary_entitlement\" "
                + "SET \"bene_rng_end_dt\" = '2012-12-31' "
                + "WHERE \"bene_sk\" = '%s' "
                + "AND \"bene_mdcr_entlmt_type_cd\" = 'A'",
            BENE_ID_HAS_A_AND_B);

    runSql(updateSql);

    var expiredCoverageId = createCoverageId("a", BENE_ID_HAS_A_AND_B);

    assertThrows(
        ResourceNotFoundException.class,
        () -> coverageRead().withId(expiredCoverageId).execute(),
        "Should throw ResourceNotFoundException as coverage was just expired via SQL.");
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
