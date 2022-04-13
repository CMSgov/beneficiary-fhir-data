package gov.cms.bfd.server.war.stu3.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.HospiceClaim;
import gov.cms.bfd.model.rif.HospiceClaimLine;
import gov.cms.bfd.server.war.FDADrugUtils;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Arrays;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;

/**
 * Transforms CCW {@link HospiceClaim} instances into FHIR {@link ExplanationOfBenefit} resources.
 */
final class HospiceClaimTransformer {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the CCW {@link HospiceClaim} to transform
   * @param includeTaxNumbers whether or not to include tax numbers in the result (see {@link
   *     ExplanationOfBenefitResourceProvider#HEADER_NAME_INCLUDE_TAX_NUMBERS}, defaults to <code>
   *     false</code>)
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     HospiceClaim}
   */
  @Trace
  static ExplanationOfBenefit transform(
      MetricRegistry metricRegistry,
      Object claim,
      Optional<Boolean> includeTaxNumbers,
      FDADrugUtils drugCodeProvider) {
    Timer.Context timer =
        metricRegistry
            .timer(MetricRegistry.name(HospiceClaimTransformer.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof HospiceClaim)) throw new BadCodeMonkeyException();
    ExplanationOfBenefit eob = transformClaim((HospiceClaim) claim);

    timer.stop();
    return eob;
  }

  /**
   * @param claimGroup the CCW {@link HospiceClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     HospiceClaim}
   */
  private static ExplanationOfBenefit transformClaim(HospiceClaim claimGroup) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Common group level fields between all claim types
    TransformerUtils.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.HOSPICE,
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

    for (HospiceClaimLine claimLine : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();
      item.setSequence(claimLine.getLineNumber().intValue());

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
    return eob;
  }
}
