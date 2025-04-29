package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;

public class PatientReadIT extends IntegrationTestBase {

  @Test
  void patientReadValidLong() {
    var patient = getFhirClient().read().resource("Patient").withId(1L).execute();
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadValidString() {
    var patientFromId = getFhirClient().read().resource("Patient").withId("1").execute();
    expect.serializer("fhir+json").toMatchSnapshot(patientFromId);
  }

  @Test
  void patientReadIdNotFound() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> getFhirClient().read().resource("Patient").withId("999").execute());
  }

  @Test
  void patientReadInvalidIdBadRequest() {
    assertThrows(
        InvalidRequestException.class,
        () -> getFhirClient().read().resource("Patient").withId("abc").execute());
  }

  @ParameterizedTest
  @EmptySource
  void patientReadNoIdBadRequest(String id) {
    // Using RestAssured here because HAPI FHIR doesn't let us send a request with a blank ID
    RestAssured.get(getServerUrl() + "/Patient" + id).then().statusCode(400);
  }
}
