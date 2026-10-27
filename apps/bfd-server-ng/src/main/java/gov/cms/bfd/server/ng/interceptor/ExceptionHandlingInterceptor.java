package gov.cms.bfd.server.ng.interceptor;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.eclipse.jetty.http.HttpException;
import org.jspecify.annotations.Nullable;

/***
 * Custom exception handling interceptor since the default one is not very customizable.
 */
@RequiredArgsConstructor
@Interceptor
public class ExceptionHandlingInterceptor {
  private static final int HTTP_ERROR_STATUS = 500;

  @Hook(Pointcut.SERVER_PRE_PROCESS_OUTGOING_EXCEPTION)
  public @Nullable BaseServerResponseException preProcessException(
      RequestDetails requestDetails,
      ServletRequestDetails servletRequestDetails,
      Throwable ex,
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse)
      throws IOException {
    // We have to explicitly set the response if some internal error is thrown as a result of a bad
    // input.
    // If this happens, the HttpException will have a valid status code that we can use to short
    // circuit the rest of the exception handling process.
    if (ex instanceof HttpException httpEx) {
      var code = httpEx.getCode();
      if (code > 0 && code < HTTP_ERROR_STATUS) {
        httpServletResponse.sendError(httpEx.getCode(), httpEx.getReason());
      }
    }
    return null;
  }

  @Hook(Pointcut.SERVER_HANDLE_EXCEPTION)
  public boolean handleException(
      RequestDetails requestDetails,
      BaseServerResponseException exception,
      HttpServletRequest request,
      HttpServletResponse response)
      throws IOException {
    if (exception.getStatusCode() < HTTP_ERROR_STATUS) {
      return true;
    }
    if (!response.isCommitted()) {
      // Force a default error message for any unexpected errors
      response.sendError(HTTP_ERROR_STATUS, "An unexpected error occurred");
    }

    return false;
  }
}
