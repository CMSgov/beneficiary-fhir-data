package gov.cms.bfd.server.ng.util;

/** Constants used for logging. */
public class LoggerConstants {

  // Private constructor to prevent instantiation
  private LoggerConstants() {
    // Intentionally empty
  }

  private static final String MDC_PREFIX = "mdc.";

  /** Name of message for logging bene_sk. */
  public static final String BENE_SK_REQUESTED = "bene_sk Requested";

  /** Name of the URI key for logging. */
  public static final String URI_KEY = "uri";

  /** Name of the URI key for MDC logging. */
  public static final String MDC_URI_KEY = MDC_PREFIX + URI_KEY;

  /** Name of the request ID key for logging. */
  public static final String REQUEST_ID_KEY = "requestId";

  /** Name of the request ID key for MDC logging. */
  public static final String MDC_REQUEST_ID_KEY = MDC_PREFIX + REQUEST_ID_KEY;

  /** Name of the remote address key for logging. */
  public static final String REMOTE_ADDRESS_KEY = "remoteAddress";

  /** Name of the remote address key for MDC logging. */
  public static final String MDC_REMOTE_ADDRESS_KEY = MDC_PREFIX + REMOTE_ADDRESS_KEY;

  /** Name of the certificate alias key for MDC logging. */
  public static final String MDC_CERTIFICATE_ALIAS = MDC_PREFIX + "certificateAlias";

  /** Name of message for logging patient match results. */
  public static final String PATIENT_MATCH_REQUESTED = "Patient Match Requested";

  /** Name of the client IP key from $IDI-MATCH calls to BFD for MDC logging. */
  public static final String MDC_CLIENT_IP_KEY = MDC_PREFIX + "clientIp";

  /** Name of the client IP header. */
  public static final String CLIENT_IP_HEADER = "X-CLIENT-IP";

  /** Name of the client Name key from $IDI-MATCH calls to BFD for MDC logging. */
  public static final String MDC_CLIENT_NAME_KEY = MDC_PREFIX + "clientName";

  /** Name of the client name header. */
  public static final String CLIENT_NAME_HEADER = "X-CLIENT-NAME";

  /** Name of the client ID key from $IDI-MATCH calls to BFD for MDC logging. */
  public static final String MDC_CLIENT_ID_KEY = MDC_PREFIX + "clientId";

  /** Name of the client ID header. */
  public static final String CLIENT_ID_HEADER = "X-CLIENT-ID";
}
