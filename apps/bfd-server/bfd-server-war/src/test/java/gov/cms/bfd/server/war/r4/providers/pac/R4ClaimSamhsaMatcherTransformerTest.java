package gov.cms.bfd.server.war.r4.providers.pac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.entities.RdaFissProcCode;
import gov.cms.bfd.model.rda.entities.RdaFissRevenueLine;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsDetail;
import gov.cms.bfd.model.rda.entities.RdaMcsDiagnosisCode;
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
 * filtering mechanics.
 */
public class R4ClaimSamhsaMatcherTransformerTest {

  /** Represents a code that will not match a samhsa matcher. */
  private static final String NON_SAMHSA_CODE = "NOTSAMHSA";

  /** Represents a code that will match a samhsa ICD9 matcher. */
  private static final String ICD_9_DX_SAMHSA_CODE = "291.0";

  /** Represents a code that will match a samhsa ICD9 proc matcher. */
  private static final String ICD_9_PROC_SAMHSA_CODE = "94.45";

  /** Represents a code that will match a samhsa ICD10 matcher. */
  private static final String ICD_10_DX_SAMHSA_CODE = "F10.10";

  /** Represents a code that will match a samhsa ICD10 matcher. */
  private static final String ICD_10_PROC_SAMHSA_CODE = "HZ30ZZZ";

  /** Represents a code that will match a CPT samhsa matcher. */
  private static final String CPT_SAMHSA_CODE = "H0005";

  /** Represents a code that will match a DRG_CD samhsa matcher. */
  private static final String DRG_SAMHSA_CD = "895";

  /** A date to use for ICD9 testing. */
  private static final LocalDate ICD_9_DATE = LocalDate.of(2000, 1, 1);

  /** A date to use for ICD10 testing. */
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
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 9 Diagnosis code (Principal)",
            ICD_9_DATE,
            List.of(ICD_9_DX_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 9 Diagnosis code (Other)",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, ICD_9_DX_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 10 Diagnosis code (Admitting)",
            ICD_10_DATE,
            List.of(NON_SAMHSA_CODE, ICD_10_DX_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 10 Diagnosis code (Principal)",
            ICD_10_DATE,
            List.of(ICD_10_DX_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 10 Diagnosis code (Other)",
            ICD_10_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, ICD_10_DX_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 9 Proc code",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, ICD_9_PROC_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "SAMHSA ICD 10 Proc code",
            ICD_10_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, ICD_10_PROC_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Non-Samhsa codes (ICD-9)",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            false,
            "should NOT be filtered but WAS."),
        arguments(
            "Non-Samhsa codes (ICD-10)",
            ICD_10_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            false,
            "should NOT be filtered but WAS."),
        arguments(
            "SAMHSA DRG_CD",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            DRG_SAMHSA_CD,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Non-Samhsa DRG_CD",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            false,
            "should NOT be filtered but WAS."),
        arguments(
            "SAMHSA CPT_CODE",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            CPT_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Non-Samhsa CPT_CODE",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            false,
            "should NOT be filtered but WAS."),
        arguments(
            "Samhsa principal diagnosis code (ICD-9)",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            ICD_9_DX_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Samhsa principal diagnosis code (ICD-10)",
            ICD_10_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            ICD_10_DX_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Samhsa admit diagnosis code (ICD-9)",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            ICD_9_DX_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Samhsa admit diagnosis code (ICD-10)",
            ICD_10_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            ICD_10_DX_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Samhsa admit diagnosis code and principal code (ICD-9)",
            ICD_9_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            ICD_9_DX_SAMHSA_CODE,
            ICD_9_DX_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."),
        arguments(
            "Samhsa admit diagnosis code and principal code (ICD-10)",
            ICD_10_DATE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            ICD_10_DX_SAMHSA_CODE,
            ICD_10_DX_SAMHSA_CODE,
            List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            NON_SAMHSA_CODE,
            NON_SAMHSA_CODE,
            true,
            "SHOULD be filtered but was NOT."));
  }

  /**
   * These tests check if the transformed FISS claims result in the expected SAMHSA filtering
   * outcomes.
   *
   * @param testName the test name for reporting
   * @param toDate the "to" date to set for the statement date
   * @param diagCodes the diag codes to use for the principal and admitting codes (index 0 and 1
   *     respectively)
   * @param procCodes the proc codes to set for the procedure(s)
   * @param drgCode the drg code to set
   * @param cptCode the cpt code to set
   * @param expectedResult the expected result
   * @param errorMessagePostFix the error message post fix
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void fissTest(
      String testName,
      LocalDate toDate,
      List<String> diagCodes,
      String principleDiag,
      String admitDiagCode,
      List<String> procCodes,
      String drgCode,
      String cptCode,
      boolean expectedResult,
      String errorMessagePostFix) {
    RdaFissClaim entity = new RdaFissClaim();

    entity.setLastUpdated(Instant.ofEpochMilli(1));
    entity.setStmtCovToDate(toDate);
    entity.setPrincipleDiag(principleDiag);
    entity.setAdmitDiagCode(admitDiagCode);
    entity.setDrgCd(drgCode);
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

    Set<RdaFissProcCode> procedures =
        IntStream.range(0, procCodes.size())
            .mapToObj(
                i -> {
                  RdaFissProcCode procCode = new RdaFissProcCode();
                  procCode.setProcDate(LocalDate.EPOCH);
                  procCode.setRdaPosition((short) (i + 1));
                  procCode.setProcCode(procCodes.get(i));

                  return procCode;
                })
            .collect(Collectors.toSet());

    RdaFissRevenueLine line = new RdaFissRevenueLine();
    line.setHcpcCd(cptCode);

    entity.setProcCodes(procedures);
    entity.setRevenueLines(Set.of(line));
    FissClaimTransformerV2 fissClaimTransformerV2 =
        new FissClaimTransformerV2(new MetricRegistry());
    McsClaimTransformerV2 mcsClaimTransformerV2 = new McsClaimTransformerV2(new MetricRegistry());

    Claim claim = fissClaimTransformerV2.transform(entity, true);

    R4ClaimSamhsaMatcher matcher =
        new R4ClaimSamhsaMatcher(fissClaimTransformerV2, mcsClaimTransformerV2);

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
   *
   * @param testName the test name for reporting
   * @param diagCodes the diagnosis codes to use in the test
   * @param procCodes the proc codes to use in the test
   * @param expectedResult the expected result
   * @param errorMessagePostFix the error message post fix
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource
  public void mcsTest(
      String testName,
      List<String> diagCodes,
      List<String> procCodes,
      boolean expectedResult,
      String errorMessagePostFix) {
    RdaMcsClaim entity = new RdaMcsClaim();

    entity.setLastUpdated(Instant.ofEpochMilli(1));

    Set<RdaMcsDiagnosisCode> diagnoses =
        IntStream.range(0, diagCodes.size())
            .mapToObj(
                i -> {
                  String[] dx = diagCodes.get(i).split(":");

                  RdaMcsDiagnosisCode diagCode = new RdaMcsDiagnosisCode();
                  diagCode.setRdaPosition((short) (i + 1));
                  diagCode.setIdrDiagIcdType(dx[0]);
                  diagCode.setIdrDiagCode(dx[1]);

                  return diagCode;
                })
            .collect(Collectors.toSet());

    entity.setDiagCodes(diagnoses);

    Set<RdaMcsDetail> procedures =
        IntStream.range(0, procCodes.size())
            .mapToObj(
                i -> {
                  RdaMcsDetail procCode = new RdaMcsDetail();
                  procCode.setIdrDtlToDate(LocalDate.EPOCH);
                  procCode.setIdrDtlNumber((short) (i + 1));
                  procCode.setIdrProcCode(procCodes.get(i));

                  return procCode;
                })
            .collect(Collectors.toSet());

    entity.setDetails(procedures);

    FissClaimTransformerV2 fissClaimTransformerV2 =
        new FissClaimTransformerV2(new MetricRegistry());
    McsClaimTransformerV2 mcsClaimTransformerV2 = new McsClaimTransformerV2(new MetricRegistry());

    Claim claim = mcsClaimTransformerV2.transform(entity, true);

    R4ClaimSamhsaMatcher matcher =
        new R4ClaimSamhsaMatcher(fissClaimTransformerV2, mcsClaimTransformerV2);

    assertEquals(expectedResult, matcher.test(claim), testName + " " + errorMessagePostFix);
  }
}
