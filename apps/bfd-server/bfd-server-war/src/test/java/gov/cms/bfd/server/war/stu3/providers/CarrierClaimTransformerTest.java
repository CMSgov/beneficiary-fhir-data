package gov.cms.bfd.server.war.stu3.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaimLine;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.NPIOrgLookup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.CCWUtils;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.server.war.utils.RDATestUtils;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
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

/** Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.CarrierClaimTransformer}. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public final class CarrierClaimTransformerTest {
  /** The transformer under test. */
  CarrierClaimTransformer carrierClaimTransformer;

  /** The Metric Registry to use for the test. */
  @Mock MetricRegistry metricRegistry;

  /** The NPI org lookup to use for the test. */
  @Mock NPIOrgLookup npiOrgLookup;

  /** The securityTagManager. */
  @Mock SecurityTagManager securityTagManager;

  /** The mock metric timer. */
  @Mock Timer metricsTimer;

  /** The mock metric timer context (used to stop the metric). */
  @Mock Timer.Context metricsTimerContext;

  Set<String> securityTags = new HashSet<>();

  /** One-time setup of objects that are normally injected. */
  @BeforeEach
  protected void setup() {
    NPIData npiData =
        NPIData.builder()
            .npi("0000000000")
            .providerOrganizationName(RDATestUtils.FAKE_NPI_ORG_NAME)
            .taxonomyCode("207X00000X")
            .taxonomyDisplay("Orthopaedic Surgery")
            .build();

    when(metricRegistry.timer(any())).thenReturn(metricsTimer);
    when(metricsTimer.time()).thenReturn(metricsTimerContext);

    carrierClaimTransformer =
        new CarrierClaimTransformer(metricRegistry, securityTagManager, false);
  }

  /**
   * Verifies that when transform is called, the metric registry is passed the correct class and
   * subtype name, is started, and stopped. Note that timer.stop() and timer.close() are equivalent
   * and one or the other may be called based on how the timer is used in code.
   */
  @Test
  public void testTransformRunsMetricTimer() {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(
            Arrays.asList(StaticRifResourceGroup.SAMPLE_A_MULTIPLE_CARRIER_LINES.getResources()));
    CarrierClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .orElseThrow();

    claim.setLastUpdated(Instant.now());

    carrierClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));

    String expectedTimerName = carrierClaimTransformer.getClass().getSimpleName() + ".transform";
    verify(metricRegistry, times(1)).timer(expectedTimerName);
    // time() starts the timer
    verify(metricsTimer, times(1)).time();
    verify(metricsTimerContext, times(1)).close();
  }

  /**
   * Verifies that {@link ClaimTransformerInterface#transform} works as expected when run against
   * the {@link StaticRifResource#SAMPLE_A_CARRIER} {@link CarrierClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException, IOException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));
    CarrierClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();

    claim.setLastUpdated(Instant.now());
    ExplanationOfBenefit eobWithLastUpdated =
        carrierClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));

    assertMatches(claim, eobWithLastUpdated);

    claim.setLastUpdated(Optional.empty());
    ExplanationOfBenefit eobWithoutLastUpdated =
        carrierClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));

    assertMatches(claim, eobWithoutLastUpdated);
  }

  /**
   * Verifies that {@link ClaimTransformerInterface#transform} works as expected when run against
   * the {@link StaticRifResource#SAMPLE_A_CARRIER} {@link CarrierClaim}. has a single care member
   * under the care team component and doesn't duplicate its results
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void shouldHaveOneCareTeamMember() throws FHIRException, IOException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(
            Arrays.asList(StaticRifResourceGroup.SAMPLE_A_MULTIPLE_CARRIER_LINES.getResources()));
    CarrierClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();

    claim.setLastUpdated(Instant.now());

    ExplanationOfBenefit eob =
        carrierClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));

    assertEquals(1, eob.getCareTeam().size());
  }

  /**
   * Tests that the transformer sets the expected values for the care team member extensions and
   * does not error when only the required care team values exist.
   */
  @Test
  public void testCareTeamExtensionsWhenOptionalValuesAbsent() {

    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    CarrierClaim loadedClaim =
        parsedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(CarrierClaim.class::cast)
            .findFirst()
            .get();
    loadedClaim.setLastUpdated(Instant.now());

    // Set the optional care team fields to empty
    for (CarrierClaimLine line : loadedClaim.getLines()) {
      line.setProviderParticipatingIndCode(Optional.empty());
      line.setProviderSpecialityCode(Optional.empty());
      line.setOrganizationNpi(Optional.empty());
    }

    ExplanationOfBenefit genEob =
        carrierClaimTransformer.transform(new ClaimWithSecurityTags<>(loadedClaim, securityTags));
    TransformerUtils.enrichEob(
        genEob,
        RDATestUtils.createTestNpiOrgLookup(),
        RDATestUtils.createFdaDrugCodeDisplayLookup());

    // Ensure the extension for PRTCPTNG_IND_CD wasnt added
    // Also the qualification coding should be empty if specialty code is not set
    // organization npi should also not be added if its optional is empty
    String prtIndCdUrl =
        CCWUtils.calculateVariableReferenceUrl(CcwCodebookVariable.PRTCPTNG_IND_CD);
    for (ExplanationOfBenefit.CareTeamComponent careTeam : genEob.getCareTeam()) {
      assertFalse(careTeam.getExtension().stream().anyMatch(i -> i.getUrl().equals(prtIndCdUrl)));
      assertFalse(
          careTeam.getExtension().stream()
              .anyMatch(i -> i.getUrl().equals(TransformerConstants.CODING_NPI_US)));
      assertTrue(careTeam.getQualification().getCoding().isEmpty());
    }
  }

  /**
   * Verifies that {@link ClaimTransformerInterface#transform} works as expected when run against
   * the {@link StaticRifResource#SAMPLE_U_CARRIER} {@link CarrierClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleURecord() throws FHIRException, IOException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));
    CarrierClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(CarrierClaim.class::cast)
            .findFirst()
            .get();

    ExplanationOfBenefit eob =
        carrierClaimTransformer.transform(new ClaimWithSecurityTags<>(claim, securityTags));
    assertMatches(claim, eob);
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link CarrierClaim}.
   *
   * @param claim the {@link CarrierClaim} that the {@link ExplanationOfBenefit} was generated from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     CarrierClaim}
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(CarrierClaim claim, ExplanationOfBenefit eob) throws FHIRException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtils.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimType.CARRIER,
        String.valueOf(claim.getClaimGroupId()),
        MedicareSegment.PART_B,
        Optional.of(claim.getDateFrom()),
        Optional.of(claim.getDateThrough()),
        Optional.of(claim.getPaymentAmount()),
        claim.getFinalAction());

    // Test to ensure common group fields between Carrier and DME match
    TransformerTestUtils.assertEobCommonGroupCarrierDMEEquals(
        eob,
        claim.getBeneficiaryId(),
        claim.getCarrierNumber(),
        claim.getClinicalTrialNumber(),
        claim.getBeneficiaryPartBDeductAmount(),
        claim.getPaymentDenialCode(),
        claim.getReferringPhysicianNpi(),
        claim.getProviderAssignmentIndicator(),
        claim.getProviderPaymentAmount(),
        claim.getBeneficiaryPaymentAmount(),
        claim.getSubmittedChargeAmount(),
        claim.getAllowedChargeAmount());

    assertEquals(5, eob.getDiagnosis().size());
    assertEquals(1, eob.getItem().size());

    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.PRPAYAMT, claim.getPrimaryPayerPaidAmount(), eob);

    CarrierClaimLine claimLine1 = claim.getLines().get(0);
    ItemComponent eobItem0 = eob.getItem().get(0);
    assertEquals(claimLine1.getLineNumber(), eobItem0.getSequence());

    TransformerTestUtils.assertCareTeamEquals(
        claimLine1.getPerformingPhysicianNpi().get(), ClaimCareteamrole.PRIMARY, eob);
    CareTeamComponent performingCareTeamEntry =
        TransformerTestUtils.findCareTeamEntryForProviderNpi(
            claimLine1.getPerformingPhysicianNpi().get(), eob.getCareTeam());
    TransformerTestUtils.assertHasCoding(
        CcwCodebookVariable.PRVDR_SPCLTY,
        claimLine1.getProviderSpecialityCode(),
        performingCareTeamEntry.getQualification());
    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.CARR_LINE_PRVDR_TYPE_CD,
        claimLine1.getProviderTypeCode(),
        performingCareTeamEntry);
    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.PRTCPTNG_IND_CD,
        claimLine1.getProviderParticipatingIndCode(),
        performingCareTeamEntry);
    TransformerTestUtils.assertExtensionCodingEquals(
        performingCareTeamEntry,
        TransformerConstants.CODING_NPI_US,
        TransformerConstants.CODING_NPI_US,
        claimLine1.getOrganizationNpi().get());

    CareTeamComponent taxNumberCareTeamEntry =
        TransformerTestUtils.findCareTeamEntryForProviderTaxNumber(
            claimLine1.getProviderTaxNumber(), eob.getCareTeam());
    // We assert that tax number entries are always null as of 01/26/BFD-4489 to ensure tax numbers
    // are never
    // included
    assertNull(taxNumberCareTeamEntry);

    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.PRVDR_STATE_CD,
        claimLine1.getProviderStateCode(),
        eobItem0.getLocation());

    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.PRVDR_STATE_CD,
        claimLine1.getProviderStateCode(),
        eobItem0.getLocation());
    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.CARR_LINE_PRCNG_LCLTY_CD,
        claimLine1.getLinePricingLocalityCode(),
        eobItem0.getLocation());

    TransformerTestUtils.assertHasCoding(
        TransformerConstants.CODING_SYSTEM_HCPCS,
        "" + claim.getHcpcsYearCode().get(),
        null,
        claimLine1.getHcpcsCode().get(),
        eobItem0.getService().getCoding());
    assertEquals(1, eobItem0.getModifier().size());
    TransformerTestUtils.assertHcpcsCodes(
        eobItem0,
        claimLine1.getHcpcsCode(),
        claimLine1.getHcpcsInitialModifierCode(),
        claimLine1.getHcpcsSecondModifierCode(),
        claim.getHcpcsYearCode(),
        0 /* index */);

    if (claimLine1.getAnesthesiaUnitCount().compareTo(BigDecimal.ZERO) > 0) {
      TransformerTestUtils.assertExtensionQuantityEquals(
          CcwCodebookVariable.CARR_LINE_ANSTHSA_UNIT_CNT,
          claimLine1.getAnesthesiaUnitCount(),
          eobItem0.getService());
    }

    TransformerTestUtils.assertExtensionCodingEquals(
        CcwCodebookVariable.CARR_LINE_MTUS_CD, claimLine1.getMtusCode(), eobItem0);

    TransformerTestUtils.assertExtensionQuantityEquals(
        CcwCodebookVariable.CARR_LINE_MTUS_CNT, claimLine1.getMtusCount(), eobItem0);

    TransformerTestUtils.assertAdjudicationReasonEquals(
        CcwCodebookVariable.CARR_LINE_RDCD_PMT_PHYS_ASTN_C,
        claimLine1.getReducedPaymentPhysicianAsstCode(),
        eobItem0.getAdjudication());

    TransformerTestUtils.assertExtensionIdentifierEquals(
        CcwCodebookVariable.CARR_LINE_CLIA_LAB_NUM,
        claimLine1.getCliaLabNumber(),
        eobItem0.getLocation());

    // verify {@link
    // TransformerUtils#mapEobType(CodeableConcept,ClaimType,Optional,Optional)}
    // method worked as expected for this claim type
    TransformerTestUtils.assertMapEobType(
        eob.getType(),
        ClaimType.CARRIER,
        Optional.of(org.hl7.fhir.dstu3.model.codesystems.ClaimType.PROFESSIONAL),
        Optional.of(claim.getNearLineRecordIdCode()),
        Optional.of(claim.getClaimTypeCode()));

    // Test to ensure common item fields between Carrier and DME match
    TransformerTestUtils.assertEobCommonItemCarrierDMEEquals(
        eobItem0,
        eob,
        claimLine1.getServiceCount(),
        claimLine1.getPlaceOfServiceCode(),
        claimLine1.getFirstExpenseDate(),
        claimLine1.getLastExpenseDate(),
        claimLine1.getBeneficiaryPaymentAmount(),
        claimLine1.getProviderPaymentAmount(),
        claimLine1.getBeneficiaryPartBDeductAmount(),
        claimLine1.getPrimaryPayerCode(),
        claimLine1.getPrimaryPayerPaidAmount(),
        claimLine1.getBetosCode(),
        claimLine1.getPaymentAmount(),
        claimLine1.getPaymentCode(),
        claimLine1.getCoinsuranceAmount(),
        claimLine1.getSubmittedChargeAmount(),
        claimLine1.getAllowedChargeAmount(),
        claimLine1.getProcessingIndicatorCode(),
        claimLine1.getServiceDeductibleCode(),
        claimLine1.getDiagnosisCode(),
        claimLine1.getDiagnosisCodeVersion(),
        claimLine1.getHctHgbTestTypeCode(),
        claimLine1.getHctHgbTestResult(),
        claimLine1.getCmsServiceTypeCode(),
        claimLine1.getNationalDrugCode());

    // Test lastUpdated
    TransformerTestUtils.assertLastUpdatedEquals(claim.getLastUpdated(), eob);
  }
}
