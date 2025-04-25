package gov.cms.bfd.server.ng.patient;

import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.Identity;
import gov.cms.bfd.server.ng.types.DateTimeRange;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatientHandler {
  private final BeneficiaryRepository beneficiaryRepository;

  public Optional<Patient> find(Long fhirId) {
    return searchByLogicalId(fhirId, new DateTimeRange());
  }

  public Optional<Patient> searchByLogicalId(final Long fhirId, final DateTimeRange lastUpdated) {
    var beneficiary =
        beneficiaryRepository.findById(fhirId, lastUpdated.lowerBound(), lastUpdated.upperBound());

    if (beneficiary.isEmpty()) {
      return Optional.empty();
    }
    var ids = beneficiaryRepository.getPatientIdentities(fhirId);
    var patient = toFhir(beneficiary.get(), ids);

    return Optional.of(patient);
  }

  private Patient toFhir(Beneficiary beneficiary, List<Identity> identities) {
    var patient = beneficiary.toFhir();

    for (var id : identities) {
      id.toFhirIdentifier().ifPresent(patient::addIdentifier);
      id.toFhirLink(patient).ifPresent(patient::addLink);
    }

    return patient;
  }
}
