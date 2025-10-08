package gov.cms.bfd.server.ng;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import ca.uhn.fhir.rest.gclient.IReadTyped;
import gov.cms.bfd.server.ng.claim.model.IcdIndicator;
import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.jupiter.api.Test;

// Integration test for ICD code formatting.
public class IcdIndicatorIT extends IntegrationTestBase {
  private static final String FORMATTED_ICD_9_CODE_25000 = "250.00";
  private static final String FORMATTED_ICD_9_CODE_E8889 = "E888.9";
  private static final String FORMATTED_ICD_9_CODE_V1005 = "V10.05";
  private static final String RAW_30495 = "30495";
  private static final String RAW_25000 = "25000";
  private static final String RAW_E8889 = "E8889";
  private static final String RAW_V1005 = "V1005";
  private static final String FORMATTED_ICD_9_CODE_30495 = "304.95";

  private IReadTyped<ExplanationOfBenefit> eobRead() {
    return getFhirClient().read().resource(ExplanationOfBenefit.class);
  }

  @Test
  void eobReadAndIcd9Formatting() {
    var eob = eobRead().withId(CLAIM_ID_ADJUDICATED_ICD_9).execute();
    assertFalse(eob.isEmpty());

    // Extract formatted diagnosis codes from the EOB.
    var eobCodes =
        eob.getDiagnosis().stream()
            .map(d -> d.getDiagnosisCodeableConcept().getCodingFirstRep().getCode())
            .toList();

    var rawCodes = List.of(RAW_E8889, RAW_V1005, RAW_30495, RAW_25000);
    var independentlyFormatted =
        rawCodes.stream()
            .collect(
                Collectors.toMap(
                    c -> c,
                    IcdIndicator.ICD_9::formatDiagnosisCode,
                    (a, b) -> a,
                    java.util.LinkedHashMap::new));

    var expectedOrder =
        List.of(
            FORMATTED_ICD_9_CODE_30495,
            FORMATTED_ICD_9_CODE_25000,
            FORMATTED_ICD_9_CODE_E8889,
            FORMATTED_ICD_9_CODE_V1005);
    assertEquals(expectedOrder, eobCodes, "ICD-9 codes should be formatted and ordered");
    assertEquals(FORMATTED_ICD_9_CODE_25000, independentlyFormatted.get(RAW_25000));
    assertEquals(FORMATTED_ICD_9_CODE_E8889, independentlyFormatted.get(RAW_E8889));
    assertEquals(FORMATTED_ICD_9_CODE_V1005, independentlyFormatted.get(RAW_V1005));

    // assert string representation instead of a snapshot.
    var expectedMap = new java.util.LinkedHashMap<String, String>();
    expectedMap.put(RAW_E8889, FORMATTED_ICD_9_CODE_E8889);
    expectedMap.put(RAW_V1005, FORMATTED_ICD_9_CODE_V1005);
    expectedMap.put(RAW_30495, FORMATTED_ICD_9_CODE_30495);
    expectedMap.put(RAW_25000, FORMATTED_ICD_9_CODE_25000);

    var actualSnapshotString = eobCodes.toString() + "|" + independentlyFormatted.toString();
    var expectedSnapshotString = expectedOrder.toString() + "|" + expectedMap.toString();
    assertEquals(
        expectedSnapshotString,
        actualSnapshotString,
        "Compact string comparison of codes and formatted map should match");
  }

  @Test
  void eobContainsAllFormattedIcd9DiagnosisCodes() {
    var eob = eobRead().withId(CLAIM_ID_ADJUDICATED_ICD_9).execute();
    var codes =
        eob.getDiagnosis().stream()
            .map(d -> d.getDiagnosisCodeableConcept().getCodingFirstRep().getCode())
            .toList();

    var expected =
        List.of(
            FORMATTED_ICD_9_CODE_30495,
            FORMATTED_ICD_9_CODE_25000,
            FORMATTED_ICD_9_CODE_E8889,
            FORMATTED_ICD_9_CODE_V1005);
    assertEquals(expected, codes, "ICD-9 diagnosis codes should be formatted and ordered");

    // compare string representations rather than using snapshot storage.
    assertEquals(expected.toString(), codes.toString(), "Compact codes string should match");
  }
}
