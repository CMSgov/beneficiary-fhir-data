package gov.cms.bfd.server.ng.claim.model.professional;

import gov.cms.bfd.server.ng.claim.model.common.ClaimDiagnosisType;
import gov.cms.bfd.server.ng.claim.model.common.ClaimProcedureBase;
import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;

/** Procedure and diagnosis info. */
@Embeddable
@Getter
class ClaimProcedureProfessional extends ClaimProcedureBase {

  @Override
  protected Optional<ExplanationOfBenefit.DiagnosisComponent> toFhirDiagnosis(
      SequenceGenerator sequenceGenerator) {
    return getDiagnosisType()
        .filter(type -> type == ClaimDiagnosisType.BASE_DIAGNOSIS_CODE)
        .flatMap(
            _ -> {
              var isPrincipal = getSequenceNumber().filter(s -> s == 1).isPresent();
              var typeCode = isPrincipal ? "principal" : "secondary";
              var typeSystem =
                  isPrincipal
                      ? ClaimDiagnosisType.PRINCIPAL.getSystem()
                      : ClaimDiagnosisType.BASE_DIAGNOSIS_CODE.getSystem();
              return buildBaseDiagnosis(sequenceGenerator, typeCode, typeSystem);
            });
  }
}
