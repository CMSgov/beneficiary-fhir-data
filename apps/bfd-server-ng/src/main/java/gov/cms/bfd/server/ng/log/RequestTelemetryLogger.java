package gov.cms.bfd.server.ng.log;

import static gov.cms.bfd.server.ng.util.LoggerConstants.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.stereotype.Component;

/** Logs request telemetry logs. */
@Component
public class RequestTelemetryLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestTelemetryLogger.class);

  /**
   * Records the current timestamp to the MDC using the supplied key.
   *
   * @param key the base log field name
   */
  public void recordTimestamp(String key) {
    MDC.put(logKey(MDC_PREFIX, key), String.valueOf(System.currentTimeMillis()));
  }

  /**
   * Records the request start timestamp to later determine request duration.
   *
   * @param request the request
   */
  public void setRequestStartTime(HttpServletRequest request) {
    request.setAttribute(REQUEST_START_TIME, System.currentTimeMillis());
  }

  /**
   * Records selected incoming request headers.
   *
   * @param request the request
   */
  public void recordRequestHeaders(HttpServletRequest request) {
    putIfPresent(HTTP_ACCEPT_ENCODING, request.getHeader("Accept-Encoding"));
  }

  /**
   * Records response metadata and request duration.
   *
   * @param request the request
   * @param response the response
   */
  public void recordResponse(HttpServletRequest request, HttpServletResponse response) {
    MDC.put(logKey(MDC_PREFIX, HTTP_RESPONSE_STATUS), String.valueOf(response.getStatus()));
    putIfPresent(HTTP_RESPONSE_X_REQUEST_ID, response.getHeader("X-Request-ID"));
    putIfPresent(HTTP_RESPONSE_CONTENT_ENCODING, response.getHeader("Content-Encoding"));
    putIfPresent(HTTP_RESPONSE_CONTENT_LENGTH, response.getHeader("Content-Length"));

    var requestStart = (Long) request.getAttribute(REQUEST_START_TIME);
    if (requestStart != null) {
      MDC.put(
          logKey(MDC_PREFIX, HTTP_RESPONSE_DURATION_MS),
          String.valueOf(System.currentTimeMillis() - requestStart));
    }
  }

  /**
   * Records the number of FHIR resources returned for the request.
   *
   * @param count number of resources returned
   */
  public void recordResourcesReturned(int count) {
    MDC.put(logKey(MDC_PREFIX, RESOURCES_RETURNED_COUNT), String.valueOf(count));
  }

  /** Emits a structured log containing all metrics and attributes of a request. */
  public void logRequestComplete() {

    var logBuilder =
        LOGGER.atInfo().setMessage("Request Completed").addKeyValue(LOG_TYPE, "requestTelemetry");

    logBuilder = addMdcIfPresent(logBuilder, HAPI_INCOMING_PRE_HANDLE);
    logBuilder = addMdcIfPresent(logBuilder, HAPI_INCOMING_PRE_PROCESS);
    logBuilder = addMdcIfPresent(logBuilder, HAPI_INCOMING_POST_PROCESS);
    logBuilder = addMdcIfPresent(logBuilder, HAPI_PROCESS_COMPLETED_NORMALLY);
    logBuilder = addMdcIfPresent(logBuilder, HAPI_PROCESS_COMPLETED);
    logBuilder = addMdcIfPresent(logBuilder, HAPI_OUTGOING_RESPONSE);

    logBuilder = addMdcIfPresent(logBuilder, HTTP_ACCEPT_ENCODING);
    logBuilder = addMdcIfPresent(logBuilder, HTTP_RESPONSE_X_REQUEST_ID);
    logBuilder = addMdcIfPresent(logBuilder, HTTP_RESPONSE_STATUS);
    logBuilder = addMdcIfPresent(logBuilder, HTTP_RESPONSE_CONTENT_LENGTH);
    logBuilder = addMdcIfPresent(logBuilder, HTTP_RESPONSE_DURATION_MS);
    logBuilder = addMdcIfPresent(logBuilder, HTTP_RESPONSE_CONTENT_ENCODING);

    logBuilder = addMdcIfPresent(logBuilder, RESOURCES_RETURNED_COUNT);

    logBuilder.log();
  }

  private void putIfPresent(String key, String value) {
    if (value != null) {
      MDC.put(logKey(MDC_PREFIX, key), value);
    }
  }

  private LoggingEventBuilder addMdcIfPresent(
      LoggingEventBuilder loggingEventBuilder, String mdcKey) {
    var value = MDC.get(MDC_PREFIX + mdcKey);
    if (value != null) {
      return loggingEventBuilder.addKeyValue(mdcKey, value);
    }
    return loggingEventBuilder;
  }
}
