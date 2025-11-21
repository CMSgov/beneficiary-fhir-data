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
   * @param claimTypeCode - Claim Type code
   * @param providerHistory - Provider History
   * @return a bundle resource containing either the Organization or Practitioner.
   */
  Optional<Resource> toFhir(ClaimTypeCode claimTypeCode, ProviderHistory providerHistory) {
    // --- 1. Institutional claims ---
    if (claimTypeCode.isBetween(5, 69)
        || claimTypeCode.isBetween(1000, 1699)
        || claimTypeCode.isBetween(2000, 2699)) {
      return Optional.of(createOrganization(providerHistory));
    }

    // --- 2. Professional claims: organization provider (NPI type 2) ---
    if (providerHistory.getNpiType() == ProviderHistory.NPI_TYPE.ORGANIZATION
        && (claimTypeCode.isBetween(71, 82)
            || claimTypeCode.isBetween(1700, 1899)
            || claimTypeCode.isBetween(2700, 2899))) {
      return Optional.of(createOrganization(providerHistory));
    }

    // --- 3. Professional claims: practitioner provider (NPI type 1) ---
    if (providerHistory.getNpiType() == ProviderHistory.NPI_TYPE.INDIVIDUAL
        && (claimTypeCode.isBetween(71, 82)
            || claimTypeCode.isBetween(1700, 1899)
            || claimTypeCode.isBetween(2700, 2899))) {
      return Optional.of(createBillingPractitioner(providerHistory));
    }

    return Optional.empty();
  }

  private Organization createOrganization(ProviderHistory providerHistory) {
    var org =
        ProviderFhirHelper.createOrganizationWithNpi(
            PROVIDER_ORG, billingNpiNumber, providerHistory.getProviderLegalName());

    billingOscarNumber.ifPresent(
        n ->
            org.addIdentifier(
                new Identifier().setSystem(SystemUrls.CMS_CERTIFICATION_NUMBERS).setValue(n)));

    return org;
  }

  private Practitioner createBillingPractitioner(ProviderHistory providerHistory) {
    var practitioner =
        ProviderFhirHelper.createPractitioner(
            PRACTITIONER_BILLING,
            billingNpiNumber,
            providerHistory.getProviderFirstName(),
            providerHistory.getProviderLastName());
    billingZip5Code.ifPresent(
        zipCode -> practitioner.addAddress(new Address().setPostalCode(zipCode)));
    return practitioner;
  }
}
