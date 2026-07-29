package gov.cms.bfd.server.ng.util;

import static gov.cms.bfd.server.ng.util.LoggerConstants.LOG_TYPE;

import gov.cms.bfd.server.ng.claim.model.ClaimBase;
import java.util.Collection;
import org.slf4j.LoggerFactory;

/** Utility class used for logging. */
public class LogUtil {

  // Private constructor to prevent instantiation
  private LogUtil() {
    // Intentionally empty
  }

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(LogUtil.class);

  /**
   * For logging bene_sk.
   *
   * @param beneSk the bene_sk
   */
  public static void logBeneSk(Long beneSk) {
    LOGGER
        .atInfo()
        .setMessage(LoggerConstants.BENE_SK_FOUND)
        .addKeyValue(LOG_TYPE, "beneFound")
        .addKeyValue("beneSk", beneSk)
        .log();
  }

  /**
   * For logging distinct bene_sk from claims.
   *
   * @param claims the claims
   */
  public static void logUniqueBeneficiaries(Collection<? extends ClaimBase> claims) {
    if (claims.isEmpty()) {
      return;
    }
    claims.stream()
        .map(claim -> claim.getBeneficiary().getBeneSk())
        .distinct()
        .forEach(LogUtil::logBeneSk);
  }
}
