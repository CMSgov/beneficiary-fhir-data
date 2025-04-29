package gov.cms.bfd.server.ng.patient;

import gov.cms.bfd.server.ng.DateUtil;
import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.types.DateTimeRange;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PatientHandler {
  private final BeneficiaryRepository beneficiaryRepository;

  public Optional<Patient> find(Long fhirId) {
    var beneficiary = beneficiaryRepository.findById(fhirId);
    return beneficiary.map(this::toFhir);
  }

  public Bundle searchByLogicalId(final Long fhirId, final DateTimeRange lastUpdated) {
    var beneficiary =
        beneficiaryRepository.findById(fhirId, lastUpdated.lowerBound(), lastUpdated.upperBound());

    return singleOrDefaultBundle(beneficiary);
  }

  public Bundle searchByIdentifier(final String identifier, final DateTimeRange lastUpdated) {
    var beneficiary =
        beneficiaryRepository.findByIdentifier(
            identifier, lastUpdated.lowerBound(), lastUpdated.upperBound());

    return singleOrDefaultBundle(beneficiary);
  }

  private Patient toFhir(Beneficiary beneficiary) {
    var identities = beneficiaryRepository.getPatientIdentities(beneficiary.getBeneSk());
    var patient = beneficiary.toFhir();

    for (var id : identities) {
      id.toFhirIdentifier().ifPresent(patient::addIdentifier);
      id.toFhirLink(patient).ifPresent(patient::addLink);
    }

    return patient;
  }

  private Bundle singleOrDefaultBundle(Optional<Beneficiary> beneficiary) {
    if (beneficiary.isEmpty()) {
      return defaultBundle();
    }
    return beneficiary.map(this::singleBundle).orElseGet(this::defaultBundle);
  }

  private Bundle defaultBundle() {
    var lastUpdated = beneficiaryRepository.beneficiaryLastUpdated();
    var bundle = new Bundle();
    bundle.setMeta(new Meta().setLastUpdated(DateUtil.toDate(lastUpdated)));
    return bundle;
  }

  private Bundle singleBundle(Beneficiary beneficiary) {
    var patient = beneficiary.toFhir();
    var lastUpdated = patient.getMeta().getLastUpdated();
    var bundle = new Bundle().addEntry(new Bundle.BundleEntryComponent().setResource(patient));
    bundle.setMeta(new Meta().setLastUpdated(lastUpdated));
    return bundle;
  }
}
