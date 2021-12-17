package gov.cms.bfd.server.war.r4.providers.preadj;

import static org.junit.Assert.assertEquals;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import gov.cms.bfd.model.rda.PreAdjMcsClaim;
import gov.cms.bfd.model.rda.PreAdjMcsDiagnosisCode;
import java.math.BigDecimal;
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
 * These tests check if the transformed fiss claims result in the expected SAMHSA filtering
 * outcomes.
 */
@RunWith(Enclosed.class)
public class R4ClaimSamhsaMatcherTransformerTest {

  private static final String NON_SAMHSA_CODE = "NOTSAMHSA";

  private static final String ICD_9_DX_SAMHSA_CODE = "291.0";
  private static final String ICD_9_PROC_SAMHSA_CODE = "94.45";
  private static final String ICD_10_DX_SAMHSA_CODE = "F10.10";
  private static final String ICD_10_PROC_SAMHSA_CODE = "HZ30ZZZ";

  @RunWith(Parameterized.class)
  public static class FissTests {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            // TODO: [PACA-263] ICD 9 codes not implemented yet, enable this test case when it is
            // {
            //      "SAMHSA ICD 9 Diagnosis code (Admitting)",
            //      NON_SAMHSA_CODE,
            //      ICD_9_DX_SAMHSA_CODE,
            //      List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            //      true,
            //      "not correctly detected."
            // },
            // {
            //      "SAMHSA ICD 9 Diagnosis code (Principal)",
            //      ICD_9_DX_SAMHSA_CODE,
            //      NON_SAMHSA_CODE,
            //      List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
            //      true,
            //      "not correctly detected."
            // },
            {
              "SAMHSA ICD 10 Diagnosis code (Admitting)",
              NON_SAMHSA_CODE,
              ICD_10_DX_SAMHSA_CODE,
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "not correctly detected."
            },
            {
              "SAMHSA ICD 10 Diagnosis code (Principal)",
              ICD_10_DX_SAMHSA_CODE,
              NON_SAMHSA_CODE,
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "not correctly detected."
            },
            // TODO: [PACA-263] ICD 9 codes not implemented yet, enable this test case when it is
            // {
            //      "SAMHSA ICD 9 Proc code",
            //      NON_SAMHSA_CODE,
            //      NON_SAMHSA_CODE,
            //      List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, ICD_9_PROC_SAMHSA_CODE),
            //      true,
            //      "not correctly detected."
            // },
            {
              "SAMHSA ICD 10 Proc code",
              NON_SAMHSA_CODE,
              NON_SAMHSA_CODE,
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, ICD_10_PROC_SAMHSA_CODE),
              true,
              "not correctly detected."
            },
            {
              "Non-Samhsa codes",
              NON_SAMHSA_CODE,
              NON_SAMHSA_CODE,
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              false,
              "incorrectly detected."
            },
          });
    }

    private final String testName;
    private final String principalDxCode;
    private final String admittingDxCode;
    private final List<String> procCodes;
    private final boolean expectedResult;
    private final String errorMessagePostFix;

    public FissTests(
        String testName,
        String principalDxCode,
        String admittingDxCode,
        List<String> procCodes,
        boolean expectedResult,
        String errorMessagePostFix) {
      this.testName = testName;
      this.principalDxCode = principalDxCode;
      this.admittingDxCode = admittingDxCode;
      this.procCodes = procCodes;
      this.expectedResult = expectedResult;
      this.errorMessagePostFix = errorMessagePostFix;
    }

    @Test
    public void test() {
      PreAdjFissClaim entity = new PreAdjFissClaim();

      entity.setDcn("abc123");
      entity.setLastUpdated(Instant.ofEpochMilli(1));
      entity.setTotalChargeAmount(new BigDecimal("1.00"));
      entity.setMedaProvId("abc123");
      entity.setNpiNumber("npinpinpin");
      entity.setMbi("mbimbimbimbi");
      entity.setPrincipleDiag(principalDxCode);
      entity.setAdmitDiagCode(admittingDxCode);

      Set<PreAdjFissProcCode> procedures =
          IntStream.range(0, procCodes.size())
              .mapToObj(
                  i -> {
                    PreAdjFissProcCode procCode = new PreAdjFissProcCode();
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

  @RunWith(Parameterized.class)
  public static class McsTests {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> parameters() {
      return List.of(
          new Object[][] {
            {
              "SAMHSA ICD 9 Diagnosis code",
              List.of("0:" + NON_SAMHSA_CODE, "1:" + ICD_9_DX_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "not correctly detected."
            },
            {
              "SAMHSA ICD 10 Diagnosis code",
              List.of("0:" + NON_SAMHSA_CODE, "0:" + ICD_10_DX_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              true,
              "not correctly detected."
            },
            // TODO: [PACA-323] Proc codes for MCS claims will be altered in a future ticket to use
            // ICD
            //  code systems, add these test cases when they are
            // {
            //  "SAMHSA ICD 9 Proc code",
            //     List.of("0:" + NON_SAMHSA_CODE, "0:" + NON_SAMHSA_CODE),
            //  List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, ICD_9_PROC_SAMHSA_CODE),
            //  true,
            //  "not correctly detected."
            // },
            // {
            //    "SAMHSA ICD 10 Proc code",
            //    List.of("0:" + NON_SAMHSA_CODE, "0:" + NON_SAMHSA_CODE),
            //    List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, ICD_10_PROC_SAMHSA_CODE),
            //    true,
            //    "not correctly detected."
            // },
            {
              "Non-Samhsa codes",
              List.of("0:" + NON_SAMHSA_CODE, "0:" + NON_SAMHSA_CODE),
              List.of(NON_SAMHSA_CODE, NON_SAMHSA_CODE, NON_SAMHSA_CODE),
              false,
              "incorrectly detected."
            },
          });
    }

    private final String testName;
    private final List<String> diagCodes;
    private final List<String> procCodes;
    private final boolean expectedResult;
    private final String errorMessagePostFix;

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
      PreAdjMcsClaim entity = new PreAdjMcsClaim();

      entity.setIdrClmHdIcn("abc123");
      entity.setLastUpdated(Instant.ofEpochMilli(1));
      entity.setIdrTotAllowed(new BigDecimal("1.00"));
      entity.setIdrBillProvNum("abc123");
      entity.setIdrBillProvNpi("npinpinpin");
      entity.setIdrClaimMbi("mbimbimbimbi");

      Set<PreAdjMcsDiagnosisCode> diagnoses =
          IntStream.range(0, diagCodes.size())
              .mapToObj(
                  i -> {
                    String[] dx = diagCodes.get(i).split(":");

                    PreAdjMcsDiagnosisCode diagCode = new PreAdjMcsDiagnosisCode();
                    diagCode.setPriority((short) i);
                    diagCode.setIdrDiagIcdType(dx[0]);
                    diagCode.setIdrDiagCode(dx[1]);

                    return diagCode;
                  })
              .collect(Collectors.toSet());

      entity.setDiagCodes(diagnoses);

      // TODO: Enable proc codes when MCS claims have been updated to use ICD code systems
      //            Set<PreAdjMcsDetail> procedures =
      //                    IntStream.range(0, procCodes.size())
      //                            .mapToObj(
      //                                    i -> {
      //                                        PreAdjMcsDetail procCode = new PreAdjMcsDetail();
      //                                        procCode.setIdrDtlToDate(LocalDate.EPOCH);
      //                                        procCode.setPriority((short) i);
      //                                        procCode.setIdrProcCode(procCodes.get(i));
      //
      //                                        return procCode;
      //                                    })
      //                            .collect(Collectors.toSet());
      //
      //            entity.setDetails(procedures);

      Claim claim = McsClaimTransformerV2.transform(new MetricRegistry(), entity);

      R4ClaimSamhsaMatcher matcher = new R4ClaimSamhsaMatcher();

      assertEquals(testName + " " + errorMessagePostFix, expectedResult, matcher.test(claim));
    }
  }
}
