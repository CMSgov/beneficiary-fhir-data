package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.HospiceClaimLine;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link HospiceClaimTransformer}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class HospiceClaimTransformerTest {
  /** The transformer under test. */
  HospiceClaimTransformer hospiceClaimTransformer;
  /** The Metric Registry to use for the test. */
  @Mock MetricRegistry metricRegistry;
  /** The mock metric timer. */
  @Mock Timer mockTimer;
  /** The mock metric timer context (used to stop the metric). */
  @Mock Timer.Context mockTimerContext;

  /** One-time setup of objects that are normally injected. */
  @BeforeEach
  protected void setup() {
    when(metricRegistry.timer(any())).thenReturn(mockTimer);
    when(mockTimer.time()).thenReturn(mockTimerContext);

    hospiceClaimTransformer = new HospiceClaimTransformer(metricRegistry, new NPIOrgLookup());
  }

  /**
   * Verifies that {@link HospiceClaimTransformer#transform} works as expected when run against the
   * {@link StaticRifResource#SAMPLE_A_HOSPICE} {@link HospiceClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException, IOException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    HospiceClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof HospiceClaim)
            .map(HospiceClaim.class::cast)
            .findFirst()
            .get();

    ExplanationOfBenefit eob = hospiceClaimTransformer.transform(claim, false);
    assertMatches(claim, eob);
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link HospiceClaim}.
   *
   * @param claim the {@link HospiceClaim} that the {@link ExplanationOfBenefit} was generated from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     HospiceClaim}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(HospiceClaim claim, ExplanationOfBenefit eob) throws FHIRException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtils.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimType.HOSPICE,
        String.valueOf(claim.getClaimGroupId()),
        MedicareSegment.PART_A,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());

    // test the common field provider number is set as expected in the EOB
    TransformerTestUtils.assertProviderNumber(eob, claim.getProviderNumber());

    TransformerTestUtils.assertInfoWithCodeEquals(
        CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
        CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
        claim.getPatientStatusCd(),
        eob);

    TransformerTestUtils.assertDateEquals(
        claim.getClaimHospiceStartDate().get(), eob.getHospitalization().getStartElement());
    TransformerTestUtils.assertDateEquals(
        claim.getBeneficiaryDischargeDate().get(), eob.getHospitalization().getEndElement());

    // Test to ensure common group fields between Inpatient, Outpatient HHA, Hospice
    // and SNF match
    TransformerTestUtils.assertEobCommonGroupInpOutHHAHospiceSNFEquals(
        eob,
        claim.getOrganizationNpi(),
        (new NPIOrgLookup()).retrieveNPIOrgDisplay(claim.getOrganizationNpi()),
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

    // test common eob information between Inpatient, HHA, Hospice and SNF claims
    // are set as
    // expected
    TransformerTestUtils.assertEobCommonGroupInpHHAHospiceSNFEquals(
        eob,
        claim.getClaimHospiceStartDate(),
        claim.getBeneficiaryDischargeDate(),
        Optional.of(claim.getUtilizationDayCount()));

    assertEquals(4, eob.getDiagnosis().size());
    assertEquals(1, eob.getItem().size());
    ItemComponent eobItem0 = eob.getItem().get(0);
    HospiceClaimLine claimLine1 = claim.getLines().get(0);
    assertEquals(claimLine1.getLineNumber(), eobItem0.getSequence());

    TransformerTestUtils.assertExtensionQuantityEquals(
        CcwCodebookVariable.BENE_HOSPC_PRD_CNT,
        claim.getHospicePeriodCount(),
        eob.getHospitalization());

    assertEquals(claim.getProviderStateCode(), eobItem0.getLocationAddress().getState());

    TransformerTestUtils.assertHcpcsCodes(
        eobItem0,
        claimLine1.getHcpcsCode(),
        claimLine1.getHcpcsInitialModifierCode(),
        claimLine1.getHcpcsSecondModifierCode(),
        Optional.empty(),
        0 /* index */);

    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_PRVDR_PMT_AMT,
        claimLine1.getProviderPaymentAmount(),
        eobItem0.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.REV_CNTR_BENE_PMT_AMT,
        claimLine1.getBenficiaryPaymentAmount(),
        eobItem0.getAdjudication());

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
        claimLine1.getNonCoveredChargeAmount().get(),
        claimLine1.getUnitCount(),
        claimControlNumber,
        claimLine1.getNationalDrugCodeQuantity(),
        claimLine1.getNationalDrugCodeQualifierCode(),
        claimLine1.getRevenueCenterRenderingPhysicianNPI(),
        1 /* index */);

    TransformerTestUtils.assertCareTeamEquals(
        claimLine1.getRevenueCenterRenderingPhysicianNPI().get(), ClaimCareteamrole.PRIMARY, eob);

    // Test to ensure item level fields between Outpatient, HHA and Hospice match
    TransformerTestUtils.assertEobCommonItemRevenueOutHHAHospice(
        eobItem0, claimLine1.getRevenueCenterDate(), claimLine1.getPaymentAmount());

    // verify {@link
    // TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
    // method worked as expected for this claim type
    TransformerTestUtils.assertMapEobType(
        eob.getType(),
        ClaimType.HOSPICE,
        Optional.of(org.hl7.fhir.dstu3.model.codesystems.ClaimType.INSTITUTIONAL),
        Optional.of(claim.getNearLineRecordIdCode()),
        Optional.of(claim.getClaimTypeCode()));

    // Test lastUpdated
    TransformerTestUtils.assertLastUpdatedEquals(claim.getLastUpdated(), eob);
  }
}
