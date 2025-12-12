package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.claim.model.Contract;
import gov.cms.bfd.server.ng.util.SystemUrls;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;

/** Factory class for creating common FHIR Organization resources, such as the CMS Organization. */
public final class OrganizationFactory {

  private OrganizationFactory() {}

  /**
   * Creates a FHIR {@link Organization} resource representing the CMS.
   *
   * @param id the resource ID to assign to the Organization
   * @param profile the FHIR profile URL to set in the Organization's Meta
   * @return A fully populated FHIR {@link Organization} resource for CMS.
   */
  public static Organization createCmsOrganization(String id, String profile) {
    Organization cmsOrg = new Organization();
    cmsOrg.setId(id);

    // Set the Meta information, including the C4BB Organization profile.
    Meta orgMeta = new Meta();
    orgMeta.addProfile(profile);
    cmsOrg.setMeta(orgMeta);
    cmsOrg.setActive(true);
    cmsOrg.setName("Centers for Medicare and Medicaid Services");

    Organization.OrganizationContactComponent contact =
        new Organization.OrganizationContactComponent();

    // Set the purpose of the contact.
    CodeableConcept contactPurpose = new CodeableConcept();
    contactPurpose.addCoding(new Coding(SystemUrls.SYS_CONTACT_ENTITY_TYPE, "PATINF", "Patient"));
    contact.setPurpose(contactPurpose);

    contact.addTelecom(
        new ContactPoint()
            .setSystem(ContactPoint.ContactPointSystem.PHONE)
            .setValue("1-800-633-4227"));
    contact.addTelecom(
        new ContactPoint()
            .setSystem(ContactPoint.ContactPointSystem.PHONE)
            .setValue("TTY: 1-877-486-2048"));
    contact.addTelecom(
        new ContactPoint()
            .setSystem(ContactPoint.ContactPointSystem.URL)
            .setValue("www.medicare.gov"));
    cmsOrg.addContact(contact);

    return cmsOrg;
  }

  /**
   * Creates a FHIR {@link Organization} resource representing part c/d.
   *
   * @param id the resource ID to assign to the Organization
   * @param profile the FHIR profile URL to set in the Organization's Meta
   * @param contract the contract to set in the Organization's name and contact information
   * @return A fully populated FHIR {@link Organization} resource for part c/d.
   */
  public static Organization createInsurerOrganization(
      String id, String profile, Contract contract) {
    var insurerOrg = new Organization();
    insurerOrg.setId(id);

    // Set the Meta information, including the C4BB Organization profile.
    var orgMeta = new Meta();
    orgMeta.addProfile(profile);
    insurerOrg.setMeta(orgMeta);
    insurerOrg.setActive(true);
    contract.getContractName().ifPresent(insurerOrg::setName);

    var contact = new Organization.OrganizationContactComponent();

    // Set the purpose of the contact.
    var contactPurpose = new CodeableConcept();
    contactPurpose.addCoding(new Coding(SystemUrls.SYS_CONTACT_ENTITY_TYPE, "PATINF", "Patient"));
    contact.setPurpose(contactPurpose);
    var contactInfo = contract.getContractPlanContactInfo();
    contactInfo
        .getContractPlanContactFreeNumber()
        .ifPresent(
            phoneNumber ->
                addPhone(contact, contactInfo.getContractPlanFreeExtensionNumber(), phoneNumber));
    contactInfo
        .getContractPlanContactNumber()
        .ifPresent(
            phoneNumber ->
                addPhone(
                    contact, contactInfo.getContractPlanContactExtensionNumber(), phoneNumber));
    insurerOrg.addContact(contact);

    return insurerOrg;
  }

  private static void addPhone(
      Organization.OrganizationContactComponent contact,
      Optional<String> extensionNumber,
      String phoneNumber) {
    var fullTeleNumber = extensionNumber.map(ext -> ext + "-").orElse("") + phoneNumber;
    contact.addTelecom(
        new ContactPoint()
            .setSystem(ContactPoint.ContactPointSystem.PHONE)
            .setValue(fullTeleNumber));
  }

  /**
   * Creates a FHIR {@link Organization} resource representing CMS with default ID and default
   * profile.
   *
   * @return a fully populated FHIR {@link Organization} resource for CMS
   */
  public static Organization createCmsOrganization() {
    return createCmsOrganization("cms-org", SystemUrls.PROFILE_C4BB_ORGANIZATION_2_1_0);
  }

  /**
   * Creates a FHIR {@link Organization} resource representing part c/d with default ID and default
   * profile.
   *
   * @param contract the contract to set in the Organization's name and contact information
   * @return a fully populated FHIR {@link Organization} resource for part c/d
   */
  public static Organization createInsurerOrganization(Contract contract) {
    return createInsurerOrganization(
        "insurer-org", SystemUrls.PROFILE_C4BB_ORGANIZATION_2_1_0, contract);
  }
}
