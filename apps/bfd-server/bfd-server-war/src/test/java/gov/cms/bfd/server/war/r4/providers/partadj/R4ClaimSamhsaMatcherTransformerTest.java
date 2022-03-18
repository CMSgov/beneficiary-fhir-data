package gov.cms.bfd.server.war.r4.providers.partadj;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.PartAdjFissClaim;
import gov.cms.bfd.model.rda.PartAdjFissDiagnosisCode;
import gov.cms.bfd.model.rda.PartAdjFissProcCode;
import gov.cms.bfd.model.rda.PartAdjMcsClaim;
import gov.cms.bfd.model.rda.PartAdjMcsDetail;
import gov.cms.bfd.model.rda.PartAdjMcsDiagnosisCode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Claim;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * "Higher" level testing to see if the transformers are in line with the expectations of the SAMHSA
 * filtering mechanics
 */
public class R4ClaimSamhsaMatcherTransformerTest {

  private static final String NON_SAMHSA_CODE = "NOTSAMHSA";

  private static final String ICD_9_DX_SAMHSA_CODE = "291.0";
  private static final String ICD_9_PROC_SAMHSA_CODE = "94.45";
  private static final String ICD_10_DX_SAMHSA_CODE = "F10.10";
  private static final String ICD_10_PROC_SAMHSA_CODE = "HZ30ZZZ";
  private static final String CPT_SAMHSA_CODE = "H0005";

  private static final LocalDate ICD_9_DATE = LocalDate.of(2000, 1, 1);
  private static final LocalDate ICD_10_DATE = LocalDate.of(2020, 1, 1);

  /**
   * Data method for the fissTest. Used automatically via the MethodSource annotation.
   *
   * @return the data for the test
   */
  public static Stream<Arguments> fissTest() {
    return Stream.of(
        arguments(
            "SAMHSA ICD 9 Diagnosis code (Admitting)",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, ICD_9_DX_SAMHSA_CODE, NON_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 9 Diagnosis code (Principal)",
            ICD_9_DATE,
            List.of(ICD_9_DX_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 9 Diagnosis code (Other)",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, ICD_9_DX_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 10 Diagnosis code (Admitting)",
            ICD_10_DATE,
            List.of(NON_SAMHSA_CODE, ICD_10_DX_SAMHSA_CODE, NON_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 10 Diagnosis code (Principal)",
            ICD_10_DATE,
            List.of(ICD_10_DX_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 10 Diagnosis code (Other)",
            ICD_10_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, ICD_10_DX_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 9 Proc code",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, ICD_9_PROC_SAMHSA_CODE, NON_SAMHSA_CODE),
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 10 Proc code",
            ICD_10_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, ICD_10_PROC_SAMHSA_CODE, NON_SAMHSA_CODE),
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Non-Samhsa codes (ICD-9)",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            false,
            "should NOT be filtered but WAS."),
        arguments(
            "Non-Samhsa codes (ICD-10)",
            ICD_10_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            false,
            "should NOT be filtered but WAS."));
  }

  /**
   * These tests check if the transformed FISS claims result in the expected SAMHSA filtering
   * outcomes.
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void fissTest(
      String testName,
      LocalDate toDate,
      List<String> diagCodes,
      List<String> procCodes,
      boolean expectedResult,
      String errorMessagePostFix) {
    PartAdjFissClaim entity = new PartAdjFissClaim();

    String principalDxCode = diagCodes.get(0);
    String admittingDxCode = diagCodes.get(1);

    entity.setLastUpdated(Instant.ofEpochMilli(1));
    entity.setStmtCovToDate(toDate);
    entity.setPrincipleDiag(principalDxCode);
    entity.setAdmitDiagCode(admittingDxCode);

    Set<PartAdjFissDiagnosisCode> diagnoses =
        IntStream.range(0, diagCodes.size())
            .mapToObj(
                i -> {
                  PartAdjFissDiagnosisCode diagCode = new PartAdjFissDiagnosisCode();
                  diagCode.setPriority((short) i);
                  diagCode.setDiagCd2(diagCodes.get(i));

                  return diagCode;
                })
            .collect(Collectors.toSet());

    entity.setDiagCodes(diagnoses);

    Set<PartAdjFissProcCode> procedures =
        IntStream.range(0, procCodes.size())
            .mapToObj(
                i -> {
                  PartAdjFissProcCode procCode = new PartAdjFissProcCode();
                  procCode.setProcDate(LocalDate.EPOCH);
                  procCode.setPriority((short) i);
                  procCode.setProcCode(procCodes.get(i));

                  return procCode;
                })
            .collect(Collectors.toSet());

    entity.setProcCodes(procedures);

    Claim claim = FissClaimTransformerV2.transform(new MetricRegistry(), entity);

    R4ClaimSamhsaMatcher matcher = new R4ClaimSamhsaMatcher();

    assertEquals(expectedResult, matcher.test(claim), testName + " " + errorMessagePostFix);
  }

  /**
   * Data method for the mcsTest. Used automatically via the MethodSource annotation.
   *
   * @return the data for the test
   */
  public static Stream<Arguments> mcsTest() {
    return Stream.of(
        arguments(
            "SAMHSA ICD 9 Diagnosis code",
            List.of("0:" + NON_SAMHSA_CODE, "1:" + ICD_9_DX_SAMHSA_CODE, "0:" + NON_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            true,
            "not correctly detected."),
        arguments(
            "SAMHSA ICD 10 Diagnosis code",
            List.of("0:" + NON_SAMHSA_CODE, "0:" + ICD_10_DX_SAMHSA_CODE, "0:" + NON_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            true,
            "not correctly detected."),
        arguments(
            "SAMHSA CPT Proc code",
            List.of("0:" + NON_SAMHSA_CODE, "0:" + NON_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, CPT_SAMHSA_CODE, NON_SAMHSA_CODE),
            true,
            "not correctly detected."),
        arguments(
            "Non-Samhsa codes",
            List.of("0:" + NON_SAMHSA_CODE, "0:" + NON_SAMHSA_CODE),
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            false,
            "incorrectly detected."));
  }

  /**
   * These tests check if the transformed MCS claims result in the expected SAMHSA filtering
   * outcomes.
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void mcsTest(
      String testName,
      List<String> diagCodes,
      List<String> procCodes,
      boolean expectedResult,
      String errorMessagePostFix) {
    PartAdjMcsClaim entity = new PartAdjMcsClaim();

    entity.setLastUpdated(Instant.ofEpochMilli(1));

    Set<PartAdjMcsDiagnosisCode> diagnoses =
        IntStream.range(0, diagCodes.size())
            .mapToObj(
                i -> {
                  String[] dx = diagCodes.get(i).split(":");

                  PartAdjMcsDiagnosisCode diagCode = new PartAdjMcsDiagnosisCode();
                  diagCode.setPriority((short) i);
                  diagCode.setIdrDiagIcdType(dx[0]);
                  diagCode.setIdrDiagCode(dx[1]);

                  return diagCode;
                })
            .collect(Collectors.toSet());

    entity.setDiagCodes(diagnoses);

    Set<PartAdjMcsDetail> procedures =
        IntStream.range(0, procCodes.size())
            .mapToObj(
                i -> {
                  PartAdjMcsDetail procCode = new PartAdjMcsDetail();
                  procCode.setIdrDtlToDate(LocalDate.EPOCH);
                  procCode.setPriority((short) i);
                  procCode.setIdrProcCode(procCodes.get(i));

                  return procCode;
                })
            .collect(Collectors.toSet());

    entity.setDetails(procedures);

    Claim claim = McsClaimTransformerV2.transform(new MetricRegistry(), entity);

    R4ClaimSamhsaMatcher matcher = new R4ClaimSamhsaMatcher();

    assertEquals(expectedResult, matcher.test(claim), testName + " " + errorMessagePostFix);
  }
}
