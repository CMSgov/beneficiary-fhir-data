package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import io.restassured.RestAssured;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;

public class EobReadIT extends IntegrationTestBase {
  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  @Test
  void eobReadValidLong() {
    var eob = eobRead().withId(Long.parseLong(CLAIM_ID_ADJUDICATED)).execute();
    assertFalse(eob.isEmpty());
    expect.serializer("fhir+json").toMatchSnapshot(eob);
  }

  @Test
  void eobReadValidString() {
    var patient = eobRead().withId(CLAIM_ID_ADJUDICATED).execute();
    assertFalse(patient.isEmpty());
    expect.serializer("fhir+json").toMatchSnapshot(patient);
  }

  @Test
  void eobReadPhase1() {
    var eob = eobRead().withId(CLAIM_ID_PHASE_1).execute();
    assertFalse(eob.isEmpty());
    expect.serializer("fhir+json").toMatchSnapshot(eob);
  }

  @Test
  void eobReadPhase2() {
    var eob = eobRead().withId(CLAIM_ID_PHASE_2).execute();
    assertFalse(eob.isEmpty());
    assertEquals("QUEUED", eob.getOutcome().name());
    expect.serializer("fhir+json").toMatchSnapshot(eob);
  }

  @Test
  void eobReadIdNotFound() {
    assertThrows(ResourceNotFoundException.class, () -> eobRead().withId("999").execute());
  }

  @Test
  void eobReadInvalidIdBadRequest() {
    assertThrows(InvalidRequestException.class, () -> eobRead().withId("abc").execute());
  }

  @ParameterizedTest
  @EmptySource
  void eobReadNoIdBadRequest(String id) {
    // Using RestAssured here because HAPI FHIR doesn't let us send a request with a blank ID
    RestAssured.get(getServerUrl() + "/ExplanationOfBenefit" + id).then().statusCode(400);
  }
}
