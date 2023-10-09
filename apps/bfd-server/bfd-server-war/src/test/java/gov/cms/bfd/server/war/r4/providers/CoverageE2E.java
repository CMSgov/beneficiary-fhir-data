package gov.cms.bfd.server.war.r4.providers;

import gov.cms.bfd.server.war.CoverageE2EBase;
import org.junit.jupiter.api.BeforeEach;

/** Endpoint end-to-end test for the V2 Coverage endpoint. */
public class CoverageE2E extends CoverageE2EBase {

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    coverageEndpoint = baseServerUrl + "/v2/fhir/Coverage/";
  }
}
