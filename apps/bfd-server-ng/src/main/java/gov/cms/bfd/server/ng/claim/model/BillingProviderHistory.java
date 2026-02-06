package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Optional;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;

/** Billing Provider History. * */
@Embeddable
@AttributeOverride(name = "providerNpiNumber", column = @Column(name = "prvdr_blg_prvdr_npi_num"))
@AttributeOverride(name = "providerSk", column = @Column(name = "prvdr_blg_sk"))
@AttributeOverride(
    name = "providerTaxonomyCode",
    column = @Column(name = "prvdr_blg_txnmy_cmpst_cd"))
@AttributeOverride(name = "providerTypeCode", column = @Column(name = "prvdr_blg_type_cd"))
@AttributeOverride(name = "providerOscarNumber", column = @Column(name = "prvdr_blg_oscar_num"))
@AttributeOverride(name = "providerFirstName", column = @Column(name = "prvdr_blg_1st_name"))
@AttributeOverride(name = "providerMiddleName", column = @Column(name = "prvdr_blg_mdl_name"))
@AttributeOverride(name = "providerLastName", column = @Column(name = "prvdr_blg_last_name"))
@AttributeOverride(name = "providerName", column = @Column(name = "prvdr_blg_name"))
@AttributeOverride(name = "providerLegalName", column = @Column(name = "prvdr_blg_lgl_name"))
@AttributeOverride(name = "employerIdNumber", column = @Column(name = "prvdr_blg_emplr_id_num"))
public class BillingProviderHistory extends ProviderHistoryBase {

  @Column(name = "clm_blg_prvdr_zip5_cd")
  private Optional<String> billingZip5Code;

  @Override
  protected CareTeamType getCareTeamType() {
    return CareTeamType.BILLING;
  }

  private static final String PROVIDER_ORG = "provider-org";
  private static final String PRACTITIONER_BILLING = "practitioner-billing";

  /**
   * Builds the correct FHIR provider resource (Organization or Practitioner) according to FHIR
   * mapping rules.
   *
   * @return an Optional containing a bundle resource with either the Organization or Practitioner,
   *     or empty if NPI number is not present.
   */
  @Override
  Optional<DomainResource> toFhirNpiType() {
    return getProviderNpiNumber()
        .map(
            npi ->
                (getNpiType() == ProviderHistoryBase.NpiType.ORGANIZATION
                    ? createOrganization(npi)
                    : createBillingPractitioner(npi)));
  }

  private Organization createOrganization(String npiNumber) {
    var org =
        ProviderFhirHelper.createOrganizationWithNpi(
            PROVIDER_ORG, npiNumber, getProviderLegalName());

    getProviderOscarNumber()
        .ifPresent(
            n ->
                org.addIdentifier(
                    new Identifier().setSystem(SystemUrls.CMS_CERTIFICATION_NUMBERS).setValue(n)));
    billingZip5Code.ifPresent(zipCode -> org.addAddress(new Address().setPostalCode(zipCode)));

    return org;
  }

  private Practitioner createBillingPractitioner(String npiNumber) {
    var practitioner =
        ProviderFhirHelper.createPractitioner(PRACTITIONER_BILLING, npiNumber, toFhirName());
    billingZip5Code.ifPresent(
        zipCode -> practitioner.addAddress(new Address().setPostalCode(zipCode)));
    return practitioner;
  }
}
