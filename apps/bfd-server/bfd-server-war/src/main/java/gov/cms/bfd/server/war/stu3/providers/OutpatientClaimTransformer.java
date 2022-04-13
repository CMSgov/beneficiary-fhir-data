package gov.cms.bfd.server.war.stu3.providers;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.newrelic.api.agent.Trace;
import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaimLine;
import gov.cms.bfd.server.war.commons.CCWProcedure;
import gov.cms.bfd.server.war.commons.Diagnosis;
import gov.cms.bfd.server.war.commons.Diagnosis.DiagnosisLabel;
import gov.cms.bfd.server.war.commons.FdaDrugCodeDisplayLookup;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.Arrays;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Address;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit.ItemComponent;

/**
 * Transforms CCW {@link OutpatientClaim} instances into FHIR {@link ExplanationOfBenefit}
 * resources.
 */
final class OutpatientClaimTransformer {
  /**
   * @param metricRegistry the {@link MetricRegistry} to use
   * @param claim the CCW {@link OutpatientClaim} to transform
   * @param includeTaxNumbers whether or not to include tax numbers in the result (see {@link
   *     ExplanationOfBenefitResourceProvider#HEADER_NAME_INCLUDE_TAX_NUMBERS}, defaults to <code>
   *     false</code>)
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     OutpatientClaim}
   */
  @Trace
  static ExplanationOfBenefit transform(
      MetricRegistry metricRegistry,
      Object claim,
      Optional<Boolean> includeTaxNumbers,
      FdaDrugCodeDisplayLookup drugCodeDisplayLookup) {
    Timer.Context timer =
        metricRegistry
            .timer(
                MetricRegistry.name(OutpatientClaimTransformer.class.getSimpleName(), "transform"))
            .time();

    if (!(claim instanceof OutpatientClaim)) throw new BadCodeMonkeyException();
    ExplanationOfBenefit eob = transformClaim((OutpatientClaim) claim);

    timer.stop();
    return eob;
  }

  /**
   * @param claimGroup the CCW {@link OutpatientClaim} to transform
   * @return a FHIR {@link ExplanationOfBenefit} resource that represents the specified {@link
   *     OutpatientClaim}
   */
  private static ExplanationOfBenefit transformClaim(OutpatientClaim claimGroup) {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    // Common group level fields between all claim types
    TransformerUtils.mapEobCommonClaimHeaderData(
        eob,
        claimGroup.getClaimId(),
        claimGroup.getBeneficiaryId(),
        ClaimType.OUTPATIENT,
        claimGroup.getClaimGroupId().toPlainString(),
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

    if (claimGroup.getDiagnosisAdmission1Code().isPresent())
      TransformerUtils.addDiagnosisCode(
          eob,
          Diagnosis.from(
                  claimGroup.getDiagnosisAdmission1Code(),
                  claimGroup.getDiagnosisAdmission1CodeVersion(),
                  DiagnosisLabel.REASONFORVISIT)
              .get());
    if (claimGroup.getDiagnosisAdmission2Code().isPresent())
      TransformerUtils.addDiagnosisCode(
          eob,
          Diagnosis.from(
                  claimGroup.getDiagnosisAdmission2Code(),
                  claimGroup.getDiagnosisAdmission2CodeVersion(),
                  DiagnosisLabel.REASONFORVISIT)
              .get());

    if (claimGroup.getDiagnosisAdmission3Code().isPresent())
      TransformerUtils.addDiagnosisCode(
          eob,
          Diagnosis.from(
                  claimGroup.getDiagnosisAdmission3Code(),
                  claimGroup.getDiagnosisAdmission3CodeVersion(),
                  DiagnosisLabel.REASONFORVISIT)
              .get());

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

    for (OutpatientClaimLine claimLine : claimGroup.getLines()) {
      ItemComponent item = eob.addItem();
      item.setSequence(claimLine.getLineNumber().intValue());

      item.setLocation(new Address().setState((claimGroup.getProviderStateCode())));

      if (claimLine.getRevCntr1stAnsiCd().isPresent()) {
        item.addAdjudication()
            .setCategory(
                TransformerUtils.createAdjudicationCategory(
                    CcwCodebookVariable.REV_CNTR_1ST_ANSI_CD))
            .setReason(
                TransformerUtils.createCodeableConcept(
                    eob,
                    CcwCodebookVariable.REV_CNTR_1ST_ANSI_CD,
                    claimLine.getRevCntr1stAnsiCd()));
      }
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
    return eob;
  }
}
