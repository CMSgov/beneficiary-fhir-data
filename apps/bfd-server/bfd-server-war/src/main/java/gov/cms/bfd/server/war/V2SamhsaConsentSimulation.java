package gov.cms.bfd.server.war;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_SHADOW;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * V2SamhsaConsentSimulation handles filtering based on the SAMHSA (42CFRPart2) tags in the request.
 * When the shadow flag is enabled, the class will scrub resources tagged with the 42CFRPart2
 * security tag. If the shadow flag is disabled, no filtering occurs.
 */
@Service
public class V2SamhsaConsentSimulation {

  private final V2SamhsaConsentInterceptor v2SamhsaConsentInterceptor =
      new V2SamhsaConsentInterceptor();
  private static final Logger logger = LoggerFactory.getLogger(V2SamhsaConsentSimulation.class);

  /** Flag to control whether SAMHSA shadow filtering should be applied. */
  private boolean samhsaV2Shadow;

  protected V2SamhsaConsentSimulation(
      @Value("${" + SSM_PATH_SAMHSA_V2_SHADOW + ":false}") Boolean samhsaV2Shadow) {
    this.samhsaV2Shadow = samhsaV2Shadow;
  }

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

    if (!samhsaV2Shadow || !v2SamhsaConsentInterceptor.applySamhsaFiltering(theRequestDetails)) {
      return theResource;
    }

    // Process the resource if it is a Bundle
    if (theResource instanceof Bundle bundle) {
      v2SamhsaConsentInterceptor.processBundle(bundle);
      return bundle;
    }

    return theResource; // Return the unmodified resource if no redaction
  }

  /**
   * logs Missing Claim Ids.
   *
   * @param v2SamhsaScrubbedResource the request details
   * @param bundleResource the resource being processed
   */
  public void logMissingClaimIds(Bundle v2SamhsaScrubbedResource, Bundle bundleResource) {
    if (samhsaV2Shadow) {
      Set<String> scrubbedClaimIds = new HashSet<>();
      Set<String> originalClaimIds = new HashSet<>();

      // Populate the scrubbedClaimIds set with IDs from the scrubbed resource
      for (Bundle.BundleEntryComponent entry : v2SamhsaScrubbedResource.getEntry()) {
        if (entry.getResource() != null) {
          originalClaimIds.add(entry.getResource().getIdElement().getIdPart());
        }
      }

      // Populate the originalClaimIds set with IDs from the original resource
      for (Bundle.BundleEntryComponent entry : bundleResource.getEntry()) {
        originalClaimIds.add(entry.getResource().getIdElement().getIdPart());
      }

      // Find missing claim IDs by checking which entries are in the original but not in the
      // scrubbed
      Set<String> missingClaimIds = new HashSet<>(originalClaimIds);
      missingClaimIds.removeAll(scrubbedClaimIds);

      // Log the missing claim IDs
      // the goal here is to compare the old SAMHSA 2.0 scrubbed vs the SAMHSA.0 scrubbed
      if (!missingClaimIds.isEmpty()) {
        logger.error("Samhsa: claim ids missing - " + String.join(", ", missingClaimIds));
      }
    }
  }
}
