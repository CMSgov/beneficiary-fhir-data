package gov.cms.bfd.server.war.r4.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.parse.InvalidRifValueException;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.ProfileConstants;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.commons.carin.C4BBAdjudication;
import gov.cms.bfd.server.war.commons.carin.C4BBClaimPharmacyTeamRole;
import gov.cms.bfd.server.war.commons.carin.C4BBOrganizationIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBPractitionerIdentifierType;
import gov.cms.bfd.server.war.commons.carin.C4BBSupportingInfoType;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.SimpleQuantity;
import org.hl7.fhir.r4.model.codesystems.V3ActCode;

/** Transforms CCW {@link PartDEvent} instances into FHIR {@link ExplanationOfBenefit} resources. */
final class PartDEventTransformerV2 {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the CCW {@link PartDEvent} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     PartDEvent}
   */
  @Trace
  static ExplanationOfBenefit transform(
      MetricRegistry metricRegistry, Object claim, Optional<Boolean> includeTaxNumbers) {
    Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(PartDEventTransformerV2.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof PartDEvent)) {
      throw new BadCodeMonkeyException();
    }

    ExplanationOfBenefit eob = transformClaim((PartDEvent) claim);

    timer.stop();
    return eob;
  }

  /**
   * @param claimGroup the CCW {@link PartDEvent} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     PartDEvent}
   */
  private static ExplanationOfBenefit transformClaim(PartDEvent claimGroup) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    eob.getMeta().addProfile(ProfileConstants.C4BB_EOB_PHARMACY_PROFILE_URL);

    // Common group level fields between all claim types
    // Claim Type + Claim ID
    //                  => ExplanationOfBenefit.id
    // PDE_ID           => ExplanationOfBenefit.identifier
    // CLM_GRP_ID       => ExplanationOfBenefit.identifier
    // BENE_ID + Coverage Type
    //                  => ExplanationOfBenefit.insurance.coverage (reference)
    // BENE_ID          => ExplanationOfBenefit.patient (reference)
    // FINAL_ACTION     => ExplanationOfBenefit.status
    // SRVC_DT          => ExplanationOfBenefit.billablePeriod.start
    // SRVC_DT          => ExplanationOfBenefit.billablePeriod.end

    TransformerUtilsV2.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getEventId(),
        claimGroup.getBeneficiaryId(),
        ClaimTypeV2.PDE,
        claimGroup.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_D,
        Optional.of(claimGroup.getPrescriptionFillDate()),
        Optional.of(claimGroup.getPrescriptionFillDate()),
        Optional.empty(),
        claimGroup.getFinalAction());

    // RX_SRVC_RFRNC_NUM => ExplanationOfBenefit.identifier
    eob.addIdentifier(
        TransformerUtilsV2.createClaimIdentifier(
            CcwCodebookVariable.RX_SRVC_RFRNC_NUM,
            claimGroup.getPrescriptionReferenceNumber().toPlainString()));

    // map eob type codes into FHIR
    // EOB Type               => ExplanationOfBenefit.type.coding
    // Claim Type  (pharmacy) => ExplanationOfBenefit.type.coding
    TransformerUtilsV2.mapEobType(eob, ClaimTypeV2.PDE, Optional.empty(), Optional.empty());

    // Coverage object is not optional, and we want to add extensions to it. This is safe.

    // PLAN_CNTRCT_REC_ID => ExplanationOfBenefit.insurance.coverage.extension
    eob.getInsuranceFirstRep()
        .getCoverage()
        .addExtension(
            TransformerUtilsV2.createExtensionIdentifier(
                CcwCodebookVariable.PLAN_CNTRCT_REC_ID, claimGroup.getPlanContractId()));

    // PLAN_PBP_REC_NUM => ExplanationOfBenefit.insurance.coverage.extension
    eob.getInsuranceFirstRep()
        .getCoverage()
        .addExtension(
            TransformerUtilsV2.createExtensionIdentifier(
                CcwCodebookVariable.PLAN_PBP_REC_NUM, claimGroup.getPlanBenefitPackageId()));

    // PD_DT => ExplanationOfBenefit.payment.date
    if (claimGroup.getPaymentDate().isPresent()) {
      eob.getPayment().setDate(TransformerUtilsV2.convertToDate(claimGroup.getPaymentDate().get()));
    }

    ItemComponent rxItem = eob.addItem();
    // 1 => ExplanationOfBenefit.item.sequence
    rxItem.setSequence(1);

    // CMPND_CD => ExplanationOfBenefit.item.programCode
    switch (claimGroup.getCompoundCode()) {
        // COMPOUNDED
      case 2:
        /* Pharmacy dispense invoice for a compound */
        TransformerUtilsV2.addInformationSlice(eob, C4BBSupportingInfoType.COMPOUND_CODE)
            .setCode(
                new CodeableConcept()
                    .addCoding(
                        new Coding()
                            .setSystem(V3ActCode.RXCINV.getSystem())
                            .setCode(V3ActCode.RXCINV.toCode())
                            .setDisplay(V3ActCode.RXCINV.getDisplay())));

        break;

        // NOT_COMPOUNDED
      case 1:
        /*
         * Pharmacy dispense invoice not involving a compound
         */
        TransformerUtilsV2.addInformationSlice(eob, C4BBSupportingInfoType.COMPOUND_CODE)
            .setCode(
                new CodeableConcept()
                    .addCoding(
                        new Coding()
                            .setSystem(V3ActCode.RXDINV.getSystem())
                            .setCode(V3ActCode.RXDINV.toCode())
                            .setDisplay(V3ActCode.RXDINV.getDisplay())));

        break;

        // NOT_SPECIFIED
      case 0:
        /*
         * Pharmacy dispense invoice not specified - do not set a value
         */
        break;

      default:
        /*
         * Unexpected value encountered - compound code should be either
         * compounded or not compounded.
         */
        throw new InvalidRifValueException(
            "Unexpected value encountered - compound code should be either compounded or not compounded: "
                + claimGroup.getCompoundCode());
    }

    // SRVC_DT => ExplanationOfBenefit.item.servicedDate
    rxItem.setServiced(
        new DateType()
            .setValue(TransformerUtilsV2.convertToDate(claimGroup.getPrescriptionFillDate())));

    /*
     * Create an adjudication for either CVRD_D_PLAN_PD_AMT or NCVRD_PLAN_PD_AMT,
     * depending on the value of DRUG_CVRG_STUS_CD. Stick DRUG_CVRG_STUS_CD into the
     * adjudication.reason field. CARING Slicing and CARING Adjudication Value Sets.
     */
    if (claimGroup.getDrugCoverageStatusCode() == 'C') {
      // CVRD_D_PLAN_PD_AMT => ExplanationOfBenefit.item.adjudication.amount
      TransformerUtilsV2.addAdjudication(
          rxItem,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.CVRD_D_PLAN_PD_AMT,
              C4BBAdjudication.BENEFIT,
              claimGroup.getPartDPlanCoveredPaidAmount()));
    } else {
      // NCVRD_PLAN_PD_AMT => ExplanationOfBenefit.item.adjudication.amount
      TransformerUtilsV2.addAdjudication(
          rxItem,
          TransformerUtilsV2.createAdjudicationAmtSlice(
              CcwCodebookVariable.CVRD_D_PLAN_PD_AMT,
              C4BBAdjudication.NONCOVERED,
              claimGroup.getPartDPlanNonCoveredPaidAmount()));
    }

    // GDC_BLW_OOPT_AMT => ExplanationOfBenefit.item.adjudication.amount
    TransformerUtilsV2.addAdjudication(
        rxItem,
        TransformerUtilsV2.createAdjudicationAmtSlice(
            CcwCodebookVariable.GDC_BLW_OOPT_AMT,
            C4BBAdjudication.COINSURANCE,
            claimGroup.getGrossCostBelowOutOfPocketThreshold()));

    // GDC_ABV_OOPT_AMT => ExplanationOfBenefit.item.adjudication.amount
    TransformerUtilsV2.addAdjudication(
        rxItem,
        TransformerUtilsV2.createAdjudicationAmtSlice(
            CcwCodebookVariable.GDC_ABV_OOPT_AMT,
            C4BBAdjudication.COINSURANCE,
            claimGroup.getGrossCostAboveOutOfPocketThreshold()));

    // PTNT_PAY_AMT => ExplanationOfBenefit.item.adjudication.amount
    TransformerUtilsV2.addAdjudication(
        rxItem,
        TransformerUtilsV2.createAdjudicationAmtSlice(
            CcwCodebookVariable.PTNT_PAY_AMT,
            C4BBAdjudication.PAID_BY_PATIENT,
            claimGroup.getPatientPaidAmount()));

    // OTHR_TROOP_AMT => ExplanationOfBenefit.item.adjudication.amount
    TransformerUtilsV2.addAdjudication(
        rxItem,
        TransformerUtilsV2.createAdjudicationAmtSlice(
            CcwCodebookVariable.OTHR_TROOP_AMT,
            C4BBAdjudication.PRIOR_PAYER_PAID,
            claimGroup.getOtherTrueOutOfPocketPaidAmount()));

    // LICS_AMT => ExplanationOfBenefit.item.adjudication.amount
    TransformerUtilsV2.addAdjudication(
        rxItem,
        TransformerUtilsV2.createAdjudicationAmtSlice(
            CcwCodebookVariable.LICS_AMT,
            C4BBAdjudication.DISCOUNT,
            claimGroup.getLowIncomeSubsidyPaidAmount()));

    // PLRO_AMT => ExplanationOfBenefit.item.adjudication.amount
    TransformerUtilsV2.addAdjudication(
        rxItem,
        TransformerUtilsV2.createAdjudicationAmtSlice(
            CcwCodebookVariable.PLRO_AMT,
            C4BBAdjudication.PRIOR_PAYER_PAID,
            claimGroup.getPatientLiabilityReductionOtherPaidAmount()));

    // TOT_RX_CST_AMT => ExplanationOfBenefit.item.adjudication.amount
    TransformerUtilsV2.addAdjudication(
        rxItem,
        TransformerUtilsV2.createAdjudicationAmtSlice(
            CcwCodebookVariable.TOT_RX_CST_AMT,
            C4BBAdjudication.DRUG_COST,
            claimGroup.getTotalPrescriptionCost()));

    // RPTD_GAP_DSCNT_NUM => ExplanationOfBenefit.item.adjudication.amount
    TransformerUtilsV2.addAdjudication(
        rxItem,
        TransformerUtilsV2.createAdjudicationAmtSlice(
            CcwCodebookVariable.RPTD_GAP_DSCNT_NUM,
            C4BBAdjudication.DISCOUNT,
            claimGroup.getGapDiscountAmount()));

    // Validate PRESCRBR_ID
    if (claimGroup.getPrescriberIdQualifierCode() == null
        || !claimGroup.getPrescriberIdQualifierCode().equalsIgnoreCase("01")) {
      throw new InvalidRifValueException(
          "Prescriber ID Qualifier Code is invalid: " + claimGroup.getPrescriberIdQualifierCode());
    }

    // PRSCRBR_ID => ExplanationOfBenefit.careTeam.provider
    TransformerUtilsV2.addCareTeamMember(
        eob,
        rxItem,
        C4BBPractitionerIdentifierType.NPI,
        C4BBClaimPharmacyTeamRole.PRESCRIBING,
        Optional.ofNullable(claimGroup.getPrescriberId()));

    // This can't use TransformerUtilsV2.addNationalDrugCode because it maps differently
    // PROD_SRVC_ID => ExplanationOfBenefit.item.productOrService
    rxItem.setProductOrService(
        TransformerUtilsV2.createCodeableConcept(
            TransformerConstants.CODING_NDC,
            null,
            TransformerUtilsV2.retrieveFDADrugCodeDisplay(claimGroup.getNationalDrugCode()),
            claimGroup.getNationalDrugCode()));

    // QTY_DSPNSD_NUM => ExplanationOfBenefit.item.quantity
    rxItem.setQuantity(new SimpleQuantity().setValue(claimGroup.getQuantityDispensed()));

    // FILL_NUM => ExplanationOfBenefit.item.quantity.extension
    TransformerUtilsV2.addInformationSlice(eob, C4BBSupportingInfoType.REFILL_NUM)
        .setValue(new SimpleQuantity().setValue(claimGroup.getFillNumber()));

    // DAYS_SUPLY_NUM => ExplanationOfBenefit.item.quantity.extension
    TransformerUtilsV2.addInformationSlice(eob, C4BBSupportingInfoType.DAYS_SUPPLY)
        .setValue(new SimpleQuantity().setValue(claimGroup.getDaysSupply()));

    /*
     * This chart is to display the different code values for the different service provider id qualifier
     * codes below
     *   Code	    Code value
     *   01        National Provider Identifier (NPI)
     *   06        Unique Physician Identification Number (UPIN)
     *   07        National Council for Prescription Drug Programs (NCPDP) provider identifier
     *   08        State license number
     *   11        Federal tax number
     *   99        Other
     */

    C4BBOrganizationIdentifierType identifierType;

    if (!claimGroup.getServiceProviderId().isEmpty()) {
      switch (claimGroup.getServiceProviderIdQualiferCode()) {
        case "01":
          identifierType = C4BBOrganizationIdentifierType.NPI;
          break;
        case "06":
          identifierType = C4BBOrganizationIdentifierType.UPIN;
          break;
        case "07":
          identifierType = C4BBOrganizationIdentifierType.NCPDP;
          break;
        case "08":
          identifierType = C4BBOrganizationIdentifierType.SL;
          break;
        case "11":
          identifierType = C4BBOrganizationIdentifierType.TAX;
          break;
        default:
          identifierType = null;
          break;
      }

      // SRVC_PRVDR_ID => ExplanationOfBenefit.facility
      if (identifierType != null) {
        eob.setFacility(
            TransformerUtilsV2.createIdentifierReference(
                identifierType, claimGroup.getServiceProviderId()));
      }
    }

    /*
     * Storing code values in EOB.information below
     */

    // DRUG_CVRG_STUS_CD => ExplanationOfBenefit.supportingInfo.code
    TransformerUtilsV2.addInformationWithCode(
        eob,
        CcwCodebookVariable.DRUG_CVRG_STUS_CD,
        CcwCodebookVariable.DRUG_CVRG_STUS_CD,
        claimGroup.getDrugCoverageStatusCode());

    // DAW_PROD_SLCTN_CD => ExplanationOfBenefit.supportingInfo.code
    TransformerUtilsV2.addInformationWithCode(
        eob,
        CcwCodebookVariable.DAW_PROD_SLCTN_CD,
        CcwCodebookVariable.DAW_PROD_SLCTN_CD,
        claimGroup.getDispenseAsWrittenProductSelectionCode());

    // DAW_PROD_SLCTN_CD => ExplanationOfBenefit.supportingInfo.code
    if (claimGroup.getDispensingStatusCode().isPresent()) {
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.DSPNSNG_STUS_CD,
          CcwCodebookVariable.DSPNSNG_STUS_CD,
          claimGroup.getDispensingStatusCode());
    }

    // DRUG_CVRG_STUS_CD => ExplanationOfBenefit.supportingInfo.code
    TransformerUtilsV2.addInformationWithCode(
        eob,
        CcwCodebookVariable.DRUG_CVRG_STUS_CD,
        CcwCodebookVariable.DRUG_CVRG_STUS_CD,
        claimGroup.getDrugCoverageStatusCode());

    // ADJSTMT_DLTN_CD => => ExplanationOfBenefit.supportingInfo.code
    if (claimGroup.getAdjustmentDeletionCode().isPresent()) {
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.ADJSTMT_DLTN_CD,
          CcwCodebookVariable.ADJSTMT_DLTN_CD,
          claimGroup.getAdjustmentDeletionCode());
    }

    // NSTD_FRMT_CD => ExplanationOfBenefit.supportingInfo.code
    if (claimGroup.getNonstandardFormatCode().isPresent()) {
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.NSTD_FRMT_CD,
          CcwCodebookVariable.NSTD_FRMT_CD,
          claimGroup.getNonstandardFormatCode());
    }

    // PRCNG_EXCPTN_CD => ExplanationOfBenefit.supportingInfo.code
    if (claimGroup.getPricingExceptionCode().isPresent()) {
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.PRCNG_EXCPTN_CD,
          CcwCodebookVariable.PRCNG_EXCPTN_CD,
          claimGroup.getPricingExceptionCode());
    }

    // CTSTRPHC_CVRG_CD => ExplanationOfBenefit.supportingInfo.code
    if (claimGroup.getCatastrophicCoverageCode().isPresent()) {
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.CTSTRPHC_CVRG_CD,
          CcwCodebookVariable.CTSTRPHC_CVRG_CD,
          claimGroup.getCatastrophicCoverageCode());
    }

    // RX_ORGN_CD => ExplanationOfBenefit.supportingInfo:rxorigincode
    TransformerUtilsV2.addInformationSliceWithCode(
        eob,
        C4BBSupportingInfoType.RX_ORIGIN_CODE,
        CcwCodebookVariable.RX_ORGN_CD,
        CcwCodebookVariable.RX_ORGN_CD,
        claimGroup.getPrescriptionOriginationCode());

    // BRND_GNRC_CD => ExplanationOfBenefit.supportingInfo:brandgenericcode
    TransformerUtilsV2.addInformationSliceWithCode(
        eob,
        C4BBSupportingInfoType.BRAND_GENERIC_CODE,
        CcwCodebookVariable.BRND_GNRC_CD,
        CcwCodebookVariable.BRND_GNRC_CD,
        claimGroup.getBrandGenericCode());

    // PHRMCY_SRVC_TYPE_CD => ExplanationOfBenefit.facility.extension
    eob.getFacility()
        .addExtension(
            TransformerUtilsV2.createExtensionCoding(
                eob, CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD, claimGroup.getPharmacyTypeCode()));

    // PTNT_RSDNC_CD => ExplanationOfBenefit.supportingInfo.code
    TransformerUtilsV2.addInformationWithCode(
        eob,
        CcwCodebookVariable.PTNT_RSDNC_CD,
        CcwCodebookVariable.PTNT_RSDNC_CD,
        claimGroup.getPatientResidenceCode());

    // SUBMSN_CLR_CD => ExplanationOfBenefit.supportingInfo.code
    if (claimGroup.getSubmissionClarificationCode().isPresent()) {
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.SUBMSN_CLR_CD,
          CcwCodebookVariable.SUBMSN_CLR_CD,
          claimGroup.getSubmissionClarificationCode());
    }

    // Last Updated => ExplanationOfBenefit.meta.lastUpdated
    TransformerUtilsV2.setLastUpdated(eob, claimGroup.getLastUpdated());

    return eob;
  }
}
