package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

@Entity
@Table(name = "claim_procedure", schema = "idr")
public class ClaimProcedure {
  @EmbeddedId ClaimProcedureId claimProcedureId;

  @Column(name = "clm_val_sqnc_num")
  private int sequenceNumber;

  @Column(name = "clm_prcdr_prfrm_dt")
  private LocalDate procedureDate;

  @Column(name = "clm_dgns_prcdr_icd_ind")
  private IcdIndicator icdIndicator;

  @Column(name = "clm_prcdr_cd")
  private Optional<String> procedureCode;

  @Column(name = "clm_prod_type_cd")
  private ClaimDiagnosisType diagnosisType;

  @Column(name = "clm_poa_ind")
  private Optional<String> claimPoaIndicator;

  @Column(name = "clm_dgns_cd")
  private Optional<String> diagnosisCode;

  @ManyToOne
  @JoinColumn(name = "clm_uniq_id")
  private Claim claim;

  Optional<ExplanationOfBenefit.ProcedureComponent> toFhirProcedure() {
    if (procedureCode.isEmpty()) {
      return Optional.empty();
    }
    var procedure = new ExplanationOfBenefit.ProcedureComponent();
    procedure.setSequence(sequenceNumber);
    var code = sequenceNumber == 1 ? "principal" : "other";
    procedure.addType(
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(SystemUrls.CARIN_CODE_SYSTEM_CLAIM_PROCEDURE_TYPE)
                    .setCode(code)));
    if (procedureDate.isAfter(LocalDate.of(2000, 1, 1))) {
      procedure.setDate(DateUtil.toDate(procedureDate));
    }
    procedure.setProcedure(
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(icdIndicator.getProcedureSystem())
                    .setCode(procedureCode.get())));

    return Optional.of(procedure);
  }

  Optional<ExplanationOfBenefit.DiagnosisComponent> toFhirDiagnosis() {
    if (diagnosisCode.isEmpty()) {
      return Optional.empty();
    }
    var diagnosis = new ExplanationOfBenefit.DiagnosisComponent();
    diagnosis.setSequence(claimProcedureId.getRowNumber());
    diagnosis.addType(
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(diagnosisType.getSystem())
                    .setCode(diagnosisType.getFhirCode())));
    diagnosis.setDiagnosis(
        new CodeableConcept()
            .addCoding(
                new Coding()
                    .setSystem(icdIndicator.getDiagnosisSytem())
                    .setCode(diagnosisCode.get())));

    return Optional.of(diagnosis);
  }
}
