package gov.cms.bfd.server.war;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifies that access.json is written to as expected. */
public class AccessJsonE2E extends ServerRequiredTest {
  /** Verifies that access.json is written to within BFD-server-war via API call. */
  @Test
  public void verifyAccessJson() throws IOException {

    Beneficiary beneficiary = testUtils.getFirstBeneficiary(testUtils.loadSampleAData());
    String mbiHash = beneficiary.getMbiHash().orElseThrow();
    String requestString =
        baseServerUrl
            + "/v2/fhir/Patient/?identifier="
            + TransformerConstants.CODING_BBAPI_BENE_MBI_HASH
            + "|"
            + mbiHash;

    given().spec(requestAuth).expect().statusCode(200).when().get(requestString);

    // Verify that the access JSON is working, as expected.
    /*try {
      TimeUnit.MILLISECONDS.sleep(1000); // Needed to resolve a race condition
    } catch (InterruptedException ignored) {
    }*/

    Path accessLogJson =
        ServerTestUtils.getWarProjectDirectory()
            .resolve("target")
            .resolve("server-work")
            .resolve("access.json");
    assertTrue(Files.isReadable(accessLogJson));
    assertTrue(Files.size(accessLogJson) > 0);

    String content = Files.readString(accessLogJson);
    assertTrue(content.contains(BfdMDC.BENE_ID));
    assertTrue(content.contains(BfdMDC.DATABASE_QUERY_BATCH));
    assertTrue(content.contains(BfdMDC.DATABASE_QUERY_TYPE));
    assertTrue(content.contains(BfdMDC.DATABASE_QUERY_BATCH_SIZE));
    assertTrue(content.contains(BfdMDC.DATABASE_QUERY_SIZE));
    assertTrue(content.contains(BfdMDC.DATABASE_QUERY_MILLI));
    assertTrue(content.contains(BfdMDC.DATABASE_QUERY_SUCCESS));
    assertTrue(content.contains(BfdMDC.DATABASE_QUERY_SOURCE_NAME));
    assertTrue(content.contains(BfdMDC.HAPI_RESPONSE_TIMESTAMP_MILLI));
    assertTrue(content.contains(BfdMDC.HAPI_POST_PROCESS_TIMESTAMP_MILLI));
    assertTrue(content.contains(BfdMDC.HAPI_PRE_HANDLE_TIMESTAMP_MILLI));
    assertTrue(content.contains(BfdMDC.HAPI_PRE_HANDLE_TIMESTAMP_MILLI));
    assertTrue(content.contains(BfdMDC.HAPI_PRE_PROCESS_TIMESTAMP_MILLI));
    assertTrue(content.contains(BfdMDC.HAPI_PROCESSING_COMPLETED_TIMESTAMP_MILLI));
    assertTrue(content.contains(BfdMDC.HAPI_PROCESSING_COMPLETED_NORM_TIMESTAMP_MILLI));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_REQUEST_CLIENTSSL_DN));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ACCEPT));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ACCEPT_CHARSET));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ACCEPT_ENCODING));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_CONN_ENCODING));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_HOST_ENCODING));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_USER_AGENT));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_REQUEST_HTTP_METHOD));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_REQUEST_OPERATION));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_REQUEST_QUERY_STR));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_REQUEST_TYPE));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_REQUEST_URI));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_REQUEST_URL));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_PER_KB));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_MILLISECONDS));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_ENCODING));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_LOCATION));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_TYPE));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_DATE));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_LAST_MODIFIED));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_POWERED_BY));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_REQUEST_ID));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_RESPONSE_OUTPUT_SIZE_IN_BYTES));
    assertTrue(content.contains(BfdMDC.HTTP_ACCESS_RESPONSE_STATUS));
    assertTrue(content.contains(BfdMDC.RESOURCES_RETURNED));
  }
}
