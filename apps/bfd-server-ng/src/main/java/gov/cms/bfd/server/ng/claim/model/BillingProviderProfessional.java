package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;

/** Billing Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_blg_prvdr_npi_num"))
@AttributeOverride(name = "providerName", column = @Column(name = "bfd_prvdr_blg_last_or_lgl_name"))
public class BillingProviderProfessional extends ProviderHistoryBase {

  @Column(name = "prvdr_blg_1st_name")
  private Optional<String> providerFirstName;

  @Column(name = "clm_blg_prvdr_tax_num")
  private Optional<String> providerTaxNumber;

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
    providerTaxNumber.ifPresent(ptn -> org.addIdentifier(toFhirTaxIdentifier(ptn)));
    return org;
  }

  private Practitioner createBillingPractitioner(String npiNumber) {
    var practitioner = ProviderFhirHelper.createPractitioner(npiNumber, npiNumber, toFhirName());
    providerTaxNumber.ifPresent(ptn -> practitioner.addIdentifier(toFhirTaxIdentifier(ptn)));
    return practitioner;
  }

  HumanName toFhirName() {
    var name = new HumanName();
    providerFirstName.ifPresent(name::addGiven);
    getProviderName().ifPresent(name::setFamily);
    return name;
  }

  private Identifier toFhirTaxIdentifier(String value) {
    return new Identifier()
        .setSystem(SystemUrls.US_EIN)
        .setValue(value)
        .setType(
            new CodeableConcept()
                .addCoding(new Coding().setSystem(SystemUrls.HL7_IDENTIFIER).setCode("TAX")));
  }
}
