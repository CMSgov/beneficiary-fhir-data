package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;

@Embeddable
class BillingProvider {
  @Column(name = "prvdr_blg_prvdr_npi_num")
  private String billingNpiNumber;

  @Column(name = "clm_blg_prvdr_oscar_num")
  private Optional<String> billingOscarNumber;

  private static final String PROVIDER_ORG = "provider-org";

  Optional<Organization> toFhir(ClaimTypeCode claimTypeCode) {
    if (!(claimTypeCode.isBetween(5, 69)
        || claimTypeCode.isBetween(2000, 2700)
        || claimTypeCode.isBetween(1000, 1700))) {
      return Optional.empty();
    }

    var organization = OrganizationFactory.toFhir();
    organization.setId(PROVIDER_ORG);
    organization.addIdentifier(
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
        n -> {
          organization.addIdentifier(
              new Identifier().setSystem(SystemUrls.CMS_CERTIFICATION_NUMBERS).setValue(n));
        });

    organization.setName("PROVIDER ORG SOURCED FROM NPPES");

    return Optional.of(organization);
  }
}
