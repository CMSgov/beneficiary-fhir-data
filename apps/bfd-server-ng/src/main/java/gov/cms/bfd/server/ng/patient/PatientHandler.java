package gov.cms.bfd.server.ng.patient;

import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressRepository;
import gov.cms.bfd.server.ng.util.FhirUtil;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

/**
 * Handler methods for the Patient resource. This is called after the FHIR inputs from the resource
 * provider are converted into input types that are easier to work with.
 */
@Component
@RequiredArgsConstructor
public class PatientHandler {
  private final BeneficiaryRepository beneficiaryRepository;
  private final LoadProgressRepository loadProgressRepository;

  /**
   * Returns a {@link Patient} by their {@link IdType}.
   *
   * @param fhirId FHIR ID
   * @return patient
   */
  public Optional<Patient> find(final Long fhirId) {
    var beneficiary = beneficiaryRepository.findById(fhirId, new DateTimeRange());
    return beneficiary.map(this::toFhir);
  }

  /**
   * Searches for a Patient by ID.
   *
   * @param fhirId FHIR ID
   * @param lastUpdated last updated datetime
   * @return bundle
   */
  public Bundle searchByLogicalId(final Long fhirId, final DateTimeRange lastUpdated) {
    var beneficiary = beneficiaryRepository.findById(fhirId, lastUpdated);

    return FhirUtil.bundleOrDefault(
        beneficiary.map(this::toFhir), loadProgressRepository::lastUpdated);
  }

  /**
   * Searches for a Patient by identifier.
   *
   * @param identifier identifier
   * @param lastUpdated last updated datetime
   * @return bundle
   */
  public Bundle searchByIdentifier(final String identifier, final DateTimeRange lastUpdated) {
    var xrefBeneSk = beneficiaryRepository.getXrefSkFromMbi(identifier);
    var beneficiary = xrefBeneSk.flatMap(x -> beneficiaryRepository.findById(x, lastUpdated));

    return FhirUtil.bundleOrDefault(
        beneficiary.map(this::toFhir), loadProgressRepository::lastUpdated);
  }

  private Patient toFhir(Beneficiary beneficiary) {
    var identities = beneficiaryRepository.getValidBeneficiaryIdentities(beneficiary.getXrefSk());
    var patient = beneficiary.toFhir();

    for (var id : identities) {
      // check for merged bene and if mbi identifier has already been added to the patient
      if (!beneficiary.isMergedBeneficiary()
          && patient.getIdentifier().stream()
              .noneMatch(identifier -> identifier.getValue().equals(id.getMbi()))) {
        patient.addIdentifier(id.toFhirIdentifier());
      }

      id.toFhirLink(Long.parseLong(patient.getId())).ifPresent(patient::addLink);
    }

    return patient;
  }
}
