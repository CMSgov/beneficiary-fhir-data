package gov.cms.bfd.server.ng.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;

/***
 * Custom exception handling interceptor since the default one is not very customizable.
 */
@RequiredArgsConstructor
@Interceptor
public class ExceptionHandlingInterceptor {
  /**
   * Handles the server exception.
   *
   * @param requestDetails request details
   * @param exception exception
   * @param request request
   * @param response response
   * @return whether to continue the interceptor chain
   * @throws IOException if the error response fails to send
   */
  @Hook(Pointcut.SERVER_HANDLE_EXCEPTION)
  public boolean handleException(
      RequestDetails requestDetails,
      BaseServerResponseException exception,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {
    if (exception.getStatusCode() < 500) {
      return true;
    }
    // Force a default error message for any unexpected errors
    response.sendError(500, "An unexpected error occurred");
    return false;
  }
}
