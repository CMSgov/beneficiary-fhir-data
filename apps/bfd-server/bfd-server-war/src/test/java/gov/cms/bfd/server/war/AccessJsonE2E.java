package gov.cms.bfd.server.war;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Verifies that access.json is written to as expected. */
public class AccessJsonE2E extends ServerRequiredTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(AccessJsonE2E.class);

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

    Path accessLogJson =
        ServerTestUtils.getWarProjectDirectory()
            .resolve("target")
            .resolve("server-work")
            .resolve("access.json");

    // Empty the access json to avoid pollution from other tests
    try (BufferedWriter writer = Files.newBufferedWriter(accessLogJson)) {
      Files.writeString(accessLogJson, "");
    }

    given()
        .spec(requestAuth)
        .contentType("application/json")
        .expect()
        .statusCode(200)
        .when()
        .get(requestString);

    assertTrue(Files.isReadable(accessLogJson));
    assertTrue(Files.size(accessLogJson) > 0);

    String content = Files.readString(accessLogJson);

    LOGGER.info(content);

    List<String> headersToCheck =
        List.of(
            BfdMDC.BENE_ID,
            BfdMDC.DATABASE_QUERY_BATCH,
            BfdMDC.DATABASE_QUERY_TYPE,
            BfdMDC.DATABASE_QUERY_BATCH_SIZE,
            BfdMDC.DATABASE_QUERY_SIZE,
            BfdMDC.DATABASE_QUERY_MILLI,
            BfdMDC.DATABASE_QUERY_SUCCESS,
            BfdMDC.DATABASE_QUERY_SOURCE_NAME,
            BfdMDC.HAPI_RESPONSE_TIMESTAMP_MILLI,
            BfdMDC.HAPI_POST_PROCESS_TIMESTAMP_MILLI,
            BfdMDC.HAPI_PRE_HANDLE_TIMESTAMP_MILLI,
            BfdMDC.HAPI_PRE_HANDLE_TIMESTAMP_MILLI,
            BfdMDC.HAPI_PRE_PROCESS_TIMESTAMP_MILLI,
            BfdMDC.HAPI_PROCESSING_COMPLETED_TIMESTAMP_MILLI,
            BfdMDC.HAPI_PROCESSING_COMPLETED_NORM_TIMESTAMP_MILLI,
            BfdMDC.HTTP_ACCESS_REQUEST_CLIENTSSL_DN,
            BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ACCEPT,
            BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ACCEPT_ENCODING,
            BfdMDC.HTTP_ACCESS_REQUEST_HEADER_CONN_ENCODING,
            BfdMDC.HTTP_ACCESS_REQUEST_HEADER_HOST_ENCODING,
            BfdMDC.HTTP_ACCESS_REQUEST_HEADER_USER_AGENT,
            BfdMDC.HTTP_ACCESS_REQUEST_HTTP_METHOD,
            BfdMDC.HTTP_ACCESS_REQUEST_OPERATION,
            BfdMDC.HTTP_ACCESS_REQUEST_QUERY_STR,
            BfdMDC.HTTP_ACCESS_REQUEST_TYPE,
            BfdMDC.HTTP_ACCESS_REQUEST_URI,
            BfdMDC.HTTP_ACCESS_REQUEST_URL,
            BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_PER_KB,
            BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_MILLISECONDS,
            BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_MILLISECONDS,
            BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_ENCODING,
            BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_TYPE,
            BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_DATE,
            BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_LAST_MODIFIED,
            BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_POWERED_BY,
            BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_REQUEST_ID,
            BfdMDC.HTTP_ACCESS_RESPONSE_OUTPUT_SIZE_IN_BYTES,
            BfdMDC.HTTP_ACCESS_RESPONSE_STATUS,
            BfdMDC.RESOURCES_RETURNED);

    List<String> missingHeader = new ArrayList<>();
    for (String header : headersToCheck) {
      if (!content.contains(header)) {
        missingHeader.add(header);
      }
    }
    // Check we have no missing headers in a way that makes a useful assertion error if not
    assertEquals(new ArrayList<>(), missingHeader, "Missing expected headers in access.json!");
  }
}
