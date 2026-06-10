package gov.cms.bfd.server.ng.log;

import static gov.cms.bfd.server.ng.util.LoggerConstants.*;
import static gov.cms.bfd.server.ng.util.MetricRecorder.*;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import gov.cms.bfd.server.ng.util.CertificateUtil;
import gov.cms.bfd.server.ng.util.MetricRecorder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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

  private static final List<HeaderMapping> REQUEST_HEADER_MAPPINGS =
      List.of(
          new HeaderMapping(HTTP_ACCESS_REQUEST_HEADER_REQUEST_ID, "X-Request-ID"),
          new HeaderMapping(HTTP_ACCESS_REQUEST_HEADER_BULK_JOBID, "BULK-JOBID"),
          new HeaderMapping(HTTP_ACCESS_REQUEST_HEADER_BULK_CLIENTID, "BULK-CLIENTID"),
          new HeaderMapping(HTTP_ACCESS_REQUEST_HEADER_ACCEPT_ENCODING, "Accept-Encoding"),
          new HeaderMapping(CLIENT_IP_KEY, CLIENT_IP_HEADER),
          new HeaderMapping(CLIENT_NAME_KEY, CLIENT_NAME_HEADER),
          new HeaderMapping(CLIENT_ID_KEY, CLIENT_ID_HEADER));

  private record HeaderMapping(String mdcKey, String header) {}

  private final CertificateUtil certificateUtil;

  private final MetricRecorder metricRecorder;

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
    certificateUtil
        .getAliasFromCert(request)
        .ifPresent(
            alias -> {
              certificateUtil.attachCertAliasToRequest(request, alias);
              MDC.put(logKey(MDC_PREFIX, CERTIFICATE_ALIAS), alias);
            });

    putIfPresent(REQUEST_ID_KEY, request.getRequestId());
    putIfPresent(REMOTE_ADDRESS_KEY, request.getRemoteAddr());
    putIfPresent(HTTP_ACCESS_REQUEST_HTTP_METHOD, request.getMethod());
    putIfPresent(HTTP_ACCESS_REQUEST_URL, replaceAllMbis(request.getRequestURL().toString()));
    putIfPresent(HTTP_ACCESS_REQUEST_URI, replaceAllMbis(request.getRequestURI()));

    for (var mapping : REQUEST_HEADER_MAPPINGS) {
      putIfPresent(mapping.mdcKey, request.getHeader(mapping.header));
    }
  }

  /**
   * Records response metadata and emits a structured log containing a summary of all metrics.
   *
   * @param request the request
   * @param response the response
   */
  public void recordResponse(HttpServletRequest request, HttpServletResponse response) {
    var responseStatusCode = response.getStatus();
    MDC.put(logKey(MDC_PREFIX, HTTP_ACCESS_RESPONSE_STATUS), String.valueOf(responseStatusCode));
    putIfPresent(HTTP_ACCESS_RESPONSE_HEADER_ENCODING, response.getHeader("Content-Encoding"));
    putIfPresent(HTTP_ACCESS_RESPONSE_CONTENT_LENGTH, response.getHeader("Content-Length"));

    recordMetrics(request, response);

    var reuquestLogBuilder =
        LOGGER.atInfo().setMessage("Request Completed").addKeyValue(LOG_TYPE, "requestTelemetry");
    var queryParams = request.getAttribute(REQUEST_QUERY_PARAMETERS);
    if (queryParams != null) {
      reuquestLogBuilder = reuquestLogBuilder.addKeyValue(REQUEST_QUERY_PARAMETERS, queryParams);
    }
    reuquestLogBuilder.addKeyValue(HTTP_ACCESS_RESPONSE_STATUS, responseStatusCode).log();
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
   * Records HAPI request metadata into MDC.
   *
   * @param requestDetails the request details
   */
  public void recordRequestDetails(RequestDetails requestDetails) {
    putIfPresent(RESOURCE_REQUESTED, requestDetails.getResourceName());
    putIfPresent(OPERATION, requestDetails.getOperation());
    var operationType = requestDetails.getRestOperationType();
    putIfPresent(OPERATION_TYPE, operationType.getCode());

    // stores the sanitized params in the request instead of the MDC to later add them directly to
    // the log event so we can actually query them through log insights
    var params = requestDetails.getParameters();
    if (!params.isEmpty()
        && requestDetails instanceof ServletRequestDetails servletRequestDetails) {
      var sanitizedParams = new HashMap<>();
      for (var entry : params.entrySet()) {
        var sanitizedValues = Arrays.stream(entry.getValue()).map(this::replaceAllMbis).toList();
        sanitizedParams.put(entry.getKey(), sanitizedValues);
      }
      servletRequestDetails
          .getServletRequest()
          .setAttribute(REQUEST_QUERY_PARAMETERS, sanitizedParams);
    }
  }

  /**
   * Logs a request exception.
   *
   * @param exception the exception
   */
  public void logRequestException(BaseServerResponseException exception) {
    // If there's a valid status code < 500, it's an invalid input
    var logBuilder =
        exception.getStatusCode() > 0 && exception.getStatusCode() < 500
            ? LOGGER.atWarn()
            : LOGGER.atError();
    logBuilder
        .setMessage(exception.getMessage())
        .addKeyValue(LOG_TYPE, "requestException")
        .addKeyValue("stackTrace", exception.getStackTrace())
        .addKeyValue("statusCode", exception.getStatusCode())
        .log();
  }

  private void recordMetrics(HttpServletRequest request, HttpServletResponse response) {
    if (request.getAttribute(REQUEST_START_TIME) instanceof Long requestStart) {
      var duration = System.currentTimeMillis() - requestStart;

      MDC.put(
          logKey(MDC_PREFIX, HTTP_ACCESS_RESPONSE_DURATION_MILLISECONDS), String.valueOf(duration));

      metricRecorder.recordDuration(
          OVERALL_REQUEST_LATENCY_METRIC, duration, TimeUnit.MILLISECONDS);

      var resourceRequested =
          getMdcValue(RESOURCE_REQUESTED)
              .orElseGet(
                  () -> request.getRequestURI().contains("/metadata") ? "metadata" : "unknown");
      var certificateAlias = getMdcValue(CERTIFICATE_ALIAS);

      if (certificateAlias.isPresent()) {
        metricRecorder.recordDuration(
            REQUEST_LATENCY_BY_PARTNER_METRIC,
            duration,
            TimeUnit.MILLISECONDS,
            ENDPOINT,
            resourceRequested,
            CLIENT,
            certificateAlias.get());
        metricRecorder.incrementCounter(
            REQUEST_COUNT_PER_PARTNER_METRIC, CLIENT, certificateAlias.get());
        metricRecorder.incrementCounter(
            REQUEST_COUNT_PER_ENDPOINT_PER_PARTNER_METRIC,
            CLIENT,
            certificateAlias.get(),
            ENDPOINT,
            resourceRequested);
        metricRecorder.incrementCounter(
            OVERALL_REQUEST_COUNT_PER_ENDPOINT_METRIC, ENDPOINT, resourceRequested);
      }
    }

    var responseStatusCode = response.getStatus();

    if (responseStatusCode >= 400) {
      metricRecorder.incrementCounter(
          responseStatusCode < 500 ? RESPONSES_4XX_METRIC : RESPONSES_5XX_METRIC,
          RESPONSE_STATUS,
          String.valueOf(responseStatusCode));
    }
  }

  private void putIfPresent(String key, String value) {
    if (value != null) {
      MDC.put(logKey(MDC_PREFIX, key), value);
    }
  }

  private Optional<String> getMdcValue(String key) {
    return Optional.ofNullable(MDC.get(logKey(MDC_PREFIX, key)));
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
