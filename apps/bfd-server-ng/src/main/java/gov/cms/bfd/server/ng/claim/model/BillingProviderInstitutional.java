package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import java.util.Optional;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;

/** Billing Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_blg_prvdr_npi_num"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_prvdr_blg_last_or_lgl_name"))
public class BillingProviderInstitutional extends ProviderHistoryBase {

  @Column(name = "clm_blg_prvdr_zip5_cd")
  private Optional<String> billingZip5Code;

  @Column(name = "prvdr_blg_1st_name")
  private Optional<String> providerFirstName;

  @Column(name = "clm_blg_prvdr_oscar_num")
  private Optional<String> providerOscarNumber;

  /**
   * Derives the NPI_TYPE based on the presence of the providerLegalName. NPI_TYPE = INDIVIDUAL
   * means the NPI is for an individual (legal name is null/empty). NPI_TYPE = ORGANIZATION means
   * the NPI is for an organization (legal name is present).
   *
   * @return the NPI type
   */
  @Transient
  public ProviderHistoryBase.NpiType getNpiType() {
    if (providerFirstName.isEmpty()) {
      return ProviderHistoryBase.NpiType.ORGANIZATION;
    } else {
      return ProviderHistoryBase.NpiType.INDIVIDUAL;
    }
  }

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.BILLING;
  }

  /**
   * Builds the correct FHIR provider resource (Organization or Practitioner) according to FHIR
   * mapping rules.
   *
   * @return an Optional containing a bundle resource with either the Organization or Practitioner,
   *     or empty if NPI number is not present.
   */
  Optional<DomainResource> toFhirNpiType() {
    return getProviderNpiNumber()
        .map(
            npi ->
                (getNpiType() == NpiType.ORGANIZATION
                    ? createOrganization(npi)
                    : createBillingPractitioner(npi)));
  }

  private Organization createOrganization(String npiNumber) {
    var org = ProviderFhirHelper.createOrganizationWithNpi(npiNumber, npiNumber, getProviderName());

    providerOscarNumber.ifPresent(
        n ->
            org.addIdentifier(
                new Identifier().setSystem(SystemUrls.CMS_CERTIFICATION_NUMBERS).setValue(n)));
    billingZip5Code.ifPresent(zipCode -> org.addAddress(new Address().setPostalCode(zipCode)));

    return org;
  }

  private Practitioner createBillingPractitioner(String npiNumber) {
    var practitioner = ProviderFhirHelper.createPractitioner(npiNumber, npiNumber, toFhirName());
    billingZip5Code.ifPresent(
        zipCode -> practitioner.addAddress(new Address().setPostalCode(zipCode)));
    return practitioner;
  }

  HumanName toFhirName() {
    var name = new HumanName();
    providerFirstName.ifPresent(name::addGiven);
    getProviderName().ifPresent(name::setFamily);
    return name;
  }
}
