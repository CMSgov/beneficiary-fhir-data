package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SequenceGenerator;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import java.util.Optional;
import lombok.Getter;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;

/** Provider History. */
@Getter
@MappedSuperclass
public abstract class ProviderHistoryBase {
  private Optional<String> providerNpiNumber;
  private Optional<String> providerTaxonomyCode;
  private Optional<String> providerTypeCode;
  private Optional<String> providerOscarNumber;
  private Optional<String> providerFirstName;
  private Optional<String> providerMiddleName;
  private Optional<String> providerLastName;
  private Optional<String> providerName;
  private Optional<String> providerLegalName;
  private Optional<String> employerIdNumber;

  /** Represents the enum NPI Type. */
  public enum NpiType {
    /** NPI belongs to an individual. */
    INDIVIDUAL,
    /** NPI belongs to an organization. */
    ORGANIZATION
  }

  protected abstract CareTeamType getCareTeamType();

  /**
   * Derives the NPI_TYPE based on the presence of the providerLegalName. NPI_TYPE = INDIVIDUAL
   * means the NPI is for an individual (legal name is null/empty). NPI_TYPE = ORGANIZATION means
   * the NPI is for an organization (legal name is present).
   *
   * @return the NPI type
   */
  @Transient
  public NpiType getNpiType() {
    if (providerLegalName.isPresent() && !providerLegalName.get().isBlank()) {
      return NpiType.ORGANIZATION;
    } else {
      return NpiType.INDIVIDUAL;
    }
  }

  private static final String PROVIDER_PRACTITIONER = "provider-practitioner";
  private static final String PROVIDER_ORG = "provider-org";

  HumanName toFhirName() {
    var name = new HumanName();
    providerFirstName.ifPresent(name::addGiven);
    providerMiddleName.ifPresent(name::addGiven);
    providerLastName.ifPresent(name::setFamily);

    return name;
  }

  Optional<Practitioner> toFhirPractitioner() {
    return providerNpiNumber.map(
        npi -> {
          var practitioner =
              ProviderFhirHelper.createPractitioner(PROVIDER_PRACTITIONER, npi, toFhirName());
          practitioner.getName().forEach(n -> n.setUse(HumanName.NameUse.OFFICIAL));
          return practitioner;
        });
  }

  Optional<Organization> toFhirOrganization() {
    return providerNpiNumber.map(
        npi -> ProviderFhirHelper.createOrganizationWithNpi(PROVIDER_ORG, npi, providerLegalName));
  }

  Optional<DomainResource> toFhirNpiType() {
    return getNpiType() == NpiType.ORGANIZATION
        ? toFhirOrganization().map(DomainResource.class::cast)
        : toFhirPractitioner().map(DomainResource.class::cast);
  }

  Optional<CareTeamType.CareTeamComponents> toFhirCareTeamComponent(
      SequenceGenerator sequenceGenerator) {
    return providerNpiNumber.map(
        npi -> getCareTeamType().toFhir(sequenceGenerator, npi, toFhirName(), Optional.empty()));
  }

  Optional<CareTeamType.CareTeamComponents> toFhirCareTeamComponent(
      SequenceGenerator sequenceGenerator, Optional<String> pinNumber) {
    return providerNpiNumber.map(
        npi -> getCareTeamType().toFhir(sequenceGenerator, npi, toFhirName(), pinNumber));
  }
}
