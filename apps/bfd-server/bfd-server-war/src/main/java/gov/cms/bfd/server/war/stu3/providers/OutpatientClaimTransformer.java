package gov.cms.bfd.server.war.stu3.providers;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;
import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaimLine;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Transforms CCW {@link OutpatientClaim} instances into FHIR {@link ExplanationOfBenefit}
 * resources.
 */
@Component
final class OutpatientClaimTransformer implements ClaimTransformerInterface {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(OutpatientClaimTransformer.class.getSimpleName(), "transform");

  /** The securityTagManager. */
  private final SecurityTagManager securityTagManager;

  private final boolean samhsaV2Enabled;

  /**
   * Instantiates a new transformer.
   *
   * <p>Spring will wire this into a singleton bean during the initial component scan, and it will
   * be injected properly into places that need it, so this constructor should only be explicitly
   * called by tests.
   *
   * @param metricRegistry the metric registry
   * @param securityTagManager SamhsaSecurityTag lookup
   * @param samhsaV2Enabled samhsaV2Enabled flag
   */
  public OutpatientClaimTransformer(
      MetricRegistry metricRegistry,
      SecurityTagManager securityTagManager,
      @Value("${" + SSM_PATH_SAMHSA_V2_ENABLED + ":false}") Boolean samhsaV2Enabled) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.securityTagManager = requireNonNull(securityTagManager);
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

  /**
   * Transforms a claim into an {@link ExplanationOfBenefit}.
   *
   * @param claimEntity the {@link OutpatientClaim} to use
   * @param includeTaxNumber exists to satisfy {@link ClaimTransformerInterface}; ignored
   * @return a FHIR {@link ExplanationOfBenefit} resource.
   */
  @Override
  public ExplanationOfBenefit transform(
      ClaimWithSecurityTags<?> claimEntity, boolean includeTaxNumber) {

    Object claim = claimEntity.getClaimEntity();
    List<Coding> securityTags =
        securityTagManager.getClaimSecurityLevelDstu3(claimEntity.getSecurityTags());

    if (!(claim instanceof OutpatientClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob;
    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      OutpatientClaim outpatientClaim = (OutpatientClaim) claim;
      eob = transformClaim(outpatientClaim, securityTags);
    }
    return eob;
  }

  /**
   * Transforms a specified {@link InpatientClaim} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param claimGroup the CCW {@link OutpatientClaim} to transform
   * @param securityTags securityTags tag of a claim
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     OutpatientClaim}
   */
  private ExplanationOfBenefit transformClaim(
      OutpatientClaim claimGroup, List<Coding> securityTags) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Common group level fields between all claim types
    TransformerUtils.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.OUTPATIENT,
        String.valueOf(claimGroup.getClaimGroupId()),
        MedicareSegment.PART_B,
        Optional.of(claimGroup.getDateFrom()),
        Optional.of(claimGroup.getDateThrough()),
        Optional.of(claimGroup.getPaymentAmount()),
        claimGroup.getFinalAction());

    TransformerUtils.mapEobWeeklyProcessDate(eob, claimGroup.getWeeklyProcessDate());

    // map eob type codes into FHIR
    TransformerUtils.mapEobType(
        eob,
        ClaimType.OUTPATIENT,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // set the provider number which is common among several claim types
    TransformerUtils.setProviderNumber(eob, claimGroup.getProviderNumber());

    // TODO If this is actually nullable, should be Optional.
    if (claimGroup.getProfessionalComponentCharge() != null) {
      TransformerUtils.addAdjudicationTotal(
          eob,
          CcwCodebookVariable.NCH_PROFNL_CMPNT_CHRG_AMT,
          claimGroup.getProfessionalComponentCharge());
    }

    // TODO If this is actually nullable, should be Optional.
    if (claimGroup.getDeductibleAmount() != null) {
      TransformerUtils.addAdjudicationTotal(
          eob, CcwCodebookVariable.NCH_BENE_PTB_DDCTBL_AMT, claimGroup.getDeductibleAmount());
    }

    // TODO If this is actually nullable, should be Optional.
    if (claimGroup.getCoinsuranceAmount() != null) {
      TransformerUtils.addAdjudicationTotal(
          eob, CcwCodebookVariable.NCH_BENE_PTB_COINSRNC_AMT, claimGroup.getCoinsuranceAmount());
    }

    // TODO If this is actually nullable, should be Optional.
    if (claimGroup.getProviderPaymentAmount() != null) {
      TransformerUtils.addAdjudicationTotal(
          eob, CcwCodebookVariable.CLM_OP_PRVDR_PMT_AMT, claimGroup.getProviderPaymentAmount());
    }

    // TODO If this is actually nullable, should be Optional.
    if (claimGroup.getBeneficiaryPaymentAmount() != null) {
      TransformerUtils.addAdjudicationTotal(
          eob, CcwCodebookVariable.CLM_OP_BENE_PMT_AMT, claimGroup.getBeneficiaryPaymentAmount());
    }

    // Common group level fields between Inpatient, Outpatient and SNF
    TransformerUtils.mapEobCommonGroupInpOutSNF(
        eob,
        claimGroup.getBloodDeductibleLiabilityAmount(),
        claimGroup.getOperatingPhysicianNpi(),
        claimGroup.getOtherPhysicianNpi(),
        claimGroup.getClaimQueryCode(),
        claimGroup.getMcoPaidSw());

    // Common group level fields between Inpatient, Outpatient Hospice, HHA and SNF
    TransformerUtils.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        claimGroup.getOrganizationNpi(),
        CommonTransformerUtils.buildReplaceOrganization(claimGroup.getOrganizationNpi())
            .map(NPIData::getProviderOrganizationName),
        claimGroup.getClaimFacilityTypeCode(),
        claimGroup.getClaimFrequencyCode(),
        claimGroup.getClaimNonPaymentReasonCode(),
        claimGroup.getPatientDischargeStatusCode().get(),
        claimGroup.getClaimServiceClassificationTypeCode(),
        claimGroup.getClaimPrimaryPayerCode(),
        claimGroup.getAttendingPhysicianNpi(),
        claimGroup.getTotalChargeAmount(),
        claimGroup.getPrimaryPayerPaidAmount(),
        claimGroup.getFiscalIntermediaryNumber(),
        claimGroup.getFiDocumentClaimControlNumber(),
        claimGroup.getFiOriginalClaimControlNumber());

    TransformerUtils.extractDiagnoses(
            claimGroup.getDiagnosisCodes(), claimGroup.getDiagnosisCodeVersions(), Map.of())
        .forEach(d -> TransformerUtils.addDiagnosisCode(eob, d));

    // Handle Procedures
    TransformerUtils.extractCCWProcedures(
            claimGroup.getProcedureCodes(),
            claimGroup.getProcedureCodeVersions(),
            claimGroup.getProcedureDates())
        .forEach(p -> TransformerUtils.addProcedureCode(eob, p));

    for (OutpatientClaimLine claimLine : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();
      item.setSequence(claimLine.getLineNumber());
      item.setLocation(new Address().setState((claimGroup.getProviderStateCode())));
      TransformerUtils.addRevCenterAnsiAdjudication(item, eob, claimLine.getRevCntr1stAnsiCd());

      if (claimLine.getRevCntr2ndAnsiCd().isPresent()) {
        item.addAdjudication()
            .setCategory(
                TransformerUtils.createAdjudicationCategory(
                    CcwCodebookVariable.REV_CNTR_2ND_ANSI_CD))
            .setReason(
                TransformerUtils.createCodeableConcept(
                    eob,
                    CcwCodebookVariable.REV_CNTR_2ND_ANSI_CD,
                    claimLine.getRevCntr2ndAnsiCd()));
      }
      if (claimLine.getRevCntr3rdAnsiCd().isPresent()) {
        item.addAdjudication()
            .setCategory(
                TransformerUtils.createAdjudicationCategory(
                    CcwCodebookVariable.REV_CNTR_3RD_ANSI_CD))
            .setReason(
                TransformerUtils.createCodeableConcept(
                    eob,
                    CcwCodebookVariable.REV_CNTR_3RD_ANSI_CD,
                    claimLine.getRevCntr3rdAnsiCd()));
      }
      if (claimLine.getRevCntr4thAnsiCd().isPresent()) {
        item.addAdjudication()
            .setCategory(
                TransformerUtils.createAdjudicationCategory(
                    CcwCodebookVariable.REV_CNTR_4TH_ANSI_CD))
            .setReason(
                TransformerUtils.createCodeableConcept(
                    eob,
                    CcwCodebookVariable.REV_CNTR_4TH_ANSI_CD,
                    claimLine.getRevCntr4thAnsiCd()));
      }

      TransformerUtils.mapHcpcs(
          eob,
          item,
          Optional.empty(),
          claimLine.getHcpcsCode(),
          Arrays.asList(
              claimLine.getHcpcsInitialModifierCode(), claimLine.getHcpcsSecondModifierCode()));

      if (claimLine.getNationalDrugCode().isPresent()) {
        item.getService()
            .addExtension(
                TransformerUtils.createExtensionCoding(
                    eob,
                    CcwCodebookVariable.REV_CNTR_IDE_NDC_UPC_NUM,
                    claimLine.getNationalDrugCode()));
      }

      item.addAdjudication()
          .setCategory(
              TransformerUtils.createAdjudicationCategory(
                  CcwCodebookVariable.REV_CNTR_BLOOD_DDCTBL_AMT))
          .setAmount(TransformerUtils.createMoney(claimLine.getBloodDeductibleAmount()));

      item.addAdjudication()
          .setCategory(
              TransformerUtils.createAdjudicationCategory(
                  CcwCodebookVariable.REV_CNTR_CASH_DDCTBL_AMT))
          .setAmount(TransformerUtils.createMoney(claimLine.getCashDeductibleAmount()));

      item.addAdjudication()
          .setCategory(
              TransformerUtils.createAdjudicationCategory(
                  CcwCodebookVariable.REV_CNTR_COINSRNC_WGE_ADJSTD_C))
          .setAmount(TransformerUtils.createMoney(claimLine.getWageAdjustedCoinsuranceAmount()));

      item.addAdjudication()
          .setCategory(
              TransformerUtils.createAdjudicationCategory(
                  CcwCodebookVariable.REV_CNTR_RDCD_COINSRNC_AMT))
          .setAmount(TransformerUtils.createMoney(claimLine.getReducedCoinsuranceAmount()));

      item.addAdjudication()
          .setCategory(
              TransformerUtils.createAdjudicationCategory(
                  CcwCodebookVariable.REV_CNTR_1ST_MSP_PD_AMT))
          .setAmount(TransformerUtils.createMoney(claimLine.getFirstMspPaidAmount()));

      item.addAdjudication()
          .setCategory(
              TransformerUtils.createAdjudicationCategory(
                  CcwCodebookVariable.REV_CNTR_2ND_MSP_PD_AMT))
          .setAmount(TransformerUtils.createMoney(claimLine.getSecondMspPaidAmount()));

      item.addAdjudication()
          .setCategory(
              TransformerUtils.createAdjudicationCategory(
                  CcwCodebookVariable.REV_CNTR_PRVDR_PMT_AMT))
          .setAmount(TransformerUtils.createMoney(claimLine.getProviderPaymentAmount()));

      item.addAdjudication()
          .setCategory(
              TransformerUtils.createAdjudicationCategory(
                  CcwCodebookVariable.REV_CNTR_BENE_PMT_AMT))
          .setAmount(TransformerUtils.createMoney(claimLine.getBenficiaryPaymentAmount()));

      item.addAdjudication()
          .setCategory(
              TransformerUtils.createAdjudicationCategory(
                  CcwCodebookVariable.REV_CNTR_PTNT_RSPNSBLTY_PMT))
          .setAmount(TransformerUtils.createMoney(claimLine.getPatientResponsibilityAmount()));

      // Common item level fields between Outpatient, HHA and Hospice
      TransformerUtils.mapEobCommonItemRevenueOutHHAHospice(
          item, claimLine.getRevenueCenterDate(), claimLine.getPaymentAmount());

      // Common item level fields between Inpatient, Outpatient, HHA, Hospice and SNF
      TransformerUtils.mapEobCommonItemRevenue(
          item,
          eob,
          claimLine.getRevenueCenterCode(),
          claimLine.getRateAmount(),
          claimLine.getTotalChargeAmount(),
          claimLine.getNonCoveredChargeAmount(),
          claimLine.getUnitCount(),
          claimLine.getNationalDrugCodeQuantity(),
          claimLine.getNationalDrugCodeQualifierCode(),
          claimLine.getRevenueCenterRenderingPhysicianNPI());

      // set revenue center status indicator codes for the claim
      // Dt: 6-18-20 Handling for optional status code claim line: BFD-252
      if (claimLine.getStatusCode().isPresent()) {
        item.getRevenue()
            .addExtension(
                TransformerUtils.createExtensionCoding(
                    eob, CcwCodebookVariable.REV_CNTR_STUS_IND_CD, claimLine.getStatusCode()));
      }
    }
    TransformerUtils.setLastUpdated(eob, claimGroup.getLastUpdated());

    if (samhsaV2Enabled) {
      eob.getMeta().setSecurity(securityTags);
    }
    return eob;
  }
}
