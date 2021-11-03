package gov.cms.bfd.server.war.r4.providers.preadj;

import static org.junit.Assert.assertEquals;

import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.List;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.PositiveIntType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class R4ClaimSamhsaMatcherIT {

  private static final String NON_SAMHSA_CODE = "NOTSAMHSA";

  private static final String ICD_9_DX_SAMHSA_CODE = "291.0";
  private static final String ICD_9_PROC_SAMHSA_CODE = "94.45";
  private static final String ICD_10_DX_SAMHSA_CODE = "F10.10";
  private static final String ICD_10_PROC_SAMHSA_CODE = "HZ30ZZZ";
  private static final String CPT_SAMHSA_CODE = "H0009";

  private static final Coding SAMSHA_ICD_9_PROC_CODING =
      new Coding(IcdCode.CODING_SYSTEM_ICD_9, ICD_9_PROC_SAMHSA_CODE, "");
  private static final Coding SAMSHA_ICD_9_DX_CODING =
      new Coding(IcdCode.CODING_SYSTEM_ICD_9, ICD_9_DX_SAMHSA_CODE, "");
  private static final Coding ICD_9_CODING =
      new Coding(IcdCode.CODING_SYSTEM_ICD_9, NON_SAMHSA_CODE, "");
  private static final Coding SAMSHA_ICD_10_PROC_CODING =
      new Coding(IcdCode.CODING_SYSTEM_ICD_10, ICD_10_PROC_SAMHSA_CODE, "");
  private static final Coding SAMSHA_ICD_10_DX_CODING =
      new Coding(IcdCode.CODING_SYSTEM_ICD_10, ICD_10_DX_SAMHSA_CODE, "");
  private static final Coding ICD_10_CODING =
      new Coding(IcdCode.CODING_SYSTEM_ICD_10, NON_SAMHSA_CODE, "");
  private static final Coding SAMSHA_CPT_CODING =
      new Coding(TransformerConstants.CODING_SYSTEM_CPT, CPT_SAMHSA_CODE, "");
  private static final Coding CPT_CODING =
      new Coding(TransformerConstants.CODING_SYSTEM_CPT, NON_SAMHSA_CODE, "");
  private static final Coding SMAHSA_HCPCS_CODING =
      new Coding(TransformerConstants.CODING_SYSTEM_HCPCS, CPT_SAMHSA_CODE, "");
  private static final Coding HCPCS_CODING =
      new Coding(TransformerConstants.CODING_SYSTEM_HCPCS, NON_SAMHSA_CODE, "");
  private static final Coding NULL_HCPCS_CODING =
      new Coding(TransformerConstants.CODING_SYSTEM_HCPCS, null, "");
  private static final Coding UNKNOWN_SYSTEM_CODING = new Coding("unknown system", "anything", "");

  private static final Claim.ProcedureComponent NON_SAMHSA_PROC_COMP =
      new Claim.ProcedureComponent()
          .setProcedure(
              new CodeableConcept().setCoding(List.of(ICD_9_CODING, ICD_10_CODING, CPT_CODING)));

  private static final Claim.DiagnosisComponent NON_SAMHSA_DIAG_COMP =
      new Claim.DiagnosisComponent()
          .setDiagnosis(
              new CodeableConcept().setCoding(List.of(ICD_9_CODING, ICD_10_CODING, CPT_CODING)));

  private static final Claim.ItemComponent NON_SAMHSA_LINE_ITEM =
      new Claim.ItemComponent(
          new PositiveIntType(0),
          new CodeableConcept().setCoding(List.of(HCPCS_CODING, NULL_HCPCS_CODING)));

  @Parameterized.Parameters(name = "{index}: {0}")
  public static Iterable<Object[]> parameters() {
    return List.of(
        new Object[][] {
          {
            "Samhsa ICD 9 proc code",
            List.of(
                NON_SAMHSA_PROC_COMP,
                new Claim.ProcedureComponent()
                    .setProcedure(
                        new CodeableConcept()
                            .setCoding(List.of(ICD_9_CODING, SAMSHA_ICD_9_PROC_CODING)))),
            List.of(NON_SAMHSA_DIAG_COMP),
            List.of(NON_SAMHSA_LINE_ITEM),
            true,
            "not correctly detected."
          },
          {
            "Samhsa ICD 10 proc code",
            List.of(
                NON_SAMHSA_PROC_COMP,
                new Claim.ProcedureComponent()
                    .setProcedure(
                        new CodeableConcept()
                            .setCoding(List.of(ICD_10_CODING, SAMSHA_ICD_10_PROC_CODING)))),
            List.of(NON_SAMHSA_DIAG_COMP),
            List.of(NON_SAMHSA_LINE_ITEM),
            true,
            "not correctly detected."
          },
          {
            "Samhsa CPT proc code",
            List.of(
                NON_SAMHSA_PROC_COMP,
                new Claim.ProcedureComponent()
                    .setProcedure(
                        new CodeableConcept()
                            .setCoding(List.of(ICD_10_CODING, SAMSHA_CPT_CODING)))),
            List.of(NON_SAMHSA_DIAG_COMP),
            List.of(NON_SAMHSA_LINE_ITEM),
            true,
            " not correctly detected."
          },
          {
            "Unknown system proc code",
            List.of(
                NON_SAMHSA_PROC_COMP,
                new Claim.ProcedureComponent()
                    .setProcedure(
                        new CodeableConcept()
                            .setCoding(List.of(ICD_10_CODING, UNKNOWN_SYSTEM_CODING)))),
            List.of(NON_SAMHSA_DIAG_COMP),
            List.of(NON_SAMHSA_LINE_ITEM),
            true,
            "not correctly detected."
          },
          {
            "Samhsa ICD 9 diag code",
            List.of(NON_SAMHSA_PROC_COMP),
            List.of(
                NON_SAMHSA_DIAG_COMP,
                new Claim.DiagnosisComponent()
                    .setDiagnosis(
                        new CodeableConcept()
                            .setCoding(List.of(ICD_9_CODING, SAMSHA_ICD_9_DX_CODING)))),
            List.of(NON_SAMHSA_LINE_ITEM),
            true,
            "not correctly detected."
          },
          {
            "Samhsa ICD 10 diag code",
            List.of(NON_SAMHSA_PROC_COMP),
            List.of(
                NON_SAMHSA_DIAG_COMP,
                new Claim.DiagnosisComponent()
                    .setDiagnosis(
                        new CodeableConcept()
                            .setCoding(List.of(ICD_10_CODING, SAMSHA_ICD_10_DX_CODING)))),
            List.of(NON_SAMHSA_LINE_ITEM),
            true,
            "not correctly detected."
          },
          {
            "Samhsa CPT diag code",
            List.of(NON_SAMHSA_PROC_COMP),
            List.of(
                NON_SAMHSA_DIAG_COMP,
                new Claim.DiagnosisComponent()
                    .setDiagnosis(
                        new CodeableConcept()
                            .setCoding(List.of(ICD_10_CODING, SAMSHA_CPT_CODING)))),
            List.of(NON_SAMHSA_LINE_ITEM),
            true,
            "not correctly detected."
          },
          {
            "Unknown system diag code",
            List.of(NON_SAMHSA_PROC_COMP),
            List.of(
                NON_SAMHSA_DIAG_COMP,
                new Claim.DiagnosisComponent()
                    .setDiagnosis(
                        new CodeableConcept()
                            .setCoding(List.of(ICD_10_CODING, UNKNOWN_SYSTEM_CODING)))),
            List.of(NON_SAMHSA_LINE_ITEM),
            true,
            "not correctly detected."
          },
          {
            "Samhsa HCPCS line item code",
            List.of(NON_SAMHSA_PROC_COMP),
            List.of(NON_SAMHSA_DIAG_COMP),
            List.of(
                NON_SAMHSA_LINE_ITEM,
                new Claim.ItemComponent(
                    new PositiveIntType(1),
                    new CodeableConcept().setCoding(List.of(HCPCS_CODING, SMAHSA_HCPCS_CODING)))),
            true,
            "not correctly detected."
          },
          {
            "Unknown system line item code",
            List.of(NON_SAMHSA_PROC_COMP),
            List.of(NON_SAMHSA_DIAG_COMP),
            List.of(
                NON_SAMHSA_LINE_ITEM,
                new Claim.ItemComponent(
                    new PositiveIntType(1),
                    new CodeableConcept().setCoding(List.of(HCPCS_CODING, UNKNOWN_SYSTEM_CODING)))),
            true,
            "not correctly detected."
          },
          {
            "Non-Samhsa codes",
            List.of(NON_SAMHSA_PROC_COMP),
            List.of(NON_SAMHSA_DIAG_COMP),
            List.of(NON_SAMHSA_LINE_ITEM),
            false,
            "incorrectly detected."
          },
        });
  }

  private final String testName;
  private final List<Claim.ProcedureComponent> procedures;
  private final List<Claim.DiagnosisComponent> diagnoses;
  private final List<Claim.ItemComponent> items;
  private final boolean expectedResult;
  private final String errorMessagePostFix;

  public R4ClaimSamhsaMatcherIT(
      String testName,
      List<Claim.ProcedureComponent> procedures,
      List<Claim.DiagnosisComponent> diagnoses,
      List<Claim.ItemComponent> items,
      boolean expectedResult,
      String errorMessagePostFix) {
    this.testName = testName;
    this.procedures = procedures;
    this.diagnoses = diagnoses;
    this.items = items;
    this.expectedResult = expectedResult;
    this.errorMessagePostFix = errorMessagePostFix;
  }

  @Test
  public void test() {
    Claim claim = new Claim();

    claim.setProcedure(procedures);
    claim.setDiagnosis(diagnoses);
    claim.setItem(items);

    R4ClaimSamhsaMatcher matcher = new R4ClaimSamhsaMatcher();

    assertEquals(testName + " " + errorMessagePostFix, expectedResult, matcher.test(claim));
  }
}
