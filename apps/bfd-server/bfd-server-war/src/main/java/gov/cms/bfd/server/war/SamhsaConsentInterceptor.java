package gov.cms.bfd.server.war;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentOutcome;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentContextServices;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SAMHSAConsentInterceptor handles filtering based on the SAMHSA (42CFRPart2) tags in the request.
 * When the feature flag is enabled, the interceptor scrubs resources tagged with the 42CFRPart2
 * security tag. If the feature flag is disabled, no filtering occurs.
 */
// @Interceptor
public class SamhsaConsentInterceptor implements IConsentService {

  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(SamhsaConsentInterceptor.class);

  /** Flag to control whether SAMHSA filtering should be applied. */
  private boolean excludeSAMHSA;

  /**
   * Invoked once at the start of every request.
   *
   * @param theRequestDetails theRequestDetails
   * @param theContextServices theContextServices
   */
  @Override
  public ConsentOutcome startOperation(
      RequestDetails theRequestDetails, IConsentContextServices theContextServices) {
    // This means that all requests should flow through the consent service
    // This has performance implications - If you know that some requests
    // don't need consent checking it is a good idea to return
    // ConsentOutcome.AUTHORIZED instead for those requests.
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

    logger.info("SAMHSAConsentInterceptor - canSeeResource invoked.");
    boolean excludeSamhsa = true;

    // Check if the resource is a Bundle and if it has entries
    if (theResource instanceof Bundle) {
      Bundle bundle = (Bundle) theResource;

      // Iterate through each entry in the Bundle
      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        IBaseResource entryResource = entry.getResource();

        // Check if the entry is of type Claim
        if (entryResource instanceof Claim) {
          Claim claimResource = (Claim) entryResource;

          // Check if the Claim has security tags
          if (claimResource.getMeta() != null && claimResource.getMeta().getSecurity() != null) {

            // Iterate through the security tags in the meta
            for (IBaseCoding securityTag : claimResource.getMeta().getSecurity()) {
              // Check if any of the security tags match SAMHSA-related criteria
              if (excludeSamhsa
                  && ("R".equals(securityTag.getCode())
                      || "42CFRPart2".equals(securityTag.getCode()))) {

                // If any matching security tag is found and excludeSamhsa is true, redact or
                // exclude this resource
                redactSensitiveData(claimResource);

                // Return AUTHORIZED to indicate this resource is redacted or excluded
                return ConsentOutcome.AUTHORIZED;
              }
            }
          }
        }
      }
    }

    // If no matching security tags are found, allow the resource to proceed
    return ConsentOutcome.PROCEED;
  }

  /**
   * Can redactSensitiveData.
   *
   * @param claimResource the claimResource
   */
  private void redactSensitiveData(Claim claimResource) {
    // Here you would redact or modify the resource as needed.
    // with sensitive data, you might clear fields, null them, or delete the security tag.
    logger.info("SAMHSAConsentInterceptor - redactSensitiveData.");
    claimResource.setContained(
        null); // Hiding the contained resource :to be updated to the Samhsa data
  }

  /** Can redactSensitiveData. */
  @Override
  public void completeOperationSuccess(
      RequestDetails theRequestDetails, IConsentContextServices theContextServices) {
    logger.info("SAMHSAConsentInterceptor - completeOperationSuccess.");
    // We could write an audit trail entry in here
  }

  /** Can redactSensitiveData. */
  @Override
  public void completeOperationFailure(
      RequestDetails theRequestDetails,
      BaseServerResponseException theException,
      IConsentContextServices theContextServices) {
    logger.info("SAMHSAConsentInterceptor - completeOperationFailure.");
    // We could write an audit trail entry in here
  }
}
