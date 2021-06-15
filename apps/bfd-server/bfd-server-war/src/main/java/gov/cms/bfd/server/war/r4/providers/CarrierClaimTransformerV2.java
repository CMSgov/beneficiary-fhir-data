package gov.cms.bfd.server.war.r4.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.CarrierClaimLine;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.Diagnosis.DiagnosisLabel;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudication;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimProfessionalAndNonClinicianCareTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBPractitionerIdentifierType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Optional;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.CareTeamComponent;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;

/**
 * Transforms CCW {@link CarrierClaim} instances into FHIR {@link ExplanationOfBenefit} resources.
 */
public class CarrierClaimTransformerV2 {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the CCW {@link CarrierClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     CarrierClaim}
   */
  @Trace
  static ExplanationOfBenefit transform(
      MetricRegistry metricRegistry, Object claim, Optional<Boolean> includeTaxNumbers) {
    Timer.Context timer =
        metricRegistry
            .timer(
                MetricRegistry.name(CarrierClaimTransformerV2.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof CarrierClaim)) {
      throw new BadCodeMonkeyException();
    }

    ExplanationOfBenefit eob = transformClaim((CarrierClaim) claim, includeTaxNumbers);

    timer.stop();
    return eob;
  }

  /**
   * @param claimGroup the CCW {@link CarrierClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     CarrierClaim}
   */
  private static ExplanationOfBenefit transformClaim(
      CarrierClaim claimGroup, Optional<Boolean> includeTaxNumbers) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Required values not directly mapped
    eob.getMeta().addProfile(ProfileConstants.C4BB_EOB_NONCLINICIAN_PROFILE_URL);

    // TODO: ExplanationOfBenefit.outcome is a required field. Needs to be mapped.
    // eob.setOutcome(?)

    // Common group level fields between all claim types
    // Claim Type + Claim ID => ExplanationOfBenefit.id
    // CLM_ID => ExplanationOfBenefit.identifier
    // CLM_GRP_ID => ExplanationOfBenefit.identifier
    // BENE_ID + Coverage Type => ExplanationOfBenefit.insurance.coverage
    // (reference)
    // BENE_ID => ExplanationOfBenefit.patient (reference)
    // FINAL_ACTION => ExplanationOfBenefit.status
    // CLM_FROM_DT => ExplanationOfBenefit.billablePeriod.start
    // CLM_THRU_DT => ExplanationOfBenefit.billablePeriod.end
    // CLM_PMT_AMT => ExplanationOfBenefit.payment.amount
    TransformerUtilsV2.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimTypeV2.CARRIER,
        claimGroup.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_B,
        Optional.of(claimGroup.getDateFrom()),
        Optional.of(claimGroup.getDateThrough()),
        Optional.of(claimGroup.getPaymentAmount()),
        claimGroup.getFinalAction());

    // NCH_WKLY_PROC_DT => ExplanationOfBenefit.supportinginfo.timingDate
    TransformerUtilsV2.addInformation(
        eob,
        TransformerUtilsV2.createInformationRecievedDateSlice(
            eob,
            CcwCodebookVariable.NCH_WKLY_PROC_DT,
            Optional.of(claimGroup.getWeeklyProcessDate())));

    // map eob type codes into FHIR
    // NCH_CLM_TYPE_CD            => ExplanationOfBenefit.type.coding
    // EOB Type                   => ExplanationOfBenefit.type.coding
    // Claim Type (Professional)  => ExplanationOfBenefit.type.coding
    // NCH_NEAR_LINE_REC_IDENT_CD => ExplanationOfBenefit.extension
    TransformerUtilsV2.mapEobType(
        eob,
        ClaimTypeV2.CARRIER,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // CARR_CLM_PRMRY_PYR_PD_AMT => ExplanationOfBenefit.total.amount
    TransformerUtilsV2.addTotal(
        eob,
        TransformerUtilsV2.createTotalAdjudicationAmountSlice(
            eob,
            CcwCodebookVariable.CLM_TOT_CHRG_AMT,
            C4BBAdjudication.PRIOR_PAYER_PAID,
            claimGroup.getPrimaryPayerPaidAmount()));

    // Common group level fields between Carrier and DME
    // BENE_ID =>
    // CARR_NUM                       => ExplanationOfBenefit.extension
    // CLM_CLNCL_TRIL_NUM             => ExplanationOfBenefit.extension
    // CARR_CLM_CASH_DDCTBL_APLD_AMT  => ExplanationOfBenefit.benefitBalance.financial
    // CARR_CLM_PMT_DNL_CD            => ExplanationOfBenefit.extension
    // RFR_PHYSN_NPI                  => ExplanationOfBenefit.referral.identifier
    //                                => ExplanationOfBenefit.careteam.provider
    // RFR_PHYSN_UPIN                 => ExplanationOfBenefit.referral.identifier
    //                                => ExplanationOfBenefit.careteam.provider
    // CARR_CLM_PRVDR_ASGNMT_IND_SW   => ExplanationOfBenefit.extension
    // NCH_CLM_PRVDR_PMT_AMT          => ExplanationOfBenefit.benefitBalance.financial
    // NCH_CLM_BENE_PMT_AMT           => ExplanationOfBenefit.benefitBalance.financial
    // NCH_CARR_CLM_SBMTD_CHRG_AMT    => ExplanationOfBenefit.benefitBalance.financial
    // NCH_CARR_CLM_ALOWD_AMT         => ExplanationOfBenefit.benefitBalance.financial
    // CLM_DISP_CD                    => ExplanationOfBenefit.disposition
    // CARR_CLM_CNTL_NUM              => ExplanationOfBenefit.extension
    TransformerUtilsV2.mapEobCommonGroupCarrierDME(
        eob,
        claimGroup.getBeneficiaryId(),
        claimGroup.getCarrierNumber(),
        claimGroup.getClinicalTrialNumber(),
        claimGroup.getBeneficiaryPartBDeductAmount(),
        claimGroup.getPaymentDenialCode(),
        claimGroup.getReferringPhysicianNpi(),
        claimGroup.getReferringPhysicianUpin(),
        claimGroup.getProviderAssignmentIndicator(),
        claimGroup.getProviderPaymentAmount(),
        claimGroup.getBeneficiaryPaymentAmount(),
        claimGroup.getSubmittedChargeAmount(),
        claimGroup.getAllowedChargeAmount(),
        claimGroup.getClaimDispositionCode(),
        claimGroup.getClaimCarrierControlNumber());

    // PRNCPAL_DGNS_CD => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_CD(1-12) => diagnosis.diagnosisCodeableConcept
    // ICD_DGNS_VRSN_CD(1-12) => diagnosis.diagnosisCodeableConcept
    for (Diagnosis diagnosis : DiagnosisUtilV2.extractDiagnoses(claimGroup)) {
      DiagnosisUtilV2.addDiagnosisCode(eob, diagnosis, ClaimTypeV2.CARRIER);
    }

    // CARR_CLM_RFRNG_PIN_NUM => ExplanationOfBenefit.careteam.provider
    TransformerUtilsV2.addCareTeamMember(
        eob,
        C4BBPractitionerIdentifierType.NPI,
        C4BBClaimProfessionalAndNonClinicianCareTeamRole.REFERRING,
        Optional.ofNullable(claimGroup.getReferringProviderIdNumber()));

    // CARR_CLM_ENTRY_CD => ExplanationOfBenefit.extension
    eob.addExtension(
        TransformerUtilsV2.createExtensionCoding(
            eob, CcwCodebookVariable.CARR_CLM_ENTRY_CD, claimGroup.getClaimEntryCode()));

    // Process line items
    for (CarrierClaimLine line : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();
      // LINE_NUM => ExplanationOfBenefit.item.sequence
      item.setSequence(line.getLineNumber().intValue());

      // PRF_PHYSN_NPI => ExplanationOfBenefit.careTeam.provider
      Optional<CareTeamComponent> performing =
          TransformerUtilsV2.addCareTeamMember(
              eob,
              item,
              C4BBPractitionerIdentifierType.NPI,
              C4BBClaimProfessionalAndNonClinicianCareTeamRole.PERFORMING,
              line.getPerformingPhysicianNpi());

      // Fall back to UPIN if NPI not present
      if (!line.getPerformingPhysicianNpi().isPresent()) {
        performing =
            TransformerUtilsV2.addCareTeamMember(
                eob,
                item,
                C4BBPractitionerIdentifierType.UPIN,
                C4BBClaimProfessionalAndNonClinicianCareTeamRole.PERFORMING,
                line.getPerformingPhysicianUpin());
      }

      // Update the responsible flag
      performing.ifPresent(
          p -> {
            p.setResponsible(true);

            // PRVDR_SPCLTY => ExplanationOfBenefit.careTeam.qualification
            p.setQualification(
                TransformerUtilsV2.createCodeableConcept(
                    eob, CcwCodebookVariable.PRVDR_SPCLTY, line.getProviderSpecialityCode()));

            // CARR_LINE_PRVDR_TYPE_CD => ExplanationOfBenefit.careTeam.extension
            p.addExtension(
                TransformerUtilsV2.createExtensionCoding(
                    eob, CcwCodebookVariable.CARR_LINE_PRVDR_TYPE_CD, line.getProviderTypeCode()));

            // PRTCPTNG_IND_CD => ExplanationOfBenefit.careTeam.extension
            p.addExtension(
                TransformerUtilsV2.createExtensionCoding(
                    eob,
                    CcwCodebookVariable.PRTCPTNG_IND_CD,
                    line.getProviderParticipatingIndCode()));
          });

      // ORG_NPI_NUM => ExplanationOfBenefit.careTeam.provider
      TransformerUtilsV2.addCareTeamMember(
          eob,
          item,
          C4BBPractitionerIdentifierType.NPI,
          C4BBClaimProfessionalAndNonClinicianCareTeamRole.PRIMARY,
          line.getOrganizationNpi());

      // CARR_LINE_RDCD_PMT_PHYS_ASTN_C => ExplanationOfBenefit.item.adjudication
      TransformerUtilsV2.addAdjudication(
          item,
          TransformerUtilsV2.createAdjudicationDenialReasonSlice(
              eob,
              CcwCodebookVariable.CARR_LINE_RDCD_PMT_PHYS_ASTN_C,
              String.valueOf(line.getReducedPaymentPhysicianAsstCode())));

      // HCPCS_CD               => ExplanationOfBenefit.item.productOrService
      // HCPCS_1ST_MDFR_CD      => ExplanationOfBenefit.item.modifier
      // HCPCS_2ND_MDFR_CD      => ExplanationOfBenefit.item.modifier
      // CARR_CLM_HCPCS_YR_CD   => ExplanationOfBenefit.item.modifier.version
      TransformerUtilsV2.mapHcpcs(
          eob,
          item,
          line.getHcpcsCode(),
          claimGroup.getHcpcsYearCode(),
          Arrays.asList(line.getHcpcsInitialModifierCode(), line.getHcpcsSecondModifierCode()));

      // tax num should be as a extension
      if (includeTaxNumbers.orElse(false)) {
        item.addExtension(
            TransformerUtilsV2.createExtensionCoding(
                eob, CcwCodebookVariable.TAX_NUM, line.getProviderTaxNumber()));
      }

      // CARR_LINE_ANSTHSA_UNIT_CNT => ExplanationOfBenefit.item.extension
      if (line.getAnesthesiaUnitCount().compareTo(BigDecimal.ZERO) > 0) {
        item.addExtension(
            TransformerUtilsV2.createExtensionQuantity(
                CcwCodebookVariable.CARR_LINE_ANSTHSA_UNIT_CNT, line.getAnesthesiaUnitCount()));
      }

      // CARR_LINE_MTUS_CNT => ExplanationOfBenefit.item.extension
      if (!line.getMtusCount().equals(BigDecimal.ZERO)) {
        item.addExtension(
            TransformerUtilsV2.createExtensionQuantity(
                CcwCodebookVariable.CARR_LINE_MTUS_CNT, line.getMtusCount()));
      }

      // CARR_LINE_MTUS_CD => ExplanationOfBenefit.item.extension
      line.getMtusCode()
          .ifPresent(
              code ->
                  item.addExtension(
                      TransformerUtilsV2.createExtensionCoding(
                          eob, CcwCodebookVariable.CARR_LINE_MTUS_CNT, code)));

      // Common item level fields between Carrier and DME
      // LINE_SRVC_CNT            => ExplanationOfBenefit.item.quantity
      // LINE_CMS_TYPE_SRVC_CD    => ExplanationOfBenefit.item.category
      // LINE_PLACE_OF_SRVC_CD    => ExplanationOfBenefit.item.location
      // BETOS_CD                 => ExplanationOfBenefit.item.extension
      // LINE_1ST_EXPNS_DT        => ExplanationOfBenefit.item.servicedPeriod
      // LINE_LAST_EXPNS_DT       => ExplanationOfBenefit.item.servicedPeriod
      // LINE_NCH_PMT_AMT         => ExplanationOfBenefit.item.adjudication
      // LINE_PMT_80_100_CD       => ExplanationOfBenefit.item.adjudication.extension
      // PAID_TO_PATIENT          => ExplanationOfBenefit.item.adjudication
      // LINE_PRVDR_PMT_AMT       => ExplanationOfBenefit.item.adjudication
      // LINE_BENE_PTB_DDCTBL_AMT => ExplanationOfBenefit.item.adjudication
      // LINE_BENE_PRMRY_PYR_CD   => ExplanationOfBenefit.item.extension
      // LINE_BENE_PRMRY_PYR_PD_AMT => ExplanationOfBenefit.item.adjudication
      // LINE_COINSRNC_AMT        => ExplanationOfBenefit.item.adjudication
      // LINE_SBMTD_CHRG_AMT      => ExplanationOfBenefit.item.adjudication
      // LINE_ALOWD_CHRG_AMT      => ExplanationOfBenefit.item.adjudication
      // LINE_BENE_PRMRY_PYR_CD   => ExplanationOfBenefit.item.extension
      // LINE_SERVICE_DEDUCTIBLE  => ExplanationOfBenefit.item.extension
      // LINE_HCT_HGB_TYPE_CD     => Observation.code
      // LINE_HCT_HGB_RSLT_NUM    => Observation.value
      // LINE_NDC_CD              => ExplanationOfBenefit.item.productOrService
      TransformerUtilsV2.mapEobCommonItemCarrierDME(
          item,
          eob,
          includeTaxNumbers,
          claimGroup.getClaimId(),
          item.getSequence(),
          line.getServiceCount(),
          line.getPlaceOfServiceCode(),
          line.getFirstExpenseDate(),
          line.getLastExpenseDate(),
          line.getBeneficiaryPaymentAmount(),
          line.getProviderPaymentAmount(),
          line.getBeneficiaryPartBDeductAmount(),
          line.getPrimaryPayerCode(),
          line.getPrimaryPayerPaidAmount(),
          line.getBetosCode(),
          line.getPaymentAmount(),
          line.getPaymentCode(),
          line.getCoinsuranceAmount(),
          line.getSubmittedChargeAmount(),
          line.getAllowedChargeAmount(),
          line.getProcessingIndicatorCode(),
          line.getServiceDeductibleCode(),
          line.getHctHgbTestTypeCode(),
          line.getHctHgbTestResult(),
          line.getCmsServiceTypeCode(),
          line.getNationalDrugCode(),
          line.getProviderTaxNumber());

      // LINE_ICD_DGNS_CD      => ExplanationOfBenefit.item.diagnosisSequence
      // LINE_ICD_DGNS_VRSN_CD => ExplanationOfBenefit.item.diagnosisSequence
      DiagnosisUtilV2.addDiagnosisLink(
          eob,
          item,
          Diagnosis.from(
              line.getDiagnosisCode(), line.getDiagnosisCodeVersion(), DiagnosisLabel.OTHER),
          ClaimTypeV2.CARRIER);

      // PRVDR_STATE_CD => ExplanationOfBenefit.item.location.extension
      line.getProviderStateCode()
          .ifPresent(
              code ->
                  item.getLocation()
                      .addExtension(
                          TransformerUtilsV2.createExtensionCoding(
                              eob, CcwCodebookVariable.PRVDR_STATE_CD, code)));

      // PRVDR_ZIP => ExplanationOfBenefit.item.location.extension
      line.getProviderZipCode()
          .ifPresent(
              code ->
                  item.getLocation()
                      .addExtension(
                          TransformerUtilsV2.createExtensionCoding(
                              eob, CcwCodebookVariable.PRVDR_ZIP, code)));

      // CARR_LINE_PRCNG_LCLTY_CD => ExplanationOfBenefit.item.location.extension
      item.getLocation()
          .addExtension(
              TransformerUtilsV2.createExtensionCoding(
                  eob,
                  CcwCodebookVariable.CARR_LINE_PRCNG_LCLTY_CD,
                  line.getLinePricingLocalityCode()));

      // CARR_LINE_CLIA_LAB_NUM => ExplanationOfBenefit.item.location.extension
      line.getCliaLabNumber()
          .ifPresent(
              num ->
                  item.getLocation()
                      .addExtension(
                          TransformerUtilsV2.createExtensionIdentifier(
                              CcwCodebookVariable.CARR_LINE_CLIA_LAB_NUM, num)));
    }

    TransformerUtilsV2.setLastUpdated(eob, claimGroup.getLastUpdated());
    return eob;
  }
}
