package gov.cms.bfd.server.war.stu3.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.server.war.ExplanationOfBenefitE2EBase;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

  /**
   * Test eob by patient id returns tax numbers for applicable claim types when IncludeTaxNumbers =
   * true.
   */
  @Test
  public void testEobByPatientIdWithIncludeTaxNumbersTrueExpectTaxNumbers() {

    List<Object> loadedData = testUtils.loadSampleAData();
    String patientId = testUtils.getPatientId(loadedData);
    // IncludeTaxNumbers is a header, so added below in restAssured API
    String requestString = eobEndpoint + "?patient=" + patientId + "&_IncludeTaxNumbers=true";
    CarrierClaim carrierClaim = ServerTestUtils.getClaim(loadedData, CarrierClaim.class);
    DMEClaim dmeClaim = ServerTestUtils.getClaim(loadedData, DMEClaim.class);

    // make sure all 8 entries come back as expected and no 400/500/other errors
    given()
        .spec(requestAuth)
        .given()
        .expect()
        .log()
        .body()
        .body("resourceType", equalTo("Bundle"))
        // we should have 8 claim type entries
        .body("entry.size()", equalTo(8))
        /* Check there are tax numbers on applicable claims; carrier and DME
        Tax num value (for v1) is found in the eob.careTeam[N].provider.identifier[N].value (for
        the appropriate url) */
        .body(
            "entry.find { it.resource.id.contains('dme') }.resource.careTeam.find { it.provider.identifier.system == 'http://terminology.hl7.org/CodeSystem/v2-0203' }.provider.identifier.value",
            equalTo(dmeClaim.getLines().get(0).getProviderTaxNumber()))
        .body(
            "entry.find { it.resource.id.contains('carrier') }.resource.careTeam.find { it.provider.identifier.system == 'http://terminology.hl7.org/CodeSystem/v2-0203' }.provider.identifier.value",
            equalTo(carrierClaim.getLines().get(0).getProviderTaxNumber()))
        .statusCode(200)
        .when()
        .get(requestString);
  }
}
