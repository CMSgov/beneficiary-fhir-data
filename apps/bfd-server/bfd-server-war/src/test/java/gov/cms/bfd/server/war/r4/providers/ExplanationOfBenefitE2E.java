package gov.cms.bfd.server.war.r4.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.server.war.ExplanationOfBenefitE2EBase;
import gov.cms.bfd.server.war.ServerTestUtils;
import io.restassured.http.Headers;
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
   * Verifies that the EOB returns a 200 with the expected DD paths for each claim type returned
   * when searching via POST.
   */
  @Test
  public void testEobPostByPatientIdWithValidPatientIdExpect200() {

    List<Object> loadedData = testUtils.loadSampleAData();
    String patientId = testUtils.getPatientId(loadedData);
    // _search needed to distinguish the POST version of the endpoint (HAPI-FHIR)
    String requestString = eobEndpoint + "_search";
    OutpatientClaim expectedOutpatientClaim =
        ServerTestUtils.getClaim(loadedData, OutpatientClaim.class);

    String formParams = "patient=" + patientId;

    given()
        .header("Content-Type", "application/x-www-form-urlencoded")
        .spec(requestAuth)
        .body(formParams)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        // we should have 8 claim type entries
        .body("entry.size()", equalTo(8))
        // the claim types of these entries should all be ExplanationOfBenefit
        .body("entry.resource.resourceType", everyItem(equalTo("ExplanationOfBenefit")))
        // Check our response has the various claim types by checking their metadata ids for
        // each claim type
        .body(
            "entry.resource.id",
            hasItems(
                containsString("hha"),
                containsString("inpatient"),
                containsString("carrier"),
                containsString("dme"),
                containsString("snf"),
                containsString("pde"),
                containsString("hospice"),
                containsString("hha")))
        // Find the outpatient claim, then find the extension for fi_doc_clm_cntl_num, then get the
        // value, compare to the loaded data value. This validates we added our extension with the
        // right path, with the right value.
        .body(
            "entry.find { it.resource.id.contains('outpatient') }.resource.extension.find { it.url == 'https://bluebutton.cms.gov/resources/variables/fi_doc_clm_cntl_num' }.valueIdentifier.value",
            equalTo(expectedOutpatientClaim.getFiDocumentClaimControlNumber().orElseThrow()))
        .statusCode(200)
        .when()
        .post(requestString);
  }

  /**
   * Verifies that the EOB returns a 200 when passing in _elements and searching via POST and the
   * response is smaller when using _elements to filter some fields.
   */
  @Test
  public void testEobPostByPatientIdWithElementsExpectSmallerPayload() {

    List<Object> loadedData = testUtils.loadSampleAData();
    String patientId = testUtils.getPatientId(loadedData);
    // _search needed to distinguish the POST version of the endpoint (HAPI-FHIR)
    String requestString = eobEndpoint + "_search";

    // Add some element filtering as one might from a partner
    String carinRequiredFields =
        "identifier.value,identifier.type,status,use,billablePeriod.start,insurer,provider,"
            + "related.relationship,related.reference,payee.type,payee.party,outcome,careTeam.provider,careTeam.role,"
            + "supportingInfo,insurance.focal,insurance.coverage,item.sequence,item.noteNumber,item.adjudication,"
            + "total,payment.type,processNote,patient.meta.lastUpdated,patient.identifier,patient.deceased,patient.address.country";
    String ab2dRequiredFields =
        "billablePeriod.end,item.extension,item.locationCodeableConcept,item.productOrService,"
            + "item.diagnosisSequence,item.locationAddress.state,item.quantity.value,item.servicedDate,item.servicedPeriod.end,"
            + "item.servicedPeriod.start,patient,procedure.date,procedure.procedureCodeableConcept,diagnosis,extension,facility.extension,"
            + "type.coding";

    String formParams =
        "patient=" + patientId + "&_elements=" + carinRequiredFields + "," + ab2dRequiredFields;

    Headers headers =
        given()
            // Needed POST header for passing the params in the POST body
            .header("Content-Type", "application/x-www-form-urlencoded")
            .spec(requestAuth)
            .body(formParams)
            .expect()
            .body("resourceType", equalTo("Bundle"))
            .statusCode(200)
            .when()
            .post(requestString)
            .headers();

    // get the response size for the filtered payload
    int filteredResponseSize = Integer.parseInt(headers.get("Content-Length").getValue());

    // Make another call with no filters and make sure the filtered one is smaller
    headers =
        given()
            // Needed POST header for passing the params in the POST body
            .header("Content-Type", "application/x-www-form-urlencoded")
            .spec(requestAuth)
            .body("patient=" + patientId)
            .expect()
            .body("resourceType", equalTo("Bundle"))
            .statusCode(200)
            .when()
            .post(requestString)
            .headers();

    // get the response size for the unfiltered payload
    int unfilteredResponseSize = Integer.parseInt(headers.get("Content-Length").getValue());

    assertTrue(unfilteredResponseSize > filteredResponseSize);
  }

  /** Verifies that the EOB returns a 400 when passing in _elements and searching via GET. */
  @Test
  public void testEobGetByPatientIdWithElementsExpect400() {

    List<Object> loadedData = testUtils.loadSampleAData();
    String patientId = testUtils.getPatientId(loadedData);
    String requestString =
        eobEndpoint + "?patient=" + patientId + "&_elements=extension.url,identifier.system";

    given()
        .spec(requestAuth)
        .expect()
        .statusCode(400)
        .body("issue.severity", hasItem("error"))
        .body("issue.diagnostics", hasItem("_elements tag is only supported via POST request"))
        .when()
        .get(requestString);
  }
}
