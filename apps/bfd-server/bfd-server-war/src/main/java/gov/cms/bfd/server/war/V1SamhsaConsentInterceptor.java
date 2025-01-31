package gov.cms.bfd.server.war;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentOutcome;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentContextServices;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;
import java.util.Optional;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Claim;
import org.hl7.fhir.dstu3.model.ClaimResponse;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Meta;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** V1SamhsaConsentInterceptor handles filtering for V1 data types (like V1 Claims). */
public class V1SamhsaConsentInterceptor implements IConsentService {

  /** The logger. */
  private static final Logger logger = LoggerFactory.getLogger(V1SamhsaConsentInterceptor.class);

  /** Flag to control whether SAMHSA filtering should be applied. */
  private boolean samhsaV2Enabled;

  /**
   * SamhsaConsentInterceptor Constructor.
   *
   * @param samhsaV2Enabled samhsaV2Enabled
   */
  public V1SamhsaConsentInterceptor(Boolean samhsaV2Enabled) {
    this.samhsaV2Enabled = samhsaV2Enabled;
  }

  @Override
  public ConsentOutcome startOperation(
      RequestDetails theRequestDetails, IConsentContextServices theContextServices) {
    return ConsentOutcome.PROCEED;
  }

  @Override
  public ConsentOutcome willSeeResource(
      RequestDetails theRequestDetails,
      IBaseResource theResource,
      IConsentContextServices theContextServices) {

    logger.info("V1SamhsaConsentInterceptor - willSeeResource.");
    // Check if SAMHSA 2.0 filtering is enabled
    //    if (!samhsa2_0Enabled) {
    //      // If the feature flag is false, skip filtering and proceed with the resource
    //      logger.info("SAMHSA 2.0 filtering is disabled, proceeding without filtering.");
    //      return ConsentOutcome.PROCEED;
    //    }

    // Extract 'excludeSAMHSA' parameter from the request URL
    boolean excludeSamhsa =
        Optional.ofNullable(theRequestDetails.getParameters().get("excludeSAMHSA"))
            .flatMap(params -> params.length > 0 ? Optional.of(params[0]) : Optional.empty())
            .map(Boolean::parseBoolean)
            .orElse(false);

    // Handle Bundle
    if (theResource instanceof Bundle) {
      Bundle bundle = (Bundle) theResource;

      // Iterate through each entry in the Bundle
      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        IBaseResource entryResource = entry.getResource();

        // Use helper method to check and redact based on security tags
        if (shouldRedactResource(entryResource, excludeSamhsa)) {
          // Depending on the type of resource, call the appropriate redaction method
          if (entryResource instanceof ExplanationOfBenefit) {
            redactEobSensitiveData((ExplanationOfBenefit) entryResource);
          } else if (entryResource instanceof Claim) {
            redactClaimSensitiveData((Claim) entryResource);
          } else if (entryResource instanceof ClaimResponse) {
            redactClaimResponseSensitiveData((ClaimResponse) entryResource);
          }

          return ConsentOutcome.AUTHORIZED;
        }
      }
    }

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

    // Extract the meta information depending on the resource type
    if (resource instanceof ExplanationOfBenefit) {
      meta = ((ExplanationOfBenefit) resource).getMeta();
    } else if (resource instanceof Claim) {
      meta = ((Claim) resource).getMeta();
    } else if (resource instanceof ClaimResponse) {
      meta = ((ClaimResponse) resource).getMeta();
    }

    // If meta or security tags are not present, no need to redact
    if (meta == null || meta.getSecurity() == null) {
      return false;
    }

    // Iterate through the security tags in the meta
    for (Coding securityTag : meta.getSecurity()) {
      // Check if the security tag matches the "R" or "42CFRPart2" code
      if (excludeSamhsa
          && ("R".equals(securityTag.getCode()) || "42CFRPart2".equals(securityTag.getCode()))) {
        logger.info("Matched SAMHSA security tag, redacting resource.");
        return true;
      }
    }

    return false;
  }

  /**
   * Can redact sensitive data in ExplanationOfBenefit resource.
   *
   * @param eob the resource
   */
  private void redactEobSensitiveData(ExplanationOfBenefit eob) {
    logger.info("V1SamhsaConsentInterceptor - redactEobSensitiveData.");
    // Implement redaction logic for ExplanationOfBenefit
    // Example: eob.setContained(null); // Redact contained resources as an example
  }

  /**
   * Can redact sensitive data in Claim resource.
   *
   * @param claim the resource
   */
  private void redactClaimSensitiveData(Claim claim) {
    logger.info("V1SamhsaConsentInterceptor - redactClaimSensitiveData.");
    // Implement redaction logic for Claim
    // Example: claim.setContained(null); // Redact contained resources as an example
  }

  /**
   * Can redact sensitive data in ClaimResponse resource.
   *
   * @param claimResponse the resource
   */
  private void redactClaimResponseSensitiveData(ClaimResponse claimResponse) {
    logger.info("V1SamhsaConsentInterceptor - redactClaimResponseSensitiveData.");
    // Implement redaction logic for ClaimResponse
    // Example: claimResponse.setContained(null); // Redact contained resources as an example
  }

  @Override
  public void completeOperationSuccess(
      RequestDetails theRequestDetails, IConsentContextServices theContextServices) {
    logger.info("V1SamhsaConsentInterceptor - completeOperationSuccess.");
  }

  @Override
  public void completeOperationFailure(
      RequestDetails theRequestDetails,
      BaseServerResponseException theException,
      IConsentContextServices theContextServices) {
    logger.info("V1SamhsaConsentInterceptor - completeOperationFailure.");
  }
}
