package gov.cms.bfd.server.ng.claim.model;

import gov.cms.bfd.server.ng.util.SystemUrls;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;

class OrganizationFactory {
  private OrganizationFactory() {}

  static Organization toFhir() {
    var organization = new Organization();
    organization.setMeta(
        new Meta()
            .addProfile(SystemUrls.PROFILE_CARIN_BB_ORGANIZATION_2_1_0)
            .addProfile(SystemUrls.PROFILE_US_CORE_ORGANIZATION_6_1_0));
    organization.setActive(true);
    return organization;
  }
}
