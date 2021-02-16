package gov.cms.bfd.server.war.r4.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.parse.InvalidRifValueException;
import gov.cms.bfd.server.war.commons.IdentifierType;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.math.BigDecimal;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.r4.model.SimpleQuantity;
import org.hl7.fhir.r4.model.codesystems.ClaimCareteamrole;
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
  static ExplanationOfBenefit transform(MetricRegistry metricRegistry, Object claim) {
    Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(PartDEventTransformerV2.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof PartDEvent)) throw new BadCodeMonkeyException();
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

    eob.getMeta().addProfile(TransformerConstants.C4BB_EOB_PHARMACY_PROFILE_URL);

    // Common group level fields between all claim types
    TransformerUtilsV2.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getEventId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.PDE,
        claimGroup.getClaimGroupId().toPlainString(),
        MedicareSegment.PART_D,
        Optional.empty(),
        Optional.empty(),
        Optional.empty(),
        claimGroup.getFinalAction());

    eob.addIdentifier(
        TransformerUtilsV2.createIdentifier(
            CcwCodebookVariable.RX_SRVC_RFRNC_NUM,
            claimGroup.getPrescriptionReferenceNumber().toPlainString()));

    // map eob type codes into FHIR
    TransformerUtilsV2.mapEobType(eob, ClaimType.PDE, Optional.empty(), Optional.empty());

    eob.addInsurance()
        .getCoverage()
        .addExtension(
            TransformerUtilsV2.createExtensionIdentifier(
                CcwCodebookVariable.PLAN_CNTRCT_REC_ID, claimGroup.getPlanContractId()));

    eob.addInsurance()
        .getCoverage()
        .addExtension(
            TransformerUtilsV2.createExtensionIdentifier(
                CcwCodebookVariable.PLAN_PBP_REC_NUM, claimGroup.getPlanBenefitPackageId()));

    if (claimGroup.getPaymentDate().isPresent()) {
      eob.getPayment().setDate(TransformerUtilsV2.convertToDate(claimGroup.getPaymentDate().get()));
    }

    ItemComponent rxItem = eob.addItem();
    rxItem.setSequence(1);

    ExplanationOfBenefit.DetailComponent detail = new ExplanationOfBenefit.DetailComponent();
    switch (claimGroup.getCompoundCode()) {
        // COMPOUNDED
      case 2:
        /* Pharmacy dispense invoice for a compound */
        detail
            .addProgramCode()
            .addCoding(
                new Coding()
                    .setSystem(V3ActCode.RXCINV.getSystem())
                    .setCode(V3ActCode.RXCINV.toCode())
                    .setDisplay(V3ActCode.RXCINV.getDisplay()));
        break;
        // NOT_COMPOUNDED
      case 1:
        /*
         * Pharmacy dispense invoice not involving a compound
         */
        detail
            .addProgramCode()
            .addCoding(
                new Coding()
                    .setSystem(V3ActCode.RXCINV.getSystem())
                    .setCode(V3ActCode.RXDINV.toCode())
                    .setDisplay(V3ActCode.RXDINV.getDisplay()));
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

    rxItem.addDetail(detail);

    rxItem.setServiced(
        new DateType()
            .setValue(TransformerUtilsV2.convertToDate(claimGroup.getPrescriptionFillDate())));

    /*
     * Create an adjudication for either CVRD_D_PLAN_PD_AMT or NCVRD_PLAN_PD_AMT,
     * depending on the value of DRUG_CVRG_STUS_CD. Stick DRUG_CVRG_STUS_CD into the
     * adjudication.reason field. CARING Slicing and CARING Adjudication Value Sets.
     */
    CodeableConcept planPaidAmountAdjudicationCategory;
    BigDecimal planPaidAmountAdjudicationValue;
    if (claimGroup.getDrugCoverageStatusCode() == 'C') {
      planPaidAmountAdjudicationCategory =
          TransformerUtilsV2.createAdjudicationCategory(
              CcwCodebookVariable.CVRD_D_PLAN_PD_AMT, "benefit", "Benefit Amount");
      planPaidAmountAdjudicationValue = claimGroup.getPartDPlanCoveredPaidAmount();
    } else {
      planPaidAmountAdjudicationCategory =
          TransformerUtilsV2.createAdjudicationCategory(
              CcwCodebookVariable.NCVRD_PLAN_PD_AMT, "noncovered", "Noncovered");
      planPaidAmountAdjudicationValue = claimGroup.getPartDPlanNonCoveredPaidAmount();
    }
    rxItem
        .addAdjudication()
        .setCategory(planPaidAmountAdjudicationCategory)
        .setReason(
            TransformerUtilsV2.createCodeableConcept(
                eob, CcwCodebookVariable.DRUG_CVRG_STUS_CD, claimGroup.getDrugCoverageStatusCode()))
        .setAmount(TransformerUtilsV2.createMoney(planPaidAmountAdjudicationValue));

    rxItem
        .addAdjudication()
        .setCategory(
            TransformerUtilsV2.createAdjudicationCategory(
                CcwCodebookVariable.GDC_BLW_OOPT_AMT, "coinsurance", "Coinsurance"))
        .setAmount(
            TransformerUtilsV2.createMoney(claimGroup.getGrossCostBelowOutOfPocketThreshold()));

    rxItem
        .addAdjudication()
        .setCategory(
            TransformerUtilsV2.createAdjudicationCategory(
                CcwCodebookVariable.GDC_ABV_OOPT_AMT, "coinsurance", "Coinsurance"))
        .setAmount(
            TransformerUtilsV2.createMoney(claimGroup.getGrossCostAboveOutOfPocketThreshold()));

    rxItem
        .addAdjudication()
        .setCategory(
            TransformerUtilsV2.createAdjudicationCategory(
                CcwCodebookVariable.PTNT_PAY_AMT, "paidbypatient", "Paid by patient"))
        .setAmount(TransformerUtilsV2.createMoney(claimGroup.getPatientPaidAmount()));

    rxItem
        .addAdjudication()
        .setCategory(
            TransformerUtilsV2.createAdjudicationCategory(
                CcwCodebookVariable.OTHR_TROOP_AMT, "priorpayerpaid", "Prior payer paid"))
        .setAmount(TransformerUtilsV2.createMoney(claimGroup.getOtherTrueOutOfPocketPaidAmount()));

    rxItem
        .addAdjudication()
        .setCategory(
            TransformerUtilsV2.createAdjudicationCategory(
                CcwCodebookVariable.LICS_AMT, "discount", "Discount"))
        .setAmount(TransformerUtilsV2.createMoney(claimGroup.getLowIncomeSubsidyPaidAmount()));

    rxItem
        .addAdjudication()
        .setCategory(
            TransformerUtilsV2.createAdjudicationCategory(
                CcwCodebookVariable.PLRO_AMT, "priorpayerpaid", "Prior payer paid"))
        .setAmount(
            TransformerUtilsV2.createMoney(
                claimGroup.getPatientLiabilityReductionOtherPaidAmount()));

    rxItem
        .addAdjudication()
        .setCategory(
            TransformerUtilsV2.createAdjudicationCategory(
                CcwCodebookVariable.TOT_RX_CST_AMT, "drugcost", "Drug cost"))
        .setAmount(TransformerUtilsV2.createMoney(claimGroup.getTotalPrescriptionCost()));

    rxItem
        .addAdjudication()
        .setCategory(
            TransformerUtilsV2.createAdjudicationCategory(
                CcwCodebookVariable.RPTD_GAP_DSCNT_NUM, "discount", "Discount"))
        .setAmount(TransformerUtilsV2.createMoney(claimGroup.getGapDiscountAmount()));

    if (claimGroup.getPrescriberIdQualifierCode() == null
        || !claimGroup.getPrescriberIdQualifierCode().equalsIgnoreCase("01"))
      throw new InvalidRifValueException(
          "Prescriber ID Qualifier Code is invalid: " + claimGroup.getPrescriberIdQualifierCode());

    if (claimGroup.getPrescriberId() != null) {
      TransformerUtilsV2.addCareTeamPractitioner(
          eob,
          rxItem,
          TransformerConstants.CODING_NPI_US,
          claimGroup.getPrescriberId(),
          ClaimCareteamrole.PRIMARY);
    }

    rxItem.setProductOrService(
        TransformerUtilsV2.createCodeableConcept(
            TransformerConstants.CODING_NDC,
            null,
            TransformerUtilsV2.retrieveFDADrugCodeDisplay(claimGroup.getNationalDrugCode()),
            claimGroup.getNationalDrugCode()));

    SimpleQuantity quantityDispensed = new SimpleQuantity();
    quantityDispensed.setValue(claimGroup.getQuantityDispensed());
    rxItem.setQuantity(quantityDispensed);

    rxItem
        .getQuantity()
        .addExtension(
            TransformerUtilsV2.createExtensionQuantity(
                CcwCodebookVariable.FILL_NUM, claimGroup.getFillNumber()));

    rxItem
        .getQuantity()
        .addExtension(
            TransformerUtilsV2.createExtensionQuantity(
                CcwCodebookVariable.DAYS_SUPLY_NUM, claimGroup.getDaysSupply()));

    /*
     * This chart is to display the different code values for the different service provider id qualifier
     * codes below
     *   Code	    Code value
     *   01        National Provider Identifier (NPI)
     *   06        Unique Physician Identification Number (UPIN)
     *   07        National Council for Prescription Drug Programs (NCPDP) provider identifier
     *   08        State license number   11
     *   Federal tax number   99        Other
     */

    IdentifierType identifierType;

    if (!claimGroup.getServiceProviderId().isEmpty()) {
      switch (claimGroup.getServiceProviderIdQualiferCode()) {
        case "01":
          identifierType = IdentifierType.NPI;
          break;
        case "06":
          identifierType = IdentifierType.UPIN;
          break;
        case "07":
          identifierType = IdentifierType.NCPDP;
          break;
        case "08":
          identifierType = IdentifierType.SL;
          break;
        case "11":
          identifierType = IdentifierType.TAX;
          break;
        default:
          identifierType = null;
          break;
      }

      if (identifierType != null) {
        eob.setProvider(
            TransformerUtilsV2.createIdentifierReference(
                TransformerConstants.CODING_NPI_US, claimGroup.getServiceProviderId()));
        eob.setFacility(
            TransformerUtilsV2.createIdentifierReference(
                TransformerConstants.CODING_NPI_US, claimGroup.getServiceProviderId()));
      }

      eob.getFacility()
          .addExtension(
              TransformerUtilsV2.createExtensionCoding(
                  eob, CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD, claimGroup.getPharmacyTypeCode()));
    }

    /*
     * Storing code values in EOB.information below
     */

    TransformerUtilsV2.addInformationWithCode(
        eob,
        CcwCodebookVariable.DAW_PROD_SLCTN_CD,
        CcwCodebookVariable.DAW_PROD_SLCTN_CD,
        claimGroup.getDispenseAsWrittenProductSelectionCode());

    if (claimGroup.getDispensingStatusCode().isPresent())
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.DSPNSNG_STUS_CD,
          CcwCodebookVariable.DSPNSNG_STUS_CD,
          claimGroup.getDispensingStatusCode());

    TransformerUtilsV2.addInformationWithCode(
        eob,
        CcwCodebookVariable.DRUG_CVRG_STUS_CD,
        CcwCodebookVariable.DRUG_CVRG_STUS_CD,
        claimGroup.getDrugCoverageStatusCode());

    if (claimGroup.getAdjustmentDeletionCode().isPresent())
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.ADJSTMT_DLTN_CD,
          CcwCodebookVariable.ADJSTMT_DLTN_CD,
          claimGroup.getAdjustmentDeletionCode());

    if (claimGroup.getNonstandardFormatCode().isPresent())
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.NSTD_FRMT_CD,
          CcwCodebookVariable.NSTD_FRMT_CD,
          claimGroup.getNonstandardFormatCode());

    if (claimGroup.getPricingExceptionCode().isPresent())
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.PRCNG_EXCPTN_CD,
          CcwCodebookVariable.PRCNG_EXCPTN_CD,
          claimGroup.getPricingExceptionCode());

    if (claimGroup.getCatastrophicCoverageCode().isPresent())
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.CTSTRPHC_CVRG_CD,
          CcwCodebookVariable.CTSTRPHC_CVRG_CD,
          claimGroup.getCatastrophicCoverageCode());

    if (claimGroup.getPrescriptionOriginationCode().isPresent())
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.RX_ORGN_CD,
          CcwCodebookVariable.RX_ORGN_CD,
          claimGroup.getPrescriptionOriginationCode());

    if (claimGroup.getBrandGenericCode().isPresent())
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.BRND_GNRC_CD,
          CcwCodebookVariable.BRND_GNRC_CD,
          claimGroup.getBrandGenericCode());

    TransformerUtilsV2.addInformationWithCode(
        eob,
        CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD,
        CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD,
        claimGroup.getPharmacyTypeCode());

    TransformerUtilsV2.addInformationWithCode(
        eob,
        CcwCodebookVariable.PTNT_RSDNC_CD,
        CcwCodebookVariable.PTNT_RSDNC_CD,
        claimGroup.getPatientResidenceCode());

    if (claimGroup.getSubmissionClarificationCode().isPresent())
      TransformerUtilsV2.addInformationWithCode(
          eob,
          CcwCodebookVariable.SUBMSN_CLR_CD,
          CcwCodebookVariable.SUBMSN_CLR_CD,
          claimGroup.getSubmissionClarificationCode());

    TransformerUtilsV2.setLastUpdated(eob, claimGroup.getLastUpdated());
    return eob;
  }
}
