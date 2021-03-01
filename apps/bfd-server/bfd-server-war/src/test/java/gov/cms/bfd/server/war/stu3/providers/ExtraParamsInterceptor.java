package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import java.io.IOException;

/** A HAPI {@link IClientInterceptor} that allows us to add HTTP headers to our requests. */
public class ExtraParamsInterceptor implements IClientInterceptor {
  private RequestHeaders requestHeader;

  @Override
  public void interceptRequest(IHttpRequest theRequest) {
    // inject headers values
    requestHeader
        .getNVPairs()
        .forEach(
            (n, v) -> {
              theRequest.addHeader(n, v.toString());
            });
  }

  /**
   * @see
   *     ca.uhn.fhir.rest.client.api.IClientInterceptor#interceptResponse(ca.uhn.fhir.rest.client.api.IHttpResponse)
   */
  @Override
  public void interceptResponse(IHttpResponse theResponse) throws IOException {
    // nothing needed here
  }

  public void setHeaders(RequestHeaders requestHeader) {
    this.requestHeader = requestHeader;
  }
}
