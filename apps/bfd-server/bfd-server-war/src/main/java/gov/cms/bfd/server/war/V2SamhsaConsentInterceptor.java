package gov.cms.bfd.server.war;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentOutcome;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentContextServices;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseCoding;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.hl7.fhir.r4.model.ClaimResponse;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Meta;
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

  /** Flag to control whether SAMHSA filtering should be applied. */
  private boolean samhsaV2Enabled;

  /**
   * SamhsaConsentInterceptor Constructor.
   *
   * @param samhsaV2Enabled samhsaV2Enabled
   */
  public V2SamhsaConsentInterceptor(Boolean samhsaV2Enabled) {
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

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

    // Check if SAMHSA 2.0 filtering is enabled
    if (!samhsaV2Enabled) {
      // If the feature flag is false, skip filtering and proceed with the resource
      logger.info("SAMHSA 2.0 filtering is disabled, proceeding without filtering.");
      return ConsentOutcome.PROCEED;
    }

    // Extract 'excludeSAMHSA' parameter from the request URL
    boolean excludeSamhsa =
        Optional.ofNullable(theRequestDetails.getParameters().get("excludeSAMHSA"))
            .flatMap(params -> params.length > 0 ? Optional.of(params[0]) : Optional.empty())
            .map(Boolean::parseBoolean)
            .orElse(false);

    // Check if the resource is a Bundle and if it has entries
    if (theResource instanceof Bundle) {
      Bundle bundle = (Bundle) theResource;

      // Iterate through each entry in the Bundle
      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        IBaseResource entryResource = entry.getResource();

        // Use helper method to check and redact based on security tags
        if (shouldRedactResource(entryResource, excludeSamhsa)) {
          // If the resource is any of the target types (Claim, ClaimResponse, or
          // ExplanationOfBenefit), redact sensitive data
          if (entryResource instanceof Claim) {
            redactClaimSensitiveData((Claim) entryResource);
          } else if (entryResource instanceof ClaimResponse) {
            redactClaimResponseSensitiveData((ClaimResponse) entryResource);
          } else if (entryResource instanceof ExplanationOfBenefit) {
            redactEobSensitiveData((ExplanationOfBenefit) entryResource);
          }

          // Return AUTHORIZED to indicate this resource is redacted or excluded
          return ConsentOutcome.AUTHORIZED;
        }
      }
    }

    // If no matching security tags are found, allow the resource to proceed
    return ConsentOutcome.PROCEED;
  }

  /**
   * Helper method to determine if a resource should be redacted based on security tags.
   *
   * @param resource The resource to check.
   * @param excludeSamhsa Flag to determine whether to exclude SAMHSA.
   * @return true if the resource should be redacted, false otherwise.
   */
  private boolean shouldRedactResource(IBaseResource resource, boolean excludeSamhsa) {

    Meta meta = null;
    if (resource instanceof Claim) {
      meta = ((Claim) resource).getMeta();
    } else if (resource instanceof ClaimResponse) {
      meta = ((ClaimResponse) resource).getMeta();
    } else if (resource instanceof ExplanationOfBenefit) {
      meta = ((ExplanationOfBenefit) resource).getMeta();
    }
    // If meta or security tags are not present, no need to redact
    if (meta == null || meta.getSecurity() == null) {
      return false;
    }

    // Iterate through the security tags in the meta
    for (IBaseCoding securityTag : meta.getSecurity()) {
      // Check if the security tag matches the "R" or "42CFRPart2" code
      if (excludeSamhsa
          && ("R".equals(securityTag.getCode()) || "42CFRPart2".equals(securityTag.getCode()))) {
        logger.info("Matched SAMHSA security tag, redacting resource.");
        return true;
      }
    }

    return false; // No matching security tags, no redaction needed
  }

  /**
   * Redacts sensitive data in the Claim resource.
   *
   * @param claimResource the claimResource
   */
  private void redactClaimSensitiveData(Claim claimResource) {
    // Implement redaction logic for Claim
    logger.info("V2SamhsaConsentInterceptor - redactClaimSensitiveData.");
    //    claimResource.setContained(null); // Example: Redact contained resources
  }

  /**
   * Redacts sensitive data in the ClaimResponse resource.
   *
   * @param claimResponse the claimResponse
   */
  private void redactClaimResponseSensitiveData(ClaimResponse claimResponse) {
    // Implement redaction logic for ClaimResponse
    logger.info("V2SamhsaConsentInterceptor - redactClaimResponseSensitiveData.");
    //    claimResponse.setContained(null); // Example: Redact contained resources
  }

  /**
   * Redacts sensitive data in the ExplanationOfBenefit resource.
   *
   * @param eobResource the ExplanationOfBenefit resource
   */
  private void redactEobSensitiveData(ExplanationOfBenefit eobResource) {
    // Implement redaction logic for ExplanationOfBenefit
    logger.info("V2SamhsaConsentInterceptor - redactEobSensitiveData.");
    //    eobResource.setContained(null); // Example: Redact contained resources
  }

  /** completeOperationSuccess. */
  @Override
  public void completeOperationSuccess(
      RequestDetails theRequestDetails, IConsentContextServices theContextServices) {
    logger.info("V2SamhsaConsentInterceptor - completeOperationSuccess.");
    // We could write an audit trail entry in here
  }

  /** completeOperationFailure. */
  @Override
  public void completeOperationFailure(
      RequestDetails theRequestDetails,
      BaseServerResponseException theException,
      IConsentContextServices theContextServices) {
    logger.info("V2SamhsaConsentInterceptor - completeOperationFailure.");
    // We could write an audit trail entry in here
  }
}
