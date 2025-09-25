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

    var rawCodes = List.of("30492", "25000", "E8889", "V1005");
    var independentlyFormatted =
        rawCodes.stream()
            .collect(
                Collectors.toMap(
                    c -> c,
                    c -> IcdIndicator.ICD_9.formatCode(c),
                    (a, b) -> a,
                    java.util.LinkedHashMap::new));

    var expectedOrder = List.of("304.92", "250.00", "E888.9", "V10.05");
    assertEquals(expectedOrder, eobCodes, "ICD-9 codes should be formatted and ordered");
    assertEquals("250.00", independentlyFormatted.get("25000"));
    assertEquals("E888.9", independentlyFormatted.get("E8889"));
    assertEquals("V10.05", independentlyFormatted.get("V1005"));

    // Snapshot a compact payload instead of the entire EOB to reduce churn.
    var snapshotPayload =
        new java.util.LinkedHashMap<String, Object>() {
          {
            put("eobDiagnosisCodes", eobCodes);
            put("formattedFromIndicator", independentlyFormatted);
          }
        };
    expect.scenario("eobAndIcd9Formats").toMatchSnapshot(snapshotPayload);
  }

  @Test
  void eobContainsAllFormattedIcd9DiagnosisCodes() {
    var eob = eobRead().withId(CLAIM_ID_ADJUDICATED_ICD_9).execute();
    var codes =
        eob.getDiagnosis().stream()
            .map(d -> d.getDiagnosisCodeableConcept().getCodingFirstRep().getCode())
            .toList();

    // Expected order should reflect CLM_VAL_SQNC_NUM in the CSV for signature 322823692140.
    var expected = List.of("304.92", "250.00", "E888.9", "V10.05");
    assertEquals(expected, codes, "ICD-9 diagnosis codes should be formatted and ordered");

    var snapshotPayload = java.util.Map.of("codes", codes);
    expect.scenario("icd9DiagnosisCodes").toMatchSnapshot(snapshotPayload);
  }

  @Test
  public void snapshotFormattedCodes() {
    var icd10 = IcdIndicator.ICD_10.formatCode("F1010");
    var icd9Diag = IcdIndicator.ICD_9.formatCode("12345");
    var icd9Proc = IcdIndicator.ICD_9.formatProcedureCode("12345");

    var snapshotPayload =
        new java.util.LinkedHashMap<String, String>() {
          {
            put("icd9Proc", icd9Proc);
            put("icd10", icd10);
            put("icd9Diag", icd9Diag);
          }
        };

    expect.scenario("icdFormattedValues").toMatchSnapshot(snapshotPayload);
  }
}
