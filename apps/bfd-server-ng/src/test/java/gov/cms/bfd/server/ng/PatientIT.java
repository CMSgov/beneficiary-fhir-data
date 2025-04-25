package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertThrows;

import au.com.origin.snapshots.Expect;
import au.com.origin.snapshots.junit5.SnapshotExtension;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import java.io.IOException;
import java.net.URISyntaxException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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

  private String getServerUrl() {
    return "http://localhost:" + port + "/v3/fhir";
  }

  private IGenericClient getFhirClient() {
    FhirContext ctx = FhirContext.forR4();
    return ctx.newRestfulGenericClient("http://localhost:" + port + "/v3/fhir");
  }

  @Test
  void patientReadValidLong() {
    var patient = getFhirClient().read().resource("Patient").withId(1L).execute();
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadValidString() {
    var patient = getFhirClient().read().resource("Patient").withId("1").execute();
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @ParameterizedTest
  @ValueSource(strings = {"abc"})
  void patientReadInvalidId(String id) {
    assertThrows(
        InvalidRequestException.class,
        () -> getFhirClient().read().resource("Patient").withId(id).execute());
  }

  @ParameterizedTest
  @EmptySource
  void patientReadNoId(String id) throws URISyntaxException, IOException, InterruptedException {}
}
