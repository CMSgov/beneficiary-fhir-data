package gov.cms.bfd.server.war;

import static gov.cms.bfd.server.war.commons.StringUtils.parseBoolean;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentOutcome;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentContextServices;
import ca.uhn.fhir.rest.server.interceptor.consent.IConsentService;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.sharedutils.TagCode;
import org.hl7.fhir.dstu3.model.Bundle;
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

    // Extract 'excludeSAMHSA' parameter from the request URL
    boolean excludeSAMHSA = parseBoolean(theRequestDetails.getParameters().get("excludeSAMHSA"));
    boolean filterSamhsa =
        CommonTransformerUtils.shouldFilterSamhsa(String.valueOf(excludeSAMHSA), theRequestDetails);

    // Handle Bundle
    if (theResource instanceof Bundle bundle) {
      // Iterate through each entry in the Bundle
      for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
        IBaseResource entryResource = entry.getResource();

        if (shouldRedactResource(entryResource, filterSamhsa)) {
          redactSensitiveData(entry);
        }
      }
    }

    return ConsentOutcome.PROCEED;
  }

  /**
   * Helper method to determine if a resource should be redacted based on security tags.
   *
   * @param resource The resource to check.
   * @param filterSamhsa Flag to determine whether to filter SAMHSA.
   * @return true if the resource should be redacted, false otherwise.
   */
  private boolean shouldRedactResource(IBaseResource resource, boolean filterSamhsa) {
    Meta meta = null;

    // Extract the meta information depending on the resource type
    if (resource instanceof ExplanationOfBenefit eob) {
      meta = eob.getMeta();
    }

    // If meta or security tags are not present, no need to redact
    if (meta == null || meta.getSecurity() == null) {
      return false;
    }

    // Iterate through the security tags in the meta
    for (Coding securityTag : meta.getSecurity()) {
      // Check if the security tag matches the "R" or "42CFRPart2" code
      if (filterSamhsa
          && (TagCode.R.name().equals(securityTag.getCode())
              || TagCode._42CFRPart2.name().equals(securityTag.getCode()))) {
        logger.info("Matched SAMHSA security tag, redacting resource.");
        return true;
      }
    }

    return false;
  }

  /**
   * Redacts sensitive data in the resource.
   *
   * @param entry the entry
   */
  private void redactSensitiveData(Bundle.BundleEntryComponent entry) {
    // Implement redaction logic for Claim
    logger.info("V1SamhsaConsentInterceptor - redactSensitiveData.");
    entry.setResource(null);
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
