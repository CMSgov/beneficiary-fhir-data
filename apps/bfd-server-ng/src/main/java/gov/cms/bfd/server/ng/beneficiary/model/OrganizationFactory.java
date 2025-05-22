package gov.cms.bfd.server.ng.beneficiary.model;

import gov.cms.bfd.server.ng.IdrConstants;
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
   * @return A fully populated FHIR {@link Organization} resource for CMS.
   */
  public static Organization createCmsOrganization() {
    Organization cmsOrg = new Organization();
    cmsOrg.setId("cms-org");

    // Set the Meta information, including the C4BB Organization profile.
    Meta orgMeta = new Meta();
    orgMeta.addProfile(IdrConstants.PROFILE_C4BB_ORGANIZATION);
    cmsOrg.setMeta(orgMeta);
    cmsOrg.setActive(true);
    cmsOrg.setName("Centers for Medicare and Medicaid Services");

    Organization.OrganizationContactComponent contact =
        new Organization.OrganizationContactComponent();

    // Set the purpose of the contact.
    CodeableConcept contactPurpose = new CodeableConcept();
    contactPurpose.addCoding(new Coding(IdrConstants.SYS_CONTACT_ENTITY_TYPE, "PATINF", "Patient"));
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
}
