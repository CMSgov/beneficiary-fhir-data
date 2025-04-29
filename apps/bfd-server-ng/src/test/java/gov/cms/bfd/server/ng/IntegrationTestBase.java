package gov.cms.bfd.server.ng;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@Import(IntegrationTestConfiguration.class)
@ExtendWith({SnapshotExtension.class})
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class IntegrationTestBase {
  @LocalServerPort protected int port;
  protected Expect expect;

  protected String getServerUrl() {
    return "http://localhost:" + port + "/v3/fhir";
  }

  protected IGenericClient getFhirClient() {
    FhirContext ctx = FhirContext.forR4();
    return ctx.newRestfulGenericClient(getServerUrl());
  }
}
