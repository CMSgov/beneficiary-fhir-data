package gov.cms.bfd.server.war.r4.providers.pac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.entities.RdaFissRevenueLine;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.codesystems.ExDiagnosistype;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** FISS claim transformer tests. */
public class FissClaimTransformerV2Test {
  /** Test diagnosis code 1. */
  private static final String DIAG_CODE1 = "DIAG_CODE1";

  /** Test diagnosis code 2. */
  private static final String DIAG_CODE2 = "DIAG_CODE2";

  /** Test diagnosis code 3. */
  private static final String DIAG_CODE3 = "DIAG_CODE3";

  /**
   * Test arguments.
   *
   * @return test arguments.
   */
  public static Stream<Arguments> diagnosisCodeTest() {
    return Stream.of(
        arguments(
            "Different admit and principal diagnosis codes and both codes included in main diagnosis code list",
            List.of(DIAG_CODE1, DIAG_CODE2, DIAG_CODE3),
            DIAG_CODE1,
            DIAG_CODE2,
            3),
        arguments(
            "Same code for admit and principal diagnosis and both codes included in main diagnosis code list",
            List.of(DIAG_CODE1, DIAG_CODE2, DIAG_CODE3),
            DIAG_CODE1,
            DIAG_CODE1,
            3),
        arguments(
            "Different admit and principal diagnosis codes and both codes NOT included in main diagnosis code list",
            List.of(DIAG_CODE3),
            DIAG_CODE1,
            DIAG_CODE2,
            3),
        arguments(
            "Same code for admit and principal diagnosis and both codes NOT included in main diagnosis code list",
            List.of(DIAG_CODE2),
            DIAG_CODE1,
            DIAG_CODE1,
            2),
        arguments(
            "Null diagnosis code",
            new ArrayList<String>() {
              {
                add(null);
                add(DIAG_CODE2);
              }
            },
            DIAG_CODE1,
            DIAG_CODE2,
            2),
        arguments(
            "Null principal diagnosis code", List.of(DIAG_CODE1, DIAG_CODE2), null, DIAG_CODE2, 2),
        arguments(
            "Null admit diagnosis code", List.of(DIAG_CODE1, DIAG_CODE2), DIAG_CODE2, null, 2),
        arguments("All codes missing", List.of(), null, null, 0),
        arguments(
            "Single null in the list",
            new ArrayList<String>() {
              {
                add(null);
              }
            },
            DIAG_CODE1,
            DIAG_CODE2,
            2));
  }

  /**
   * Tests that the diagnosis codes are set correctly.
   *
   * @param testName Test name.
   * @param diagCodes Diagnosis codes.
   * @param principalDiagCode Principal diagnosis code.
   * @param admitDiagCode Admit diagnosis code.
   * @param numberOfRecords Number of expected records.
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void diagnosisCodeTest(
      String testName,
      List<String> diagCodes,
      String principalDiagCode,
      String admitDiagCode,
      int numberOfRecords) {

    RdaFissClaim entity = new RdaFissClaim();

    entity.setLastUpdated(Instant.ofEpochMilli(1));
    entity.setStmtCovToDate(LocalDate.of(2020, 1, 1));
    entity.setPrincipleDiag(principalDiagCode);
    entity.setAdmitDiagCode(admitDiagCode);
    entity.setDrgCd("testDrg");
    Set<RdaFissDiagnosisCode> diagnoses =
        IntStream.range(0, diagCodes.size())
            .mapToObj(
                i -> {
                  RdaFissDiagnosisCode diagCode = new RdaFissDiagnosisCode();
                  diagCode.setRdaPosition((short) (i + 1));
                  diagCode.setDiagCd2(diagCodes.get(i));

                  return diagCode;
                })
            .collect(Collectors.toSet());

    entity.setDiagCodes(diagnoses);

    RdaFissRevenueLine line = new RdaFissRevenueLine();
    line.setHcpcCd("testCpt");

    entity.setRevenueLines(Set.of(line));
    FissClaimTransformerV2 fissClaimTransformerV2 =
        new FissClaimTransformerV2(new MetricRegistry());

    Claim claim = fissClaimTransformerV2.transform(entity, true);

    assertEquals(numberOfRecords, claim.getDiagnosis().size());

    List<Claim.DiagnosisComponent> admitCoding =
        claim.getDiagnosis().stream()
            .filter(diagnosis -> hasDiagnosisTypeCoding(diagnosis, ExDiagnosistype.ADMITTING))
            .toList();

    List<Claim.DiagnosisComponent> principalCoding =
        claim.getDiagnosis().stream()
            .filter(diagnosis -> hasDiagnosisTypeCoding(diagnosis, ExDiagnosistype.PRINCIPAL))
            .toList();

    assertEquals(admitDiagCode == null ? 0 : 1, admitCoding.size());
    assertEquals(principalDiagCode == null ? 0 : 1, principalCoding.size());

    if (admitDiagCode != null && admitDiagCode.equals(principalDiagCode)) {
      assertEquals(admitCoding, principalCoding);
    }

    if (admitDiagCode != null) {
      assertCodeEquals(admitDiagCode, admitCoding);
    }

    if (principalDiagCode != null) {
      assertCodeEquals(principalDiagCode, principalCoding);
    }

    short currentSequence = 1;
    for (Claim.DiagnosisComponent component : claim.getDiagnosis()) {
      assertEquals(currentSequence, component.getSequence());
      currentSequence++;
    }
  }

  /**
   * Checks if the component has an instance of the diagnosis type.
   *
   * @param diag Diagnosis component.
   * @param diagnosisType Diagnosis type.
   * @return If it has an instance.
   */
  static boolean hasDiagnosisTypeCoding(
      Claim.DiagnosisComponent diag, ExDiagnosistype diagnosisType) {

    long count =
        diag.getType().stream()
            .map(c -> c.getCoding().getFirst())
            .filter(c -> diagnosisType.toCode().equals(c.getCode()))
            .filter(c -> diagnosisType.getSystem().equals(c.getSystem()))
            .count();
    assertTrue(
        count < 2, String.format("More than one entry for diagnosis type %s", diagnosisType));
    return count > 0;
  }

  /**
   * Asserts that the code is set on the component.
   *
   * @param code Code.
   * @param components Components.
   */
  static void assertCodeEquals(String code, List<Claim.DiagnosisComponent> components) {
    assertEquals(1, components.size());
    CodeableConcept diagnosis = (CodeableConcept) components.getFirst().getDiagnosis();
    assertEquals(1, diagnosis.getCoding().size());
    assertEquals(code, diagnosis.getCoding().getFirst().getCode());
  }
}