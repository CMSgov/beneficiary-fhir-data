package gov.cms.bfd.server.ng.eob;

import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.claim.model.PatientReferenceFactory;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EobHandler {
  private BeneficiaryRepository beneficiaryRepository;
  private ClaimRepository claimRepository;

  /**
   * Returns a {@link Patient} by their {@link IdType}.
   *
   * @param fhirId FHIR ID
   * @return patient
   */
  public Optional<ExplanationOfBenefit> find(final Long fhirId) {
    var claim = claimRepository.findById(fhirId);

    return claim.map(
        c -> {
          var claimProcedures = claimRepository.getClaimProcedures(List.of(c.getClaimUniqueId()));
          var beneXrefSk = beneficiaryRepository.getXrefBeneSk(c.getBeneSk()).get();
          var eob = c.toFhir();
          eob.setPatient(PatientReferenceFactory.toFhir(beneXrefSk));
          for (var procedure : claimProcedures) {
            procedure.toFhirProcedure().ifPresent(eob::addProcedure);
            procedure.toFhirDiagnosis().ifPresent(eob::addDiagnosis);
          }
          return eob;
        });
  }
}
