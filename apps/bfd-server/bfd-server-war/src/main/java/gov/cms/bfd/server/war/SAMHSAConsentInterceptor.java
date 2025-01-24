package gov.cms.bfd.server.war;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import gov.cms.bfd.model.rif.samhsa.CarrierTag;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import java.util.Set;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SAMHSAConsentInterceptor handles filtering based on the SAMHSA (42CFRPart2) tags in the request.
 * When the feature flag is enabled, the interceptor scrubs resources tagged with the 42CFRPart2
 * security tag. If the feature flag is disabled, no filtering occurs.
 */
@Interceptor
public class SAMHSAConsentInterceptor {

  /** The Security tag manager. */
  private final SecurityTagManager securityTagManager;

  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(SAMHSAConsentInterceptor.class);

  /** Flag to control whether SAMHSA filtering should be applied. */
  private boolean excludeSAMHSA;

  /**
   * Constructor for the SAMHSAConsentInterceptor.
   *
   * @param securityTagManager the security Tag Manager
   */
  public SAMHSAConsentInterceptor(SecurityTagManager securityTagManager) {
    this.securityTagManager = securityTagManager;
    this.excludeSAMHSA = true;
  }

  /**
   * Intercepts incoming requests and checks for SAMHSA 42CFRPart2 tags. If the feature flag is
   * enabled and the resource has a 42CFRPart2 tag, it throws an exception.
   *
   * @param theRequestDetails theRequestDetails
   * @return true if the request is allowed, false otherwise
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLER_SELECTED)
  public boolean interceptRequest(RequestDetails theRequestDetails) {
    // Check if the SAMHSA filtering is enabled
    if (!excludeSAMHSA) {
      return true; // Allow the request if filtering is not enabled
    }
    DomainResource resource = (DomainResource) theRequestDetails.getResource();

    // Extract claimId (you may get this dynamically depending on your actual implementation)
    String claimId = resource.getIdElement().getIdPart(); // Assuming ID is the claim ID for example
    Class<?> tagClass = CarrierTag.class; // for now
    if (isTaggedWithSAMHSA(claimId, tagClass)) {
      logger.info("Excluding resource with 42CFRPart2 tag: {}", resource.getId());
      // Block the resource by throwing an exception
      throw new AuthenticationException(
          "Resource is excluded due to SAMHSA 42CFRPart2 restrictions.");
    }
    return true; // Continue processing the request if no filtering is required
  }

  /**
   * Pointcut to log timestamp in milliseconds when a request is post-processed.
   *
   * @param claimId the claim Id
   * @param tagClass the tagClass
   * @return the output {@link Identifier}
   */
  // This method checks if the resource has SAMHSA security tag
  private boolean isTaggedWithSAMHSA(String claimId, Class<?> tagClass) {
    Set<String> securityTags = securityTagManager.queryTagsForClaim(claimId, tagClass);

    return !securityTags.isEmpty();
  }
}
