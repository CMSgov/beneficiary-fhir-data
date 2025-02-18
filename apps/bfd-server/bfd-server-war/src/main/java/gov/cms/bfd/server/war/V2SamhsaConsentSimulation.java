package gov.cms.bfd.server.war;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * V2SamhsaConsentSimulation handles filtering based on the SAMHSA (42CFRPart2) tags in the request.
 * When the shadow flag is enabled, the class will scrub resources tagged with the 42CFRPart2
 * security tag. If the shadow flag is disabled, no filtering occurs.
 */
public class V2SamhsaConsentSimulation {

  private final V2SamhsaConsentInterceptor v2SamhsaConsentInterceptor =
      new V2SamhsaConsentInterceptor();
  private static final Logger logger = LoggerFactory.getLogger(V2SamhsaConsentSimulation.class);

  /**
   * Simulates V2SamhsaConsentInterceptor without actually registering V2SamhsaConsentInterceptor.
   *
   * @param theRequestDetails the request details
   * @param theResource the resource being processed
   * @return IBaseResource
   */
  public IBaseResource simulateScrubbing(
      RequestDetails theRequestDetails, IBaseResource theResource) {
    logger.debug("Simulating SAMHSA scrubbing.");

    if (!v2SamhsaConsentInterceptor.shouldApplySamhsaFiltering(theRequestDetails)) {
      return theResource;
    }

    // Process the resource if it is a Bundle
    if (theResource instanceof Bundle bundle) {
      v2SamhsaConsentInterceptor.processBundle(bundle);
    }

    return theResource; // Return the unmodified resource if no redaction
  }
}
