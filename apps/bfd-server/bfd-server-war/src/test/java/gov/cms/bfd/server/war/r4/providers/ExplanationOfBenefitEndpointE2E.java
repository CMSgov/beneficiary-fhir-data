package gov.cms.bfd.server.war.r4.providers;

import static io.restassured.RestAssured.certificate;
import static io.restassured.RestAssured.given;

import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerRequiredTest;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider;
import io.restassured.authentication.AuthenticationScheme;
import io.restassured.authentication.CertificateAuthSettings;
import io.restassured.builder.RequestSpecBuilder;
import java.util.Arrays;
import java.util.List;

import io.restassured.specification.RequestSpecification;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Endpoint end-to-end test for the V2 explanation of benefits endpoint. */
public class ExplanationOfBenefitEndpointE2E extends ServerRequiredTest {

  /** Parameter name for excluding SAMHSA. */
  public static final String EXCLUDE_SAMHSA_PARAM = "excludeSAMHSA";

  /** The base eob endpoint. */
  public static String EOB_ENDPOINT;

  /** The request spec with the auth certificate to use when connecting to the test server. */
  RequestSpecification requestAuth;

  /** Sets up the test resources. */
  @BeforeEach
  public void setupTest() {
    // Set this up once, after the server has run
    if (EOB_ENDPOINT == null) {
      // Setup the base url after the server has started
      EOB_ENDPOINT = baseServerUrl + "/v2/fhir/ExplanationOfBenefit/";
      // Get the certs for the test
      String trustStorePath = "src/test/resources/certs/test-truststore.jks";
      String keyStorePath = "src/test/resources/certs/test-keystore.p12";
      String testPassword = "changeit";
      // Set up the cert for the calls
      AuthenticationScheme testCertificate =
          certificate(
              trustStorePath,
              testPassword,
              keyStorePath,
              testPassword,
              CertificateAuthSettings.certAuthSettings()
                  .keyStoreType("pkcs12")
                  .trustStoreType("pkcs12")
                  .allowAllHostnames());
      requestAuth = new RequestSpecBuilder().setBaseUri(baseServerUrl).setAuth(testCertificate).build();
    }
  }

  /**
   * Verifies that {@link ExplanationOfBenefitResourceProvider#read} works as expected for a {@link
   * CarrierClaim}-derived {@link ExplanationOfBenefit} that exists in the DB.
   */
  @Test
  public void testReadForSampleAEob() {

    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    CarrierClaim claim =
        loadedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(CarrierClaim.class::cast)
            .findFirst()
            .get();

    String eobId = TransformerUtilsV2.buildEobId(ClaimTypeV2.CARRIER, claim.getClaimId());

    //TODO: Start doing some validation on the response!

    given()
        .spec(requestAuth)
        .expect()
        .statusCode(200)
        .when()
        .get(EOB_ENDPOINT + eobId);
  }
}
