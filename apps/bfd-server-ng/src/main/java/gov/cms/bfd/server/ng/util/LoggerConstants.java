package gov.cms.bfd.server.ng.util;

/** Constants used for logging. */
public class LoggerConstants {

  // Private constructor to prevent instantiation
  private LoggerConstants() {
    // Intentionally empty
  }

  /** Log key prefix for MDC logging. */
  public static final String MDC_PREFIX = "mdc.";

  /** Log key prefix for patient match audit logging. */
  public static final String AUDIT_PREFIX = "audit.";

  /** Name of message for logging bene_sk. */
  public static final String BENE_SK_REQUESTED = "bene_sk Requested";

  /** Name of the URI key for logging. */
  public static final String URI_KEY = "uri";

  /** Name of the request ID key for logging. */
  public static final String REQUEST_ID_KEY = "requestId";

  /** Name of the remote address key for logging. */
  public static final String REMOTE_ADDRESS_KEY = "remoteAddress";

  /** Name of the certificate alias key for MDC logging. */
  public static final String CERTIFICATE_ALIAS = "certificateAlias";

  /** Name of message for logging patient match results. */
  public static final String PATIENT_MATCH_REQUESTED = "Patient Match Requested";

  /** Name of the client IP key for logging. */
  public static final String CLIENT_IP_KEY = "clientIp";

  /** Name of the client IP header. */
  public static final String CLIENT_IP_HEADER = "X-CLIENT-IP";

  /** Name of the client name key for logging. */
  public static final String CLIENT_NAME_KEY = "clientName";

  /** Name of the client name header. */
  public static final String CLIENT_NAME_HEADER = "X-CLIENT-NAME";

  /** Name of the client ID key for logging. */
  public static final String CLIENT_ID_KEY = "clientId";

  /** Name of the client ID header. */
  public static final String CLIENT_ID_HEADER = "X-CLIENT-ID";

  /** Name of the matched beneSk for logging patient match results. */
  public static final String MATCHED_BENE_SK = "matchedBeneSk";

  /** Name of all beneSks found for logging patient match results. */
  public static final String BENE_SKS_FOUND = "beneSksFound";

  /** Name of the timestamp for logging patient match results. */
  public static final String TIMESTAMP = "timestamp";

  /** Name of all combinations evaluated for logging patient match results. */
  public static final String COMBINATIONS_EVALUATED = "combinationsEvaluated";

  /**
   * Name of the combination that successfully matched a patient for logging patient match results.
   */
  public static final String FINAL_DETERMINATION = "finalDetermination";

  /**
   * Returns a log specific key.
   *
   * @param prefix the log key prefix
   * @param key the base log field name
   * @return the prefixed log key
   */
  public static String logKey(String prefix, String key) {
    return prefix + key;
  }
}
