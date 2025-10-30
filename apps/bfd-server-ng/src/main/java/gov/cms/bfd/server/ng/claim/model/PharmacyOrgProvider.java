package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;

@Embeddable
class PharmacyOrgProvider {
  @Column(name = "clm_srvc_prvdr_gnrc_id_num")
  private String serviceProviderNpiNumber;

  private static final String PROVIDER_ORG = "provider-org";

  Optional<Organization> toFhir(ClaimTypeCode claimTypeCode) {
    if (!claimTypeCode.isBetween(1, 4)) {
      return Optional.empty();
    }

    var organization = OrganizationFactory.toFhir();
    organization.setId(PROVIDER_ORG);
    organization.addIdentifier(
        new Identifier()
            .setSystem(SystemUrls.NPI)
            .setValue(serviceProviderNpiNumber)
            .setType(
                new CodeableConcept()
                    .addCoding(
                        new Coding()
                            .setSystem(SystemUrls.HL7_IDENTIFIER)
                            .setCode("NPI")
                            .setDisplay("National provider identifier"))));

    organization.setName("PHARMACY ORG SOURCED FROM NPPES");

    return Optional.of(organization);
  }
}
