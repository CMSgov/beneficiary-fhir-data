package gov.cms.bfd.server.ng.claim.model.common;

import gov.cms.bfd.server.ng.util.SystemUrls;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Organization;

@SuppressWarnings({"checkstyle:MissingJavadocType", "checkstyle:MissingJavadocMethod"})
public class OrganizationFactory {
  private OrganizationFactory() {}

  public static Organization toFhir() {
    var organization = new Organization();
    organization.setMeta(
        new Meta()
            .addProfile(SystemUrls.PROFILE_CARIN_BB_ORGANIZATION_2_2_0)
            .addProfile(SystemUrls.PROFILE_US_CORE_ORGANIZATION_7_0_0));
    organization.setActive(true);
    return organization;
  }
}
