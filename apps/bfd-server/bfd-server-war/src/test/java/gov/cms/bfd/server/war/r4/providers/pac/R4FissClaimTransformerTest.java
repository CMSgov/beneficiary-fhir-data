package gov.cms.bfd.server.war.r4.providers.pac;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

public class R4FissClaimTransformerTest {
  private static final String DIAG_CODE1 = "DIAG_CODE1";
  private static final String DIAG_CODE2 = "DIAG_CODE2";
  private static final String DIAG_CODE3 = "DIAG_CODE3";

  public static Stream<Arguments> diagnosisCodeTest() {
    return Stream.of(
        arguments(
            "Different admit and principal diagnosis codes and both codes included in main diagnosis code list",
            List.of(DIAG_CODE1, DIAG_CODE2, DIAG_CODE3),
            DIAG_CODE1,
            DIAG_CODE2,
            3,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Same code for admit and principal diagnosis and both codes included in main diagnosis code list",
            List.of(DIAG_CODE1, DIAG_CODE2, DIAG_CODE3),
            DIAG_CODE1,
            DIAG_CODE1,
            3,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Different admit and principal diagnosis codes and both codes NOT included in main diagnosis code list",
            List.of(DIAG_CODE3),
            DIAG_CODE1,
            DIAG_CODE2,
            3,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Same code for admit and principal diagnosis and both codes NOT included in main diagnosis code list",
            List.of(DIAG_CODE2),
            DIAG_CODE1,
            DIAG_CODE1,
            2,
            "SHOULD be filtered but was NOT."),
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
            2,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Null principal diagnosis code",
            List.of(DIAG_CODE1, DIAG_CODE2),
            null,
            DIAG_CODE2,
            2,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Null admit diagnosis code",
            List.of(DIAG_CODE1, DIAG_CODE2),
            DIAG_CODE2,
            null,
            2,
            "SHOULD be filtered but was NOT."));
  }

  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void diagnosisCodeTest(
      String testName,
      List<String> diagCodes,
      String principleDiag,
      String admitDiagCode,
      int numberOfRecords,
      String message) {

    RdaFissClaim entity = new RdaFissClaim();

    entity.setLastUpdated(Instant.ofEpochMilli(1));
    entity.setStmtCovToDate(LocalDate.of(2020, 1, 1));
    entity.setPrincipleDiag(principleDiag);
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
            .filter(
                diagnosis ->
                    !getDiagnosisTypeCoding(diagnosis, ExDiagnosistype.ADMITTING)
                        .toList()
                        .isEmpty())
            .toList();

    List<Claim.DiagnosisComponent> principalCoding =
        claim.getDiagnosis().stream()
            .filter(
                diagnosis ->
                    !getDiagnosisTypeCoding(diagnosis, ExDiagnosistype.PRINCIPAL)
                        .toList()
                        .isEmpty())
            .toList();

    assertEquals(admitDiagCode == null ? 0 : 1, admitCoding.size());
    assertEquals(principleDiag == null ? 0 : 1, principalCoding.size());

    if (admitDiagCode != null && admitDiagCode.equals(principleDiag)) {
      assertEquals(admitCoding, principalCoding);
    }

    if (admitDiagCode != null) {
      assertEquals(
          admitDiagCode,
          ((CodeableConcept) admitCoding.getFirst().getDiagnosis())
              .getCoding()
              .getFirst()
              .getCode());
    }

    if (principleDiag != null) {
      assertEquals(
          principleDiag,
          ((CodeableConcept) principalCoding.getFirst().getDiagnosis())
              .getCoding()
              .getFirst()
              .getCode());
    }
  }

  static Stream<CodeableConcept> getDiagnosisTypeCoding(
      Claim.DiagnosisComponent diag, ExDiagnosistype diagnosisType) {

    return diag.getType().stream()
        .filter(c -> diagnosisType.toCode().equals(c.getCoding().getFirst().getCode()))
        .filter(c -> diagnosisType.getSystem().equals(c.getCoding().getFirst().getSystem()));
  }
}
