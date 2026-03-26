package gov.cms.bfd.server.ng.patient;

import gov.cms.bfd.server.ng.beneficiary.BeneficiaryRepository;
import gov.cms.bfd.server.ng.beneficiary.model.Beneficiary;
import gov.cms.bfd.server.ng.beneficiary.model.OrganizationFactory;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatch;
import gov.cms.bfd.server.ng.beneficiary.model.PatientMatchAuditRecord;
import gov.cms.bfd.server.ng.coverage.CoverageRepository;
import gov.cms.bfd.server.ng.input.CoverageCompositeId;
import gov.cms.bfd.server.ng.input.CoveragePart;
import gov.cms.bfd.server.ng.input.DateTimeRange;
import gov.cms.bfd.server.ng.loadprogress.LoadProgressRepository;
import gov.cms.bfd.server.ng.model.ProfileType;
import gov.cms.bfd.server.ng.util.DateUtil;
import gov.cms.bfd.server.ng.util.FhirUtil;
import gov.cms.bfd.server.ng.util.PatientMatchAuditLogUtil;
import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
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
  private final CoverageRepository coverageRepository;

  private static final String BLUEBUTTON_CLIENT_IP_HEADER = "X-BLUEBUTTON-CLIENT-IP";
  private static final String BLUEBUTTON_CLIENT_NAME_HEADER = "X-BLUEBUTTON-CLIENT-NAME";
  private static final String BLUEBUTTON_CLIENT_ID_HEADER = "X-BLUEBUTTON-CLIENT-ID";

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

  public Bundle matchPatient(Optional<PatientMatch> patientMatch, HttpServletRequest request) {
    if (patientMatch.isEmpty()) {
      return patientMatchBundle(Optional.empty());
    }
    var result = beneficiaryRepository.searchPatientMatch(patientMatch.get());
    var beneficiary = result.matchedBeneficiary();
    var clientIp = request.getHeader(BLUEBUTTON_CLIENT_IP_HEADER);
    var clientName = request.getHeader(BLUEBUTTON_CLIENT_NAME_HEADER);
    var clientId = request.getHeader(BLUEBUTTON_CLIENT_ID_HEADER);
    var auditRecord =
        new PatientMatchAuditRecord(
            Optional.ofNullable(clientIp),
            Optional.ofNullable(clientName),
            Optional.ofNullable(clientId),
            DateUtil.nowAoe(),
            result.combinations(),
            result.finalDetermination());
    PatientMatchAuditLogUtil.logPatientMatches(auditRecord);

    return patientMatchBundle(beneficiary);
  }

  /**
   * Searches for all Coverage resources associated with a given beneficiary SK.
   *
   * @param beneSk The beneficiary surrogate key.
   * @return A Bundle of Coverage resources.
   */
  public Bundle searchByBeneficiaryC4DIC(Long beneSk) {
    var beneficiaryOpt =
        coverageRepository
            .searchBeneficiaryWithCoverage(beneSk, new DateTimeRange())
            .filter(b -> !b.isMergedBeneficiary());
    if (beneficiaryOpt.isEmpty()) {
      return FhirUtil.bundleOrDefault(List.of(), loadProgressRepository::lastUpdated);
    }
    var beneficiary = beneficiaryOpt.get();

    var patient = beneficiary.toFhirPatient(ProfileType.C4DIC);
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
    return FhirUtil.bundleWithFullUrls(resources, loadProgressRepository::lastUpdated);
  }

  private Patient toFhir(Beneficiary beneficiary) {
    var identities = beneficiaryRepository.getValidBeneficiaryIdentities(beneficiary.getXrefSk());
    var patient = beneficiary.toFhirPatient(ProfileType.C4BB);

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

  private Bundle patientMatchBundle(Optional<Beneficiary> beneficiary) {
    var bundle = new Bundle();
    bundle.setId("IDI-Match");
    bundle.setMeta(new Meta().addProfile(SystemUrls.PROFILE_IDENTITY_MATCHING));
    bundle.setIdentifier(new Identifier().setAssigner(new Reference().setDisplay("CMS")));
    bundle.setType(Bundle.BundleType.SEARCHSET);
    var cmsOrg = new Organization();
    cmsOrg.setId("cms-org").setMeta(new Meta().addProfile(SystemUrls.PROFILE_US_CORE_ORGANIZATION));
    cmsOrg
        .setActive(true)
        .addType(
            new CodeableConcept(
                new Coding()
                    .setSystem(SystemUrls.HL7_ORGANIZATION_TYPE)
                    .setCode("pay")
                    .setDisplay("Payer")));
    cmsOrg.setName("CMS");

    var bundleEntry =
        new Bundle.BundleEntryComponent().setFullUrl(SystemUrls.CMS_GOV).setResource(cmsOrg);
    bundleEntry.setSearch(
        new Bundle.BundleEntrySearchComponent().setMode(Bundle.SearchEntryMode.MATCH));
    bundle.addEntry(bundleEntry);
    beneficiary.ifPresent(
        b ->
            bundle.addEntry(
                new Bundle.BundleEntryComponent()
                    .setResource(toFhir(b))
                    .setSearch(
                        new Bundle.BundleEntrySearchComponent()
                            .setMode(Bundle.SearchEntryMode.MATCH))));

    return bundle;
  }
}
