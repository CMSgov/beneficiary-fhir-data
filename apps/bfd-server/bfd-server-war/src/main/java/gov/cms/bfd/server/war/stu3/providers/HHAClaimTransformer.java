package gov.cms.bfd.server.war.stu3.providers;

import static java.util.Objects.requireNonNull;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.data.npi.dto.NPIData;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HHAClaimLine;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.BenefitComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.UnsignedIntType;
import org.hl7.fhir.dstu3.model.codesystems.BenefitCategory;
import org.springframework.stereotype.Component;

/** Transforms CCW {@link HHAClaim} instances into FHIR {@link ExplanationOfBenefit} resources. */
@Component
final class HHAClaimTransformer implements ClaimTransformerInterface {
  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The {@link NPIOrgLookup} is to provide what npi Org Name to Lookup to return. */
  private final NPIOrgLookup npiOrgLookup;

  /** The metric name. */
  private static final String METRIC_NAME =
      MetricRegistry.name(HHAClaimTransformer.class.getSimpleName(), "transform");

  /** The securityTagManager. */
  private final SecurityTagManager securityTagManager;

  /**
   * Instantiates a new transformer.
   *
   * <p>Spring will wire this into a singleton bean during the initial component scan, and it will
   * be injected properly into places that need it, so this constructor should only be explicitly
   * called by tests.
   *
   * @param metricRegistry the metric registry
   * @param npiOrgLookup the npi org lookup
   * @param securityTagManager SamhsaSecurityTags lookup
   */
  public HHAClaimTransformer(
      MetricRegistry metricRegistry,
      NPIOrgLookup npiOrgLookup,
      SecurityTagManager securityTagManager) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.npiOrgLookup = requireNonNull(npiOrgLookup);
    this.securityTagManager = requireNonNull(securityTagManager);
  }

  /**
   * Transforms a {@link HHAClaim} into an {@link ExplanationOfBenefit}.
   *
   * @param claim the {@link OutpatientClaim} to use
   * @param includeTaxNumber exists to satisfy {@link ClaimTransformerInterface}; ignored
   * @return a FHIR {@link ExplanationOfBenefit} resource.
   */
  @Trace
  @Override
  public ExplanationOfBenefit transform(Object claim, boolean includeTaxNumber) {
    List<Coding> securityTags = new ArrayList<>();
    if (!(claim instanceof HHAClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob;
    try (Timer.Context ignored = metricRegistry.timer(METRIC_NAME).time()) {
      HHAClaim hhaClaim = (HHAClaim) claim;
      //      List<Coding> securityTags =
      //          securityTagManager.getClaimSecurityLevelDstu3(
      //              String.valueOf(hhaClaim.getClaimId()), HhaTag.class);
      eob = transformClaim(hhaClaim, securityTags);
    }
    return eob;
  }

  /**
   * Transforms a specified {@link HHAClaim} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param claimGroup the CCW {@link HHAClaim} to transform
   * @param securityTags securityTags tag of a claim
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     HHAClaim}
   */
  private ExplanationOfBenefit transformClaim(HHAClaim claimGroup, List<Coding> securityTags) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Common group level fields between all claim types
    TransformerUtils.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.HHA,
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
        ClaimType.HHA,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // set the provider number which is common among several claim types
    TransformerUtils.setProviderNumber(eob, claimGroup.getProviderNumber());

    // Common group level fields between Inpatient, Outpatient Hospice, HHA and SNF
    TransformerUtils.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        claimGroup.getOrganizationNpi(),
        npiOrgLookup
            .retrieveNPIOrgDisplay(claimGroup.getOrganizationNpi())
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

    if (claimGroup.getClaimLUPACode().isPresent()) {
      TransformerUtils.addInformationWithCode(
          eob,
          CcwCodebookVariable.CLM_HHA_LUPA_IND_CD,
          CcwCodebookVariable.CLM_HHA_LUPA_IND_CD,
          claimGroup.getClaimLUPACode());
    }
    if (claimGroup.getClaimReferralCode().isPresent()) {
      TransformerUtils.addInformationWithCode(
          eob,
          CcwCodebookVariable.CLM_HHA_RFRL_CD,
          CcwCodebookVariable.CLM_HHA_RFRL_CD,
          claimGroup.getClaimReferralCode());
    }

    BenefitComponent clmHhaTotVisitCntFinancial =
        TransformerUtils.addBenefitBalanceFinancial(
            eob, BenefitCategory.MEDICAL, CcwCodebookVariable.CLM_HHA_TOT_VISIT_CNT);
    clmHhaTotVisitCntFinancial.setUsed(
        new UnsignedIntType(claimGroup.getTotalVisitCount().intValue()));

    // Common group level fields between Inpatient, HHA, Hospice and SNF
    TransformerUtils.mapEobCommonGroupInpHHAHospiceSNF(
        eob, claimGroup.getCareStartDate(), Optional.empty(), Optional.empty());

    for (HHAClaimLine claimLine : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();
      item.setSequence(claimLine.getLineNumber());
      item.setLocation(new Address().setState((claimGroup.getProviderStateCode())));
      TransformerUtils.addRevCenterAnsiAdjudication(item, eob, claimLine.getRevCntr1stAnsiCd());

      TransformerUtils.mapHcpcs(
          eob,
          item,
          Optional.empty(),
          claimLine.getHcpcsCode(),
          Arrays.asList(
              claimLine.getHcpcsInitialModifierCode(), claimLine.getHcpcsSecondModifierCode()));

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

      // Common item level fields between Outpatient, HHA and Hospice
      TransformerUtils.mapEobCommonItemRevenueOutHHAHospice(
          item, claimLine.getRevenueCenterDate(), claimLine.getPaymentAmount());

      // set revenue center status indicator codes for the claim
      item.getRevenue()
          .addExtension(
              TransformerUtils.createExtensionCoding(
                  eob, CcwCodebookVariable.REV_CNTR_STUS_IND_CD, claimLine.getStatusCode()));

      // Common group level fields between Inpatient, HHA, Hospice and SNF
      TransformerUtils.mapEobCommonGroupInpHHAHospiceSNFCoinsurance(
          eob, item, claimLine.getDeductibleCoinsuranceCd());
    }

    TransformerUtils.setLastUpdated(eob, claimGroup.getLastUpdated());

    eob.getMeta().setSecurity(securityTags);
    return eob;
  }
}
