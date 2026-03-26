package gov.cms.bfd.server.ng.util;

/** Constants used for logging. */
public class LoggerConstants {

  // Private constructor to prevent instantiation
  private LoggerConstants() {
    // Intentionally empty
  }

  /** Name of message for logging bene_sk. */
  public static final String BENE_SK_REQUESTED = "bene_sk Requested";

  /** Name of the URI key for logging. */
  public static final String URI_KEY = "uri";

  /** Name of the request ID key for logging. */
  public static final String REQUEST_ID_KEY = "requestId";

  /** Name of the remote address key for logging. */
  public static final String REMOTE_ADDRESS_KEY = "remoteAddress";

  /** Name of the certificate alias key for logging. */
  public static final String CERTIFICATE_ALIAS = "certificateAlias";

  /** Name of message for logging patient match results. */
  public static final String PATIENT_MATCH_REQUESTED = "Patient Match Requested";

  /** Name of the Client IP key from $IDI-MATCH calls to BFD for logging. */
  public static final String CLIENT_IP_KEY = "clientIp";

  public static final String CLIENT_IP_HEADER = "X-CLIENT-IP";

  /** Name of the Client Name key from $IDI-MATCH calls to BFD for logging. */
  public static final String CLIENT_NAME_KEY = "clientName";

  public static final String CLIENT_NAME_HEADER = "X-CLIENT-NAME";

  /** Name of the Client ID key from $IDI-MATCH calls to BFD for logging. */
  public static final String CLIENT_ID_KEY = "clientId";

  public static final String CLIENT_ID_HEADER = "X-CLIENT-ID";
}
