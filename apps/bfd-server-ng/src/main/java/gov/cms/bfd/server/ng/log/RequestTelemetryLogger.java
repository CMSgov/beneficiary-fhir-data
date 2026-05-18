package gov.cms.bfd.server.ng.log;

import static gov.cms.bfd.server.ng.util.LoggerConstants.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/** Logs request telemetry logs. */
@AllArgsConstructor
@Component
public class RequestTelemetryLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(RequestTelemetryLogger.class);

  private static final String OBFUSCATED_MBI = "*********";

  private static final Pattern MBI_REGEX =
      // Real MBIs disallow certain characters, but we'll be conservative here for simplicity's
      // sake, and so we can easily test against synthetic MBIs.

      // See the following link for more info on the MBI format
      // https://www.cms.gov/medicare/new-medicare-card/understanding-the-mbi-with-format.pdf
      Pattern.compile("\\d\\p{Alpha}\\p{Alnum}\\d-?\\p{Alpha}\\p{Alnum}\\d-?\\p{Alpha}{2}\\d{2}");

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
    putIfPresent(HTTP_ACCESS_REQUEST_HEADER_ACCEPT_ENCODING, request.getHeader("Accept-Encoding"));
    putIfPresent(HTTP_ACCESS_REQUEST_HTTP_METHOD, request.getMethod());
    var url = replaceAllMbis(request.getRequestURL().toString());
    var uri = replaceAllMbis(request.getRequestURI());
    putIfPresent(HTTP_ACCESS_REQUEST_URL, url);
    putIfPresent(HTTP_ACCESS_REQUEST_URI, uri);
  }

  /**
   * Records response metadata and request duration.
   *
   * @param request the request
   * @param response the response
   */
  public void recordResponse(HttpServletRequest request, HttpServletResponse response) {
    MDC.put(logKey(MDC_PREFIX, HTTP_ACCESS_RESPONSE_STATUS), String.valueOf(response.getStatus()));
    putIfPresent(HTTP_ACCESS_RESPONSE_HEADER_REQUEST_ID, response.getHeader("X-Request-ID"));
    putIfPresent(HTTP_ACCESS_RESPONSE_HEADER_ENCODING, response.getHeader("Content-Encoding"));
    putIfPresent(HTTP_ACCESS_RESPONSE_CONTENT_LENGTH, response.getHeader("Content-Length"));

    var requestStart = (Long) request.getAttribute(REQUEST_START_TIME);
    if (requestStart != null) {
      MDC.put(
          logKey(MDC_PREFIX, HTTP_ACCESS_RESPONSE_DURATION_MILLISECONDS),
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

  /**
   * Emits a structured log containing a summary of all metrics and attributes of a request as
   * populated by the MDC.
   */
  public void logRequestComplete() {
    LOGGER.atInfo().setMessage("Request Completed").addKeyValue(LOG_TYPE, "requestTelemetry").log();
  }

  private void putIfPresent(String key, String value) {
    if (value != null) {
      MDC.put(logKey(MDC_PREFIX, key), value);
    }
  }

  private String replaceAllMbis(String input) {
    if (input == null) {
      return input;
    }
    var matcher = MBI_REGEX.matcher(input);
    while (matcher.find()) {
      input = input.replace(matcher.group(), OBFUSCATED_MBI);
    }
    return input;
  }
}
