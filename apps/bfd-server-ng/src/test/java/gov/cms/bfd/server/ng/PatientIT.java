package gov.cms.bfd.server.ng;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import ca.uhn.fhir.context.FhirContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith({SnapshotExtension.class})
@Import(IntegrationTestConfiguration.class)
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PatientIT {
  @LocalServerPort private int port;
  private Expect expect;

  @Test
  void patientRead() {
    FhirContext ctx = FhirContext.forR4();
    var client = ctx.newRestfulGenericClient("http://localhost:" + port + "/v3/fhir");
    var patient = client.read().resource("Patient").withId(1L).execute();

    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }
}
