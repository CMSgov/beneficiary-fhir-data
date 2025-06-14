package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertFalse;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.Test;

public class EobReadIT extends IntegrationTestBase {
  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  @Test
  void eobReadValidLong() {
    var eob = eobRead().withId(1L).execute();
    assertFalse(eob.isEmpty());
  }
}
