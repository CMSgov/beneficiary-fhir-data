package gov.cms.bfd.server.war.r4.providers.preadj;

import static org.junit.Assert.assertEquals;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rda.PreAdjFissClaim;
import gov.cms.bfd.model.rda.PreAdjFissProcCode;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.hl7.fhir.r4.model.Claim;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * These tests check if the transformed fiss claims result in the expected SAMHSA filtering
 * outcomes.
 */
@RunWith(Parameterized.class)
public class R4ClaimSamhsaMatcherTransformerTest {

  private static final String NON_SAMHSA_CODE = "NOTSAMHSA";

  private static final String ICD_9_DX_SAMHSA_CODE = "291.0";
  private static final String ICD_9_PROC_SAMHSA_CODE = "94.45";
  private static final String ICD_10_DX_SAMHSA_CODE = "F10.10";
  private static final String ICD_10_PROC_SAMHSA_CODE = "HZ30ZZZ";

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Iterable<Object[]> parameters() {
    return List.of(
        new Object[][] {
          // TODO: ICD 9 codes not implemented yet, enable this test case when it is
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
          // TODO: ICD 9 codes not implemented yet, enable this test case when it is
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

  public R4ClaimSamhsaMatcherTransformerTest(
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
