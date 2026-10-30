package gov.cms.bfd.server.ng;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import gov.cms.bfd.server.ng.claim.EobProfile;
import org.hl7.fhir.instance.model.api.IBaseConformance;
import org.hl7.fhir.r4.model.CapabilityStatement;

/** Adds supportProfile for EoB's in the CapabilityStatement */
@Interceptor
public class CapabilityStatementInterceptor {

  /**
   * Adds each profile from EobProfile to the EoB resource on the capability statement.
   *
   * @param capabilityStatement the CapabilityStatement
   */
  @Hook(Pointcut.SERVER_CAPABILITY_STATEMENT_GENERATED)
  public void addEobProfiles(IBaseConformance capabilityStatement) {
    var allEobProfileUrls = EobProfile.getAllVersionedCanonicalUrls();

    ((CapabilityStatement) capabilityStatement)
        .getRestFirstRep().getResource().stream()
            .filter(resource -> resource.getType().equals("ExplanationOfBenefit"))
            .forEach(resource -> allEobProfileUrls.forEach(resource::addSupportedProfile));
  }
}
