package gov.cms.bfd.server.war;

import static gov.cms.bfd.server.war.commons.StringUtils.parseBooleansFromRequest;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentOutcome;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentContextServices;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;
import gov.cms.bfd.server.war.commons.AbstractResourceProvider;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.sharedutils.TagCode;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SAMHSAConsentInterceptor handles filtering based on the SAMHSA (42CFRPart2) tags in the request.
 * When the feature flag is enabled, the interceptor scrubs resources tagged with the 42CFRPart2
 * security tag. If the feature flag is disabled, no filtering occurs.
 */
public class V2SamhsaConsentInterceptor implements IConsentService {

  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(V2SamhsaConsentInterceptor.class);

  /**
   * Invoked once at the start of every request.
   *
   * @param theRequestDetails theRequestDetails
   * @param theContextServices theContextServices
   */
  @Override
  public ConsentOutcome startOperation(
      RequestDetails theRequestDetails, IConsentContextServices theContextServices) {
    return ConsentOutcome.PROCEED;
  }

  /**
   * Can a given resource be returned to the user.
   *
   * @param theRequestDetails theRequestDetails
   * @param theResource theResource
   * @param theContextServices theContextServices
   */
  @Override
  public ConsentOutcome willSeeResource(
      RequestDetails theRequestDetails,
      IBaseResource theResource,
      IConsentContextServices theContextServices) {

    logger.debug("SAMHSAConsentInterceptor - Processing willSeeResource.");

    // Determine if SAMHSA filtering is required from request parameters
    boolean excludeSamhsaParam =
        parseBooleansFromRequest(theRequestDetails, AbstractResourceProvider.EXCLUDE_SAMHSA)
            .stream()
            .findFirst()
            .orElse(false);
    boolean shouldFilterSamhsa =
        CommonTransformerUtils.shouldFilterSamhsa(
            String.valueOf(excludeSamhsaParam), theRequestDetails);

    if (!shouldFilterSamhsa) {
      return ConsentOutcome.PROCEED; // No filtering needed, proceed
    }

    // If the resource is a Bundle, check each entry for SAMHSA security tags
    if (theResource instanceof Bundle bundle) {
      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        if (shouldRedactResource(entry.getResource())) {
          redactSensitiveData(entry);
        }
      }
    }

    return ConsentOutcome.PROCEED;
  }

  /**
   * Checks if a resource should be redacted based on SAMHSA security tags.
   *
   * @param baseResource The resource to check.
   * @return true if the resource should be redacted, false otherwise.
   */
  private boolean shouldRedactResource(IBaseResource baseResource) {
    if (baseResource instanceof Resource resource && resource.getMeta() != null) {
      for (IBaseCoding securityTag : resource.getMeta().getSecurity()) {
        // Check for SAMHSA-related tags
        if (isSamhsaSecurityTag(securityTag)) {
          logger.info("Matched SAMHSA security tag, redacting resource.");
          return true;
        }
      }
    }
    return false; // No matching SAMHSA tags found
  }

  /**
   * Determines if a security tag is related to SAMHSA (42CFRPart2).
   *
   * @param securityTag the security tag
   * @return true if it has Samhsa security tag, false otherwise
   */
  private boolean isSamhsaSecurityTag(IBaseCoding securityTag) {
    String code = securityTag.getCode();
    return TagCode._42CFRPart2.toString().equalsIgnoreCase(code);
  }

  /**
   * Redacts sensitive data in the resource.
   *
   * @param entry the entry
   */
  private void redactSensitiveData(Bundle.BundleEntryComponent entry) {
    logger.debug("V2SamhsaConsentInterceptor - redactSensitiveData.");
    entry.setResource(null);
  }

  @Override
  public void completeOperationSuccess(
      RequestDetails theRequestDetails, IConsentContextServices theContextServices) {
    logger.debug("V2SamhsaConsentInterceptor - completeOperationSuccess.");
  }

  @Override
  public void completeOperationFailure(
      RequestDetails theRequestDetails,
      BaseServerResponseException theException,
      IConsentContextServices theContextServices) {
    logger.info("V2SamhsaConsentInterceptor - Operation failed.", theException);
  }
}
