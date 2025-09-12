package gov.cms.bfd.server.ng.controller;

import gov.cms.bfd.server.ng.LoggerConstants;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Handles any error redirects and redacts information that shouldn't be returned to the user. */
@RestController
public class GlobalExceptionController implements ErrorController {
  private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionController.class);

  /**
   * Handler for the error redirect. Spring will invoke /error automatically for error responses.
   *
   * @param request request
   * @return error response
   */
  @RequestMapping("/error")
  public String handleError(HttpServletRequest request) {
    var status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    var message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
    // Note: this doesn't include the query string
    var originalUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    var logBuilder = LOGGER.atError();
    // Unexpected errors could contain anything, so we should not return the text to the user.
    var responseMessage = "an unknown error occurred";

    if (status != null) {
      var statusCode = Integer.parseInt(status.toString());

      if (statusCode < 500) {
        if (message != null) {
          // For "expected" errors, we can assume the supplied error message is user-friendly, if
          // present
          responseMessage = message.toString();
        }
        // Sub-500 response codes indicate a bad request rather than an internal failure
        logBuilder = LOGGER.atWarn();
      }
      logBuilder = logBuilder.addKeyValue("statusCode", statusCode);
    }
    // Since this is controller is hit from a redirect, we don't have the context from the normal
    // MDC filter, so we need to add them to the log here explicitly,
    logBuilder
        .setMessage(responseMessage)
        .addKeyValue(LoggerConstants.URI_KEY, originalUri)
        .addKeyValue(LoggerConstants.REQUEST_ID_KEY, request.getRequestId())
        .addKeyValue(LoggerConstants.REMOTE_ADDRESS_KEY, request.getRemoteAddr())
        .log();

    return responseMessage;
  }
}
