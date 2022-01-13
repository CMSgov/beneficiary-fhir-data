package gov.cms.bfd.server.war.r4.providers.preadj;

import static org.junit.Assert.assertEquals;

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
import org.hl7.fhir.r4.model.Claim;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * "Higher" level testing to see if the transformers are in line with the expectations of the SAMHSA
 * filtering mechanics
 */
@RunWith(Enclosed.class)
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
   * These tests check if the transformed FISS claims result in the expected SAMHSA filtering
   * outcomes.
   */
  @RunWith(Parameterized.class)
  public static class FissTests {

    private final String testName;
    private final LocalDate toDate;
    private final List<String> diagCodes;
    private final List<String> procCodes;
    private final boolean expectedResult;
    private final String errorMessagePostFix;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {
              "SAMHSA ICD 9 Diagnosis code (Admitting)",
              ICD_9_DATE,
              List.of(NON_SAMHSA_CODE, ICD_9_DX_SAMHSA_CODE, NON_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "SHOULD be filtered but was NOT."
            },
            {
              "SAMHSA ICD 9 Diagnosis code (Principal)",
              ICD_9_DATE,
              List.of(ICD_9_DX_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "SHOULD be filtered but was NOT."
            },
            {
              "SAMHSA ICD 9 Diagnosis code (Other)",
              ICD_9_DATE,
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, ICD_9_DX_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "SHOULD be filtered but was NOT."
            },
            {
              "SAMHSA ICD 10 Diagnosis code (Admitting)",
              ICD_10_DATE,
              List.of(NON_SAMHSA_CODE, ICD_10_DX_SAMHSA_CODE, NON_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "SHOULD be filtered but was NOT."
            },
            {
              "SAMHSA ICD 10 Diagnosis code (Principal)",
              ICD_10_DATE,
              List.of(ICD_10_DX_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "SHOULD be filtered but was NOT."
            },
            {
              "SAMHSA ICD 10 Diagnosis code (Other)",
              ICD_10_DATE,
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, ICD_10_DX_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "SHOULD be filtered but was NOT."
            },
            {
              "SAMHSA ICD 9 Proc code",
              ICD_9_DATE,
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, ICD_9_PROC_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "SHOULD be filtered but was NOT."
            },
            {
              "SAMHSA ICD 10 Proc code",
              ICD_10_DATE,
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, ICD_10_PROC_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "SHOULD be filtered but was NOT."
            },
            {
              "Non-Samhsa codes (ICD-9)",
              ICD_9_DATE,
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              false,
              "should NOT be filtered but WAS."
            },
            {
              "Non-Samhsa codes (ICD-10)",
              ICD_10_DATE,
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              false,
              "should NOT be filtered but WAS."
            },
          });
    }

    public FissTests(
        String testName,
        LocalDate toDate,
        List<String> diagCodes,
        List<String> procCodes,
        boolean expectedResult,
        String errorMessagePostFix) {
      this.testName = testName;
      this.toDate = toDate;
      this.diagCodes = diagCodes;
      this.procCodes = procCodes;
      this.expectedResult = expectedResult;
      this.errorMessagePostFix = errorMessagePostFix;
    }

    @Test
    public void test() {
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

      assertEquals(testName + " " + errorMessagePostFix, expectedResult, matcher.test(claim));
    }
  }

  /**
   * These tests check if the transformed MCS claims result in the expected SAMHSA filtering
   * outcomes.
   */
  @RunWith(Parameterized.class)
  public static class McsTests {

    private final String testName;
    private final List<String> diagCodes;
    private final List<String> procCodes;
    private final boolean expectedResult;
    private final String errorMessagePostFix;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {
              "SAMHSA ICD 9 Diagnosis code",
              List.of("0:" + NON_SAMHSA_CODE, "1:" + ICD_9_DX_SAMHSA_CODE, "0:" + NON_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "not correctly detected."
            },
            {
              "SAMHSA ICD 10 Diagnosis code",
              List.of("0:" + NON_SAMHSA_CODE, "0:" + ICD_10_DX_SAMHSA_CODE, "0:" + NON_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "not correctly detected."
            },
            {
              "SAMHSA CPT Proc code",
              List.of("0:" + NON_SAMHSA_CODE, "0:" + NON_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, CPT_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "not correctly detected."
            },
            {
              "Non-Samhsa codes",
              List.of("0:" + NON_SAMHSA_CODE, "0:" + NON_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              false,
              "incorrectly detected."
            },
          });
    }

    public McsTests(
        String testName,
        List<String> diagCodes,
        List<String> procCodes,
        boolean expectedResult,
        String errorMessagePostFix) {
      this.testName = testName;
      this.diagCodes = diagCodes;
      this.procCodes = procCodes;
      this.expectedResult = expectedResult;
      this.errorMessagePostFix = errorMessagePostFix;
    }

    @Test
    public void test() {
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

      assertEquals(testName + " " + errorMessagePostFix, expectedResult, matcher.test(claim));
    }
  }
}
