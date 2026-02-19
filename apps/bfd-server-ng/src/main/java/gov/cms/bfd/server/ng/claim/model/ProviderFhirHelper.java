package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.Reference;

/**
 * Helper class for converting provider data to FHIR resources. Centralizes common logic to avoid
 * duplication.
 */
class ProviderFhirHelper {
  private ProviderFhirHelper() {}

  /**
   * Creates a FHIR {@link Organization} populated with an NPI identifier and optional legal name.
   *
   * @param id the logical ID to assign to the Organization
   * @param npiNumber the NPI identifier value
   * @param legalName an optional legal name to set on the Organization
   * @return a FHIR {@link Organization} containing the provided identifier and name
   */
  public static Organization createOrganizationWithNpi(
      String id, String npiNumber, Optional<String> legalName) {
    var organization = OrganizationFactory.toFhir();
    organization.setId(id);
    organization.addIdentifier(
        new Identifier()
            .setSystem(SystemUrls.NPI)
            .setValue(npiNumber)
            .setType(
                new CodeableConcept()
                    .addCoding(
                        new Coding()
                            .setSystem(SystemUrls.HL7_IDENTIFIER)
                            .setCode("NPI")
                            .setDisplay("National provider identifier"))));

    legalName.ifPresent(organization::setName);
    return organization;
  }

  /**
   * Creates a FHIR {@link Practitioner} with CARIN BB and US Core profile metadata, an NPI
   * identifier, and optional first and last names.
   *
   * @param id the logical ID to assign to the Practitioner
   * @param npiNumber the practitioner's NPI identifier value
   * @param name provider name
   * @return a fully populated FHIR {@link Practitioner} instance
   */
  public static Practitioner createPractitioner(String id, String npiNumber, HumanName name) {
    var practitioner = new Practitioner();
    practitioner.setId(id);
    practitioner.setMeta(
        new Meta()
            .addProfile(SystemUrls.PROFILE_CARIN_BB_PRACTITIONER_2_1_0)
            .addProfile(SystemUrls.PROFILE_US_CORE_PRACTITIONER_6_1_0));

    practitioner.addIdentifier(
        new Identifier()
            .setSystem(SystemUrls.NPI)
            .setValue(npiNumber)
            .setType(
                new CodeableConcept()
                    .addCoding(
                        new Coding()
                            .setSystem(SystemUrls.HL7_IDENTIFIER)
                            .setCode("NPI")
                            .setDisplay("National provider identifier"))));

    practitioner.setName(List.of(name));
    return practitioner;
  }

  /**
   * Creates a FHIR {@link Practitioner} with CARIN BB and US Core profile metadata, an NPI
   * identifier, and optional first and last names.
   *
   * @param npiNumber the practitioner's NPI identifier value
   * @param name provider name
   * @return a fully populated FHIR {@link Practitioner} instance
   */
  public static Reference createProviderReference(String npiNumber, Optional<String> name) {
    var reference = new Reference();

    reference.setIdentifier(
        new Identifier()
            .setSystem(SystemUrls.NPI)
            .setValue(npiNumber)
            .setType(
                new CodeableConcept()
                    .addCoding(
                        new Coding()
                            .setSystem(SystemUrls.HL7_IDENTIFIER)
                            .setCode("NPI")
                            .setDisplay("National provider identifier"))));
    name.ifPresent(reference::setDisplay);
    return reference;
  }

  /**
   * Creates a FHIR {@link Reference} to a provider (Practitioner or Organization), including
   * identifier with proper type and system based on provider qualifier code.
   *
   * @param idNumber the provider's identifier value
   * @param qualifierCode the type of identifier (PRVDR_ID_QLFYR_CD)
   * @param displayName optional provider name
   * @return a fully populated FHIR {@link Reference} instance
   */
  public static Reference createProviderReference(
      String idNumber, ProviderIdQualifierCode qualifierCode, Optional<String> displayName) {

    var reference = new Reference();
    var identifier = new Identifier();

    // Set identifier type coding
    var type = qualifierCode.toFhir();

    // If it's an NPI, add the NPI coding
    if (qualifierCode == ProviderIdQualifierCode._01) {
      type.addCoding(
          new Coding()
              .setSystem(SystemUrls.HL7_IDENTIFIER)
              .setCode("NPI")
              .setDisplay("National provider identifier"));
      identifier.setSystem(SystemUrls.NPI);
    } else {
      // For other types, use generic system
      identifier.setSystem(SystemUrls.CMS_GENERIC_ID_NUM);
    }

    identifier.setType(type).setValue(idNumber);
    reference.setIdentifier(identifier);

    displayName.ifPresent(reference::setDisplay);
    return reference;
  }
}
