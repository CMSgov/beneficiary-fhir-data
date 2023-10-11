package gov.cms.bfd.server.war.stu3.providers;

import gov.cms.bfd.server.war.CoverageE2EBase;
import org.junit.jupiter.api.BeforeEach;

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
}
