package gov.cms.bfd.server.ng.controller;

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

    if (status != null) {
      var statusCode = Integer.parseInt(status.toString());
      // For "expected" errors, we can assume the supplied error message is user-friendly, if
      // present
      if (statusCode < 500) {
        var responseMessage = message == null ? "error" : message.toString();
        LOGGER
            .atWarn()
            .setMessage(responseMessage)
            .addKeyValue("statusCode", statusCode)
            .addKeyValue("originalUri", originalUri)
            .log();
        return responseMessage;
      }
    }
    // Unexpected errors could contain anything, so we should not return the text to the user.
    return "an unknown error occurred";
  }
}
