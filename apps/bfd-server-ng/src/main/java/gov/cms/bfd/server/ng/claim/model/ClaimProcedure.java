package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Procedure and diagnosis info. */
@Embeddable
@Getter
public class ClaimProcedure {

  @Convert(converter = NonZeroIntConverter.class)
  @Column(name = "clm_val_sqnc_num_prod")
  private Optional<Integer> sequenceNumber;

  @Column(name = "clm_prcdr_prfrm_dt")
  private Optional<LocalDate> procedureDate;

  @Column(name = "clm_dgns_prcdr_icd_ind")
  private Optional<IcdIndicator> icdIndicator;

  @Column(name = "clm_prcdr_cd") // SAMHSA
  private Optional<String> procedureCode;

  @Column(name = "clm_prod_type_cd")
  private Optional<ClaimDiagnosisType> diagnosisType;

  @Column(name = "clm_poa_ind")
  private Optional<String> claimPoaIndicator;

  @Column(name = "clm_dgns_cd") // SAMHSA
  private Optional<String> diagnosisCode;

  private static final LocalDate DEFAULT_PROCEDURE_DATE = LocalDate.of(2000, 1, 1);

  Optional<ExplanationOfBenefit.ProcedureComponent> toFhirProcedure() {
    if (procedureCode.isEmpty() || sequenceNumber.isEmpty() || icdIndicator.isEmpty()) {
      return Optional.empty();
    }
    var procedure = new ExplanationOfBenefit.ProcedureComponent();
    procedure.setSequence(sequenceNumber.get());
    var code = sequenceNumber.get() == 1 ? "principal" : "other";
    procedure.addType(
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.CARIN_CODE_SYSTEM_CLAIM_PROCEDURE_TYPE)
                    .setCode(code)));
    procedureDate.ifPresent(
        d -> {
          if (d.isAfter(DEFAULT_PROCEDURE_DATE)) {
            procedure.setDateElement(DateUtil.toFhirDate(d));
          }
        });

    String formattedProcedureCode = icdIndicator.get().formatProcedureCode(procedureCode.get());
    procedure.setProcedure(
        new CodeableConcept(
            new Coding()
                .setSystem(icdIndicator.get().getProcedureSystem())
                .setCode(formattedProcedureCode)));

    return Optional.of(procedure);
  }

  Optional<ExplanationOfBenefit.DiagnosisComponent> toFhirDiagnosis(
      int bfdRowId, ClaimTypeCode claimTypeCode) {
    if (diagnosisCode.isEmpty()) {
      return Optional.empty();
    }
    ClaimContext claimContext;
    if (claimTypeCode.isBetween(5, 69)
        || claimTypeCode.isBetween(2000, 2699)
        || claimTypeCode.isBetween(1000, 1699)) {
      claimContext = ClaimContext.INSTITUTIONAL;
    } else if (claimTypeCode.isBetween(71, 82)
        || claimTypeCode.isBetween(2700, 2899)
        || claimTypeCode.isBetween(1700, 1899)) {
      claimContext = ClaimContext.PROFESSIONAL;
    } else {
      return Optional.empty();
    }
    var diagnosis = new ExplanationOfBenefit.DiagnosisComponent();
    diagnosis.setSequence(bfdRowId);

    diagnosisType.ifPresent(
        d ->
            diagnosis.addType(
                new CodeableConcept(
                    new Coding().setSystem(d.getSystem()).setCode(d.getFhirCode(claimContext)))));

    String formattedCode = icdIndicator.get().formatDiagnosisCode(diagnosisCode.get());
    diagnosis.setDiagnosis(
        new CodeableConcept(
            new Coding()
                .setSystem(icdIndicator.get().getDiagnosisSystem())
                .setCode(formattedCode)));

    this.claimPoaIndicator.ifPresent(
        poaCode -> {
          var onAdmissionConcept = new CodeableConcept();
          onAdmissionConcept.addCoding().setSystem(SystemUrls.POA_CODING).setCode(poaCode);
          diagnosis.setOnAdmission(onAdmissionConcept);
        });

    return Optional.of(diagnosis);
  }
}
