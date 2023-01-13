package gov.cms.bfd.server.war;

import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.server.sharedutils.BfdMDC;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class AccessJsonIT {
  /**
   * Integration tests for Access JSON.
   *
   * <p>These tests require the application WAR to be built and available in the local projects'
   * <code>target/</code> directories. Accordingly, they may not run correctly in Eclipse: if the
   * binaries aren't built yet, they'll just fail, but if older binaries exist (because you haven't
   * rebuilt them), it'll run using the old code, which probably isn't what you want.
   */
  /**
   * Verifies that access JSON is properly configured.
   *
   * @throws IOException (indicates a test error)
   */
  @Test
  public void normalUsage() throws IOException {
    // Verify that the access log is working, as expected.
    try {
      TimeUnit.MILLISECONDS.sleep(500); // Needed in some configurations to resolve a race condition
    } catch (InterruptedException e) {
    }

    Path accessLogJson =
        ServerTestUtils.getWarProjectDirectory()
            .resolve("target")
            .resolve("server-work")
            .resolve("access.json");
    assertTrue(Files.isReadable(accessLogJson));
    assertTrue(Files.size(accessLogJson) > 0);
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.BENE_ID));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.DATABASE_QUERY_BATCH));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.DATABASE_QUERY_TYPE));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.DATABASE_QUERY_BATCH_SIZE));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.DATABASE_QUERY_SIZE));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.DATABASE_QUERY_MILLI));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.DATABASE_QUERY_SUCCESS));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.DATABASE_QUERY_SOURCE_NAME));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HAPI_RESPONSE_TIMESTAMP_MILLI));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HAPI_POST_PROCESS_TIMESTAMP_MILLI));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HAPI_PRE_HANDLE_TIMESTAMP_MILLI));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HAPI_PRE_HANDLE_TIMESTAMP_MILLI));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HAPI_PRE_PROCESS_TIMESTAMP_MILLI));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HAPI_PROCESSING_COMPLETED_TIMESTAMP_MILLI));
    assertTrue(
        Files.readString(accessLogJson)
            .contains(BfdMDC.HAPI_PROCESSING_COMPLETED_NORM_TIMESTAMP_MILLI));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_CLIENTSSL_DN));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_TAX_NUMBERS));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ACCEPT));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ACCEPT_CHARSET));
    assertTrue(
        Files.readString(accessLogJson)
            .contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ACCEPT_ENCODING));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_ADDRESS_FIELDS));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_CONN_ENCODING));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_HOST_ENCODING));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_IDENTIFIERS));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_HEADER_USER_AGENT));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_HTTP_METHOD));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_OPERATION));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_QUERY_STR));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_TYPE));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_REQUEST_URL));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_PER_KB));
    assertTrue(
        Files.readString(accessLogJson)
            .contains(BfdMDC.HTTP_ACCESS_RESPONSE_DURATION_MILLISECONDS));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_ENCODING));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_LOCATION));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_TYPE));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_DATE));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_LAST_MODIFIED));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_POWERED_BY));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_RESPONSE_HEADER_REQUEST_ID));
    assertTrue(
        Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_RESPONSE_OUTPUT_SIZE_IN_BYTES));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.HTTP_ACCESS_RESPONSE_STATUS));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.JPA_QUERY_INCLUDE_TRUE));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.JPA_QUERY_DURATION_NANOSECONDS));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.JPA_QUERY_RECORD_COUNT));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.REQUEST_START_KEY));
    assertTrue(Files.readString(accessLogJson).contains(BfdMDC.RESOURCES_RETURNED));
  }
}
