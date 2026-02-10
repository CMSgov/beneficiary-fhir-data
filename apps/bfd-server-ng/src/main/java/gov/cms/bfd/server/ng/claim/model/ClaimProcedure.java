package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
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
public class ClaimProcedure extends ClaimProcedureBase {

  @Column(name = "clm_prcdr_prfrm_dt")
  private Optional<LocalDate> procedureDate;

  @Column(name = "clm_prcdr_cd") // SAMHSA
  private Optional<String> procedureCode;

  @Column(name = "clm_poa_ind")
  private Optional<String> claimPoaIndicator;

  private static final LocalDate DEFAULT_PROCEDURE_DATE = LocalDate.of(2000, 1, 1);

  @Override
  Optional<ExplanationOfBenefit.ProcedureComponent> toFhirProcedure() {
    if (procedureCode.isEmpty() || getSequenceNumber().isEmpty() || getIcdIndicator().isEmpty()) {
      return Optional.empty();
    }
    var procedure = new ExplanationOfBenefit.ProcedureComponent();
    procedure.setSequence(getSequenceNumber().get());
    var code = getSequenceNumber().get() == 1 ? "principal" : "other";
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

    String formattedProcedureCode =
        getIcdIndicator().get().formatProcedureCode(procedureCode.get());
    procedure.setProcedure(
        new CodeableConcept(
            new Coding()
                .setSystem(getIcdIndicator().get().getProcedureSystem())
                .setCode(formattedProcedureCode)));

    return Optional.of(procedure);
  }

  @Override
  Optional<ExplanationOfBenefit.DiagnosisComponent> toFhirDiagnosis(
      SequenceGenerator sequenceGenerator, ClaimContext claimContext) {
    var diagnosis = super.toFhirDiagnosis(sequenceGenerator, claimContext);

    diagnosis.ifPresent(
        diagnosisComponent ->
            this.claimPoaIndicator.ifPresent(
                poaCode -> {
                  var onAdmissionConcept = new CodeableConcept();
                  poaCode
                      .chars()
                      .forEach(
                          c ->
                              onAdmissionConcept
                                  .addCoding()
                                  .setSystem(SystemUrls.POA_CODING)
                                  .setCode(Character.toString(c)));
                  diagnosisComponent.setOnAdmission(onAdmissionConcept);
                }));

    return diagnosis;
  }
}
