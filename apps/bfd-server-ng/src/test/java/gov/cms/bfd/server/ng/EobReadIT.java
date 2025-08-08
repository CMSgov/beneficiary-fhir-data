package gov.cms.bfd.server.ng;

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
    var eob = eobRead().withId(1071939711295L).execute();
    assertFalse(eob.isEmpty());
    expect.serializer("fhir+json").toMatchSnapshot(eob);
  }

  @Test
  void eobReadValidString() {
    var patient = eobRead().withId("1071939711295").execute();
    assertFalse(patient.isEmpty());
    expect.serializer("fhir+json").toMatchSnapshot(patient);
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
