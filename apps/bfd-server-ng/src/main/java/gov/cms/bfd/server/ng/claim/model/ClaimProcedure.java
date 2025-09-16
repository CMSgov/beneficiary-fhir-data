package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.converter.NonZeroIntConverter;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Procedure and diagnosis info. */
@Embeddable
public class ClaimProcedure {

  @Convert(converter = NonZeroIntConverter.class)
  @Column(name = "clm_val_sqnc_num_prod")
  private Optional<Integer> sequenceNumber;

  @Column(name = "clm_prcdr_prfrm_dt")
  private Optional<LocalDate> procedureDate;

  @Column(name = "clm_dgns_prcdr_icd_ind")
  private Optional<IcdIndicator> icdIndicator;

  @Column(name = "clm_prcdr_cd")
  private Optional<String> procedureCode;

  @Column(name = "clm_prod_type_cd")
  private Optional<ClaimDiagnosisType> diagnosisType;

  @Column(name = "clm_poa_ind")
  private Optional<String> claimPoaIndicator;

  @Column(name = "clm_dgns_cd")
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

    procedure.setProcedure(
        new CodeableConcept(
            new Coding()
                .setSystem(icdIndicator.get().getProcedureSystem())
                .setCode(procedureCode.get())));

    return Optional.of(procedure);
  }

  Optional<ExplanationOfBenefit.DiagnosisComponent> toFhirDiagnosis(int bfdRowId) {
    if (diagnosisCode.isEmpty()) {
      return Optional.empty();
    }
    var diagnosis = new ExplanationOfBenefit.DiagnosisComponent();
    diagnosis.setSequence(bfdRowId);
    diagnosisType.ifPresent(
        d -> {
          diagnosis.addType(
              new CodeableConcept(new Coding().setSystem(d.getSystem()).setCode(d.getFhirCode())));
        });

    String formattedCode = icdIndicator.get().formatCode(diagnosisCode.get());
    diagnosis.setDiagnosis(
        new CodeableConcept(
            new Coding().setSystem(icdIndicator.get().getDiagnosisSytem()).setCode(formattedCode)));

    this.claimPoaIndicator.ifPresent(
        poaCode -> {
          var onAdmissionConcept = new CodeableConcept();
          onAdmissionConcept.addCoding().setSystem(SystemUrls.POA_CODING).setCode(poaCode);
          diagnosis.setOnAdmission(onAdmissionConcept);
        });

    return Optional.of(diagnosis);
  }
}
