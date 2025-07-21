package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import io.restassured.RestAssured;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;

public class PatientReadIT extends IntegrationTestBase {
  private IReadTyped<Patient> patientRead() {
    return getFhirClient().read().resource(Patient.class);
  }

  @Test
  void patientReadValidLong() {
    var patient = patientRead().withId(405764107L).execute();
    assertFalse(patient.isEmpty());
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadValidString() {
    var patient = patientRead().withId("405764107").execute();
    assertFalse(patient.isEmpty());
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadMergedBene() {
    var patient = patientRead().withId("792872340").execute();
    assertFalse(patient.isEmpty());
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadUnMergedWithHistoricKillCredit() {
    var patient = patientRead().withId("178083966").execute();
    assertFalse(patient.isEmpty());
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadMergedWithKillCredit() {
    var patient = patientRead().withId("878934873").execute();
    assertFalse(patient.isEmpty());
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadIdNotFound() {
    assertThrows(ResourceNotFoundException.class, () -> patientRead().withId("999").execute());
  }

  @Test
  void patientReadInvalidIdBadRequest() {
    assertThrows(InvalidRequestException.class, () -> patientRead().withId("abc").execute());
  }

  @ParameterizedTest
  @EmptySource
  void patientReadNoIdBadRequest(String id) {
    // Using RestAssured here because HAPI FHIR doesn't let us send a request with a blank ID
    RestAssured.get(getServerUrl() + "/Patient" + id).then().statusCode(400);
  }
}
