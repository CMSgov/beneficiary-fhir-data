package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.adapters.CodeableConcept;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Unit tests for {@link Stu3EobSamhsaMatcherTest}. Integration with {@link
 * ExplanationOfBenefitResourceProvider} is covered by {@link
 * ExplanationOfBenefitResourceProviderIT#searchForEobsWithSamhsaFiltering()} and related
 * integration tests.
 */
@RunWith(Enclosed.class)
public final class Stu3EobSamhsaMatcherTest {
  // TODO complete and verify that these exactly match real values in our DB
  public static final String SAMPLE_SAMHSA_CPT_CODE = "4320F";
  public static final String SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE = "29189";
  public static final String SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE = "9445";
  public static final String SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE = "F1010";
  public static final String SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE = "HZ2ZZZZ";
  public static final String SAMPLE_SAMHSA_DRG_CODE = "522";

  private static final String DRG =
      CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD);

  @RunWith(Parameterized.class)
  public static class ContainsOnlyKnownSystemsTests {

    private final String name;
    private final List<String> systems;
    private final boolean expectedResult;
    private final String errorMessage;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> parameters() {
      final String HCPCS = TransformerConstants.CODING_SYSTEM_HCPCS;
      final String OTHER = "other system";
      return List.of(
          new Object[][] {
            {
              "Empty list",
              Collections.emptyList(),
              false,
              "should NOT return true (all known systems), but DID."
            },
            {
              "HCPCS only systems",
              List.of(HCPCS, HCPCS, HCPCS),
              true,
              "SHOULD return true (all known systems), but did NOT."
            },
            {
              "Other system only",
              List.of(OTHER, OTHER),
              false,
              "should NOT return true (all known systems), but DID."
            },
            {
              "HCPCS and other systems",
              List.of(HCPCS, HCPCS, OTHER),
              false,
              "should NOT return true (all known systems), but DID."
            },
          });
    }

    public ContainsOnlyKnownSystemsTests(
        String name, List<String> systems, boolean expectedResult, String errorMessage) {
      this.name = name;
      this.systems = systems;
      this.expectedResult = expectedResult;
      this.errorMessage = errorMessage;
    }

    @Test
    public void test() {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

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
              .collect(Collectors.toUnmodifiableList());

      doReturn(codings).when(mockConcept).getCoding();

      assertEquals(
          name + " " + errorMessage, expectedResult, matcher.containsOnlyKnownSystems(mockConcept));
    }
  }

  public static class NonParameterizedTests {
    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * false</code> for claims that have no SAMHSA-related codes.
     */
    @Test
    public void nonSamhsaRelatedClaims() {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      // Note: none of our SAMPLE_A claims have SAMHSA-related codes (by default).
      List<Object> sampleRifRecords =
          ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
      List<ExplanationOfBenefit> sampleEobs =
          sampleRifRecords.stream()
              .map(
                  r -> {
                    // FIXME remove most `else if`s once filtering fully supports all claim types
                    if (r instanceof Beneficiary) return null;
                    else if (r instanceof BeneficiaryHistory) return null;
                    else if (r instanceof MedicareBeneficiaryIdHistory) return null;

                    return TransformerUtils.transformRifRecordToEob(
                        new MetricRegistry(), r, Optional.empty());
                  })
              .filter(ExplanationOfBenefit.class::isInstance)
              .collect(Collectors.toList());

      for (ExplanationOfBenefit sampleEob : sampleEobs)
        Assert.assertFalse(
            "Unexpected SAMHSA filtering of EOB: " + sampleEob.getId(), matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#CARRIER} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchCarrierClaimsByIcd9Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#CARRIER} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchCarrierClaimsByIcd10Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#CARRIER} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchCarrierClaimsByCptProcedure() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#CARRIER} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchCarrierClaimsByCptProcedureForNewCodes() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();
      String SAMPLE_SAMHSA_CPT_NEW_CODE = "G2067";

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_NEW_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#DME} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchDmeClaimsByIcd9Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.DME);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#DME} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchDmeClaimsByIcd10Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.DME);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#DME} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchDmeClaimsByCptProcedure() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.DME);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#INPATIENT} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchInpatientClaimsByIcd9Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosis().get(1).getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#INPATIENT} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchInpatientClaimsByIcd10Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosis().get(1).getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#INPATIENT} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchInpatientClaimsByIcd9Procedure() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#INPATIENT} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchInpatientClaimsByIcd10Procedure() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#INPATIENT} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related drg codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchInpatientClaimsByDrg() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
      sampleEob
          .getDiagnosisFirstRep()
          .getPackageCode()
          .addCoding()
          .setSystem(Stu3EobSamhsaMatcherTest.DRG)
          .setCode(SAMPLE_SAMHSA_DRG_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#OUTPATIENT} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchOutpatientClaimsByIcd9Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#OUTPATIENT} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchOutpatientClaimsByIcd10Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#OUTPATIENT} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchOutpatientClaimsByCptProcedure() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#OUTPATIENT} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchOutpatientClaimsByIcd9Procedure() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#OUTPATIENT} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchOutpatientClaimsByIcd10Procedure() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
      Coding sampleEobDiagnosis =
          sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#HHA} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchHhaClaimsByIcd9Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HHA);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#HHA} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchHhaClaimsByIcd10Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HHA);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#HHA} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchHhaClaimsByCptProcedure() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HHA);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#HOSPICE} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchHospiceClaimsByIcd9Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HOSPICE);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#HOSPICE} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchHospiceClaimsByIcd10Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HOSPICE);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosisFirstRep().getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#HOSPICE} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchHospiceClaimsByCptProcedure() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HOSPICE);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#SNF} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchSnfClaimsByIcd9Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosis().get(1).getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#SNF} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchSnfClaimsByIcd10Diagnosis() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
      Coding sampleEobDiagnosis =
          sampleEob.getDiagnosis().get(1).getDiagnosisCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#SNF} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchSnfClaimsByCptProcedure() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
      Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
      sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#SNF} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchSnfClaimsByIcd9Procedure() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
      Coding sampleEobDiagnosis =
          sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_9)
          .setCode(SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#SNF} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchSnfClaimsByIcd10Procedure() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
      Coding sampleEobDiagnosis =
          sampleEob.getProcedureFirstRep().getProcedureCodeableConcept().getCodingFirstRep();
      sampleEobDiagnosis
          .setSystem(IcdCode.CODING_SYSTEM_ICD_10)
          .setCode(SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * Verifies that {@link
     * gov.cms.bfd.server.war.stu3.providers.Stu3EobSamhsaMatcher#test(ExplanationOfBenefit)}
     * returns <code>
     * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#SNF} {@link
     * ExplanationOfBenefit}s that have SAMHSA-related drg codes.
     *
     * @throws FHIRException (indicates problem with test data)
     */
    @Test
    public void matchSnfClaimsByDrg() throws FHIRException {
      Stu3EobSamhsaMatcher matcher = new Stu3EobSamhsaMatcher();

      ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
      sampleEob
          .getDiagnosisFirstRep()
          .getPackageCode()
          .addCoding()
          .setSystem(Stu3EobSamhsaMatcherTest.DRG)
          .setCode(SAMPLE_SAMHSA_DRG_CODE);

      Assert.assertTrue(matcher.test(sampleEob));
    }

    /**
     * @param claimType the {@link gov.cms.bfd.server.war.stu3.providers.ClaimType} to get a sample
     *     {@link ExplanationOfBenefit} for
     * @return a sample {@link ExplanationOfBenefit} of the specified {@link
     *     gov.cms.bfd.server.war.stu3.providers.ClaimType} (derived from the {@link
     *     StaticRifResourceGroup#SAMPLE_A} sample RIF records)
     */
    private ExplanationOfBenefit getSampleAClaim(ClaimType claimType) {
      List<Object> sampleRifRecords =
          ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
      Object sampleRifRecordForClaimType =
          sampleRifRecords.stream()
              .filter(claimType.getEntityClass()::isInstance)
              .findFirst()
              .get();
      ExplanationOfBenefit sampleEobForClaimType =
          TransformerUtils.transformRifRecordToEob(
              new MetricRegistry(), sampleRifRecordForClaimType, Optional.empty());

      return sampleEobForClaimType;
    }
  }
}
