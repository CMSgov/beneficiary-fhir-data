package gov.cms.bfd.server.war.r4.providers;

import gov.cms.bfd.server.war.PatientE2EBase;
import org.junit.jupiter.api.BeforeEach;

/**
 * Endpoint end-to-end test for the V2 Patient endpoint. Most test logic should be placed in {@link
 * PatientE2EBase} to be shared, unless there are version-specific paths or functionality to test.
 *
 * <p>To run individual tests in-IDE, ensure you use a view that shows inherited tests (like
 * IntelliJ's Structure panel with the "Inherited" option at the top)
 */
public class PatientE2E extends PatientE2EBase {

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    patientEndpoint = baseServerUrl + "/v2/fhir/Patient/";
  }
}
