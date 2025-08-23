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
    var patient = patientRead().withId(Long.parseLong(BENE_ID_PART_A_ONLY)).execute();
    assertFalse(patient.isEmpty());
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadValidString() {
    var patient = patientRead().withId(BENE_ID_PART_A_ONLY).execute();
    assertEquals(1, patient.getIdentifier().size());
    assertEquals(0, patient.getLink().size());
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadMergedBene() {
    var patient = patientRead().withId(HISTORICAL_MERGED_BENE_SK).execute();

    assertFalse(patient.isEmpty(), "Patient should not be empty");
    assertEquals(0, patient.getIdentifier().size(), "Expected no identifiers");
    assertEquals(1, patient.getLink().size(), "Expected exactly one link");

    var link = patient.getLinkFirstRep();
    var other = link.getOther();

    assertEquals(Patient.LinkType.REPLACEDBY, link.getType(), "Link type should be REPLACEDBY");
    assertEquals(
        CURRENT_MERGED_BENE_SK,
        other.getDisplay(),
        String.format("Link display should be '%s'", CURRENT_MERGED_BENE_SK));

    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadUnMergedWithHistoricBenes() {
    var patient = patientRead().withId(CURRENT_MERGED_BENE_SK).execute();
    assertFalse(patient.isEmpty());
    assertEquals(2, patient.getIdentifier().size());
    assertFalse(
        patient.getIdentifier().stream()
            .anyMatch(i -> i.getValue().equals(HISTORICAL_MERGED_MBI_KILL_CREDIT)));

    assertEquals(2, patient.getLink().size());
    assertTrue(
        patient.getLink().stream().allMatch(link -> link.getType() == Patient.LinkType.REPLACES),
        "Expected all link types to be 'replaces'");
    assertTrue(
        patient.getLink().stream()
            .map(link -> link.getOther().getDisplay())
            .anyMatch(HISTORICAL_MERGED_BENE_SK2::equals),
        String.format("Expected to find a link with display '%s'", HISTORICAL_MERGED_BENE_SK2));
    assertTrue(
        patient.getLink().stream()
            .map(link -> link.getOther().getDisplay())
            .anyMatch(HISTORICAL_MERGED_BENE_SK::equals),
        String.format("Expected to find a link with display '%s'", HISTORICAL_MERGED_BENE_SK));

    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void patientReadUnMergedWithHistoricKillCredit() {
    var patient = patientRead().withId(HISTORICAL_MERGED_BENE_SK_KILL_CREDIT).execute();
    assertFalse(patient.isEmpty());
    assertEquals(1, patient.getIdentifier().size());
    assertEquals(0, patient.getLink().size());
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
