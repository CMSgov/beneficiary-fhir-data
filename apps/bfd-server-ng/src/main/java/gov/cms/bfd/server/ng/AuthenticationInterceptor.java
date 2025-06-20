package gov.cms.bfd.server.ng;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Interceptor
public class AuthenticationInterceptor {
  private final Configuration configuration;

  @Hook(Pointcut.SERVER_INCOMING_REQUEST_POST_PROCESSED)
  public boolean incomingRequestPostProcessed(
      RequestDetails theRequestDetails,
      HttpServletRequest theRequest,
      HttpServletResponse theResponse)
      throws AuthenticationException {
    String authHeader = theRequest.getHeader("Authorization");

    // Return true to allow the request to proceed
    return true;
  }
}
