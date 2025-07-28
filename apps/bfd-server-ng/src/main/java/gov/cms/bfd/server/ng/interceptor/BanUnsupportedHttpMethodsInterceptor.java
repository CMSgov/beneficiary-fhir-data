package gov.cms.bfd.server.ng.interceptor;

import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * Copy of the BanUnsupportedHttpMethodsInterceptor from HAPI FHIR, which sadly does not support
 * customizing the allowed methods.
 *
 * <p>It's a security best practice to disallow any HTTP methods that aren't used in the server.
 */
@Interceptor
public class BanUnsupportedHttpMethodsInterceptor extends InterceptorAdapter {
  private final Set<RequestTypeEnum> allowedMethods =
      Set.of(
          RequestTypeEnum.GET, RequestTypeEnum.POST, RequestTypeEnum.HEAD, RequestTypeEnum.OPTIONS);

  @Override
  public boolean incomingRequestPreProcessed(
      HttpServletRequest theRequest, HttpServletResponse theResponse) {
    RequestTypeEnum requestType = RequestTypeEnum.valueOf(theRequest.getMethod());
    if (allowedMethods.contains(requestType)) {
      return true;
    }

    throw new MethodNotAllowedException("Method not supported: " + theRequest.getMethod());
  }
}
