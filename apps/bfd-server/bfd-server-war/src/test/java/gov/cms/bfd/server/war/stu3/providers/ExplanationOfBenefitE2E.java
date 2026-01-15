package gov.cms.bfd.server.war.stu3.providers;

import static io.restassured.RestAssured.given;

import gov.cms.bfd.server.war.ExplanationOfBenefitE2EBase;
import org.junit.jupiter.api.BeforeEach;

/**
 * Endpoint end-to-end test for the V1 explanation of benefits endpoint. Most test logic should be
 * placed in {@link ExplanationOfBenefitE2EBase} to be shared, unless there are version-specific
 * paths or functionality to test.
 *
 * <p>To run individual tests in-IDE, ensure you use a view that shows inherited tests (like
 * IntelliJ's Structure panel with the "Inherited" option at the top)
 */
public class ExplanationOfBenefitE2E extends ExplanationOfBenefitE2EBase {

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    eobEndpoint = baseServerUrl + "/v1/fhir/ExplanationOfBenefit/";
  }
}
