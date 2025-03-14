package gov.cms.bfd.server.war;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_SHADOW;

import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import java.util.List;
import org.hl7.fhir.r4.model.Coding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SamhsaV2InterceptorShadow handles filtering based on the SAMHSA (42CFRPart2) tags in the request.
 * When the shadow flag is enabled, the class will scrub resources tagged with the 42CFRPart2
 * security tag. If the shadow flag is disabled, no filtering occurs.
 */
@Service
public class SamhsaV2InterceptorShadow {

  private final V2SamhsaConsentInterceptor v2SamhsaConsentInterceptor =
      new V2SamhsaConsentInterceptor();

  private final V1SamhsaConsentInterceptor v1SamhsaConsentInterceptor =
      new V1SamhsaConsentInterceptor();

  private static final Logger logger = LoggerFactory.getLogger(SamhsaV2InterceptorShadow.class);

  /** Flag to control whether SAMHSA shadow filtering should be applied. */
  private final boolean samhsaV2Shadow;

  /** The securityTagManager. */
  private final SecurityTagManager securityTagManager;

  protected SamhsaV2InterceptorShadow(
      @Value("${" + SSM_PATH_SAMHSA_V2_SHADOW + ":false}") Boolean samhsaV2Shadow,
      SecurityTagManager securityTagManager) {
    this.samhsaV2Shadow = samhsaV2Shadow;
    this.securityTagManager = securityTagManager;
  }

  /**
   * logs Missing Claim Ids.
   *
   * @param claimWithSecurityTags the resource being processed
   * @param hasSamhsaData the hasNoSamhsaData boolean
   */
  public void logMissingClaim(
      ClaimWithSecurityTags<?> claimWithSecurityTags, boolean hasSamhsaData) {

    // Get security tags from the claim entity
    List<Coding> securityTags =
        securityTagManager.getClaimSecurityLevel(claimWithSecurityTags.getSecurityTags());

    // Check the redaction status of the interceptor against the SAMHSA exclusion flag
    boolean toRedact = v2SamhsaConsentInterceptor.shouldRedactResource(securityTags);

    // If redaction status doesn't match the SAMHSA exclusion flag, log the claim
    if (toRedact != hasSamhsaData) {
      String claimId = securityTagManager.getClaimId(claimWithSecurityTags.getClaimEntity());
      String claim =
          claimWithSecurityTags
              .getClaimEntity()
              .getClass()
              .getSimpleName(); // Add class type for context

      // Logging the error with better message formatting and more context
      logger.error(
          "Samhsa: claim IDs not matching between old SAMHSA filter and new SAMHSA 2.0 - "
              + "Claim ID: {} (Class: {})",
          claimId,
          claim);
    }
  }

  /**
   * logs Missing Claim Ids.
   *
   * @param claimWithSecurityTags the resource being processed
   * @param hasSamhsaData the hasNoSamhsaData boolean
   */
  public void logMissingClaimV1(
      ClaimWithSecurityTags<?> claimWithSecurityTags, boolean hasSamhsaData) {

    // Get security tags from the claim entity
    List<org.hl7.fhir.dstu3.model.Coding> securityTags =
        securityTagManager.getClaimSecurityLevelDstu3(claimWithSecurityTags.getSecurityTags());

    // Check the redaction status of the interceptor against the SAMHSA exclusion flag
    boolean toRedact = v1SamhsaConsentInterceptor.shouldRedactResource(securityTags);

    // If redaction status doesn't match the SAMHSA exclusion flag, log the claim
    if (toRedact != hasSamhsaData) {
      String claimId = securityTagManager.getClaimId(claimWithSecurityTags.getClaimEntity());
      String claim =
          claimWithSecurityTags
              .getClaimEntity()
              .getClass()
              .getSimpleName(); // Add class type for context

      // Logging the error with better message formatting and more context
      logger.error(
          "Samhsa: claim IDs not matching between old SAMHSA filter and new SAMHSA 2.0 - "
              + "Claim ID: {} (Class: {})",
          claimId,
          claim);
    }
  }
}
