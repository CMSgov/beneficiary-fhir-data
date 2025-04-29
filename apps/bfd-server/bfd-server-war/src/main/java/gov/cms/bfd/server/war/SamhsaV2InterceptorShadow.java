package gov.cms.bfd.server.war;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_SHADOW;

import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

  private static final String SAMHSA_MISMATCH_ERROR_MESSAGE =
      "Samhsa: Claim ID mismatch between old SAMHSA filter (hasSamhsaData) and new SAMHSA 2.0 (hasSamhsaDataV2) - ";

  /** Regex used to recognize real mbis. */
  private static final Pattern pattern =
      Pattern.compile(
          "\\b[1-9](?![SLOIBZ])[A-Z](?![SLOIBZ])[A-Z\\d]\\d(?![SLOIBZ])[A-Z](?![SLOIBZ])[A-Z\\d]\\d(?![SLOIBZ])[A-Z]{2}\\d{2}\\b");

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

    List<Coding> securityTags =
        securityTagManager.getClaimSecurityLevel(claimWithSecurityTags.getSecurityTags());

    // Check the redaction status of the interceptor against the SAMHSA exclusion flag
    boolean hasSamhsaDataV2 = v2SamhsaConsentInterceptor.shouldRedactResource(securityTags);

    if (hasSamhsaDataV2 != hasSamhsaData) {
      Object claimEntity = claimWithSecurityTags.getClaimEntity();
      String claimId = securityTagManager.getClaimId(claimEntity);
      String claimClass = claimEntity.getClass().getSimpleName();
      boolean isSynthetic = isSynthetic(claimEntity, claimId);

      // Identify which flag has the mismatch
      String missingSource = (hasSamhsaDataV2 ? "hasSamhsaData" : "hasSamhsaDataV2");

      // Log the mismatch between the old and new SAMHSA filters
      if (!isSynthetic) {
        logger.error(
            SAMHSA_MISMATCH_ERROR_MESSAGE + "Claim ID: {} (Class: {}) - Missing in: {}",
            claimId,
            claimClass,
            missingSource);
      }
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

    List<org.hl7.fhir.dstu3.model.Coding> securityTags =
        securityTagManager.getClaimSecurityLevelDstu3(claimWithSecurityTags.getSecurityTags());

    // Check the redaction status of the interceptor against the SAMHSA exclusion flag
    boolean hasSamhsaDataV2 = v1SamhsaConsentInterceptor.shouldRedactResource(securityTags);

    if (hasSamhsaDataV2 != hasSamhsaData) {
      Object claimEntity = claimWithSecurityTags.getClaimEntity();
      String claimId = securityTagManager.getClaimId(claimEntity);
      String claimClass = claimEntity.getClass().getSimpleName();
      boolean isSynthetic = isSynthetic(claimEntity, claimId);

      // Identify which flag has the mismatch
      String missingSource = (hasSamhsaDataV2 ? "hasSamhsaData" : "hasSamhsaDataV2");

      // Log the mismatch between the old and new SAMHSA filters
      if (!isSynthetic) {
        logger.error(
            SAMHSA_MISMATCH_ERROR_MESSAGE + "Claim ID: {} (Class: {}) - Missing in: {}",
            claimId,
            claimClass,
            missingSource);
      }
    }
  }

  /**
   * Validate if mbi is real.
   *
   * @param mbi the hasNoSamhsaData boolean
   * @return boolean if it is a Valid MBI
   */
  public static boolean isValidMBI(String mbi) {
    Matcher matcher = pattern.matcher(mbi);
    return matcher.find();
  }

  /**
   * Validate if data is Synthetic.
   *
   * @param claimEntity the claim Entity
   * @param claimId the claimId
   * @return boolean if it is a synthetic claim
   */
  public static boolean isSynthetic(Object claimEntity, String claimId) {
    if (claimEntity instanceof RdaFissClaim fissClaim) {
      return !isValidMBI(fissClaim.getMbi());
    } else if (claimEntity instanceof RdaMcsClaim mcsClaim) {
      return !isValidMBI(String.valueOf(mcsClaim.getMbiRecord().getMbi()));
    } else {
      return Long.parseLong(claimId) < 0;
    }
  }
}
