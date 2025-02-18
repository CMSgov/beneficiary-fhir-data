package gov.cms.bfd.server.war;

import static gov.cms.bfd.server.war.SpringConfiguration.SSM_PATH_SAMHSA_V2_SHADOW;

import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.InpatientClaim;
import gov.cms.bfd.model.rif.entities.OutpatientClaim;
import gov.cms.bfd.model.rif.entities.SNFClaim;
import gov.cms.bfd.server.war.commons.SecurityTagManager;
import gov.cms.bfd.server.war.r4.providers.pac.R4ClaimSamhsaMatcher;
import gov.cms.bfd.server.war.r4.providers.pac.common.ClaimWithSecurityTags;
import java.util.List;
import java.util.Set;
import org.apache.poi.ss.formula.functions.T;
import org.hl7.fhir.r4.model.Coding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * V2SamhsaConsentSimulation handles filtering based on the SAMHSA (42CFRPart2) tags in the request.
 * When the shadow flag is enabled, the class will scrub resources tagged with the 42CFRPart2
 * security tag. If the shadow flag is disabled, no filtering occurs.
 */
@Service
public class V2SamhsaConsentSimulation {

  private final V2SamhsaConsentInterceptor v2SamhsaConsentInterceptor =
      new V2SamhsaConsentInterceptor();
  private static final Logger logger = LoggerFactory.getLogger(V2SamhsaConsentSimulation.class);

  /** Flag to control whether SAMHSA shadow filtering should be applied. */
  private boolean samhsaV2Shadow;

  /** The securityTagManager. */
  private final SecurityTagManager securityTagManager;

  /** The samhsa matcher. */
  private final R4ClaimSamhsaMatcher samhsaMatcher;

  protected V2SamhsaConsentSimulation(
      @Value("${" + SSM_PATH_SAMHSA_V2_SHADOW + ":false}") Boolean samhsaV2Shadow,
      R4ClaimSamhsaMatcher samhsaMatcher,
      SecurityTagManager securityTagManager) {
    this.samhsaV2Shadow = samhsaV2Shadow;
    this.samhsaMatcher = samhsaMatcher;
    this.securityTagManager = securityTagManager;
  }

  /**
   * logs Missing Claim Ids.
   *
   * @param entity the resource being processed
   * @param hasNoSamhsaData the hasNoSamhsaData boolean
   */
  public void logMissingClaim(Object entity, boolean hasNoSamhsaData) {
    if (samhsaV2Shadow && entity instanceof ClaimWithSecurityTags<?> claimWithSecurityTags) {

      // Get security tags from the claim entity
      List<Coding> securityTags =
          securityTagManager.getClaimSecurityLevel(claimWithSecurityTags.getSecurityTags());

      // Check the redaction status of the interceptor against the SAMHSA exclusion flag
      boolean notToRedact = !(v2SamhsaConsentInterceptor.shouldRedactResource(securityTags));

      // If redaction status doesn't match the SAMHSA exclusion flag, log the claim
      if (notToRedact != hasNoSamhsaData) {
        String claimId = getClaimId(claimWithSecurityTags.getClaimEntity());
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

  /**
   * logs Missing Claim Ids.
   *
   * @param entities the resource being processed
   * @param hasNoSamhsaData the hasNoSamhsaData boolean
   */
  public void logMissingClaim(List<ClaimWithSecurityTags<T>> entities, boolean hasNoSamhsaData) {

    if (samhsaV2Shadow) {
      boolean notToRedact = false;

      for (ClaimWithSecurityTags claimWithSecurityTags : entities) {
        Set<String> securityTagsSet = claimWithSecurityTags.getSecurityTags();
        List<Coding> securityTags = securityTagManager.getClaimSecurityLevel(securityTagsSet);

        // Check the redaction status of the interceptor against the SAMHSA exclusion flag
        notToRedact = !(v2SamhsaConsentInterceptor.shouldRedactResource(securityTags));

        if (notToRedact != hasNoSamhsaData) {
          String claimId = getClaimId(claimWithSecurityTags.getClaimEntity());
          String claim =
              claimWithSecurityTags
                  .getClaimEntity()
                  .getClass()
                  .getSimpleName(); // Add class type for context

          // Logging the error with better message formatting and more context
          logger.error(
              "Samhsa: claimWithSecurityTags IDs not matching between old SAMHSA filter and new SAMHSA 2.0 - "
                  + "Claim ID: {} (Class: {})",
              claimId,
              claim);
        }
      }
    }
  }

  /**
   * Get Claim Ids.
   *
   * @param entity the resource being processed
   * @return String claim Id
   */
  public String getClaimId(Object entity) {
    if (entity == null) {
      return null; // return null if the entity is null
    }

    return switch (entity) {
      case RdaMcsClaim rdaMcsClaim -> rdaMcsClaim.getIdrClmHdIcn();
      case RdaFissClaim rdaFissClaim -> rdaFissClaim.getClaimId();
      case CarrierClaim carrierClaim -> String.valueOf(carrierClaim.getClaimId());
      case DMEClaim dmeClaim -> String.valueOf(dmeClaim.getClaimId());
      case HHAClaim hhaClaim -> String.valueOf(hhaClaim.getClaimId());
      case HospiceClaim hospiceClaim -> String.valueOf(hospiceClaim.getClaimId());
      case InpatientClaim inpatientClaim -> String.valueOf(inpatientClaim.getClaimId());
      case OutpatientClaim outpatientClaim -> String.valueOf(outpatientClaim.getClaimId());
      case SNFClaim snfClaim -> String.valueOf(snfClaim.getClaimId());
      default -> null; // Return null for unsupported claim types
    };
  }
}
