package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaimLine;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.NPIOrgLookup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CCWProcedure;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.OutpatientClaimTransformer}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class OutpatientClaimTransformerTest {

  /** The transformer under test. */
  OutpatientClaimTransformer outpatientClaimTransformer;

  /** The Metric Registry to use for the test. */
  @Mock MetricRegistry metricRegistry;

  /** The SamhsaSecurityTag lookup. */
  @Mock SecurityTagManager securityTagManager;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  Set<String> securityTags = new HashSet<>();

  /** One-time setup of objects that are normally injected. */
  @BeforeEach
  public void setup() throws IOException {
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);

    outpatientClaimTransformer =
        new OutpatientClaimTransformer(metricRegistry, securityTagManager, false);
  }

  /**
   * Verifies that when transform is called, the metric registry is passed the correct class and
   * subtype name, is started, and stopped. Note that timer.stop() and timer.close() are equivalent
   * and one or the other may be called based on how the timer is used in code.
   */
  @Test
  public void testTransformRunsMetricTimer() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    OutpatientClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof OutpatientClaim)
            .map(OutpatientClaim.class::cast)
            .findFirst()
            .orElseThrow();

    outpatientClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));

    String expectedTimerName = outpatientClaimTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /**
   * Verifies that {@link
   * ClaimTransformerInterface#transform} works as expected
   * when run against the {@link StaticRifResource#SAMPLE_A_OUTPATIENT} {@link OutpatientClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException, IOException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    OutpatientClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof OutpatientClaim)
            .map(OutpatientClaim.class::cast)
            .findFirst()
            .get();

    ExplanationOfBenefit eob =
        outpatientClaimTransformer.transform(
            new ClaimWithSecurityTags<>(claim, securityTags));
    assertMatches(claim, eob);
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link OutpatientClaim}.
   *
   * @param claim the {@link OutpatientClaim} that the {@link ExplanationOfBenefit} was generated
   *     from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     OutpatientClaim}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(OutpatientClaim claim, ExplanationOfBenefit eob)
      throws FHIRException, IOException {
    NPIOrgLookup localNpiLookup = RDATestUtils.createTestNpiOrgLookup();

    // Test to ensure group level fields between all claim types match
    TransformerTestUtils.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimType.OUTPATIENT,
        String.valueOf(claim.getClaimGroupId()),
        MedicareSegment.PART_B,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());

    // test the common field provider number is set as expected in the EOB
    TransformerTestUtils.assertProviderNumber(eob, claim.getProviderNumber());

    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_BENE_PTB_DDCTBL_AMT, claim.getDeductibleAmount(), eob);
    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_PROFNL_CMPNT_CHRG_AMT, claim.getProfessionalComponentCharge(), eob);
    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.NCH_BENE_PTB_COINSRNC_AMT, claim.getCoinsuranceAmount(), eob);
    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.CLM_OP_PRVDR_PMT_AMT, claim.getProviderPaymentAmount(), eob);
    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.CLM_OP_BENE_PMT_AMT, claim.getBeneficiaryPaymentAmount(), eob);

    // Test to ensure common group fields between Inpatient, Outpatient and SNF
    TransformerTestUtils.assertEobCommonGroupInpOutSNFEquals(
        eob,
        claim.getBloodDeductibleLiabilityAmount(),
        claim.getOperatingPhysicianNpi(),
        claim.getOtherPhysicianNpi(),
        claim.getClaimQueryCode(),
        claim.getMcoPaidSw());

    // Test to ensure common group fields between Inpatient, Outpatient HHA, Hospice
    // and SNF match
    TransformerTestUtils.assertEobCommonGroupInpOutHHAHospiceSNFEquals(
        eob,
        claim.getOrganizationNpi(),
        Optional.empty(),
        claim.getClaimFacilityTypeCode(),
        claim.getClaimFrequencyCode(),
        claim.getClaimNonPaymentReasonCode(),
        claim.getPatientDischargeStatusCode().get(),
        claim.getClaimServiceClassificationTypeCode(),
        claim.getClaimPrimaryPayerCode(),
        claim.getAttendingPhysicianNpi(),
        claim.getTotalChargeAmount(),
        claim.getPrimaryPayerPaidAmount(),
        claim.getFiscalIntermediaryNumber(),
        claim.getFiDocumentClaimControlNumber(),
        claim.getFiOriginalClaimControlNumber());

    assertTrue(
        countDiagnosisCodes(claim) >= eob.getDiagnosis().size(),
        "Expect actual diagnosis count is less than or equal to the claim count");

    if (claim.getProcedure1Code().isPresent()) {
      CCWProcedure ccwProcedure =
          new CCWProcedure(
              claim.getProcedure1Code(),
              claim.getProcedure1CodeVersion(),
              claim.getProcedure1Date());
      TransformerTestUtils.assertHasCoding(
          ccwProcedure.getFhirSystem(),
          claim.getProcedure1Code().get(),
          eob.getProcedure().get(0).getProcedureCodeableConcept().getCoding());
      assertEquals(
          claim.getProcedure1Date().get().atStartOfDay(ZoneId.systemDefault()).toInstant(),
          eob.getProcedure().get(0).getDate().toInstant());
    }

    assertTrue(1 <= eob.getItem().size(), "Expect actual item count is above 0");
    ItemComponent eobItem0 = eob.getItem().get(0);
    OutpatientClaimLine claimLine1 = claim.getLines().get(0);
    assertEquals(claimLine1.getLineNumber(), eobItem0.getSequence());

    assertEquals(claim.getProviderStateCode(), eobItem0.getLocationAddress().getState());

    // TODO re-map as described in CBBF-111
    /*
     * TransformerTestUtils.assertHasCoding(TransformerConstants.CODING_NDC,
     * claimLine1.getNationalDrugCode().get(), eobItem0.getService());
     */

    TransformerTestUtils.assertAdjudicationReasonEquals(
        CcwCodebookVariable.REV_CNTR_1ST_ANSI_CD,
        claimLine1.getRevCntr1stAnsiCd(),
        eobItem0.getAdjudication());
    TransformerTestUtils.assertAdjudicationReasonEquals(
        CcwCodebookVariable.REV_CNTR_2ND_ANSI_CD,
        claimLine1.getRevCntr2ndAnsiCd(),
        eobItem0.getAdjudication());
    TransformerTestUtils.assertAdjudicationReasonEquals(
        CcwCodebookVariable.REV_CNTR_3RD_ANSI_CD,
        claimLine1.getRevCntr3rdAnsiCd(),
        eobItem0.getAdjudication());
    TransformerTestUtils.assertAdjudicationReasonEquals(
        CcwCodebookVariable.REV_CNTR_4TH_ANSI_CD,
        claimLine1.getRevCntr4thAnsiCd(),
        eobItem0.getAdjudication());

    TransformerTestUtils.assertHcpcsCodes(
        eobItem0,
        claimLine1.getHcpcsCode(),
        claimLine1.getHcpcsInitialModifierCode(),
        claimLine1.getHcpcsSecondModifierCode(),
        Optional.empty(),
        0 /* index */);

    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.REV_CNTR_IDE_NDC_UPC_NUM,
        claimLine1.getNationalDrugCode(),
        eobItem0.getService());

    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_BLOOD_DDCTBL_AMT,
        claimLine1.getBloodDeductibleAmount(),
        eobItem0.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_CASH_DDCTBL_AMT,
        claimLine1.getCashDeductibleAmount(),
        eobItem0.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_COINSRNC_WGE_ADJSTD_C,
        claimLine1.getWageAdjustedCoinsuranceAmount(),
        eobItem0.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_RDCD_COINSRNC_AMT,
        claimLine1.getReducedCoinsuranceAmount(),
        eobItem0.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_1ST_MSP_PD_AMT,
        claimLine1.getFirstMspPaidAmount(),
        eobItem0.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_1ST_MSP_PD_AMT,
        claimLine1.getSecondMspPaidAmount(),
        eobItem0.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_PRVDR_PMT_AMT,
        claimLine1.getProviderPaymentAmount(),
        eobItem0.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_BENE_PMT_AMT,
        claimLine1.getBenficiaryPaymentAmount(),
        eobItem0.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_PTNT_RSPNSBLTY_PMT,
        claimLine1.getPatientResponsibilityAmount(),
        eobItem0.getAdjudication());

    String claimControlNumber = "0000000000";

    // Test to ensure item level fields between Inpatient, Outpatient, HHA, Hopsice
    // and SNF match
    TransformerTestUtils.assertEobCommonItemRevenueEquals(
        eobItem0,
        eob,
        claimLine1.getRevenueCenterCode(),
        claimLine1.getRateAmount(),
        claimLine1.getTotalChargeAmount(),
        claimLine1.getNonCoveredChargeAmount(),
        claimLine1.getUnitCount(),
        claimControlNumber,
        claimLine1.getNationalDrugCodeQuantity(),
        claimLine1.getNationalDrugCodeQualifierCode(),
        claimLine1.getRevenueCenterRenderingPhysicianNPI(),
        1 /* index */);

    // Test to ensure item level fields between Outpatient, HHA and Hospice match
    TransformerTestUtils.assertEobCommonItemRevenueOutHHAHospice(
        eobItem0, claimLine1.getRevenueCenterDate(), claimLine1.getPaymentAmount());

    // verify {@link
    // TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
    // method worked as expected for this claim type
    TransformerTestUtils.assertMapEobType(
        eob.getType(),
        ClaimType.OUTPATIENT,
        Optional.of(org.hl7.fhir.dstu3.model.codesystems.ClaimType.PROFESSIONAL),
        Optional.of(claim.getNearLineRecordIdCode()),
        Optional.of(claim.getClaimTypeCode()));

    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.REV_CNTR_STUS_IND_CD,
        claimLine1.getStatusCode(),
        eobItem0.getRevenue());

    // Test lastUpdated
    TransformerTestUtils.assertLastUpdatedEquals(claim.getLastUpdated(), eob);
  }

  /**
   * Counts the diagnosis code method names in the specified claim.
   *
   * @param claim the claim to count diagnosis codes from
   * @return the number of diagnosis codes
   */
  public static long countDiagnosisCodes(OutpatientClaim claim) {
    Stream<String> methodNames =
        Stream.concat(
            Stream.concat(
                IntStream.range(1, 26).mapToObj(i -> "getDiagnosis" + i + "Code"),
                IntStream.range(1, 5).mapToObj(i -> "getDiagnosisAdmission" + i + "Code")),
            Stream.concat(
                IntStream.range(1, 16).mapToObj(i -> "getDiagnosisExternal" + i + "Code"),
                Stream.of("getDiagnosisPrincipalCode", "getDiagnosisExternalFirstCode")));

    return methodNames
        .filter(
            methodName -> {
              try {
                // invoke the claim.getDiagnosisXXCode() method
                final Optional<String> optional =
                    (Optional<String>) claim.getClass().getDeclaredMethod(methodName).invoke(claim);
                return optional.isPresent();
              } catch (Exception e) {
                return false;
              }
            })
        .count();
  }
}
