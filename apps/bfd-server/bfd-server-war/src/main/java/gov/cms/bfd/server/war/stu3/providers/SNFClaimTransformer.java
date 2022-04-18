package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.SNFClaim;
import gov.cms.bfd.model.rif.SNFClaimLine;
import gov.cms.bfd.server.war.commons.CCWProcedure;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.SupportingInformationComponent;
import org.hl7.fhir.dstu3.model.Period;

/** Transforms CCW {@link SNFClaim} instances into FHIR {@link ExplanationOfBenefit} resources. */
final class SNFClaimTransformer {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the CCW {@link SNFClaim} to transform
   * @param includeTaxNumbers whether or not to include tax numbers in the result (see {@link
   *     ExplanationOfBenefitResourceProvider#HEADER_NAME_INCLUDE_TAX_NUMBERS}, defaults to <code>
   *     false</code>)
   * @param drugCodeDisplayLookup the {@FdaDrugCodeDisplayLookup } to return FDA Drug Codes
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     SNFClaim}
   */
  @Trace
  static ExplanationOfBenefit transform(
      MetricRegistry metricRegistry,
      Object claim,
      Optional<Boolean> includeTaxNumbers,
      FdaDrugCodeDisplayLookup drugCodeDisplayLookup) {
    Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(SNFClaimTransformer.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof SNFClaim)) throw new BadCodeMonkeyException();

    timer.stop();
    return transformClaim((SNFClaim) claim);
  }

  /**
   * @param claimGroup the CCW {@link SNFClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     SNFClaim}
   */
  private static ExplanationOfBenefit transformClaim(SNFClaim claimGroup) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Common group level fields between all claim types
    TransformerUtils.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.SNF,
        claimGroup.getClaimGroupId().toPlainString(),
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

    // add EOB information to fields that are common between the Inpatient and SNF claim types
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
      TransformerUtils.validatePeriodDates(
          claimGroup.getQualifiedStayFromDate(), claimGroup.getQualifiedStayThroughDate());
      SupportingInformationComponent nchQlfydStayInfo =
          TransformerUtils.addInformation(eob, CcwCodebookVariable.NCH_QLFYD_STAY_FROM_DT);
      Period nchQlfydStayPeriod = new Period();
      if (claimGroup.getQualifiedStayFromDate().isPresent())
        nchQlfydStayPeriod.setStart(
            TransformerUtils.convertToDate((claimGroup.getQualifiedStayFromDate().get())),
            TemporalPrecisionEnum.DAY);
      if (claimGroup.getQualifiedStayThroughDate().isPresent())
        nchQlfydStayPeriod.setEnd(
            TransformerUtils.convertToDate((claimGroup.getQualifiedStayThroughDate().get())),
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

    for (Diagnosis diagnosis :
        TransformerUtils.extractDiagnoses1Thru12(
            claimGroup.getDiagnosisAdmittingCode(),
            claimGroup.getDiagnosisAdmittingCodeVersion(),
            claimGroup.getDiagnosisPrincipalCode(),
            claimGroup.getDiagnosisPrincipalCodeVersion(),
            claimGroup.getDiagnosis1Code(),
            claimGroup.getDiagnosis1CodeVersion(),
            claimGroup.getDiagnosis2Code(),
            claimGroup.getDiagnosis2CodeVersion(),
            claimGroup.getDiagnosis3Code(),
            claimGroup.getDiagnosis3CodeVersion(),
            claimGroup.getDiagnosis4Code(),
            claimGroup.getDiagnosis4CodeVersion(),
            claimGroup.getDiagnosis5Code(),
            claimGroup.getDiagnosis5CodeVersion(),
            claimGroup.getDiagnosis6Code(),
            claimGroup.getDiagnosis6CodeVersion(),
            claimGroup.getDiagnosis7Code(),
            claimGroup.getDiagnosis7CodeVersion(),
            claimGroup.getDiagnosis8Code(),
            claimGroup.getDiagnosis8CodeVersion(),
            claimGroup.getDiagnosis9Code(),
            claimGroup.getDiagnosis9CodeVersion(),
            claimGroup.getDiagnosis10Code(),
            claimGroup.getDiagnosis10CodeVersion(),
            claimGroup.getDiagnosis11Code(),
            claimGroup.getDiagnosis11CodeVersion(),
            claimGroup.getDiagnosis12Code(),
            claimGroup.getDiagnosis12CodeVersion()))
      TransformerUtils.addDiagnosisCode(eob, diagnosis);

    for (Diagnosis diagnosis :
        TransformerUtils.extractDiagnoses13Thru25(
            claimGroup.getDiagnosis13Code(),
            claimGroup.getDiagnosis13CodeVersion(),
            claimGroup.getDiagnosis14Code(),
            claimGroup.getDiagnosis14CodeVersion(),
            claimGroup.getDiagnosis15Code(),
            claimGroup.getDiagnosis15CodeVersion(),
            claimGroup.getDiagnosis16Code(),
            claimGroup.getDiagnosis16CodeVersion(),
            claimGroup.getDiagnosis17Code(),
            claimGroup.getDiagnosis17CodeVersion(),
            claimGroup.getDiagnosis18Code(),
            claimGroup.getDiagnosis18CodeVersion(),
            claimGroup.getDiagnosis19Code(),
            claimGroup.getDiagnosis19CodeVersion(),
            claimGroup.getDiagnosis20Code(),
            claimGroup.getDiagnosis20CodeVersion(),
            claimGroup.getDiagnosis21Code(),
            claimGroup.getDiagnosis21CodeVersion(),
            claimGroup.getDiagnosis22Code(),
            claimGroup.getDiagnosis22CodeVersion(),
            claimGroup.getDiagnosis23Code(),
            claimGroup.getDiagnosis23CodeVersion(),
            claimGroup.getDiagnosis24Code(),
            claimGroup.getDiagnosis24CodeVersion(),
            claimGroup.getDiagnosis25Code(),
            claimGroup.getDiagnosis25CodeVersion()))
      TransformerUtils.addDiagnosisCode(eob, diagnosis);

    for (Diagnosis diagnosis :
        TransformerUtils.extractExternalDiagnoses1Thru12(
            claimGroup.getDiagnosisExternalFirstCode(),
                claimGroup.getDiagnosisExternalFirstCodeVersion(),
            claimGroup.getDiagnosisExternal1Code(), claimGroup.getDiagnosisExternal1CodeVersion(),
            claimGroup.getDiagnosisExternal2Code(), claimGroup.getDiagnosisExternal2CodeVersion(),
            claimGroup.getDiagnosisExternal3Code(), claimGroup.getDiagnosisExternal3CodeVersion(),
            claimGroup.getDiagnosisExternal4Code(), claimGroup.getDiagnosisExternal4CodeVersion(),
            claimGroup.getDiagnosisExternal5Code(), claimGroup.getDiagnosisExternal5CodeVersion(),
            claimGroup.getDiagnosisExternal6Code(), claimGroup.getDiagnosisExternal6CodeVersion(),
            claimGroup.getDiagnosisExternal7Code(), claimGroup.getDiagnosisExternal7CodeVersion(),
            claimGroup.getDiagnosisExternal8Code(), claimGroup.getDiagnosisExternal8CodeVersion(),
            claimGroup.getDiagnosisExternal9Code(), claimGroup.getDiagnosisExternal9CodeVersion(),
            claimGroup.getDiagnosisExternal10Code(), claimGroup.getDiagnosisExternal10CodeVersion(),
            claimGroup.getDiagnosisExternal11Code(), claimGroup.getDiagnosisExternal11CodeVersion(),
            claimGroup.getDiagnosisExternal12Code(),
                claimGroup.getDiagnosisExternal12CodeVersion()))
      TransformerUtils.addDiagnosisCode(eob, diagnosis);

    for (CCWProcedure procedure :
        TransformerUtils.extractCCWProcedures(
            claimGroup.getProcedure1Code(),
            claimGroup.getProcedure1CodeVersion(),
            claimGroup.getProcedure1Date(),
            claimGroup.getProcedure2Code(),
            claimGroup.getProcedure2CodeVersion(),
            claimGroup.getProcedure2Date(),
            claimGroup.getProcedure3Code(),
            claimGroup.getProcedure3CodeVersion(),
            claimGroup.getProcedure3Date(),
            claimGroup.getProcedure4Code(),
            claimGroup.getProcedure4CodeVersion(),
            claimGroup.getProcedure4Date(),
            claimGroup.getProcedure5Code(),
            claimGroup.getProcedure5CodeVersion(),
            claimGroup.getProcedure5Date(),
            claimGroup.getProcedure6Code(),
            claimGroup.getProcedure6CodeVersion(),
            claimGroup.getProcedure6Date(),
            claimGroup.getProcedure7Code(),
            claimGroup.getProcedure7CodeVersion(),
            claimGroup.getProcedure7Date(),
            claimGroup.getProcedure8Code(),
            claimGroup.getProcedure8CodeVersion(),
            claimGroup.getProcedure8Date(),
            claimGroup.getProcedure9Code(),
            claimGroup.getProcedure9CodeVersion(),
            claimGroup.getProcedure9Date(),
            claimGroup.getProcedure10Code(),
            claimGroup.getProcedure10CodeVersion(),
            claimGroup.getProcedure10Date(),
            claimGroup.getProcedure11Code(),
            claimGroup.getProcedure11CodeVersion(),
            claimGroup.getProcedure11Date(),
            claimGroup.getProcedure12Code(),
            claimGroup.getProcedure12CodeVersion(),
            claimGroup.getProcedure12Date(),
            claimGroup.getProcedure13Code(),
            claimGroup.getProcedure13CodeVersion(),
            claimGroup.getProcedure13Date(),
            claimGroup.getProcedure14Code(),
            claimGroup.getProcedure14CodeVersion(),
            claimGroup.getProcedure14Date(),
            claimGroup.getProcedure15Code(),
            claimGroup.getProcedure15CodeVersion(),
            claimGroup.getProcedure15Date(),
            claimGroup.getProcedure16Code(),
            claimGroup.getProcedure16CodeVersion(),
            claimGroup.getProcedure16Date(),
            claimGroup.getProcedure17Code(),
            claimGroup.getProcedure17CodeVersion(),
            claimGroup.getProcedure17Date(),
            claimGroup.getProcedure18Code(),
            claimGroup.getProcedure18CodeVersion(),
            claimGroup.getProcedure18Date(),
            claimGroup.getProcedure19Code(),
            claimGroup.getProcedure19CodeVersion(),
            claimGroup.getProcedure19Date(),
            claimGroup.getProcedure20Code(),
            claimGroup.getProcedure20CodeVersion(),
            claimGroup.getProcedure20Date(),
            claimGroup.getProcedure21Code(),
            claimGroup.getProcedure21CodeVersion(),
            claimGroup.getProcedure21Date(),
            claimGroup.getProcedure22Code(),
            claimGroup.getProcedure22CodeVersion(),
            claimGroup.getProcedure22Date(),
            claimGroup.getProcedure23Code(),
            claimGroup.getProcedure23CodeVersion(),
            claimGroup.getProcedure23Date(),
            claimGroup.getProcedure24Code(),
            claimGroup.getProcedure24CodeVersion(),
            claimGroup.getProcedure24Date(),
            claimGroup.getProcedure25Code(),
            claimGroup.getProcedure25CodeVersion(),
            claimGroup.getProcedure25Date())) TransformerUtils.addProcedureCode(eob, procedure);

    for (SNFClaimLine claimLine : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();
      item.setSequence(claimLine.getLineNumber().intValue());

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
