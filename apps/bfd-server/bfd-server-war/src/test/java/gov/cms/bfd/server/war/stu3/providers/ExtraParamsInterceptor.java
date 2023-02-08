package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import gov.cms.bfd.server.war.commons.RequestHeaders;
import java.io.IOException;

/** A HAPI {@link IClientInterceptor} that allows us to add HTTP headers to our requests. */
public class ExtraParamsInterceptor implements IClientInterceptor {
  /** The request header. */
  private RequestHeaders requestHeader;

  /** {@inheritDoc} */
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

  /** {@inheritDoc} */
  @Override
  public void interceptResponse(IHttpResponse theResponse) throws IOException {
    // nothing needed here
  }

  /**
   * Sets the {@link #requestHeader}.
   *
   * @param requestHeader the request header
   */
  public void setHeaders(RequestHeaders requestHeader) {
    this.requestHeader = requestHeader;
  }
}
