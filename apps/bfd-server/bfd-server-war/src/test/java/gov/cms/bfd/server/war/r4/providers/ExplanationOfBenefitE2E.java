package gov.cms.bfd.server.war.r4.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.server.war.ExplanationOfBenefitE2EBase;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CommonHeaders;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Endpoint end-to-end test for the V2 explanation of benefits endpoint. Most test logic should be
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
    eobEndpoint = baseServerUrl + "/v2/fhir/ExplanationOfBenefit/";
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
    String requestString = eobEndpoint + "?patient=" + patientId;
    CarrierClaim carrierClaim = ServerTestUtils.getClaim(loadedData, CarrierClaim.class);
    DMEClaim dmeClaim = ServerTestUtils.getClaim(loadedData, DMEClaim.class);

    // make sure all 8 entries come back as expected and no 400/500/other errors
    given()
        .spec(requestAuth)
        .given()
        .header(CommonHeaders.HEADER_NAME_INCLUDE_TAX_NUMBERS, "true")
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // we should have 8 claim type entries
        .body("entry.size()", equalTo(8))
        .body("total", equalTo(8))
        // Check there are tax numbers on applicable claims; carrier and DME
        // Tax num value is found in the carrier.resource.item.extension.valueCoding.code (for the
        // appropriate url)
        .body(
            "entry.find { it.resource.id.contains('dme') }.resource.item[0].extension.find { it.url == 'https://bluebutton.cms.gov/resources/variables/tax_num' }.valueCoding.code",
            equalTo(dmeClaim.getLines().get(0).getProviderTaxNumber()))
        .body(
            "entry.find { it.resource.id.contains('carrier') }.resource.item[0].extension.valueCoding.code.flatten()",
            hasItem(carrierClaim.getLines().get(0).getProviderTaxNumber()))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that EOB search by patient id does not filter SAMHSA results when excludeSAMHSA is set
   * to false.
   */
  @Test
  public void testEobByPatientIdWithExcludeSamhsaFalseExpectNoFiltering() {

    // Adjust the sampleA data that was loaded to include some samhsa data by passing true
    String patientId = testUtils.getPatientId(testUtils.loadSampleASamhsaData());
    String requestString = eobEndpoint + "?patient=" + patientId + "&excludeSAMHSA=false";

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(8))
        // Check nothing is filtered and samhsa data is returned
        .body(
            "entry.find { it.resource.id.contains('carrier') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE))
        .body(
            "entry.find { it.resource.id.contains('hha') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE))
        .body(
            "entry.find { it.resource.id.contains('dme') }.resource.item.productOrService.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_CPT_CODE))
        .body(
            "entry.find { it.resource.id.contains('hospice') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE))
        .body(
            "entry.find { it.resource.id.contains('inpatient') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE))
        .body(
            "entry.find { it.resource.id.contains('outpatient') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE))
        .body(
            "entry.find { it.resource.id.contains('snf') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE))
        // Make sure pde is there too
        .body("entry.resource.id", hasItem(containsString("pde")))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verifies that EOB search by patient id does not filter SAMHSA results when excludeSAMHSA is not
   * explicitly set (should default to false internally).
   */
  @Test
  public void testEobByPatientIdWithExcludeSamhsaDefaultExpectNoFiltering() {

    // Adjust the sampleA data that was loaded to include some samhsa data by passing true
    String patientId = testUtils.getPatientId(testUtils.loadSampleASamhsaData());
    String requestString = eobEndpoint + "?patient=" + patientId;

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .body("entry.size()", equalTo(8))
        // Check nothing is filtered and samhsa data is returned
        .body(
            "entry.find { it.resource.id.contains('carrier') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE))
        .body(
            "entry.find { it.resource.id.contains('hha') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE))
        .body(
            "entry.find { it.resource.id.contains('dme') }.resource.item.productOrService.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_CPT_CODE))
        .body(
            "entry.find { it.resource.id.contains('hospice') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE))
        .body(
            "entry.find { it.resource.id.contains('inpatient') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE))
        .body(
            "entry.find { it.resource.id.contains('outpatient') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE))
        .body(
            "entry.find { it.resource.id.contains('snf') }.resource.diagnosis.diagnosisCodeableConcept.coding.code.flatten()",
            hasItem(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE))
        // Make sure pde is there too
        .body("entry.resource.id", hasItem(containsString("pde")))
        .statusCode(200)
        .when()
        .get(requestString);
  }
}
