package gov.cms.bfd.server.ng.patient;

import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.OrganizationFactory;
import gov.cms.bfd.server.ng.coverage.CoverageRepository;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.util.FhirBundleBuilder;
import gov.cms.bfd.server.ng.util.FhirUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
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

  private final CoverageRepository coverageRepository;

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
        beneficiary.map(this::toFhir), beneficiaryRepository::beneficiaryLastUpdated);
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
        beneficiary.map(this::toFhir), beneficiaryRepository::beneficiaryLastUpdated);
  }

  /**
   * Searches for all Coverage resources associated with a given beneficiary SK.
   *
   * @param beneSk The beneficiary surrogate key.
   * @return A Bundle of Coverage resources.
   */
  public Bundle searchByBeneficiary(Long beneSk) {
    var beneficiaryOpt =
        coverageRepository
            .searchBeneficiaryWithCoverage(beneSk, new DateTimeRange())
            .filter(b -> !b.isMergedBeneficiary());
    if (beneficiaryOpt.isEmpty()) {
      return FhirUtil.bundleOrDefault(List.of(), beneficiaryRepository::beneficiaryLastUpdated);
    }
    var beneficiary = beneficiaryOpt.get();

    var patient = beneficiary.toFhir(SystemUrls.PROFILE_C4DIC_PATIENT);
    // Two more organization may be needed, once mappings for Part C and D are added.
    var cmsOrg =
        OrganizationFactory.createCmsOrganization(
            UUID.randomUUID().toString(), SystemUrls.PROFILE_C4DIC_ORGANIZATION);

    var coverages =
        Arrays.stream(CoveragePart.values())
            .map(
                c ->
                    beneficiary.toFhirCoverageIfPresentC4DIC(
                        new CoverageCompositeId(c, beneficiary.getBeneSk()), cmsOrg.getId()))
            .flatMap(Optional::stream);

    var resources = Stream.concat(Stream.of(patient, cmsOrg), coverages);

    return FhirBundleBuilder.fromResources(resources.map(c -> c))
        .withLastUpdated(beneficiaryRepository::beneficiaryLastUpdated)
        .includeFullUrls(true) // optional
        .build();
  }

  private Patient toFhir(Beneficiary beneficiary) {
    var identities = beneficiaryRepository.getValidBeneficiaryIdentities(beneficiary.getXrefSk());
    var patient = beneficiary.toFhir();

    for (var id : identities) {
      // check for merged bene and if mbi identifier has already been added to the patient
      if (!beneficiary.isMergedBeneficiary()
          && patient.getIdentifier().stream()
              .noneMatch(identifier -> identifier.getValue().equals(id.getId().getMbi()))) {
        patient.addIdentifier(id.toFhirIdentifier());
      }

      id.toFhirLink(Long.parseLong(patient.getId()))
          .filter(generatedLink -> !isDuplicateLink(patient, generatedLink))
          .ifPresent(patient::addLink);
    }

    return patient;
  }

  private boolean isDuplicateLink(Patient patient, Patient.PatientLinkComponent newLink) {
    return patient.getLink().stream()
        .anyMatch(
            existingLink ->
                existingLink.getType() == newLink.getType()
                    && existingLink
                        .getOther()
                        .getReference()
                        .equals(newLink.getOther().getReference()));
  }
}
