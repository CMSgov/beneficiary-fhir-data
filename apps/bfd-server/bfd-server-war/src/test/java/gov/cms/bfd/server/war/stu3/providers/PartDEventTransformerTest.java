package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.FDADrugCodeDisplayLookup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.IdentifierType;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Date;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.codesystems.V3ActCode;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.PartDEventTransformer}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class PartDEventTransformerTest {
  /** The transformer under test. */
  PartDEventTransformer partdEventTransformer;

  /** The Metric Registry to use for the test. */
  @Mock MetricRegistry metricRegistry;

  /** The metrics timer. Used for determining the timer was started. */
  @Mock Timer metricsTimer;

  /** The metrics timer context. Used for determining the timer was stopped. */
  @Mock Timer.Context metricsTimerContext;

  Set<String> securityTags = new HashSet<>();

  /** One-time setup of objects that are normally injected. */
  @BeforeEach
  protected void setup() throws IOException {
    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);

    partdEventTransformer = new PartDEventTransformer(metricRegistry);
  }

  /**
   * Verifies that when transform is called, the metric registry is passed the correct class and
   * subtype name, is started, and stopped. Note that timer.stop() and timer.close() are equivalent
   * and one or the other may be called based on how the timer is used in code.
   */
  @Test
  public void testTransformRunsMetricTimer() {
    PartDEvent claim = getPartDEventClaim();
    partdEventTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));

    String expectedTimerName = partdEventTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /**
   * Verifies that {@link ClaimTransformerInterface#transform} works as expected when run against
   * the {@link StaticRifResource#SAMPLE_A_PDE} {@link PartDEvent}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException, IOException {
    PartDEvent claim = getPartDEventClaim();
    ExplanationOfBenefit eob =
        partdEventTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));

    TransformerUtils.enrichEob(
        eob, RDATestUtils.createTestNpiOrgLookup(), RDATestUtils.createFdaDrugCodeDisplayLookup());
    assertMatches(claim, eob);
  }

  /**
   * Verifies that {@link IdentifierType#NPI} values can be found after {@link
   * ClaimTransformerInterface#transform} is run on the sample A part D claim.
   *
   * @throws FHIRException (indicates test failure)
   * @throws IOException (indicates test failure)
   */
  @Test
  public void transformSampleARecordWithNPI() throws FHIRException, IOException {
    String serviceProviderIdQualiferCode = "01";
    String serviceProviderCode = IdentifierType.NPI.getSystem();
    checkOrgAndFacility(serviceProviderIdQualiferCode, serviceProviderCode);
  }

  /**
   * Verifies that {@link IdentifierType#UPIN} values can be found after {@link
   * ClaimTransformerInterface#transform} is run on the sample A part D claim.
   *
   * @throws FHIRException (indicates test failure)
   * @throws IOException (indicates test failure)
   */
  @Test
  public void transformSampleARecordWithUPIN() throws FHIRException, IOException {
    String serviceProviderIdQualiferCode = "06";
    String serviceProviderCode = IdentifierType.UPIN.getSystem();
    checkOrgAndFacility(serviceProviderIdQualiferCode, serviceProviderCode);
  }

  /**
   * Verifies that {@link IdentifierType#NCPDP} values can be found after {@link
   * ClaimTransformerInterface#transform} is run on the sample A part D claim.
   *
   * @throws FHIRException (indicates test failure)
   * @throws IOException (indicates test failure)
   */
  @Test
  public void transformSampleARecordWithNCPDP() throws FHIRException, IOException {
    String serviceProviderIdQualiferCode = "07";
    String serviceProviderCode = IdentifierType.NCPDP.getSystem();
    checkOrgAndFacility(serviceProviderIdQualiferCode, serviceProviderCode);
  }

  /**
   * Verifies that {@link IdentifierType#SL} values can be found after {@link
   * ClaimTransformerInterface#transform} is run on the sample A part D claim.
   *
   * @throws FHIRException (indicates test failure)
   * @throws IOException (indicates test failure)
   */
  @Test
  public void transformSampleARecordWithStateLicenseNumber() throws FHIRException, IOException {
    String serviceProviderIdQualiferCode = "08";
    String serviceProviderCode = IdentifierType.SL.getSystem();
    checkOrgAndFacility(serviceProviderIdQualiferCode, serviceProviderCode);
  }

  /**
   * Verifies that {@link IdentifierType#TAX} values can be found after {@link
   * ClaimTransformerInterface#transform} is run on the sample A part D claim.
   *
   * @throws FHIRException (indicates test failure)
   * @throws IOException (indicates test failure)
   */
  @Test
  public void transformSampleARecordWithFederalTaxNumber() throws FHIRException, IOException {
    String serviceProviderIdQualiferCode = "11";
    String serviceProviderCode = IdentifierType.TAX.getSystem();
    checkOrgAndFacility(serviceProviderIdQualiferCode, serviceProviderCode);
  }

  /**
   * Verifies that {@link ClaimTransformerInterface#transform} works as expected when run against
   * the {@link String serviceProviderIdQualiferCode} and {@link String serviceProviderCode}.
   *
   * @param serviceProviderIdQualiferCode the service provider id qualifier code
   * @param serviceProviderCode the service provider code
   * @throws IOException (indicates test failure)
   */
  private void checkOrgAndFacility(String serviceProviderIdQualiferCode, String serviceProviderCode)
      throws IOException {
    PartDEvent claim = getPartDEventClaim();
    claim.setServiceProviderIdQualiferCode(serviceProviderIdQualiferCode);
    ExplanationOfBenefit eob =
        partdEventTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));

    TransformerUtils.enrichEob(
        eob, RDATestUtils.createTestNpiOrgLookup(), RDATestUtils.createFdaDrugCodeDisplayLookup());
    TransformerTestUtils.assertReferenceEquals(
        serviceProviderCode, claim.getServiceProviderId(), eob.getOrganization());
    TransformerTestUtils.assertReferenceEquals(
        serviceProviderCode, claim.getServiceProviderId(), eob.getFacility());
  }

  /**
   * Gets the part d event claim from the sample A file.
   *
   * @return the part d event claim
   */
  private PartDEvent getPartDEventClaim() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    PartDEvent claim =
        parsedRecords.stream()
            .filter(r -> r instanceof PartDEvent)
            .map(PartDEvent.class::cast)
            .findFirst()
            .get();
    return claim;
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link PartDEvent}.
   *
   * @param claim the {@link PartDEvent} that the {@link ExplanationOfBenefit} was generated from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     PartDEvent}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(PartDEvent claim, ExplanationOfBenefit eob)
      throws FHIRException, IOException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtils.assertEobCommonClaimHeaderData(
        eob,
        claim.getEventId(),
        claim.getBeneficiaryId(),
        ClaimType.PDE,
        String.valueOf(claim.getClaimGroupId()),
        MedicareSegment.PART_D,
        Optional.of(claim.getPrescriptionFillDate()),
        Optional.of(claim.getPrescriptionFillDate()),
        Optional.empty(),
        claim.getFinalAction());

    TransformerTestUtils.assertExtensionIdentifierEquals(
        CcwCodebookVariable.PLAN_CNTRCT_REC_ID,
        claim.getPlanContractId(),
        eob.getInsurance().getCoverage());
    TransformerTestUtils.assertExtensionIdentifierEquals(
        CcwCodebookVariable.PLAN_PBP_REC_NUM,
        claim.getPlanBenefitPackageId(),
        eob.getInsurance().getCoverage());

    assertEquals("01", claim.getServiceProviderIdQualiferCode());
    assertEquals("01", claim.getPrescriberIdQualifierCode());

    ItemComponent rxItem = eob.getItem().stream().filter(i -> i.getSequence() == 1).findAny().get();
    FDADrugCodeDisplayLookup drugCodeDisplayLookup = RDATestUtils.createFdaDrugCodeDisplayLookup();
    TransformerTestUtils.assertHasCoding(
        TransformerConstants.CODING_NDC,
        null,
        drugCodeDisplayLookup
            .retrieveFDADrugCodeDisplay(Set.of(claim.getNationalDrugCode()))
            .get(claim.getNationalDrugCode()),
        claim.getNationalDrugCode(),
        rxItem.getService().getCoding());

    TransformerTestUtils.assertHasCoding(
        V3ActCode.RXDINV.getSystem(),
        V3ActCode.RXDINV.toCode(),
        rxItem.getDetail().get(0).getType().getCoding());

    assertEquals(
        Date.valueOf(claim.getPrescriptionFillDate()), rxItem.getServicedDateType().getValue());

    TransformerTestUtils.assertReferenceEquals(
        TransformerConstants.CODING_NPI_US, claim.getServiceProviderId(), eob.getOrganization());
    TransformerTestUtils.assertReferenceEquals(
        TransformerConstants.CODING_NPI_US, claim.getServiceProviderId(), eob.getFacility());

    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD, claim.getPharmacyTypeCode(), eob.getFacility());

    if (claim.getDrugCoverageStatusCode() == 'C')
      TransformerTestUtils.assertAdjudicationAmountEquals(
          CcwCodebookVariable.CVRD_D_PLAN_PD_AMT,
          claim.getPartDPlanCoveredPaidAmount(),
          rxItem.getAdjudication());
    else
      TransformerTestUtils.assertAdjudicationAmountEquals(
          CcwCodebookVariable.NCVRD_PLAN_PD_AMT,
          claim.getPartDPlanCoveredPaidAmount(),
          rxItem.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.PTNT_PAY_AMT, claim.getPatientPaidAmount(), rxItem.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.OTHR_TROOP_AMT,
        claim.getOtherTrueOutOfPocketPaidAmount(),
        rxItem.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.LICS_AMT,
        claim.getLowIncomeSubsidyPaidAmount(),
        rxItem.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.PLRO_AMT,
        claim.getPatientLiabilityReductionOtherPaidAmount(),
        rxItem.getAdjudication());
    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.TOT_RX_CST_AMT,
        claim.getTotalPrescriptionCost(),
        rxItem.getAdjudication());

    TransformerTestUtils.assertAdjudicationAmountEquals(
        CcwCodebookVariable.RPTD_GAP_DSCNT_NUM,
        claim.getGapDiscountAmount(),
        rxItem.getAdjudication());

    TransformerTestUtils.assertExtensionQuantityEquals(
        CcwCodebookVariable.FILL_NUM, claim.getFillNumber(), rxItem.getQuantity());

    TransformerTestUtils.assertExtensionQuantityEquals(
        CcwCodebookVariable.DAYS_SUPLY_NUM, claim.getDaysSupply(), rxItem.getQuantity());

    // verify {@link
    // TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
    // method worked as expected for this claim type
    TransformerTestUtils.assertMapEobType(
        eob.getType(),
        ClaimType.PDE,
        Optional.of(org.hl7.fhir.dstu3.model.codesystems.ClaimType.PHARMACY),
        Optional.empty(),
        Optional.empty());

    TransformerTestUtils.assertInfoWithCodeEquals(
        CcwCodebookVariable.DAW_PROD_SLCTN_CD,
        CcwCodebookVariable.DAW_PROD_SLCTN_CD,
        claim.getDispenseAsWrittenProductSelectionCode(),
        eob);
    if (claim.getDispensingStatusCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.DSPNSNG_STUS_CD,
          CcwCodebookVariable.DSPNSNG_STUS_CD,
          claim.getDispensingStatusCode(),
          eob);
    TransformerTestUtils.assertInfoWithCodeEquals(
        CcwCodebookVariable.DRUG_CVRG_STUS_CD,
        CcwCodebookVariable.DRUG_CVRG_STUS_CD,
        claim.getDrugCoverageStatusCode(),
        eob);
    if (claim.getAdjustmentDeletionCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.ADJSTMT_DLTN_CD,
          CcwCodebookVariable.ADJSTMT_DLTN_CD,
          claim.getAdjustmentDeletionCode(),
          eob);
    if (claim.getNonstandardFormatCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.NSTD_FRMT_CD,
          CcwCodebookVariable.NSTD_FRMT_CD,
          claim.getNonstandardFormatCode(),
          eob);
    if (claim.getPricingExceptionCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.PRCNG_EXCPTN_CD,
          CcwCodebookVariable.PRCNG_EXCPTN_CD,
          claim.getPricingExceptionCode(),
          eob);
    if (claim.getCatastrophicCoverageCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.CTSTRPHC_CVRG_CD,
          CcwCodebookVariable.CTSTRPHC_CVRG_CD,
          claim.getCatastrophicCoverageCode(),
          eob);
    if (claim.getPrescriptionOriginationCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.RX_ORGN_CD,
          CcwCodebookVariable.RX_ORGN_CD,
          claim.getPrescriptionOriginationCode(),
          eob);
    if (claim.getBrandGenericCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.BRND_GNRC_CD,
          CcwCodebookVariable.BRND_GNRC_CD,
          claim.getBrandGenericCode(),
          eob);
    TransformerTestUtils.assertInfoWithCodeEquals(
        CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD,
        CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD,
        claim.getPharmacyTypeCode(),
        eob);
    TransformerTestUtils.assertInfoWithCodeEquals(
        CcwCodebookVariable.PTNT_RSDNC_CD,
        CcwCodebookVariable.PTNT_RSDNC_CD,
        claim.getPatientResidenceCode(),
        eob);
    if (claim.getSubmissionClarificationCode().isPresent())
      TransformerTestUtils.assertInfoWithCodeEquals(
          CcwCodebookVariable.SUBMSN_CLR_CD,
          CcwCodebookVariable.SUBMSN_CLR_CD,
          claim.getSubmissionClarificationCode(),
          eob);
    TransformerTestUtils.assertLastUpdatedEquals(claim.getLastUpdated(), eob);
    try {
      TransformerTestUtils.assertFDADrugCodeDisplayEquals(
          claim.getNationalDrugCode(), RDATestUtils.FAKE_DRUG_CODE_DISPLAY);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
