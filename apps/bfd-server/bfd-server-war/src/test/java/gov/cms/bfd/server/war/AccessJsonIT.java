package gov.cms.bfd.server.war;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.sharedutils.BfdMDC;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

/** Verifies that access.json is written to as expected. */
public class AccessJsonIT extends ServerRequiredTest {
  /** Verifies that access.json is written to within BFD-server-war via API call. */
  @Test
  public void VerifyAccessJson() throws IOException {
    /*
     * Write to access.json by checking {@link
     * gov.cms.bfd.server.war.r4.providers.R4PatientResourceProvider#searchByIdentifier(ca.uhn.fhir.rest.param.TokenParam)}
     * for a {@link Patient} that does exist in the DB, including identifiers to return the unhashed
     * HICN and MBI.
     */
    List<Object> loadedRecords =
        ServerTestUtils.get()
            .loadData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    IGenericClient fhirClient = ServerTestUtils.get().createFhirClientV2();

    Beneficiary beneficiary =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .findFirst()
            .get();
    Bundle searchResults =
        fhirClient
            .search()
            .forResource(Patient.class)
            .where(
                Patient.IDENTIFIER
                    .exactly()
                    .systemAndIdentifier(
                        TransformerConstants.CODING_BBAPI_BENE_MBI_HASH,
                        beneficiary.getMbiHash().get()))
            .returnBundle(Bundle.class)
            .execute();

    assertNotNull(searchResults);
    Patient patientFromSearchResult = (Patient) searchResults.getEntry().get(0).getResource();

    /*
     * Ensure the unhashed values for MBI is present.
     */
    Boolean mbiUnhashedPresent = false;
    Iterator<Identifier> identifiers = patientFromSearchResult.getIdentifier().iterator();
    while (identifiers.hasNext()) {
      Identifier identifier = identifiers.next();
      if (identifier
          .getSystem()
          .equals(TransformerConstants.CODING_BBAPI_MEDICARE_BENEFICIARY_ID_UNHASHED)) {
        mbiUnhashedPresent = true;
      }
    }

    assertTrue(mbiUnhashedPresent);

    // Verify that the access JSON is working, as expected.
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
    assertTrue(content.contains(BfdMDC.RESOURCES_RETURNED));
  }
}
