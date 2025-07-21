package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    assertEquals(1, patient.getIdentifier().size());
    assertEquals(1, patient.getLink().size());
    assertEquals("181968400", patient.getLinkFirstRep().getOther().getDisplay());
    assertFalse(patient.isEmpty());
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadMergedBene() {
    var patient = patientRead().withId("792872340").execute();

    assertFalse(patient.isEmpty(), "Patient should not be empty");
    assertEquals(0, patient.getIdentifier().size(), "Expected no identifiers");
    assertEquals(1, patient.getLink().size(), "Expected exactly one link");

    var link = patient.getLinkFirstRep();
    var other = link.getOther();

    assertEquals(Patient.LinkType.REPLACEDBY, link.getType(), "Link type should be REPLACEDBY");
    assertEquals("178083966", other.getDisplay(), "Link display should be '178083966'");


    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadUnMergedWithHistoricBenes() {
    var patient = patientRead().withId("517782585").execute();
    assertFalse(patient.isEmpty());
    assertEquals(1, patient.getIdentifier().size());
    assertEquals(2, patient.getLink().size());
    assertTrue(
        patient.getLink().stream().allMatch(link -> link.getType() == Patient.LinkType.REPLACES),
        "Expected all link types to be 'replaces'");
    assertTrue(
        patient.getLink().stream()
            .map(link -> link.getOther().getDisplay())
            .anyMatch("121212121"::equals),
        "Expected to find a link with display '121212121'");
    assertTrue(
        patient.getLink().stream()
            .map(link -> link.getOther().getDisplay())
            .anyMatch("848484848"::equals),
        "Expected to find a link with display '848484848'");
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadUnMergedWithHistoricKillCredit() {
    var patient = patientRead().withId("178083966").execute();
    assertFalse(patient.isEmpty());
    assertEquals(1, patient.getIdentifier().size());
    assertTrue(
        patient.getLink().stream().allMatch(link -> link.getType() == Patient.LinkType.REPLACES),
        "Expected all link types to be 'replaces'");
    assertEquals(1, patient.getLink().size());
    assertEquals("792872340", patient.getLinkFirstRep().getOther().getDisplay());
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
