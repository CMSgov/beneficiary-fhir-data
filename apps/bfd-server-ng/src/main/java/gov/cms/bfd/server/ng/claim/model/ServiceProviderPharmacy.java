package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import java.util.Optional;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;

/** Attending Provider History. * */
@Embeddable
@AttributeOverride(
    name = "providerNpiNumber",
    column = @Column(name = "clm_srvc_prvdr_gnrc_id_num"))
@AttributeOverride(name = "providerName", column = @Column(name = "prvdr_srvc_last_or_lgl_name"))
public class ServiceProviderPharmacy extends ProviderHistoryBase {

  @Column(name = "prvdr_srvc_1st_name")
  private Optional<String> providerFirstName;

  @Column(name = "prvdr_srvc_id_qlfyr_cd")
  private Optional<ProviderIdQualifierCode> providerQualifierCode;

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.SERVICE;
  }

  @Override
  @Transient
  protected ProviderHistoryBase.NpiType getNpiType() {
    if (providerFirstName.isEmpty() || getProviderNpiNumber().isEmpty()) {
      return ProviderHistoryBase.NpiType.ORGANIZATION;
    } else {
      return ProviderHistoryBase.NpiType.INDIVIDUAL;
    }
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
    boolean useBillingId =
        providerQualifierCode.isEmpty()
            || providerQualifierCode.get() != ProviderIdQualifierCode._01;

    var org =
        ProviderFhirHelper.createOrganizationWithNpi(
            useBillingId ? "BILLING_ID" : npiNumber, npiNumber, getProviderName());

    if (useBillingId) {
      org.setMeta(null);
    }

    getProviderNpiNumber().ifPresent(npi -> org.addIdentifier(toFhirIdentifier(npi)));

    return org;
  }

  private Practitioner createBillingPractitioner(String npiNumber) {
    var practitioner = ProviderFhirHelper.createPractitioner(npiNumber, npiNumber, toFhirName());
    getProviderNpiNumber().ifPresent(npi -> practitioner.addIdentifier(toFhirIdentifier(npi)));
    return practitioner;
  }

  HumanName toFhirName() {
    var name = new HumanName();
    providerFirstName.ifPresent(name::addGiven);
    getProviderName().ifPresent(name::setFamily);
    return name;
  }

  private Identifier toFhirIdentifier(String value) {
    var identifier = new Identifier().setSystem(SystemUrls.CMS_GENERIC_ID_NUM).setValue(value);
    providerQualifierCode.ifPresent(qualifierCode -> identifier.setType(qualifierCode.toFhir()));
    return identifier;
  }
}
