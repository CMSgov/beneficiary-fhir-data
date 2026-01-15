package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link Stu3EobSamhsaMatcherTest}. Integration with {@link
 * ExplanationOfBenefitResourceProvider} is covered by {@link
 * ExplanationOfBenefitE2E#testEobByPatientIdWithExcludeSamhsaTrueExpectFiltering} and related E2E
 * tests.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class Stu3EobSamhsaMatcherTest {

  /** The securityTagManager. */
  @Mock private SecurityTagManager securityTagManager;

  /** The SAMHSA CPT code. */
  public static final String SAMPLE_SAMHSA_CPT_CODE = "G0137";

  /** The SAMHSA ICD9 diagnosis code. */
  public static final String SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE = "29189";

  /** The SAMHSA ICD9 procedure code. */
  public static final String SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE = "9445";

  /** The SAMHSA ICD10 diagnosis code. */
  public static final String SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE = "F1010";

  /** The SAMHSA ICD10 procedure code. */
  public static final String SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE = "HZ2ZZZZ";

  /** The SAMHSA DRG code. */
  public static final String SAMPLE_SAMHSA_DRG_CODE = "895";

  /** The DRG reference url. */
  private static final String DRG =
      CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD);

  /**
   * Sets the data for use in the parameterized tests.
   *
   * @return the stream fed into the test
   */
  public static Stream<Arguments> data() {
    final String HCPCS = TransformerConstants.CODING_SYSTEM_HCPCS;
    final String OTHER = "other system";
    final String DATA_ABSENT = TransformerConstants.CODING_DATA_ABSENT;
    return Stream.of(
        arguments(
            "Empty list",
            Collections.emptyList(),
            false,
            "should NOT return true (all known systems), but DID."),
        arguments(
            "HCPCS only systems",
            List.of(HCPCS, HCPCS, HCPCS),
            true,
            "SHOULD return true (all known systems), but did NOT."),
        arguments(
            "Other system only",
            List.of(OTHER, OTHER),
            false,
            "should NOT return true (all known systems), but DID."),
        arguments(
            "HCPCS and other systems",
            List.of(HCPCS, HCPCS, OTHER),
            false,
            "should NOT return true (all known systems), but DID."),
        arguments(
            "Data Absent Reason",
            List.of(DATA_ABSENT),
            true,
            "should return true (all known systems), but DID."));
  }

  /**
   * Tests that for each data set {@link Stu3EobSamhsaMatcher#containsOnlyKnownSystems} returns the
   * expected value.
   *
   * @param name the test description for reporting
   * @param systems the systems to test
   * @param expectedResult the expected result
   * @param errorMessage the error message if the test fails
   */
  @ParameterizedTest(name = "{index}: {0}")
  @MethodSource("data")
  public void containsOnlyKnownSystemsTest(
      String name, List<String> systems, boolean expectedResult, String errorMessage) {
    Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

    CodeableConcept mockConcept = mock(CodeableConcept.class);

    List<gov.cms.bfd.server.war.adapters.Coding> codings =
        systems.stream()
            .map(
                system -> {
                  gov.cms.bfd.server.war.adapters.Coding mockCoding =
                      mock(gov.cms.bfd.server.war.adapters.Coding.class);
                  doReturn(system).when(mockCoding).getSystem();
                  return mockCoding;
                })
            .toList();

    doReturn(codings).when(mockConcept).getCoding();

    assertEquals(
        expectedResult, matcher.containsOnlyKnownSystems(mockConcept), name + " " + errorMessage);
  }

  /** Nested class for holding the non-parameterized tests. */
  @Nested
  public class NonParameterizedTests {
    /**
     * Verifies that {@link Stu3EobSamhsaMatcher#test} returns {@code false} for claims that have no
     * SAMHSA-related codes.
     */
    @Test
    public void nonSamhsaRelatedClaims() throws IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);
      // Note: none of our SAMPLE_A claims have SAMHSA-related codes (by default).
      List<Object> sampleRifRecords =
          ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
      List<ExplanationOfBenefit> sampleEobs =
          sampleRifRecords.stream()
              .map(
                  r -> {
                    // FIXME remove most `else if`s once filtering fully supports all claim types
                    if (r instanceof Beneficiary || r instanceof BeneficiaryHistory) return null;

                    return TransformerTestUtils.transformRifRecordToEob(
                        r, new MetricRegistry(), securityTagManager);
                  })
              .filter(ExplanationOfBenefit.class::isInstance)
              .collect(Collectors.toList());

      for (ExplanationOfBenefit sampleEob : sampleEobs)
        assertFalse(
            matcher.test(sampleEob), "Unexpected SAMHSA filtering of EOB: " + sampleEob.getId());
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#CARRIER} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchCarrierClaimsByIcd9Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#CARRIER} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchCarrierClaimsByIcd10Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#CARRIER} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchCarrierClaimsByCptProcedure() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#CARRIER} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchCarrierClaimsByCptProcedureForNewCodes() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);
      String SAMPLE_SAMHSA_CPT_NEW_CODE = "G2067";

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_NEW_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#DME} {@link ExplanationOfBenefit}s that have SAMHSA-related
     * ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchDmeClaimsByIcd9Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.DME);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#DME} {@link ExplanationOfBenefit}s that have SAMHSA-related
     * ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchDmeClaimsByIcd10Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.DME);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#DME} {@link ExplanationOfBenefit}s that have SAMHSA-related
     * CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchDmeClaimsByCptProcedure() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.DME);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#INPATIENT} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchInpatientClaimsByIcd9Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosis().get(1).getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#INPATIENT} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchInpatientClaimsByIcd10Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosis().get(1).getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#INPATIENT} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchInpatientClaimsByIcd9Procedure() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#INPATIENT} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchInpatientClaimsByIcd10Procedure() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#INPATIENT} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related drg codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchInpatientClaimsByDrg() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
      sampleEob
          .getDiagnosisFirstRep()
          .getPackageCode()
          .addCoding()
          .setSystem(Stu3EobSamhsaMatcherTest.DRG)
          .setCode(SAMPLE_SAMHSA_DRG_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#OUTPATIENT} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchOutpatientClaimsByIcd9Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#OUTPATIENT} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchOutpatientClaimsByIcd10Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#OUTPATIENT} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchOutpatientClaimsByCptProcedure() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#OUTPATIENT} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchOutpatientClaimsByIcd9Procedure() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#OUTPATIENT} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchOutpatientClaimsByIcd10Procedure() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#HHA} {@link ExplanationOfBenefit}s that have SAMHSA-related
     * ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchHhaClaimsByIcd9Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HHA);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#HHA} {@link ExplanationOfBenefit}s that have SAMHSA-related
     * ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchHhaClaimsByIcd10Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HHA);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#HHA} {@link ExplanationOfBenefit}s that have SAMHSA-related
     * CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchHhaClaimsByCptProcedure() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HHA);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#HOSPICE} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchHospiceClaimsByIcd9Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HOSPICE);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#HOSPICE} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchHospiceClaimsByIcd10Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HOSPICE);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#HOSPICE} {@link ExplanationOfBenefit}s that have
     * SAMHSA-related CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchHospiceClaimsByCptProcedure() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HOSPICE);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#SNF} {@link ExplanationOfBenefit}s that have SAMHSA-related
     * ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchSnfClaimsByIcd9Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosis().get(1).getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#SNF} {@link ExplanationOfBenefit}s that have SAMHSA-related
     * ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchSnfClaimsByIcd10Diagnosis() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosis().get(1).getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#SNF} {@link ExplanationOfBenefit}s that have SAMHSA-related
     * CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchSnfClaimsByCptProcedure() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#SNF} {@link ExplanationOfBenefit}s that have SAMHSA-related
     * ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchSnfClaimsByIcd9Procedure() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
      Coding sampleEobDiagnosis =
          sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#SNF} {@link ExplanationOfBenefit}s that have SAMHSA-related
     * ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchSnfClaimsByIcd10Procedure() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
      Coding sampleEobDiagnosis =
          sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link ClaimType#SNF} {@link ExplanationOfBenefit}s that have SAMHSA-related
     * drg codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchSnfClaimsByDrg() throws FHIRException, IOException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher(false);

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
      sampleEob
          .getDiagnosisFirstRep()
          .getPackageCode()
          .addCoding()
          .setSystem(Stu3EobSamhsaMatcherTest.DRG)
          .setCode(SAMPLE_SAMHSA_DRG_CODE);

      assertTrue(matcher.test(sampleEob));
    }

    /**
     * Gets the sample A claim from a file resource.
     *
     * @param claimType the {@link ClaimType} to get a sample {@link ExplanationOfBenefit} for
     * @return a sample {@link ExplanationOfBenefit} of the specified {@link ClaimType} (derived
     *     from the {@link StaticRifResourceGroup#SAMPLE_A} sample RIF records)
     * @throws IOException the io exception
     */
    private ExplanationOfBenefit getSampleAClaim(ClaimType claimType) throws IOException {
      List<Object> sampleRifRecords =
          ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
      Object sampleRifRecordForClaimType =
          sampleRifRecords.stream()
              .filter(claimType.getEntityClass()::isInstance)
              .findFirst()
              .get();
      ExplanationOfBenefit sampleEobForClaimType =
          TransformerTestUtils.transformRifRecordToEob(
              sampleRifRecordForClaimType, new MetricRegistry(), securityTagManager);

      return sampleEobForClaimType;
    }
  }
}
