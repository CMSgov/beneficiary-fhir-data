package gov.cms.bfd.server.war.stu3.providers;

import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.CarrierClaimLine;
import gov.cms.bfd.model.rif.samples.StaticRifResource;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.hl7.fhir.exceptions.FHIRException;
import org.junit.Assert;
import org.junit.Test;

/** Unit tests for {@link gov.cms.bfd.server.war.stu3.providers.CarrierClaimTransformer}. */
public final class CarrierClaimTransformerTest {
  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CarrierClaimTransformer#transform(Object)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_A_CARRIER} {@link CarrierClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleARecord() throws FHIRException {
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
        CarrierClaimTransformer.transform(new MetricRegistry(), claim, Optional.of(true));
    assertMatches(claim, eobWithLastUpdated, Optional.of(true));

    claim.setLastUpdated(null);
    ExplanationOfBenefit eobWithoutLastUpdated =
        CarrierClaimTransformer.transform(new MetricRegistry(), claim, Optional.of(true));
    assertMatches(claim, eobWithoutLastUpdated, Optional.of(true));
  }

  /**
   * Verifies that {@link
   * gov.cms.bfd.server.war.stu3.providers.CarrierClaimTransformer#transform(Object)} works as
   * expected when run against the {@link StaticRifResource#SAMPLE_U_CARRIER} {@link CarrierClaim}.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  public void transformSampleURecord() throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_U.getResources()));
    CarrierClaim claim =
        parsedRecords.stream()
            .filter(r -> r instanceof CarrierClaim)
            .map(r -> (CarrierClaim) r)
            .findFirst()
            .get();

    ExplanationOfBenefit eob =
        CarrierClaimTransformer.transform(new MetricRegistry(), claim, Optional.of(true));
    assertMatches(claim, eob, Optional.of(true));
  }

  /**
   * Verifies that the {@link ExplanationOfBenefit} "looks like" it should, if it were produced from
   * the specified {@link CarrierClaim}.
   *
   * @param claim the {@link CarrierClaim} that the {@link ExplanationOfBenefit} was generated from
   * @param eob the {@link ExplanationOfBenefit} that was generated from the specified {@link
   *     CarrierClaim}
   * @param includedTaxNumbers whether or not to include tax numbers are expected to be included in
   *     the result (see {@link
   *     ExplanationOfBenefitResourceProvider#HEADER_NAME_INCLUDE_TAX_NUMBERS}, defaults to <code>
   *     false</code>)
   * @throws FHIRException (indicates test failure)
   */
  static void assertMatches(
      CarrierClaim claim, ExplanationOfBenefit eob, Optional<Boolean> includedTaxNumbers)
      throws FHIRException {
    // Test to ensure group level fields between all claim types match
    TransformerTestUtils.assertEobCommonClaimHeaderData(
        eob,
        claim.getClaimId(),
        claim.getBeneficiaryId(),
        ClaimType.CARRIER,
        claim.getClaimGroupId().toPlainString(),
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

    Assert.assertEquals(5, eob.getDiagnosis().size());
    Assert.assertEquals(1, eob.getItem().size());

    TransformerTestUtils.assertAdjudicationTotalAmountEquals(
        CcwCodebookVariable.PRPAYAMT, claim.getPrimaryPayerPaidAmount(), eob);

    CarrierClaimLine claimLine1 = claim.getLines().get(0);
    ItemComponent eobItem0 = eob.getItem().get(0);
    Assert.assertEquals(claimLine1.getLineNumber(), new BigDecimal(eobItem0.getSequence()));

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
        "" + claimLine1.getOrganizationNpi().get());

    CareTeamComponent taxNumberCareTeamEntry =
        TransformerTestUtils.findCareTeamEntryForProviderTaxNumber(
            claimLine1.getProviderTaxNumber(), eob.getCareTeam());
    if (includedTaxNumbers.orElse(false)) {
      Assert.assertNotNull(taxNumberCareTeamEntry);
    } else {
      Assert.assertNull(taxNumberCareTeamEntry);
    }

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
    Assert.assertEquals(1, eobItem0.getModifier().size());
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
