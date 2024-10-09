package gov.cms.bfd.server.sharedutils;

import org.slf4j.MDC;
import org.slf4j.spi.MDCAdapter;

/**
 * Wrapper for to the {@link MDC} class (used in logging). Historically, we have used "." to delimit
 * parts of the MDC keys, such as "http_access.request.header". For AWS CloudWatch Metrics, though,
 * the "." character is not supported, so we need to change these to "_".
 *
 * <p>Note that some static MDC expressions below are not used to explicitly set these values in our
 * requests/responses; some MDC values are automatically added internal to our libraries and the
 * values in this class are used only for convenience in tests.
 */
public class BfdMDC {

  /**
   * Delimiter that used to be used to separate the parts of MDC field names; we want to replace
   * this with TO_DELIMITER.
   */
  public static final String FROM_DELIMITER = ".";

  /**
   * Delimiter that we want to be used to separate the parts of MDC field names; we want to replace
   * FROM_DELIMITER with this.
   */
  public static final String TO_DELIMITER = "_";

  /**
   * MDC Adapter explicitly set in {@link #setMDCAdapter(MDCAdapter)} (if any). Unless this is set,
   * use the MDC class's adapter. You normally wouldn't set this explicitly, but it can be useful
   * for mocked test MDC adapters, for example.
   */
  private static MDCAdapter mdcAdapter = null;

  /** MDC key for the mdc prefix database query. */
  private static final String DATABASE_QUERY_PREFIX = "database_query_bene_by_id";

  /** MDC key for the mdc prefix header. */
  private static final String HEADER_PREFIX = "header";

  /** MDC key for the include true prefix. */
  private static final String INCLUDE_TRUE_PREFIX = "include_true";

  /** MDC key for the include HICNS and MBIs prefix. */
  private static final String INCLUDE_HICNS_MBIS_PREFIX = "include_hicns_and_mbis";

  /** MDC key for the mdc prefix jpa query. */
  private static final String JPA_PREFIX = "jpa_query_bene_by_id";

  /** MDC key for the mdc prefix http access. */
  private static final String MDC_PREFIX = "http_access";

  /** MDC key for the mdc prefix hapi server. */
  private static final String MDC_HAPI_PREFIX = "hapi_server";

  /** MDC key for the http request prefix. */
  private static final String REQUEST_PREFIX = "request";

  /** MDC key for the http response prefix. */
  private static final String RESPONSE_PREFIX = "response";

  /** MDC key for the hapi incoming request prefix. */
  private static final String HAPI_INCOMING_REQ_PREFIX = "incoming_request";

  /** MDC key for the hapi timestamp prefix. */
  private static final String HAPI_TIMESTAMP_PREFIX = "timestamp_in_millis";

  /** MDC key for the bene_id. */
  public static final String BENE_ID = computeMDCKey("bene_id");

  /** MDC key for the database query batch. */
  public static final String DATABASE_QUERY_BATCH =
      computeMDCKey(DATABASE_QUERY_PREFIX, INCLUDE_HICNS_MBIS_PREFIX, "batch");

  /** MDC key for the database query type. */
  public static final String DATABASE_QUERY_TYPE =
      computeMDCKey(DATABASE_QUERY_PREFIX, INCLUDE_HICNS_MBIS_PREFIX, "type");

  /** MDC key for the database query batch size. */
  public static final String DATABASE_QUERY_BATCH_SIZE =
      computeMDCKey(DATABASE_QUERY_PREFIX, INCLUDE_HICNS_MBIS_PREFIX, "batch_size");

  /** MDC key for the database query size. */
  public static final String DATABASE_QUERY_SIZE =
      computeMDCKey(DATABASE_QUERY_PREFIX, INCLUDE_HICNS_MBIS_PREFIX, "size");

  /** MDC key for the database query duration in milliseconds. */
  public static final String DATABASE_QUERY_MILLI =
      computeMDCKey(DATABASE_QUERY_PREFIX, INCLUDE_HICNS_MBIS_PREFIX, "duration_milliseconds");

  /** MDC key for the database query success. */
  public static final String DATABASE_QUERY_SUCCESS =
      computeMDCKey(DATABASE_QUERY_PREFIX, INCLUDE_HICNS_MBIS_PREFIX, "success");

  /** MDC key for the database query datasource name. */
  public static final String DATABASE_QUERY_SOURCE_NAME =
      computeMDCKey(DATABASE_QUERY_PREFIX, INCLUDE_HICNS_MBIS_PREFIX, "datasource_name");

  /** MDC key for the HAPI outgoing response timestamp in milliseconds. */
  public static final String HAPI_RESPONSE_TIMESTAMP_MILLI =
      computeMDCKey(MDC_HAPI_PREFIX, "outgoing_response", HAPI_TIMESTAMP_PREFIX);

  /** MDC key for the HAPI incoming request post-process timestamp in milliseconds. */
  public static final String HAPI_POST_PROCESS_TIMESTAMP_MILLI =
      computeMDCKey(
          MDC_HAPI_PREFIX, HAPI_INCOMING_REQ_PREFIX, "post_process", HAPI_TIMESTAMP_PREFIX);

  /** MDC key for the HAPI incoming request pre-handle timestamp in milliseconds. */
  public static final String HAPI_PRE_HANDLE_TIMESTAMP_MILLI =
      computeMDCKey(MDC_HAPI_PREFIX, HAPI_INCOMING_REQ_PREFIX, "pre_handle", HAPI_TIMESTAMP_PREFIX);

  /** MDC key for the HAPI incoming request pre-process timestamp in milliseconds. */
  public static final String HAPI_PRE_PROCESS_TIMESTAMP_MILLI =
      computeMDCKey(
          MDC_HAPI_PREFIX, HAPI_INCOMING_REQ_PREFIX, "pre_process", HAPI_TIMESTAMP_PREFIX);

  /** MDC key for the HAPI processing completed timestamp in milliseconds. */
  public static final String HAPI_PROCESSING_COMPLETED_TIMESTAMP_MILLI =
      computeMDCKey(MDC_HAPI_PREFIX, "processing_completed", HAPI_TIMESTAMP_PREFIX);

  /** MDC key for the HAPI processing completed normally timestamp in milliseconds. */
  public static final String HAPI_PROCESSING_COMPLETED_NORM_TIMESTAMP_MILLI =
      computeMDCKey(MDC_HAPI_PREFIX, "processing_completed_normally", HAPI_TIMESTAMP_PREFIX);

  /** MDC key for the http request client ssl dn. */
  public static final String HTTP_ACCESS_REQUEST_CLIENTSSL_DN =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "clientSSL_DN");

  /** MDC key for the http request header include tax numbers. */
  public static final String HTTP_ACCESS_REQUEST_HEADER_TAX_NUMBERS =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, HEADER_PREFIX, "IncludeTaxNumbers");

  /** MDC key for the http request header accept. */
  public static final String HTTP_ACCESS_REQUEST_HEADER_ACCEPT =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, HEADER_PREFIX, "Accept");

  /** MDC key for the http request header accept charset. */
  public static final String HTTP_ACCESS_REQUEST_HEADER_ACCEPT_CHARSET =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, HEADER_PREFIX, "Accept-Charset");

  /** MDC key for the http request header accept encoding. */
  public static final String HTTP_ACCESS_REQUEST_HEADER_ACCEPT_ENCODING =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, HEADER_PREFIX, "Accept-Encoding");

  /** MDC key for the http request header include address fields. */
  public static final String HTTP_ACCESS_REQUEST_HEADER_ADDRESS_FIELDS =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, HEADER_PREFIX, "IncludeAddressFields");

  /** MDC key for the http request header connection. */
  public static final String HTTP_ACCESS_REQUEST_HEADER_CONN_ENCODING =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, HEADER_PREFIX, "Connection");

  /** MDC key for the http request header host. */
  public static final String HTTP_ACCESS_REQUEST_HEADER_HOST_ENCODING =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, HEADER_PREFIX, "Host");

  /** MDC key for the http request header include identifiers. */
  public static final String HTTP_ACCESS_REQUEST_HEADER_IDENTIFIERS =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, HEADER_PREFIX, "IncludeIdentifiers");

  /** MDC key for the http request header user agent. */
  public static final String HTTP_ACCESS_REQUEST_HEADER_USER_AGENT =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, HEADER_PREFIX, "User-Agent");

  /** MDC key for the http request http method. */
  public static final String HTTP_ACCESS_REQUEST_HTTP_METHOD =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "http_method");

  /** MDC key for the http request operation. */
  public static final String HTTP_ACCESS_REQUEST_OPERATION =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "operation");

  /** MDC key for the http request query string. */
  public static final String HTTP_ACCESS_REQUEST_QUERY_STR =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "query_string");

  /** MDC key for the http request type. */
  public static final String HTTP_ACCESS_REQUEST_TYPE =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "type");

  /** MDC key for the http request url. */
  public static final String HTTP_ACCESS_REQUEST_URL =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "url");

  /** MDC key for the http request uri. */
  public static final String HTTP_ACCESS_REQUEST_URI =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, "uri");

  /** MDC key for the http request header content type. */
  public static final String HTTP_ACCESS_REQUEST_HEADER_CONTENT_TYPE =
      computeMDCKey(MDC_PREFIX, REQUEST_PREFIX, HEADER_PREFIX, "Content-Type");

  /** MDC key for the http request_Start_Key. */
  public static final String REQUEST_START_KEY =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "start_milliseconds");

  /** MDC key for the http response duration per kb. */
  public static final String HTTP_ACCESS_RESPONSE_DURATION_PER_KB =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "duration_per_kb");

  /** MDC key for the http response duration milliseconds. */
  public static final String HTTP_ACCESS_RESPONSE_DURATION_MILLISECONDS =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "duration_milliseconds");

  /** MDC key for the http response header content encoding. */
  public static final String HTTP_ACCESS_RESPONSE_HEADER_ENCODING =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, HEADER_PREFIX, "Content-Encoding");

  /**
   * MDC key for the http response header content location. This gets set on read operations to
   * return the caller uri path, likely set by HAPI-FHIR or Spring (as it's programmatically added
   * to the MDC along with other response headers in RequestResponsePopulateMdcFilter).
   */
  public static final String HTTP_ACCESS_RESPONSE_HEADER_CONTENT_LOCATION =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, HEADER_PREFIX, "Content-Location");

  /** MDC key for the http response header content length. */
  public static final String HTTP_ACCESS_RESPONSE_CONTENT_LENGTH =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, HEADER_PREFIX, "Content-Length");

  /** MDC key for the http response header content type. */
  public static final String HTTP_ACCESS_RESPONSE_HEADER_CONTENT_TYPE =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, HEADER_PREFIX, "Content-Type");

  /** MDC key for the http response header date. */
  public static final String HTTP_ACCESS_RESPONSE_HEADER_DATE =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, HEADER_PREFIX, "Date");

  /** MDC key for the http response header last modified. */
  public static final String HTTP_ACCESS_RESPONSE_HEADER_LAST_MODIFIED =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, HEADER_PREFIX, "Last-Modified");

  /** MDC key for the http response header x powered by. */
  public static final String HTTP_ACCESS_RESPONSE_HEADER_POWERED_BY =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, HEADER_PREFIX, "X-Powered-By");

  /** MDC key for the http response header x request id. */
  public static final String HTTP_ACCESS_RESPONSE_HEADER_REQUEST_ID =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, HEADER_PREFIX, "X-Request-ID");

  /** MDC key for the http output size in bytes. */
  public static final String HTTP_ACCESS_RESPONSE_OUTPUT_SIZE_IN_BYTES =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "output_size_in_bytes");

  /** MDC key for the http output status. */
  public static final String HTTP_ACCESS_RESPONSE_STATUS =
      computeMDCKey(MDC_PREFIX, RESPONSE_PREFIX, "status");

  /** MDC key for the jpa query include true. */
  public static final String JPA_QUERY_INCLUDE_TRUE =
      computeMDCKey(JPA_PREFIX, INCLUDE_TRUE_PREFIX);

  /** MDC key for the jpa query include true duration nanoseconds. */
  public static final String JPA_QUERY_DURATION_NANOSECONDS =
      computeMDCKey(JPA_PREFIX, INCLUDE_TRUE_PREFIX, "duration_nanoseconds");

  /** MDC key for the jpa query include true record count. */
  public static final String JPA_QUERY_RECORD_COUNT =
      computeMDCKey(JPA_PREFIX, INCLUDE_TRUE_PREFIX, "record_count");

  /** MDC key for the resources returned. */
  public static final String RESOURCES_RETURNED = computeMDCKey("resources_returned_count");

  /** MDC key for MBI Hash. */
  public static final String MBI_HASH = "mbi_hash";

  /** MDC key for MBI Id. */
  public static final String MBI_ID = "mbi_id";

  /**
   * Set the MDC Adapter. Normally this is not needed, but it can be useful in testing.
   *
   * @param adapter The MDC Adapter to use.
   */
  public static void setMDCAdapter(MDCAdapter adapter) {
    mdcAdapter = adapter;
  }

  /**
   * Get the MDC Adapter, generally via {@link MDC#getMDCAdapter()}.
   *
   * @return The MDC Adapter being used.
   */
  public static MDCAdapter getMDCAdapter() {
    if (mdcAdapter == null) {
      return MDC.getMDCAdapter();
    }
    return mdcAdapter;
  }

  /**
   * Compute a key for {@link MDC} from several parts.
   *
   * @param keys Pieces of the key to be joined together
   * @return The final key
   */
  public static String computeMDCKey(String... keys) {
    return String.join("_", keys);
  }

  /**
   * Reformat an identifier for an {@link MDC} key, replacing "." with "_".
   *
   * @param key Key to reformat
   * @return Text of the key that we'll put into MDC
   */
  public static String formatMDCKey(String key) {
    return key.replace(FROM_DELIMITER, TO_DELIMITER);
  }

  /**
   * Wrapper for {@link MDC#put(String,String)}, except that we format the key.
   *
   * @param key Key for later finding the value
   * @param value Value put into MDC
   */
  public static void put(String key, String value) {
    getMDCAdapter().put(formatMDCKey(key), value);
  }

  /**
   * Wrapper for {@link MDC#get(String)}, except that we format the key.
   *
   * @param key Key for later finding the value
   * @return Value put into MDC
   */
  public static String get(String key) {
    return getMDCAdapter().get(formatMDCKey(key));
  }

  /** Wrapper for {@link org.slf4j.MDC#clear()}. */
  public static void clear() {
    getMDCAdapter().clear();
  }
}
