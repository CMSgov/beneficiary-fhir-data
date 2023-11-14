package gov.cms.bfd.server.war.stu3.providers;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.data.npi.lookup.NPIOrgLookup;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.model.rif.entities.SNFClaimLine;
import gov.cms.bfd.server.war.commons.ClaimType;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.dstu3.model.Period;
import org.springframework.stereotype.Component;

/** Transforms CCW {@link SNFClaim} instances into FHIR {@link ExplanationOfBenefit} resources. */
@Component
public class SNFClaimTransformer implements ClaimTransformerInterface {

  /** The Metric registry. */
  private final MetricRegistry metricRegistry;

  /** The {@link NPIOrgLookup} is to provide what npi Org Name to Lookup to return. */
  private final NPIOrgLookup npiOrgLookup;

  /**
   * Instantiates a new transformer.
   *
   * <p>Spring will wire this into a singleton bean during the initial component scan, and it will
   * be injected properly into places that need it, so this constructor should only be explicitly
   * called by tests.
   *
   * @param metricRegistry the metric registry
   * @param npiOrgLookup the npi org lookup
   */
  public SNFClaimTransformer(MetricRegistry metricRegistry, NPIOrgLookup npiOrgLookup) {
    this.metricRegistry = requireNonNull(metricRegistry);
    this.npiOrgLookup = requireNonNull(npiOrgLookup);
  }

  /**
   * Transforms a claim into an {@link ExplanationOfBenefit}.
   *
   * @param claim the {@link SNFClaim} to use
   * @param includeTaxNumber exists to satisfy {@link ClaimTransformerInterface}; ignored
   * @return a FHIR {@link ExplanationOfBenefit} resource.
   */
  @Trace
  @Override
  public ExplanationOfBenefit transform(Object claim, boolean includeTaxNumber) {
    if (!(claim instanceof SNFClaim)) {
      throw new BadCodeMonkeyException();
    }
    ExplanationOfBenefit eob;
    try (Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(SNFClaimTransformer.class.getSimpleName(), "transform"))
            .time()) {
      eob = transformClaim((SNFClaim) claim);
    }
    return eob;
  }

  /**
   * Transforms a specified {@link SNFClaim} into a FHIR {@link ExplanationOfBenefit}.
   *
   * @param claimGroup the CCW {@link SNFClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     SNFClaim}
   */
  private ExplanationOfBenefit transformClaim(SNFClaim claimGroup) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Common group level fields between all claim types
    TransformerUtils.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.SNF,
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
        ClaimType.SNF,
        Optional.of(claimGroup.getNearLineRecordIdCode()),
        Optional.of(claimGroup.getClaimTypeCode()));

    // set the provider number which is common among several claim types
    TransformerUtils.setProviderNumber(eob, claimGroup.getProviderNumber());

    // add EOB information to fields that are common between the Inpatient and SNF
    // claim types
    TransformerUtils.addCommonEobInformationInpatientSNF(
        eob,
        claimGroup.getAdmissionTypeCd(),
        claimGroup.getSourceAdmissionCd(),
        claimGroup.getNoncoveredStayFromDate(),
        claimGroup.getNoncoveredStayThroughDate(),
        claimGroup.getCoveredCareThroughDate(),
        claimGroup.getMedicareBenefitsExhaustedDate(),
        claimGroup.getDiagnosisRelatedGroupCd());

    if (claimGroup.getPatientStatusCd().isPresent()) {
      TransformerUtils.addInformationWithCode(
          eob,
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          CcwCodebookVariable.NCH_PTNT_STUS_IND_CD,
          claimGroup.getPatientStatusCd().get());
    }

    // Common group level fields between Inpatient, HHA, Hospice and SNF
    TransformerUtils.mapEobCommonGroupInpHHAHospiceSNF(
        eob,
        claimGroup.getClaimAdmissionDate(),
        claimGroup.getBeneficiaryDischargeDate(),
        Optional.of(claimGroup.getUtilizationDayCount()));

    /*
     * add field values to the benefit balances that are common between the
     * Inpatient and SNF claim types
     */
    TransformerUtils.addCommonGroupInpatientSNF(
        eob,
        claimGroup.getCoinsuranceDayCount(),
        claimGroup.getNonUtilizationDayCount(),
        claimGroup.getDeductibleAmount(),
        claimGroup.getPartACoinsuranceLiabilityAmount(),
        claimGroup.getBloodPintsFurnishedQty(),
        claimGroup.getNoncoveredCharge(),
        claimGroup.getTotalDeductionAmount(),
        claimGroup.getClaimPPSCapitalDisproportionateShareAmt(),
        claimGroup.getClaimPPSCapitalExceptionAmount(),
        claimGroup.getClaimPPSCapitalFSPAmount(),
        claimGroup.getClaimPPSCapitalIMEAmount(),
        claimGroup.getClaimPPSCapitalOutlierAmount(),
        claimGroup.getClaimPPSOldCapitalHoldHarmlessAmount());

    if (claimGroup.getQualifiedStayFromDate().isPresent()
        || claimGroup.getQualifiedStayThroughDate().isPresent()) {
      CommonTransformerUtils.validatePeriodDates(
          claimGroup.getQualifiedStayFromDate(), claimGroup.getQualifiedStayThroughDate());
      SupportingInformationComponent nchQlfydStayInfo =
          TransformerUtils.addInformation(eob, CcwCodebookVariable.NCH_QLFYD_STAY_FROM_DT);
      Period nchQlfydStayPeriod = new Period();
      if (claimGroup.getQualifiedStayFromDate().isPresent())
        nchQlfydStayPeriod.setStart(
            CommonTransformerUtils.convertToDate((claimGroup.getQualifiedStayFromDate().get())),
            TemporalPrecisionEnum.DAY);
      if (claimGroup.getQualifiedStayThroughDate().isPresent())
        nchQlfydStayPeriod.setEnd(
            CommonTransformerUtils.convertToDate((claimGroup.getQualifiedStayThroughDate().get())),
            TemporalPrecisionEnum.DAY);
      nchQlfydStayInfo.setTiming(nchQlfydStayPeriod);
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
        npiOrgLookup.retrieveNPIOrgDisplay(claimGroup.getOrganizationNpi()),
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

    // Handle Procedures
    TransformerUtils.extractCCWProcedures(
            claimGroup.getProcedureCodes(),
            claimGroup.getProcedureCodeVersions(),
            claimGroup.getProcedureDates())
        .forEach(p -> TransformerUtils.addProcedureCode(eob, p));

    for (SNFClaimLine claimLine : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();
      item.setSequence(claimLine.getLineNumber());

      item.setLocation(new Address().setState((claimGroup.getProviderStateCode())));

      TransformerUtils.mapHcpcs(
          eob, item, Optional.empty(), claimLine.getHcpcsCode(), Collections.emptyList());

      // Common item level fields between Inpatient, Outpatient, HHA, Hospice and SNF
      TransformerUtils.mapEobCommonItemRevenue(
          item,
          eob,
          claimLine.getRevenueCenter(),
          claimLine.getRateAmount(),
          claimLine.getTotalChargeAmount(),
          claimLine.getNonCoveredChargeAmount(),
          BigDecimal.valueOf(claimLine.getUnitCount()),
          claimLine.getNationalDrugCodeQuantity(),
          claimLine.getNationalDrugCodeQualifierCode(),
          claimLine.getRevenueCenterRenderingPhysicianNPI());

      // Common group level field coinsurance between Inpatient, HHA, Hospice and SNF
      TransformerUtils.mapEobCommonGroupInpHHAHospiceSNFCoinsurance(
          eob, item, claimLine.getDeductibleCoinsuranceCd());
    }
    TransformerUtils.setLastUpdated(eob, claimGroup.getLastUpdated());
    return eob;
  }
}
