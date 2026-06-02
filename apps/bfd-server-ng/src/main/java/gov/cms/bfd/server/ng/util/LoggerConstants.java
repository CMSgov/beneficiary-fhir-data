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

  /** Name of the logType log key. */
  public static final String LOG_TYPE = "logType";

  /** Name of message for logging bene_sk. */
  public static final String BENE_SK_FOUND = "bene_sk found";

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

  /** Name of the timestamp when the HAPI request reached pre-handle processing. */
  public static final String HAPI_INCOMING_PRE_HANDLE =
      "hapi.server_incoming_request_pre_handle_timestamp_in_millis";

  /** Name of the timestamp when the HAPI request began pre-processing. */
  public static final String HAPI_INCOMING_PRE_PROCESS =
      "hapi.server_incoming_request_pre_process_timestamp_in_millis";

  /** Name of the timestamp when the HAPI request completed post-processing. */
  public static final String HAPI_INCOMING_POST_PROCESS =
      "hapi.server_incoming_request_post_process_timestamp_in_millis";

  /** Name of the timestamp when the HAPI server began sending the response. */
  public static final String HAPI_OUTGOING_RESPONSE =
      "hapi.server_outgoing_response_timestamp_in_millis";

  /** Name of the timestamp when the HAPI requested completed normally. */
  public static final String HAPI_PROCESS_COMPLETED_NORMALLY =
      "hapi.server_processing_completed_normally_timestamp_in_millis";

  /** Name of the timestamp when the HAPI requested fully completed processing. */
  public static final String HAPI_PROCESS_COMPLETED =
      "hapi.server_processing_completed_timestamp_in_millis";

  /** Name of the timestamp when the request started. */
  public static final String REQUEST_START_TIME = "request_start_time";

  /** Name of the Accept-Encoding header. */
  public static final String HTTP_ACCESS_REQUEST_HEADER_ACCEPT_ENCODING =
      "http_access_request_header_Accept-Encoding";

  /** Name of the X-Request-ID header. */
  public static final String HTTP_ACCESS_REQUEST_HEADER_REQUEST_ID =
      "http_access_request_header_X-Request-ID";

  /** Name of the outgoing response status code. */
  public static final String HTTP_ACCESS_RESPONSE_STATUS = "http_access_response_status";

  /** Name of the Content-Length header. */
  public static final String HTTP_ACCESS_RESPONSE_CONTENT_LENGTH =
      "http_access_response_header_Content-Length";

  /** Name of the total request duration in milliseconds. */
  public static final String HTTP_ACCESS_RESPONSE_DURATION_MILLISECONDS =
      "http_access_response_duration_milliseconds";

  /** Name of the Content-Encoding header. */
  public static final String HTTP_ACCESS_RESPONSE_HEADER_ENCODING =
      "http_access_response_header_Content-Encoding";

  /** Name of the HTTP request method. */
  public static final String HTTP_ACCESS_REQUEST_HTTP_METHOD = "http_access_request_http_method";

  /** Name of the HTTP request url. */
  public static final String HTTP_ACCESS_REQUEST_URL = "http_access_request_url";

  /** Name of the HTTP request uri. */
  public static final String HTTP_ACCESS_REQUEST_URI = "http_access_request_uri";

  /** Name for the number of FHIR resources returned in the response. */
  public static final String RESOURCES_RETURNED_COUNT = "resources_returned_count";

  /** Name of the bulk-jobid header. */
  public static final String HTTP_ACCESS_REQUEST_HEADER_BULK_JOBID =
      "http_access_request_header_BULK-JOBID";

  /**
   * Name of the bulk-clientid header. This is a unique identifier of the bulk client for whom this
   * request is for (i.e. UUID, ACO ID, NPI, Part D Contract)
   */
  public static final String HTTP_ACCESS_REQUEST_HEADER_BULK_CLIENTID =
      "http_access_request_header_BULK-CLIENTID";

  /** Name for the query parameters used in a request. */
  public static final String REQUEST_QUERY_PARAMETERS = "http_access_request_query_parameters";

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
