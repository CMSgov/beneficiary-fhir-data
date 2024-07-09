package gov.cms.bfd.server.war.stu3.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItem;

import gov.cms.bfd.server.war.CoverageE2EBase;
import gov.cms.bfd.server.war.r4.providers.R4CoverageResourceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Endpoint end-to-end test for the V1 Coverage endpoint. Most test logic should be placed in {@link
 * CoverageE2EBase} to be shared, unless there are version-specific paths or functionality to test.
 *
 * <p>To run individual tests in-IDE, ensure you use a view that shows inherited tests (like
 * IntelliJ's Structure panel with the "Inherited" option at the top)
 */
public class CoverageE2E extends CoverageE2EBase {

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    coverageEndpoint = baseServerUrl + "/v1/fhir/Coverage/";
  }

  /**
   * Verifies that {@link R4CoverageResourceProvider#read} returns a 400 and error message when
   * requesting a contract id which is improperly formatted.
   */
  @Test
  public void testReadWhenImproperlyFormattedCoverageIdExpect400() {
    String requestString = coverageEndpoint + "bad-format";

    given()
        .spec(requestAuth)
        .expect()
        .body("issue.severity", hasItem("error"))
        .body(
            "issue.diagnostics",
            hasItem(
                "Coverage ID pattern: 'bad-format' does not match expected pattern: {alphaNumericString}-{singleCharacter}-{idNumber}"))
        .statusCode(400)
        .when()
        .get(requestString);
  }
}
