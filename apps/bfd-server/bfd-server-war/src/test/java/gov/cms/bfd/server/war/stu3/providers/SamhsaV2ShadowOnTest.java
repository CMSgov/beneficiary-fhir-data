package gov.cms.bfd.server.war.stu3.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItems;

import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.server.war.ServerRequiredTest;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests in the case where Samhsa 2.0 shadow flag is ON* */
class SamhsaV2ShadowOnTest extends ServerRequiredTest {

  /** Test utils. */
  private static final RDATestUtils rdaTestUtils = new RDATestUtils();

  /** The base eob endpoint. */
  protected static String eobEndpoint;

  /** The base claim endpoint. */
  private static String claimEndpoint;

  /** The base claim response endpoint. */
  private static String claimResponseEndpoint;

  @BeforeAll
  protected static synchronized void setup() throws IOException {
    //    setup(true);
    eobEndpoint = baseServerUrl + "/v2/fhir/ExplanationOfBenefit/";
    claimEndpoint = baseServerUrl + "/v2/fhir/Claim/";
    claimResponseEndpoint = baseServerUrl + "/v2/fhir/ClaimResponse/";
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

  /** Test that an EOB read request returns a 404 if the eob Id does not match any claims. */
  @Test
  public void testReadForMissingEobExpect404() {
    // Set eobId to something that will return no results
    String eobId = "carrier-9999999999";

    given()
        .spec(requestAuth)
        .expect()
        .body("issue[0].severity", equalTo("error"))
        .body(
            "issue[0].diagnostics",
            equalTo("HAPI-0971: Resource ExplanationOfBenefit/carrier-9999999999 is not known"))
        .statusCode(404)
        .when()
        .get(eobEndpoint + eobId);
  }

  /**
   * Verifies that the EOB returns a 200 with the expected DD paths for each claim type returned.
   */
  @Test
  public void testEobByPatientIdWithValidPatientIdExpect200() {

    List<Object> loadedData = testUtils.loadSampleAData();
    String patientId = testUtils.getPatientId(loadedData);
    String requestString = eobEndpoint + "?patient=" + patientId;
    OutpatientClaim expectedOutpatientClaim = testUtils.getClaim(loadedData, OutpatientClaim.class);

    given()
        .spec(requestAuth)
        .expect()
        // our top level is a bundle
        .body("resourceType", equalTo("Bundle"))
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
        // right path, with the right value. This is an example of a "data correctness" check.
        .body(
            "entry.find { it.resource.id.contains('outpatient') }.resource.extension.find { it.url == 'https://bluebutton.cms.gov/resources/variables/fi_doc_clm_cntl_num' }.valueIdentifier.value",
            equalTo(expectedOutpatientClaim.getFiDocumentClaimControlNumber().orElseThrow()))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Verify the response totals are accurate when SAMHSA data is excluded and the client is not
   * allowed to see SAMHSA data.
   */
  @Test
  public void testClaimFindByPatientWithExcludeSamhsaTrue() {
    String requestString =
        claimEndpoint + "?mbi=" + RDATestUtils.MBI_OLD_HASH + "&excludeSAMHSA=true";

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("Bundle"))
        .statusCode(200)
        .when()
        .get(requestString);
  }

  /**
   * Tests to see if the response size is accurate when SAMHSA is not excluded and the client can
   * see SAMHSA data.
   */
  @Test
  void shouldGetClaimResponseResourcesByMbiHashWithSamhsaAllowed() {
    String requestString =
        claimResponseEndpoint
            + "?mbi="
            + RDATestUtils.MBI_OLD_HASH
            + "&service-date=gt1970-07-18&service-date=lt1970-07-25";

    // Test passes as long as we get a 200 with an entry and not an error
    given()
        .spec(getRequestAuth(SAMHSA_KEYSTORE))
        .expect()
        .statusCode(200)
        .when()
        .get(requestString);
  }
}
