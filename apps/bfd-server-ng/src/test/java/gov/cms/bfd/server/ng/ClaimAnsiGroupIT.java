package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.AdjudicationComponent;
import org.junit.jupiter.api.Test;

/**
 * Integration test to verify that revenue-center ANSI group codes from the pipeline CSV fixtures
 * are loaded and exposed on the generated EOBs as adjudication codings.
 */
public class ClaimAnsiGroupIT extends IntegrationTestBase {

  @Test
  void eobContainsAnsiGroupCodings() {
    var eob =
        getFhirClient()
            .read()
            .resource(ExplanationOfBenefit.class)
            .withId(CLAIM_ID_ADJUDICATED)
            .execute();
    assertFalse(eob.isEmpty());

    // Collect adjudication codings from the EOB
    List<AdjudicationComponent> adjs = eob.getAdjudication();
    boolean foundGroupCoding =
        adjs.stream()
            .flatMap(
                a -> {
                  if (a.getReason() == null) return java.util.stream.Stream.empty();
                  return a.getReason().getCoding().stream();
                })
            .map(Coding::getSystem)
            .filter(s -> s != null)
            .anyMatch(
                s -> s.toLowerCase().contains("adjustment") || s.toLowerCase().contains("ansi"));

    // At least one adjudication coding should reference an ANSI group code system when group codes
    // are present in the source CSV fixtures.
    assertTrue(
        foundGroupCoding, "Expected at least one ANSI group coding in adjudication components");
  }
}
