package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HHAClaimLine;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.NPIOrgLookup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link HHAClaimTransformer}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class HHAClaimTransformerTest {
  /** The transformer under test. */
  HHAClaimTransformer hhaClaimTransformer;

  /** The Metric Registry to use for the test. */
  @Mock MetricRegistry metricRegistry;

  /** The securityTagManager. */
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

    hhaClaimTransformer = new HHAClaimTransformer(metricRegistry, securityTagManager, false);
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
    HHAClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(HHAClaim.class::cast)
            .findFirst()
            .orElseThrow();

    hhaClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));

    String expectedTimerName = hhaClaimTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /**
   * Verifies that {@link ClaimTransformerInterface#transform} works as expected when run against
   * the {@link StaticRifResource#SAMPLE_A_HHA} {@link HHAClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException, IOException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    HHAClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof HHAClaim)
            .map(HHAClaim.class::cast)
            .findFirst()
            .get();

    ExplanationOfBenefit eob =
        hhaClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
    assertMatches(claim, eob);
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link HHAClaim}.
   *
   * @param claim the {@link HHAClaim} that the {@link ExplanationOfBenefit} was generated from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     HHAClaim}
   * @throws FHIRException (indicates test failure)
   */
  public static void assertMatches(HHAClaim claim, ExplanationOfBenefit eob)
      throws FHIRException, IOException {
    // interesting conumdrum here....we should be using Mock(s) for unit tests, but this static
    // method is invoked from ITs (integration tests) which means that our BeforeEach setup will
    // not create the NPIOrgLookup so we need to explicitly create one here.
    NPIOrgLookup localNpiLookup = RDATestUtils.createTestNpiOrgLookup();

    // Test to ensure group level fields between all claim types match
    TransformerTestUtils.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimType.HHA,
        String.valueOf(claim.getClaimGroupId()),
        MedicareSegment.PART_B,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());

    // test the common field provider number is set as expected in the EOB
    TransformerTestUtils.assertProviderNumber(eob, claim.getProviderNumber());

    // Test to ensure common group fields between Inpatient, Outpatient HHA, Hospice
    // and SNF match
    TransformerTestUtils.assertEobCommonGroupInpOutHHAHospiceSNFEquals(
        eob,
        claim.getOrganizationNpi(),
        Optional.empty(),
        claim.getClaimFacilityTypeCode(),
        claim.getClaimFrequencyCode(),
        claim.getClaimNonPaymentReasonCode(),
        claim.getPatientDischargeStatusCode(),
        claim.getClaimServiceClassificationTypeCode(),
        claim.getClaimPrimaryPayerCode(),
        claim.getAttendingPhysicianNpi(),
        claim.getTotalChargeAmount(),
        claim.getPrimaryPayerPaidAmount(),
        claim.getFiscalIntermediaryNumber(),
        claim.getFiDocumentClaimControlNumber(),
        claim.getFiOriginalClaimControlNumber());

    assertEquals(4, eob.getDiagnosis().size());

    if (claim.getClaimLUPACode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.CLM_HHA_LUPA_IND_CD,
          CcwCodebookVariable.CLM_HHA_LUPA_IND_CD,
          claim.getClaimLUPACode(),
          eob);
    if (claim.getClaimReferralCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.CLM_HHA_RFRL_CD,
          CcwCodebookVariable.CLM_HHA_RFRL_CD,
          claim.getClaimReferralCode(),
          eob);

    TransformerTestUtils.assertBenefitBalanceUsedIntEquals(
        BenefitCategory.MEDICAL,
        CcwCodebookVariable.CLM_HHA_TOT_VISIT_CNT,
        claim.getTotalVisitCount().intValue(),
        eob);

    assertEquals(1, eob.getItem().size());
    ItemComponent eobItem0 = eob.getItem().get(0);
    HHAClaimLine claimLine1 = claim.getLines().get(0);
    assertEquals(claimLine1.getLineNumber(), eobItem0.getSequence());

    assertEquals(claim.getProviderStateCode(), eobItem0.getLocationAddress().getState());

    TransformerTestUtils.assertAdjudicationReasonEquals(
        CcwCodebookVariable.REV_CNTR_1ST_ANSI_CD,
        claimLine1.getRevCntr1stAnsiCd(),
        eobItem0.getAdjudication());

    TransformerTestUtils.assertHcpcsCodes(
        eobItem0,
        claimLine1.getHcpcsCode(),
        claimLine1.getHcpcsInitialModifierCode(),
        claimLine1.getHcpcsSecondModifierCode(),
        Optional.empty(),
        0 /* index */);

    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.REV_CNTR_STUS_IND_CD,
        claimLine1.getStatusCode().get(),
        eobItem0.getRevenue());

    // test common eob information between Inpatient, HHA, Hospice and SNF claims
    // are set as
    // expected
    TransformerTestUtils.assertEobCommonGroupInpHHAHospiceSNFEquals(
        eob, claim.getCareStartDate(), Optional.empty(), Optional.empty());

    // Test to ensure common group field coinsurance between Inpatient, HHA, Hospice
    // and SNF match
    TransformerTestUtils.assertEobCommonGroupInpHHAHospiceSNFCoinsuranceEquals(
        eobItem0, claimLine1.getDeductibleCoinsuranceCd());

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
        ClaimType.HHA,
        // FUTURE there currently is not an equivalent CODING_FHIR_CLAIM_TYPE mapping
        // for this claim type. If added then the Optional empty parameter below should
        // be updated to match expected result.
        Optional.empty(),
        Optional.of(claim.getNearLineRecordIdCode()),
        Optional.of(claim.getClaimTypeCode()));

    // Test lastUpdated
    TransformerTestUtils.assertLastUpdatedEquals(claim.getLastUpdated(), eob);
  }
}
