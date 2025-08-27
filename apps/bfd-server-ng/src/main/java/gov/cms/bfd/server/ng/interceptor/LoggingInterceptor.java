package gov.cms.bfd.server.ng.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

/**
 * Interceptor for logging HAPI FHIR requests. HAPI FHIR's built-in interceptor doesn't support
 * setting the log level or structured logging.
 */
@Interceptor
public class LoggingInterceptor {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingInterceptor.class);

  /**
   * Log exceptions.
   *
   * @param requestDetails requestDetails
   * @param exception exception
   * @param request request
   * @param response response
   * @return boolean
   */
  @Hook(Pointcut.SERVER_HANDLE_EXCEPTION)
  public boolean handleException(
      RequestDetails requestDetails,
      BaseServerResponseException exception,
      HttpServletRequest request,
      HttpServletResponse response) {
    // If there's a valid status code < 500, it's an invalid input
    var logBuilder =
        exception.getStatusCode() > 0 && exception.getStatusCode() < 500
            ? LOGGER.atWarn()
            : LOGGER.atError();

    addCommonAttrs(
            logBuilder
                .setMessage(exception.getMessage())
                .addKeyValue("stackTrace", exception.getStackTrace())
                .addKeyValue("statusCode", exception.getStatusCode()),
            requestDetails)
        .log();

    return true;
  }

  /**
   * Log successful requests.
   *
   * @param requestDetails request details
   */
  @Hook(Pointcut.SERVER_PROCESSING_COMPLETED_NORMALLY)
  public void processingCompletedNormally(ServletRequestDetails requestDetails) {
    addCommonAttrs(LOGGER.atInfo().setMessage("processed request"), requestDetails).log();
  }

  private LoggingEventBuilder addCommonAttrs(
      LoggingEventBuilder eventBuilder, RequestDetails requestDetails) {
    var operationType = requestDetails.getRestOperationType();
    return eventBuilder
        .addKeyValue("resource", requestDetails.getResourceName())
        .addKeyValue("operation", requestDetails.getOperation())
        .addKeyValue("operationType", operationType == null ? "" : operationType.getCode());
  }
}
