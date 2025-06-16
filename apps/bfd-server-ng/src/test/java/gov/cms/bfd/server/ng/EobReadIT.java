package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertFalse;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
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
}
