package gov.cms.bfd.server.war.stu3.providers;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED;
import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaimLine;
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
 * Transforms CCW {@link HospiceClaim} instances into FHIR {@link ExplanationOfBenefit} resources.
 */
@Component
final class HospiceClaimTransformer implements ClaimTransformerInterface {
  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(HospiceClaimTransformer.class.getSimpleName(), "transform");

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
   * @param securityTagManager securityTagManager
   * @param samhsaV2Enabled samhsaV2Enabled flag
   */
  public HospiceClaimTransformer(
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
   * @param claimEntity the {@link HospiceClaim} to use
   * @return a FHIR {@link ExplanationOfBenefit} resource.
   */
  @Override
  public ExplanationOfBenefit transform(ClaimWithSecurityTags<?> claimEntity) {

    Object claim = claimEntity.getClaimEntity();
    List<Coding> securityTags =
        securityTagManager.getClaimSecurityLevelDstu3(claimEntity.getSecurityTags());

    if (!(claim instanceof HospiceClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob;
    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      HospiceClaim hospiceClaim = (HospiceClaim) claim;
      eob = transformClaim(hospiceClaim, securityTags);
    }
    return eob;
  }

  /**
   * Transforms a specified {@link HospiceClaim} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param claimGroup the CCW {@link HospiceClaim} to transform
   * @param securityTags securityTags tag of a claim
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     HospiceClaim}
   */
  private ExplanationOfBenefit transformClaim(HospiceClaim claimGroup, List<Coding> securityTags) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Common group level fields between all claim types
    TransformerUtils.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.HOSPICE,
        String.valueOf(claimGroup.getClaimGroupId()),
        MedicareSegment.PART_A,
        Optional.of(claimGroup.getDateFrom()),
        Optional.of(claimGroup.getDateThrough()),
        Optional.of(claimGroup.getPaymentAmount()),
        claimGroup.getFinalAction());

    TransformerUtils.mapEobWeeklyProcessDate(eob, claimGroup.getWeeklyProcessDate());

    // map eob type codes into FHIR
    TransformerUtils.mapEobType(
        eob,
        ClaimType.HOSPICE,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // set the provider number which is common among several claim types
    TransformerUtils.setProviderNumber(eob, claimGroup.getProviderNumber());

    if (claimGroup.getPatientStatusCd().isPresent()) {
      TransformerUtils.addInformationWithCode(
          eob,
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          claimGroup.getPatientStatusCd());
    }

    // Common group level fields between Inpatient, HHA, Hospice and SNF
    TransformerUtils.mapEobCommonGroupInpHHAHospiceSNF(
        eob,
        claimGroup.getClaimHospiceStartDate(),
        claimGroup.getBeneficiaryDischargeDate(),
        Optional.of(claimGroup.getUtilizationDayCount()));

    if (claimGroup.getHospicePeriodCount().isPresent()) {
      eob.getHospitalization()
          .addExtension(
              TransformerUtils.createExtensionQuantity(
                  CcwCodebookVariable.BENE_HOSPC_PRD_CNT, claimGroup.getHospicePeriodCount()));
    }

    // Common group level fields between Inpatient, Outpatient Hospice, HHA and SNF
    TransformerUtils.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        claimGroup.getOrganizationNpi(),
        CommonTransformerUtils.buildReplaceOrganization(claimGroup.getOrganizationNpi())
            .map(NPIData::getProviderOrganizationName),
        claimGroup.getClaimFacilityTypeCode(),
        claimGroup.getClaimFrequencyCode(),
        claimGroup.getClaimNonPaymentReasonCode(),
        claimGroup.getPatientDischargeStatusCode(),
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

    for (HospiceClaimLine claimLine : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();
      item.setSequence(claimLine.getLineNumber());

      item.setLocation(new Address().setState((claimGroup.getProviderStateCode())));

      TransformerUtils.mapHcpcs(
          eob,
          item,
          Optional.empty(),
          claimLine.getHcpcsCode(),
          Arrays.asList(
              claimLine.getHcpcsInitialModifierCode(), claimLine.getHcpcsSecondModifierCode()));

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

      // Common item level fields between Inpatient, Outpatient, HHA, Hospice and SNF
      TransformerUtils.mapEobCommonItemRevenue(
          item,
          eob,
          claimLine.getRevenueCenterCode(),
          claimLine.getRateAmount(),
          claimLine.getTotalChargeAmount(),
          claimLine.getNonCoveredChargeAmount().get(),
          claimLine.getUnitCount(),
          claimLine.getNationalDrugCodeQuantity(),
          claimLine.getNationalDrugCodeQualifierCode(),
          claimLine.getRevenueCenterRenderingPhysicianNPI());

      // Common item level fields between Outpatient, HHA and Hospice
      TransformerUtils.mapEobCommonItemRevenueOutHHAHospice(
          item, claimLine.getRevenueCenterDate(), claimLine.getPaymentAmount());

      // Common group level field coinsurance between Inpatient, HHA, Hospice and SNF
      TransformerUtils.mapEobCommonGroupInpHHAHospiceSNFCoinsurance(
          eob, item, claimLine.getDeductibleCoinsuranceCd());
    }
    TransformerUtils.setLastUpdated(eob, claimGroup.getLastUpdated());

    if (samhsaV2Enabled) {
      eob.getMeta().setSecurity(securityTags);
    }

    return eob;
  }
}
