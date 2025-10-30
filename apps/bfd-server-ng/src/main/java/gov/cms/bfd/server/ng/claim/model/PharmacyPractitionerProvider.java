package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Meta;

@Embeddable
class PharmacyPractitionerProvider {
  @Column(name = "clm_srvc_prvdr_gnrc_id_num")
  private String serviceProviderNpiNumber;

  private static final String PROVIDER_ORG = "provider-practitioner";

  Optional<Practitioner> toFhir(ClaimTypeCode claimTypeCode) {
    if (!claimTypeCode.isBetween(1, 4)) {
      return Optional.empty();
    }

    var practitioner = new Practitioner();
    practitioner.setId(PROVIDER_ORG);
    practitioner.setMeta(
        new Meta()
            .addProfile(SystemUrls.PROFILE_CARIN_BB_ORGANIZATION_2_1_0)
            .addProfile(SystemUrls.PROFILE_US_CORE_ORGANIZATION_6_1_0));

    practitioner.addIdentifier(
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
    var name = new HumanName();
    name.setFamily("LAST NAME HERE");
    name.setUse(HumanName.NameUse.OFFICIAL);
    practitioner.setName(List.of(name));

    return Optional.of(practitioner);
  }
}
