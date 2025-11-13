package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Resource;

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
   * @param npiType - NPI type
   */
  Optional<Resource> toFhir(ClaimTypeCode claimTypeCode, Integer npiType) {
    // --- 1. Institutional claims ---
    if (claimTypeCode.isBetween(5, 69)
        || claimTypeCode.isBetween(1000, 1699)
        || claimTypeCode.isBetween(2000, 2699)) {
      return Optional.of(createOrganization());
    }

    // --- 2. Professional claims: organization provider (NPI type 2) ---
    if (npiType != null
        && npiType == 2
        && (claimTypeCode.isBetween(71, 82)
            || claimTypeCode.isBetween(1700, 1899)
            || claimTypeCode.isBetween(2700, 2899))) {
      return Optional.of(createOrganization());
    }

    // --- 3. Professional claims: practitioner provider (NPI type 1) ---
    if (npiType != null
        && npiType == 1
        && (claimTypeCode.isBetween(71, 82)
            || claimTypeCode.isBetween(1700, 1899)
            || claimTypeCode.isBetween(2700, 2899))) {
      return Optional.of(createBillingPractitioner());
    }

    return Optional.empty();
  }

  private Organization createOrganization() {
    Organization org = OrganizationFactory.toFhir();
    org.setId(PROVIDER_ORG);
    org.addIdentifier(
        new Identifier()
            .setSystem(SystemUrls.NPI)
            .setValue(billingNpiNumber)
            .setType(
                new CodeableConcept()
                    .addCoding(
                        new Coding()
                            .setSystem(SystemUrls.HL7_IDENTIFIER)
                            .setCode("NPI")
                            .setDisplay("National provider identifier"))));

    billingOscarNumber.ifPresent(
        n ->
            org.addIdentifier(
                new Identifier().setSystem(SystemUrls.CMS_CERTIFICATION_NUMBERS).setValue(n)));

    org.setName("PROVIDER ORG SOURCED FROM NPPES");
    return org;
  }

  private Practitioner createBillingPractitioner() {
    Practitioner practitioner = new Practitioner();
    practitioner.setId(PRACTITIONER_BILLING);
    practitioner.addIdentifier(
        new Identifier().setSystem(SystemUrls.NPI).setValue(billingNpiNumber));
    practitioner.setActive(true);
    practitioner.setName(List.of(new HumanName().setText("Billing Practitioner")));
    billingZip5Code.ifPresent(
        zipCode -> practitioner.addAddress(new Address().setPostalCode(zipCode)));
    return practitioner;
  }
}
