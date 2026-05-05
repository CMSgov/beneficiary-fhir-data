package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Procedure and diagnosis info. */
@Embeddable
@Getter
public class ClaimProcedureProfessional extends ClaimProcedureBase {

  @Override
  public Optional<ExplanationOfBenefit.DiagnosisComponent> toFhirDiagnosis(
      SequenceGenerator sequenceGenerator) {
    if (getDiagnosisType()
        .filter(type -> type == ClaimDiagnosisType.BASE_DIAGNOSIS_CODE)
        .isEmpty()) {
      return Optional.empty();
    }
    var type =
        getSequenceNumber()
            .filter(sequence -> sequence == 1)
            .map(_ -> "principal")
            .orElse("secondary");
    return buildBaseDiagnosis(
        sequenceGenerator, type, ClaimDiagnosisType.BASE_DIAGNOSIS_CODE.getSystem());
  }
}
