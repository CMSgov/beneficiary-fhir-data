package gov.cms.bfd.server.war.r4.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerRequiredTest;
import gov.cms.bfd.server.war.ServerTestUtils;
import io.restassured.path.json.JsonPath;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Endpoint end-to-end test for the V2 explanation of benefits endpoint. */
public class ExplanationOfBenefitEndpointE2E extends ServerRequiredTest {

  /** The base eob endpoint. */
  public static String EOB_ENDPOINT;

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    // Set this up once, after the server has run
    if (EOB_ENDPOINT == null) {
      EOB_ENDPOINT = baseServerUrl + "/v2/fhir/ExplanationOfBenefit/";
    }
  }

  /**
   * Verifies that an EOB carrier claim can be requested using the read endpoint, successfully
   * returns a 200 response, and has the claim id and a couple other EOB details present in the
   * body.
   */
  @Test
  public void testReadEobIdExpectClmIdInResponse() {

    CarrierClaim claim = getCarrierClaim();
    String claimId = String.valueOf(claim.getClaimId());
    String eobId = TransformerUtilsV2.buildEobId(ClaimTypeV2.CARRIER, claim.getClaimId());

    given()
        .spec(requestAuth)
        .expect()
        .body("resourceType", equalTo("ExplanationOfBenefit"))
        .body("id", equalTo(eobId))
        /* RestAssured uses Groovy under the hood, so our search query closures can use Groovy's collection API.
         * See https://www.baeldung.com/rest-assured-groovy for some examples.
         */
        .body(
            "identifier.find {it.system == 'https://bluebutton.cms.gov/resources/variables/clm_id'}.value",
            equalTo(claimId))
        .statusCode(200)
        .when()
        .get(EOB_ENDPOINT + eobId);

    // TODO: Remove; example

    /* An alternative is also using extract() to just get the response and use jsonPath to validate things piecemeal.
     * This avoids needing to know the groovy collection API (GPath) details, but is more verbose/complex, and a little less powerful.
     * This example doesn't get the nice check that we're validating the clm_id, and we can't cast nicely to an Identifier either. We'd need extra logic for that. */
    JsonPath jsonResponse =
        given().spec(requestAuth).get(EOB_ENDPOINT + eobId).then().extract().response().jsonPath();
    List<String> values = jsonResponse.getList("value");
    for (String value : values) {
      assertEquals(claimId, value);
    }
  }

  /** Verifies that the EOB returns a 400 when called with a negative count parameter. */
  @Test
  public void testEobByIdWithNegativeCountExpect400() {

    String patientId = getPatientId();
    String requestString = EOB_ENDPOINT + "?patient=" + patientId + "&count=-10";

    // TODO: This isnt getting the _right_ 400; need to get the right params/format for the request.
    // Look at splunk? Headers maybe?
    given().spec(requestAuth).expect().log().body().statusCode(400).when().get(requestString);
  }

  /**
   * Gets a carrier claim for basic test information.
   *
   * @return the carrier claim
   */
  public CarrierClaim getCarrierClaim() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    return loadedRecords.stream()
        .filter(r -> r instanceof CarrierClaim)
        .map(CarrierClaim.class::cast)
        .findFirst()
        .get();
  }

  /**
   * Gets the patient id.
   *
   * @return the patient id
   */
  public String getPatientId() {
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(Beneficiary.class::cast)
            .findFirst()
            .get();
    return TransformerUtilsV2.buildPatientId(beneficiary).getIdPart();
  }
}
