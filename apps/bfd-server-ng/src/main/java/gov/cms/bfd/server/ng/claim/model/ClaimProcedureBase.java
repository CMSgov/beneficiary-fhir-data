package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.MappedSuperclass;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Procedure and diagnosis info. */
@Getter
@MappedSuperclass
public abstract class ClaimProcedureBase {

  @Convert(converter = NonZeroIntConverter.class)
  @Column(name = "clm_val_sqnc_num_prod")
  private Optional<Integer> sequenceNumber;

  @Column(name = "clm_dgns_prcdr_icd_ind")
  private Optional<IcdIndicator> icdIndicator;

  @Column(name = "clm_prod_type_cd")
  private Optional<ClaimDiagnosisType> diagnosisType;

  @Column(name = "clm_dgns_cd") // SAMHSA
  private Optional<String> diagnosisCode;

  //  private static final LocalDate DEFAULT_PROCEDURE_DATE = LocalDate.of(2000, 1, 1);

  Optional<String> getDiagnosisKey() {
    return diagnosisCode.map(
        s -> s + "|" + getIcdIndicator().map(IcdIndicator::getCode).orElse(""));
  }

  Optional<Integer> getDiagnosisPriority(ClaimContext claimContext) {
    return diagnosisType.map(d -> d.getPriority(claimContext));
  }

  Optional<ExplanationOfBenefit.ProcedureComponent> toFhirProcedure() {
    return Optional.empty();
  }

  Optional<ExplanationOfBenefit.DiagnosisComponent> toFhirDiagnosis(
      SequenceGenerator sequenceGenerator, ClaimContext claimContext) {
    if (diagnosisCode.isEmpty()) {
      return Optional.empty();
    }

    var diagnosis = new ExplanationOfBenefit.DiagnosisComponent();
    diagnosis.setSequence(sequenceGenerator.next());

    diagnosisType.ifPresent(
        d ->
            diagnosis.addType(
                new CodeableConcept(
                    new Coding().setSystem(d.getSystem()).setCode(d.getFhirCode(claimContext)))));

    var formattedCode = icdIndicator.get().formatDiagnosisCode(diagnosisCode.get());
    diagnosis.setDiagnosis(
        new CodeableConcept(
            new Coding()
                .setSystem(icdIndicator.get().getDiagnosisSystem())
                .setCode(formattedCode)));

    return Optional.of(diagnosis);
  }

  void setClaimPoaIndicator(String poaIndicator) {}

  public Optional<String> getProcedureCode() {
    return Optional.empty();
  }
}
