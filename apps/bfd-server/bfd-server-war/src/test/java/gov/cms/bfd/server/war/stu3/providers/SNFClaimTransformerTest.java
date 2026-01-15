package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.entities.SNFClaimLine;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CCWProcedure;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.dstu3.model.Period;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link SNFClaimTransformer}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class SNFClaimTransformerTest {
  /** The transformer under test. */
  SNFClaimTransformer snfClaimTransformer;

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

    snfClaimTransformer = new SNFClaimTransformer(metricRegistry, securityTagManager, false);
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
    SNFClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof SNFClaim)
            .map(SNFClaim.class::cast)
            .findFirst()
            .orElseThrow();

    snfClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));

    String expectedTimerName = snfClaimTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /**
   * Verifies that {@link ClaimTransformerInterface#transform} works as expected when run against the
   * {@link StaticRifResource#SAMPLE_A_SNF} {@link SNFClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException, IOException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    SNFClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof SNFClaim)
            .map(SNFClaim.class::cast)
            .findFirst()
            .get();

    ExplanationOfBenefit eob =
        snfClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
    assertMatches(claim, eob);
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link SNFClaim}.
   *
   * @param claim the {@link SNFClaim} that the {@link ExplanationOfBenefit} was generated from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     SNFClaim}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(SNFClaim claim, ExplanationOfBenefit eob)
      throws FHIRException, IOException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtils.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimType.SNF,
        String.valueOf(claim.getClaimGroupId()),
        MedicareSegment.PART_A,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());

    // test the common field provider number is set as expected in the EOB
    TransformerTestUtils.assertProviderNumber(eob, claim.getProviderNumber());

    // test common benefit components between SNF and Inpatient claims are set as
    // expected
    TransformerTestUtils.assertCommonGroupInpatientSNF(
        eob,
        claim.getCoinsuranceDayCount(),
        claim.getNonUtilizationDayCount(),
        claim.getDeductibleAmount(),
        claim.getPartACoinsuranceLiabilityAmount(),
        claim.getBloodPintsFurnishedQty(),
        claim.getNoncoveredCharge(),
        claim.getTotalDeductionAmount(),
        claim.getClaimPPSCapitalDisproportionateShareAmt(),
        claim.getClaimPPSCapitalExceptionAmount(),
        claim.getClaimPPSCapitalFSPAmount(),
        claim.getClaimPPSCapitalIMEAmount(),
        claim.getClaimPPSCapitalOutlierAmount(),
        claim.getClaimPPSOldCapitalHoldHarmlessAmount());

    if (claim.getQualifiedStayFromDate().isPresent()
        || claim.getQualifiedStayThroughDate().isPresent()) {
      SupportingInformationComponent nchQlyfdStayInfo =
          TransformerTestUtils.assertHasInfo(CcwCodebookVariable.NCH_QLFYD_STAY_FROM_DT, eob);
      TransformerTestUtils.assertPeriodEquals(
          claim.getQualifiedStayFromDate(),
          claim.getQualifiedStayThroughDate(),
          (Period) nchQlyfdStayInfo.getTiming());
    }

    // test common eob information between SNF and Inpatient claims are set as
    // expected
    TransformerTestUtils.assertCommonEobInformationInpatientSNF(
        eob,
        claim.getNoncoveredStayFromDate(),
        claim.getNoncoveredStayThroughDate(),
        claim.getCoveredCareThroughDate(),
        claim.getMedicareBenefitsExhaustedDate(),
        claim.getDiagnosisRelatedGroupCd());

    TransformerTestUtils.assertDateEquals(
        claim.getClaimAdmissionDate().get(), eob.getHospitalization().getStartElement());
    TransformerTestUtils.assertDateEquals(
        claim.getBeneficiaryDischargeDate().get(), eob.getHospitalization().getEndElement());

    // test common eob information between Inpatient, HHA, Hospice and SNF claims
    // are set as
    // expected
    TransformerTestUtils.assertEobCommonGroupInpHHAHospiceSNFEquals(
        eob,
        claim.getClaimAdmissionDate(),
        claim.getBeneficiaryDischargeDate(),
        Optional.of(claim.getUtilizationDayCount()));

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
        claim.getPatientDischargeStatusCode(),
        claim.getClaimServiceClassificationTypeCode(),
        claim.getClaimPrimaryPayerCode(),
        claim.getAttendingPhysicianNpi(),
        claim.getTotalChargeAmount(),
        claim.getPrimaryPayerPaidAmount(),
        claim.getFiscalIntermediaryNumber(),
        claim.getFiDocumentClaimControlNumber(),
        claim.getFiOriginalClaimControlNumber());

    assertEquals(6, eob.getDiagnosis().size());

    CCWProcedure ccwProcedure =
        new CCWProcedure(
            claim.getProcedure1Code(), claim.getProcedure1CodeVersion(), claim.getProcedure1Date());
    TransformerTestUtils.assertHasCoding(
        ccwProcedure.getFhirSystem(),
        claim.getProcedure1Code().get(),
        eob.getProcedure().get(0).getProcedureCodeableConcept().getCoding());
    assertEquals(
        CommonTransformerUtils.convertToDate(claim.getProcedure1Date().get()),
        eob.getProcedure().get(0).getDate());

    assertEquals(1, eob.getItem().size());
    SNFClaimLine claimLine1 = claim.getLines().get(0);
    ItemComponent eobItem0 = eob.getItem().get(0);
    assertEquals(claimLine1.getLineNumber(), eobItem0.getSequence());
    assertEquals(claim.getProviderStateCode(), eobItem0.getLocationAddress().getState());

    TransformerTestUtils.assertHasCoding(
        CcwCodebookVariable.REV_CNTR, claimLine1.getRevenueCenter(), eobItem0.getRevenue());
    TransformerTestUtils.assertHcpcsCodes(
        eobItem0,
        claimLine1.getHcpcsCode(),
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        0 /* index */);

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
        claimLine1.getRevenueCenter(),
        claimLine1.getRateAmount(),
        claimLine1.getTotalChargeAmount(),
        claimLine1.getNonCoveredChargeAmount(),
        BigDecimal.valueOf(claimLine1.getUnitCount()),
        claimControlNumber,
        claimLine1.getNationalDrugCodeQuantity(),
        claimLine1.getNationalDrugCodeQualifierCode(),
        claimLine1.getRevenueCenterRenderingPhysicianNPI(),
        1 /* index */);

    // verify {@link
    // TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
    // method worked as expected for this claim type
    TransformerTestUtils.assertMapEobType(
        eob.getType(),
        ClaimType.SNF,
        Optional.of(org.hl7.fhir.dstu3.model.codesystems.ClaimType.INSTITUTIONAL),
        Optional.of(claim.getNearLineRecordIdCode()),
        Optional.of(claim.getClaimTypeCode()));

    // Test lastUpdated
    TransformerTestUtils.assertLastUpdatedEquals(claim.getLastUpdated(), eob);
  }
}
