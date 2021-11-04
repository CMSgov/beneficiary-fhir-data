package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.MedicareBeneficiaryIdHistory;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.IcdCode;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link SamhsaMatcherTest}. Integration with {@link
 * gov.cms.bfd.server.war.stu3.providers.ExplanationOfBenefitResourceProvider} is covered by {@link
 * ExplanationOfBenefitResourceProviderIT#searchForEobsWithSamhsaFiltering()} and related
 * integration tests.
 */
public final class SamhsaMatcherTest {
  // TODO complete and verify that these exactly match real values in our DB
  public static final String SAMPLE_SAMHSA_CPT_CODE = "4320F";
  public static final String SAMPLE_SAMHSA_ICD_9_DIAGNOSIS_CODE = "29189";
  public static final String SAMPLE_SAMHSA_ICD_9_PROCEDURE_CODE = "9445";
  public static final String SAMPLE_SAMHSA_ICD_10_DIAGNOSIS_CODE = "F1010";
  public static final String SAMPLE_SAMHSA_ICD_10_PROCEDURE_CODE = "HZ2ZZZZ";
  public static final String SAMPLE_SAMHSA_DRG_CODE = "522";

  private static final String DRG =
      TransformerUtils.calculateVariableReferenceUrl(CcwCodebookVariable.CLM_DRG_CD);

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * false</code> for claims that have no SAMHSA-related codes.
   */
  @Test
  public void nonSamhsaRelatedClaims() {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#CARRIER} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchCarrierClaimsByIcd9Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#CARRIER} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchCarrierClaimsByIcd10Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#CARRIER} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchCarrierClaimsByCptProcedure() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

    ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
    Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
    sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

    Assert.assertTrue(matcher.test(sampleEob));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#CARRIER} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchCarrierClaimsByCptProcedureForNewCodes() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();
    String SAMPLE_SAMHSA_CPT_NEW_CODE = "G2067";

    ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.CARRIER);
    Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
    sampleEobService.setCode(SAMPLE_SAMHSA_CPT_NEW_CODE);

    Assert.assertTrue(matcher.test(sampleEob));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#DME} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchDmeClaimsByIcd9Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#DME} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchDmeClaimsByIcd10Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#DME} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchDmeClaimsByCptProcedure() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

    ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.DME);
    Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
    sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

    Assert.assertTrue(matcher.test(sampleEob));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#INPATIENT} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchInpatientClaimsByIcd9Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#INPATIENT} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchInpatientClaimsByIcd10Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#INPATIENT} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchInpatientClaimsByIcd9Procedure() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#INPATIENT} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchInpatientClaimsByIcd10Procedure() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#INPATIENT} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related drg codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchInpatientClaimsByDrg() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

    ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.INPATIENT);
    sampleEob
        .getDiagnosisFirstRep()
        .getPackageCode()
        .addCoding()
        .setSystem(SamhsaMatcherTest.DRG)
        .setCode(SAMPLE_SAMHSA_DRG_CODE);

    Assert.assertTrue(matcher.test(sampleEob));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#OUTPATIENT} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchOutpatientClaimsByIcd9Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#OUTPATIENT} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchOutpatientClaimsByIcd10Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#OUTPATIENT} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchOutpatientClaimsByCptProcedure() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

    ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.OUTPATIENT);
    Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
    sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

    Assert.assertTrue(matcher.test(sampleEob));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#OUTPATIENT} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchOutpatientClaimsByIcd9Procedure() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#OUTPATIENT} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchOutpatientClaimsByIcd10Procedure() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#HHA} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchHhaClaimsByIcd9Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#HHA} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchHhaClaimsByIcd10Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#HHA} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchHhaClaimsByCptProcedure() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

    ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HHA);
    Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
    sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

    Assert.assertTrue(matcher.test(sampleEob));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#HOSPICE} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchHospiceClaimsByIcd9Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#HOSPICE} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchHospiceClaimsByIcd10Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#HOSPICE} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchHospiceClaimsByCptProcedure() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

    ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.HOSPICE);
    Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
    sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

    Assert.assertTrue(matcher.test(sampleEob));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#SNF} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchSnfClaimsByIcd9Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#SNF} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchSnfClaimsByIcd10Diagnosis() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#SNF} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related CPT procedure codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchSnfClaimsByCptProcedure() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

    ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
    Coding sampleEobService = sampleEob.getItemFirstRep().getService().getCodingFirstRep();
    sampleEobService.setCode(SAMPLE_SAMHSA_CPT_CODE);

    Assert.assertTrue(matcher.test(sampleEob));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#SNF} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 9 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchSnfClaimsByIcd9Procedure() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#SNF} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related ICD 10 diagnosis codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchSnfClaimsByIcd10Procedure() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

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
   * gov.cms.bfd.server.war.stu3.providers.SamhsaMatcher#test(ExplanationOfBenefit)} returns <code>
   * true</code> for {@link gov.cms.bfd.server.war.stu3.providers.ClaimType#SNF} {@link
   * ExplanationOfBenefit}s that have SAMHSA-related drg codes.
   *
   * @throws FHIRException (indicates problem with test data)
   */
  @Test
  public void matchSnfClaimsByDrg() throws FHIRException {
    SamhsaMatcher matcher = new SamhsaMatcher();

    ExplanationOfBenefit sampleEob = getSampleAClaim(ClaimType.SNF);
    sampleEob
        .getDiagnosisFirstRep()
        .getPackageCode()
        .addCoding()
        .setSystem(SamhsaMatcherTest.DRG)
        .setCode(SAMPLE_SAMHSA_DRG_CODE);

    Assert.assertTrue(matcher.test(sampleEob));
  }

  /**
   * Verifies that when transforming a claim into an ExplanationOfBenefit which has
   * item[n].productOrService.coding[n].system (procedure code) values which = has eob.procedure
   * SAMSHA code, then the SAMHSA matcher's test method will identify this as a SAMSHA related
   * ExplanationOfBenefit.
   */
  @Test
  public void
      testSamhsaMatcherWhenTransformedInpatientHasItemWithNoValidSamhsaProcedureCodeReturnsFalse() {
    SamhsaMatcher samhsaMatcher = new SamhsaMatcher();
    // Given
    InpatientClaim claim = getInpatientClaim();

    ExplanationOfBenefit explanationOfBenefit =
        InpatientClaimTransformer.transform(new MetricRegistry(), claim, Optional.empty());

    // Set Top level diagnosis and package code to null and coding to empty
    for (ExplanationOfBenefit.DiagnosisComponent diagnosisComponent :
        explanationOfBenefit.getDiagnosis()) {
      diagnosisComponent.setPackageCode(null);
    }

    // Set procedure to empty
    explanationOfBenefit.setProcedure(new ArrayList<>());

    // Set item level codings to non-SAMHSA
    explanationOfBenefit
        .getItem()
        .get(0)
        .getService()
        .setCoding(
            Collections.singletonList(
                new Coding(TransformerConstants.CODING_SYSTEM_HCPCS, "123456", "")));

    // When
    boolean isMatch = samhsaMatcher.test(explanationOfBenefit);

    // Then
    assertFalse(isMatch);
  }

  /**
   * Verifies that when transforming a claim into an ExplanationOfBenefit which has
   * item[n].productOrService.coding[n].system (procedure code) values which = has eob.procedure
   * SAMSHA code, then the SAMHSA matcher's test method will identify this as a SAMSHA related
   * ExplanationOfBenefit.
   */
  @Test
  public void
      testSamhsaMatcherWhenTransformedInpatientHasItemWithValidSamhsaProcedureCodeReturnsTrue() {
    SamhsaMatcher samhsaMatcher = new SamhsaMatcher();
    // Given
    InpatientClaim claim = getInpatientClaim();

    ExplanationOfBenefit explanationOfBenefit =
        InpatientClaimTransformer.transform(new MetricRegistry(), claim, Optional.empty());

    // Set Top level diagnosis and package code to null and coding to empty
    for (ExplanationOfBenefit.DiagnosisComponent diagnosisComponent :
        explanationOfBenefit.getDiagnosis()) {
      diagnosisComponent.setPackageCode(null);
    }

    // Set procedure to empty
    explanationOfBenefit.setProcedure(new ArrayList<>());

    // Set item level codings to non-SAMHSA
    explanationOfBenefit
        .getItem()
        .get(0)
        .getService()
        .setCoding(
            Collections.singletonList(
                new Coding(TransformerConstants.CODING_SYSTEM_HCPCS, "H0050", "")));

    // When
    boolean isMatch = samhsaMatcher.test(explanationOfBenefit);

    // Then
    assertTrue(isMatch);
  }

  /**
   * Verify samsha matcher for item with the given system, code and if the expectation is that there
   * should be a match for this combination.
   *
   * @param system the system value
   * @param code the code
   * @param shouldMatch if the matcher should match on this combination
   */
  public void verifySamhsaMatcherForItem(String system, String code, boolean shouldMatch) {
    SamhsaMatcher samhsaMatcher = new SamhsaMatcher();

    InpatientClaim claim = getInpatientClaim();

    ExplanationOfBenefit explanationOfBenefit =
        InpatientClaimTransformer.transform(new MetricRegistry(), claim, Optional.empty());

    // Set Top level diagnosis and package code to null so we can test item logic
    for (ExplanationOfBenefit.DiagnosisComponent diagnosisComponent :
        explanationOfBenefit.getDiagnosis()) {
      CodeableConcept codeableConcept = diagnosisComponent.getDiagnosisCodeableConcept();
      codeableConcept.setCoding(new ArrayList<>());
      diagnosisComponent.setPackageCode(null);
    }

    // Set item level code to the correct coding system
    explanationOfBenefit.getItem().get(0).getService().getCoding().get(0).setSystem(system);
    // Set allowed CPT code
    explanationOfBenefit.getItem().get(0).getService().getCoding().get(0).setCode(code);

    assertEquals(shouldMatch, samhsaMatcher.test(explanationOfBenefit));
  }

  /**
   * Generates the Claim object to be used in a test.
   *
   * @return the inpatient claim to be used for the test
   */
  public InpatientClaim getInpatientClaim() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    InpatientClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof InpatientClaim)
            .map(r -> (InpatientClaim) r)
            .findFirst()
            .orElse(null);

    if (claim != null) {
      claim.setLastUpdated(Instant.now());
    } else {
      throw new IllegalStateException(
          "Test setup issue, did not find expected InpatientClaim in sample record.");
    }

    return claim;
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
        sampleRifRecords.stream().filter(claimType.getEntityClass()::isInstance).findFirst().get();
    ExplanationOfBenefit sampleEobForClaimType =
        TransformerUtils.transformRifRecordToEob(
            new MetricRegistry(), sampleRifRecordForClaimType, Optional.empty());

    return sampleEobForClaimType;
  }
}
