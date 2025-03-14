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
import java.util.List;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.instance.model.api.IBaseCoding;
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

    logger.debug("V1SamhsaConsentInterceptor - Processing willSeeResource.");

    // No filtering needed, proceed
    if (!applySamhsaFiltering(theRequestDetails)) {
      return ConsentOutcome.PROCEED;
    }

    // Process the resource if it is a Bundle
    if (theResource instanceof Bundle bundle) {
      processBundle(bundle);
    }

    return ConsentOutcome.PROCEED;
  }

  boolean applySamhsaFiltering(RequestDetails theRequestDetails) {
    boolean excludeSamhsaParam =
        parseBooleansFromRequest(theRequestDetails, AbstractResourceProvider.EXCLUDE_SAMHSA)
            .stream()
            .findFirst()
            .orElse(false);
    return CommonTransformerUtils.shouldFilterSamhsa(
        String.valueOf(excludeSamhsaParam), theRequestDetails);
  }

  void processBundle(Bundle bundle) {
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      IBaseResource baseResource = entry.getResource();

      if (baseResource instanceof ExplanationOfBenefit eob
          && eob.getMeta() != null
          && shouldRedactResource(eob.getMeta().getSecurity())) {
        redactSensitiveData(entry);
      }
    }
  }

  /**
   * Helper method to determine if a resource should be redacted based on security tags.
   *
   * @param securityTags The security Tags.
   * @return true if the resource should be redacted, false otherwise.
   */
  boolean shouldRedactResource(List<Coding> securityTags) {
    for (IBaseCoding securityTag : securityTags) {
      // Check for SAMHSA-related tags
      if (isSamhsaSecurityTag(securityTag)) {
        logger.info("Matched SAMHSA security tag, redacting resource.");
        return true;
      }
    }
    return false; // No matching SAMHSA tags found
  }

  /**
   * Checks if the security tag is related to SAMHSA (42CFRPart2).
   *
   * @param securityTag the security Tag
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
    logger.debug("Redacting sensitive data from resource.");
    entry.setResource(null); // Redact the resource
  }

  @Override
  public void completeOperationSuccess(
      RequestDetails theRequestDetails, IConsentContextServices theContextServices) {
    logger.debug("V1SamhsaConsentInterceptor - completeOperationSuccess.");
  }

  @Override
  public void completeOperationFailure(
      RequestDetails theRequestDetails,
      BaseServerResponseException theException,
      IConsentContextServices theContextServices) {
    logger.info("V1SamhsaConsentInterceptor - Operation failed.", theException);
  }
}
