package gov.cms.bfd.server.ng.eob;

import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.claim.ClaimRepository;
import gov.cms.bfd.server.ng.claim.model.PatientReferenceFactory;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

/**
 * Handler methods for the ExplanationOfBenefit resource. This is called after the FHIR inputs from
 * the resource provider are converted into input types that are easier to work with.
 */
@Component
@RequiredArgsConstructor
public class EobHandler {
  private final BeneficiaryRepository beneficiaryRepository;
  private final ClaimRepository claimRepository;

  /**
   * Returns a {@link Patient} by their {@link IdType}.
   *
   * @param fhirId FHIR ID
   * @return patient
   */
  public Optional<ExplanationOfBenefit> find(final Long fhirId) {
    return searchByIdInner(fhirId, new DateTimeRange(), new DateTimeRange());
  }

  public Bundle searchByBene(
      Long beneSk,
      Optional<Integer> count,
      DateTimeRange serviceDate,
      DateTimeRange lastUpdated,
      Optional<Integer> startIndex) {
    var beneXrefSk = beneficiaryRepository.getXrefBeneSk(beneSk);
    if (beneXrefSk.isEmpty()) {
      return new Bundle();
    }
    var eobs =
        claimRepository.findByBeneXrefSk(
            beneXrefSk.get(), serviceDate, lastUpdated, count, startIndex);
    var fhir =
        eobs.stream()
            .map(
                e ->
                    new Bundle.BundleEntryComponent()
                        .setResource(
                            e.toFhir()
                                .setPatient(PatientReferenceFactory.toFhir(beneXrefSk.get()))))
            .toList();
    return new Bundle().setEntry(fhir);
  }

  public Bundle searchById(
      Long claimUniqueId, DateTimeRange serviceDate, DateTimeRange lastUpdated) {
    var bundle = new Bundle();
    searchByIdInner(claimUniqueId, serviceDate, lastUpdated)
        .ifPresent(e -> bundle.addEntry(new Bundle.BundleEntryComponent().setResource(e)));
    return bundle;
  }

  private Optional<ExplanationOfBenefit> searchByIdInner(
      Long claimUniqueId, DateTimeRange serviceDate, DateTimeRange lastUpdated) {
    var claim = claimRepository.findById(claimUniqueId, serviceDate, lastUpdated);

    return claim.map(
        c -> {
          var beneXrefSk =
              beneficiaryRepository.getXrefBeneSk(c.getBeneficiary().getBeneSk()).get();
          var eob = c.toFhir();
          eob.setPatient(PatientReferenceFactory.toFhir(beneXrefSk));
          return eob;
        });
  }
}
