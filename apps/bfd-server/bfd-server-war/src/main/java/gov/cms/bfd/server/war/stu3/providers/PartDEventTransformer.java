package gov.cms.bfd.server.war.stu3.providers;

import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.PartDEvent;
import gov.cms.bfd.model.rif.parse.InvalidRifValueException;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.IdentifierType;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.TransformerConstants;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.math.BigDecimal;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.DateType;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.SimpleQuantity;
import org.hl7.fhir.dstu3.model.codesystems.ClaimCareteamrole;
import org.hl7.fhir.dstu3.model.codesystems.V3ActCode;
import org.springframework.stereotype.Component;

/** Transforms CCW {@link PartDEvent} instances into FHIR {@link ExplanationOfBenefit} resources. */
@Component
final class PartDEventTransformer implements ClaimTransformerInterface {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(PartDEventTransformer.class.getSimpleName(), "transform");

  /**
   * Instantiates a new transformer.
   *
   * <p>Spring will wire this into a singleton bean during the initial component scan, and it will
   * be injected properly into places that need it, so this constructor should only be explicitly
   * called by tests.
   *
   * @param metricRegistry the metric registry
   */
  public PartDEventTransformer(MetricRegistry metricRegistry) {
    this.metricRegistry = requireNonNull(metricRegistry);
  }

  /**
   * Transforms a claim into an {@link ExplanationOfBenefit}.
   *
   * @param includeTaxNumber exists to satisfy {@link ClaimTransformerInterface}; ignored
   * @param claimEntity      the {@link PartDEvent} to use
   * @return a FHIR {@link ExplanationOfBenefit} resource.
   */
  @Override
  public ExplanationOfBenefit transform(
      ClaimWithSecurityTags<?> claimEntity) {

    Object claim = claimEntity.getClaimEntity();
    if (!(claim instanceof PartDEvent)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob;
    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      eob = transformClaim((PartDEvent) claim);
    }
    return eob;
  }

  /**
   * Transforms a specified {@link PartDEvent} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param claimGroup the CCW {@link PartDEvent} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     PartDEvent}
   */
  private ExplanationOfBenefit transformClaim(PartDEvent claimGroup) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Common group level fields between all claim types
    TransformerUtils.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getEventId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.PDE,
        String.valueOf(claimGroup.getClaimGroupId()),
        MedicareSegment.PART_D,
        Optional.of(claimGroup.getPrescriptionFillDate()),
        Optional.of(claimGroup.getPrescriptionFillDate()),
        Optional.empty(),
        claimGroup.getFinalAction());

    eob.addIdentifier(
        TransformerUtils.createIdentifier(
            CcwCodebookVariable.RX_SRVC_RFRNC_NUM,
            claimGroup.getPrescriptionReferenceNumber().toPlainString()));

    // map eob type codes into FHIR
    TransformerUtils.mapEobType(eob, ClaimType.PDE, Optional.empty(), Optional.empty());

    eob.getInsurance()
        .getCoverage()
        .addExtension(
            TransformerUtils.createExtensionIdentifier(
                CcwCodebookVariable.PLAN_CNTRCT_REC_ID, claimGroup.getPlanContractId()));

    eob.getInsurance()
        .getCoverage()
        .addExtension(
            TransformerUtils.createExtensionIdentifier(
                CcwCodebookVariable.PLAN_PBP_REC_NUM, claimGroup.getPlanBenefitPackageId()));

    if (claimGroup.getPaymentDate().isPresent()) {
      eob.getPayment()
          .setDate(CommonTransformerUtils.convertToDate(claimGroup.getPaymentDate().get()));
    }

    ItemComponent rxItem = eob.addItem();
    rxItem.setSequence(1);

    ExplanationOfBenefit.DetailComponent detail = new ExplanationOfBenefit.DetailComponent();
    switch (claimGroup.getCompoundCode()) {
      // COMPOUNDED
      case 2:
        /* Pharmacy dispense invoice for a compound */
        detail
            .getType()
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
            .getType()
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
            .setValue(CommonTransformerUtils.convertToDate(claimGroup.getPrescriptionFillDate())));

    /*
     * Create an adjudication for either CVRD_D_PLAN_PD_AMT or NCVRD_PLAN_PD_AMT,
     * depending on the value of DRUG_CVRG_STUS_CD. Stick DRUG_CVRG_STUS_CD into the
     * adjudication.reason field.
     */
    CodeableConcept planPaidAmountAdjudicationCategory;
    BigDecimal planPaidAmountAdjudicationValue;
    if (claimGroup.getDrugCoverageStatusCode() == 'C') {
      planPaidAmountAdjudicationCategory =
          TransformerUtils.createAdjudicationCategory(CcwCodebookVariable.CVRD_D_PLAN_PD_AMT);
      planPaidAmountAdjudicationValue = claimGroup.getPartDPlanCoveredPaidAmount();
    } else {
      planPaidAmountAdjudicationCategory =
          TransformerUtils.createAdjudicationCategory(CcwCodebookVariable.NCVRD_PLAN_PD_AMT);
      planPaidAmountAdjudicationValue = claimGroup.getPartDPlanNonCoveredPaidAmount();
    }
    rxItem
        .addAdjudication()
        .setCategory(planPaidAmountAdjudicationCategory)
        .setReason(
            TransformerUtils.createCodeableConcept(
                eob, CcwCodebookVariable.DRUG_CVRG_STUS_CD, claimGroup.getDrugCoverageStatusCode()))
        .setAmount(TransformerUtils.createMoney(planPaidAmountAdjudicationValue));

    rxItem
        .addAdjudication()
        .setCategory(
            TransformerUtils.createAdjudicationCategory(CcwCodebookVariable.GDC_BLW_OOPT_AMT))
        .setAmount(
            TransformerUtils.createMoney(claimGroup.getGrossCostBelowOutOfPocketThreshold()));

    rxItem
        .addAdjudication()
        .setCategory(
            TransformerUtils.createAdjudicationCategory(CcwCodebookVariable.GDC_ABV_OOPT_AMT))
        .setAmount(
            TransformerUtils.createMoney(claimGroup.getGrossCostAboveOutOfPocketThreshold()));

    rxItem
        .addAdjudication()
        .setCategory(TransformerUtils.createAdjudicationCategory(CcwCodebookVariable.PTNT_PAY_AMT))
        .setAmount(TransformerUtils.createMoney(claimGroup.getPatientPaidAmount()));

    rxItem
        .addAdjudication()
        .setCategory(
            TransformerUtils.createAdjudicationCategory(CcwCodebookVariable.OTHR_TROOP_AMT))
        .setAmount(TransformerUtils.createMoney(claimGroup.getOtherTrueOutOfPocketPaidAmount()));

    rxItem
        .addAdjudication()
        .setCategory(TransformerUtils.createAdjudicationCategory(CcwCodebookVariable.LICS_AMT))
        .setAmount(TransformerUtils.createMoney(claimGroup.getLowIncomeSubsidyPaidAmount()));

    rxItem
        .addAdjudication()
        .setCategory(TransformerUtils.createAdjudicationCategory(CcwCodebookVariable.PLRO_AMT))
        .setAmount(
            TransformerUtils.createMoney(claimGroup.getPatientLiabilityReductionOtherPaidAmount()));

    rxItem
        .addAdjudication()
        .setCategory(
            TransformerUtils.createAdjudicationCategory(CcwCodebookVariable.TOT_RX_CST_AMT))
        .setAmount(TransformerUtils.createMoney(claimGroup.getTotalPrescriptionCost()));

    rxItem
        .addAdjudication()
        .setCategory(
            TransformerUtils.createAdjudicationCategory(CcwCodebookVariable.RPTD_GAP_DSCNT_NUM))
        .setAmount(TransformerUtils.createMoney(claimGroup.getGapDiscountAmount()));

    if (claimGroup.getPrescriberIdQualifierCode() == null
        || !claimGroup.getPrescriberIdQualifierCode().equalsIgnoreCase("01")) {
      throw new InvalidRifValueException(
          "Prescriber ID Qualifier Code is invalid: " + claimGroup.getPrescriberIdQualifierCode());
    }

    if (claimGroup.getPrescriberId() != null) {
      TransformerUtils.addCareTeamPractitioner(
          eob,
          rxItem,
          TransformerConstants.CODING_NPI_US,
          claimGroup.getPrescriberId(),
          ClaimCareteamrole.PRIMARY);
    }

    rxItem.setService(
        TransformerUtils.createCodeableConcept(
            TransformerConstants.CODING_NDC,
            null,
            CommonTransformerUtils.buildReplaceDrugCode(
                Optional.of(claimGroup.getNationalDrugCode())),
            claimGroup.getNationalDrugCode()));

    SimpleQuantity quantityDispensed = new SimpleQuantity();
    quantityDispensed.setValue(claimGroup.getQuantityDispensed());
    rxItem.setQuantity(quantityDispensed);

    rxItem
        .getQuantity()
        .addExtension(
            TransformerUtils.createExtensionQuantity(
                CcwCodebookVariable.FILL_NUM, claimGroup.getFillNumber()));

    rxItem
        .getQuantity()
        .addExtension(
            TransformerUtils.createExtensionQuantity(
                CcwCodebookVariable.DAYS_SUPLY_NUM, claimGroup.getDaysSupply()));

    /*
     * This chart is to dosplay the different code values for the different service provider id qualifer
     * codes below
     *   Code	    Code value
     *   01        National Provider Identifier (NPI)
     *   06        Unique Physician Identification Number (UPIN)
     *   07        National Council for Prescription Drug Programs (NCPDP) provider identifier
     *   08        State license number
     *   11        Federal tax number
     *   99        Other
     */

    IdentifierType identifierType;

    if (!claimGroup.getServiceProviderId().isEmpty()) {
      identifierType =
          switch (claimGroup.getServiceProviderIdQualiferCode()) {
            case "01" -> IdentifierType.NPI;
            case "06" -> IdentifierType.UPIN;
            case "07" -> IdentifierType.NCPDP;
            case "08" -> IdentifierType.SL;
            case "11" -> IdentifierType.TAX;
            default -> null;
          };

      if (identifierType != null) {
        eob.setOrganization(
            TransformerUtils.createIdentifierReference(
                identifierType, claimGroup.getServiceProviderId()));
        eob.setFacility(
            TransformerUtils.createIdentifierReference(
                identifierType, claimGroup.getServiceProviderId()));
      }

      eob.getFacility()
          .addExtension(
              TransformerUtils.createExtensionCoding(
                  eob, CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD, claimGroup.getPharmacyTypeCode()));
    }

    /*
     * Storing code values in EOB.information below
     */

    TransformerUtils.addInformationWithCode(
        eob,
        CcwCodebookVariable.DAW_PROD_SLCTN_CD,
        CcwCodebookVariable.DAW_PROD_SLCTN_CD,
        claimGroup.getDispenseAsWrittenProductSelectionCode());

    if (claimGroup.getDispensingStatusCode().isPresent()) {
      TransformerUtils.addInformationWithCode(
          eob,
          CcwCodebookVariable.DSPNSNG_STUS_CD,
          CcwCodebookVariable.DSPNSNG_STUS_CD,
          claimGroup.getDispensingStatusCode());
    }

    TransformerUtils.addInformationWithCode(
        eob,
        CcwCodebookVariable.DRUG_CVRG_STUS_CD,
        CcwCodebookVariable.DRUG_CVRG_STUS_CD,
        claimGroup.getDrugCoverageStatusCode());

    if (claimGroup.getAdjustmentDeletionCode().isPresent()) {
      TransformerUtils.addInformationWithCode(
          eob,
          CcwCodebookVariable.ADJSTMT_DLTN_CD,
          CcwCodebookVariable.ADJSTMT_DLTN_CD,
          claimGroup.getAdjustmentDeletionCode());
    }

    if (claimGroup.getNonstandardFormatCode().isPresent()) {
      TransformerUtils.addInformationWithCode(
          eob,
          CcwCodebookVariable.NSTD_FRMT_CD,
          CcwCodebookVariable.NSTD_FRMT_CD,
          claimGroup.getNonstandardFormatCode());
    }

    if (claimGroup.getPricingExceptionCode().isPresent()) {
      TransformerUtils.addInformationWithCode(
          eob,
          CcwCodebookVariable.PRCNG_EXCPTN_CD,
          CcwCodebookVariable.PRCNG_EXCPTN_CD,
          claimGroup.getPricingExceptionCode());
    }

    if (claimGroup.getCatastrophicCoverageCode().isPresent()) {
      TransformerUtils.addInformationWithCode(
          eob,
          CcwCodebookVariable.CTSTRPHC_CVRG_CD,
          CcwCodebookVariable.CTSTRPHC_CVRG_CD,
          claimGroup.getCatastrophicCoverageCode());
    }

    if (claimGroup.getPrescriptionOriginationCode().isPresent()) {
      TransformerUtils.addInformationWithCode(
          eob,
          CcwCodebookVariable.RX_ORGN_CD,
          CcwCodebookVariable.RX_ORGN_CD,
          claimGroup.getPrescriptionOriginationCode());
    }

    if (claimGroup.getBrandGenericCode().isPresent()) {
      TransformerUtils.addInformationWithCode(
          eob,
          CcwCodebookVariable.BRND_GNRC_CD,
          CcwCodebookVariable.BRND_GNRC_CD,
          claimGroup.getBrandGenericCode());
    }

    TransformerUtils.addInformationWithCode(
        eob,
        CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD,
        CcwCodebookVariable.PHRMCY_SRVC_TYPE_CD,
        claimGroup.getPharmacyTypeCode());

    TransformerUtils.addInformationWithCode(
        eob,
        CcwCodebookVariable.PTNT_RSDNC_CD,
        CcwCodebookVariable.PTNT_RSDNC_CD,
        claimGroup.getPatientResidenceCode());

    if (claimGroup.getSubmissionClarificationCode().isPresent()) {
      TransformerUtils.addInformationWithCode(
          eob,
          CcwCodebookVariable.SUBMSN_CLR_CD,
          CcwCodebookVariable.SUBMSN_CLR_CD,
          claimGroup.getSubmissionClarificationCode());
    }

    TransformerUtils.setLastUpdated(eob, claimGroup.getLastUpdated());
    return eob;
  }
}
