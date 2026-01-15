package gov.cms.bfd.server.war.r4.providers.pac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.entities.RdaFissRevenueLine;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
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

  Set<String> securityTags = new HashSet<>();

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
            List.of(DIAG_CODE1, DIAG_CODE2, DIAG_CODE3),
            3),
        arguments(
            "Same code for admit and principal diagnosis and both codes included in main diagnosis code list",
            List.of(DIAG_CODE1, DIAG_CODE2, DIAG_CODE3),
            DIAG_CODE1,
            DIAG_CODE1,
            List.of(DIAG_CODE1, DIAG_CODE2, DIAG_CODE3),
            3),
        arguments(
            "Different admit and principal diagnosis codes and both codes NOT included in main diagnosis code list",
            List.of(DIAG_CODE3),
            DIAG_CODE1,
            DIAG_CODE2,
            List.of(DIAG_CODE3, DIAG_CODE2, DIAG_CODE1),
            3),
        arguments(
            "Same code for admit and principal diagnosis and both codes NOT included in main diagnosis code list",
            List.of(DIAG_CODE2),
            DIAG_CODE1,
            DIAG_CODE1,
            List.of(DIAG_CODE2, DIAG_CODE1),
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
            List.of(DIAG_CODE2, DIAG_CODE1),
            2),
        arguments(
            "Null principal diagnosis code",
            List.of(DIAG_CODE1, DIAG_CODE2),
            null,
            DIAG_CODE2,
            List.of(DIAG_CODE1, DIAG_CODE2),
            2),
        arguments(
            "Null admit diagnosis code",
            List.of(DIAG_CODE1, DIAG_CODE2),
            DIAG_CODE2,
            null,
            List.of(DIAG_CODE1, DIAG_CODE2),
            2),
        arguments("All codes missing", List.of(), null, null, List.of(), 0),
        arguments(
            "Single null in the list",
            new ArrayList<String>() {
              {
                add(null);
              }
            },
            DIAG_CODE1,
            DIAG_CODE2,
            List.of(DIAG_CODE2, DIAG_CODE1),
            2));
  }

  /**
   * Tests that the diagnosis codes are set correctly.
   *
   * @param testName Test name.
   * @param diagCodes Diagnosis codes.
   * @param principalDiagCode Principal diagnosis code.
   * @param admitDiagCode Admit diagnosis code.
   * @param expectedCodes Expected codes.
   * @param numberOfRecords Number of expected records.
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void diagnosisCodeTest(
      String testName,
      List<String> diagCodes,
      String principalDiagCode,
      String admitDiagCode,
      List<String> expectedCodes,
      int numberOfRecords) {

    RdaFissClaim entity = new RdaFissClaim();
    SecurityTagManager securityTagManager = mock(SecurityTagManager.class);

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
        new FissClaimTransformerV2(new MetricRegistry(), securityTagManager, false);

    Claim claim =
        fissClaimTransformerV2.transform(new ClaimWithSecurityTags<>(entity, securityTags));

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

    for (int i = 0; i < claim.getDiagnosis().size(); i++) {
      Claim.DiagnosisComponent component = claim.getDiagnosis().get(i);
      CodeableConcept diagnosis = (CodeableConcept) component.getDiagnosis();
      assertEquals(1, diagnosis.getCoding().size());

      assertEquals(expectedCodes.get(i), diagnosis.getCoding().getFirst().getCode());
      assertEquals(i + 1, component.getSequence());
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
