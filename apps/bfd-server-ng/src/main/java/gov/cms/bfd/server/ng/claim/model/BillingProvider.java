package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;

/** Billing Provider. * */
@Embeddable
class BillingProvider {
  @Column(name = "prvdr_blg_prvdr_npi_num")
  private String billingNpiNumber;

  @Column(name = "clm_blg_prvdr_oscar_num")
  private Optional<String> billingOscarNumber;

  @Column(name = "clm_blg_prvdr_zip5_cd")
  private Optional<String> billingZip5Code;

  private static final String PROVIDER_ORG = "provider-org";
  private static final String PRACTITIONER_BILLING = "practitioner-billing";

  /**
   * Builds the correct FHIR provider resource (Organization or Practitioner) according to FHIR
   * mapping rules.
   *
   * @param providerHistory - Provider History
   * @return a bundle resource containing either the Organization or Practitioner.
   */
  Optional<Resource> toFhir(ProviderHistory providerHistory) {

      return (providerHistory.getNpiType() == ProviderHistory.NpiType.ORGANIZATION
              ? createOrganization(providerHistory)
              : createBillingPractitioner(providerHistory))
              .map(r -> r);
  }

  private Optional<Organization> createOrganization(ProviderHistory providerHistory) {
    var org =
        ProviderFhirHelper.createOrganizationWithNpi(
            PROVIDER_ORG, billingNpiNumber, providerHistory.getProviderLegalName());

    billingOscarNumber.ifPresent(
        n ->
            org.addIdentifier(
                new Identifier().setSystem(SystemUrls.CMS_CERTIFICATION_NUMBERS).setValue(n)));

    return Optional.of(org);
  }

  private Optional<Practitioner> createBillingPractitioner(ProviderHistory providerHistory) {
    var practitioner =
        ProviderFhirHelper.createPractitioner(
            PRACTITIONER_BILLING, billingNpiNumber, providerHistory.toFhirName());
    billingZip5Code.ifPresent(
        zipCode -> practitioner.addAddress(new Address().setPostalCode(zipCode)));
    return Optional.of(practitioner);
  }
}
