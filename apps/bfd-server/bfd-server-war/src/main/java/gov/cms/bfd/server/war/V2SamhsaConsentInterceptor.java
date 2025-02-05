package gov.cms.bfd.server.war;

import static gov.cms.bfd.server.war.commons.StringUtils.parseBoolean;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentOutcome;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentContextServices;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.sharedutils.TagCode;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
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

    logger.info("SAMHSAConsentInterceptor - willSeeResource.");

    // Extract 'excludeSAMHSA' parameter from the request URL
    boolean excludeSamhsa = parseBoolean(theRequestDetails.getParameters().get("excludeSAMHSA"));
    boolean filterSamhsa =
        CommonTransformerUtils.shouldFilterSamhsa(String.valueOf(excludeSamhsa), theRequestDetails);

    // Check if the resource is a Bundle and if it has entries
    if (theResource instanceof Bundle bundle) {
      // Iterate through each entry in the Bundle
      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        IBaseResource entryResource = entry.getResource();

        if (shouldRedactResource(entryResource, filterSamhsa)) {
          redactSensitiveData(entry);
        }
      }
    }

    // If no matching security tags are found, allow the resource to proceed
    return ConsentOutcome.PROCEED;
  }

  /**
   * Helper method to determine if a resource should be redacted based on security tags.
   *
   * @param baseResource The resource to check.
   * @param filterSamhsa Flag to determine whether to filter SAMHSA.
   * @return true if the resource should be redacted, false otherwise.
   */
  private boolean shouldRedactResource(IBaseResource baseResource, boolean filterSamhsa) {
    Meta meta = null;
    if (baseResource instanceof Resource resource) {
      meta = resource.getMeta();
    }
    // If meta or security tags are not present, no need to redact
    if (meta == null || meta.getSecurity() == null) {
      return false;
    }

    // Iterate through the security tags in the meta
    for (IBaseCoding securityTag : meta.getSecurity()) {
      // Check if the security tag matches the "R" or "42CFRPart2" code
      if (filterSamhsa
          && (TagCode.R.name().equals(securityTag.getCode())
              || TagCode._42CFRPart2.name().equals(securityTag.getCode()))) {
        logger.info("Matched SAMHSA security tag, redacting resource.");
        return true;
      }
    }

    return false; // No matching security tags, no redaction needed
  }

  /**
   * Redacts sensitive data in the resource.
   *
   * @param entry the entry
   */
  private void redactSensitiveData(Bundle.BundleEntryComponent entry) {
    logger.info("V2SamhsaConsentInterceptor - redactSensitiveData.");
    entry.setResource(null);
  }

  /** completeOperationSuccess. */
  @Override
  public void completeOperationSuccess(
      RequestDetails theRequestDetails, IConsentContextServices theContextServices) {
    logger.info("V2SamhsaConsentInterceptor - completeOperationSuccess.");
  }

  /** completeOperationFailure. */
  @Override
  public void completeOperationFailure(
      RequestDetails theRequestDetails,
      BaseServerResponseException theException,
      IConsentContextServices theContextServices) {
    logger.info("V2SamhsaConsentInterceptor - completeOperationFailure.");
  }
}
