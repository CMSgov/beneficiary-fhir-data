package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import java.io.IOException;
import java.util.Optional;

/** A HAPI {@link IClientInterceptor} that allows us to add HTTP headers to our requests. */
public class ExtraParamsInterceptor implements IClientInterceptor {
  private RequestHeaders requestHeader;
  // private IHttpRequest theRequest;
  // private String includeIdentifiersValues = "";
  // private String includeAddressValues = "";

  @Override
  public void interceptRequest(IHttpRequest theRequest) {
    // String headerValue = includeIdentifiersValues;
    // String headerAddressValue = includeAddressValues;

    // inject headers values
    requestHeader
        .getNVPairs()
        .forEach(
            (n, v) -> {
              theRequest.addHeader(n, v.toString());
            });
    // theRequest.addHeader(PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, headerValue);
    // theRequest.addHeader(
    //     PatientResourceProvider.HEADER_NAME_INCLUDE_ADDRESS_FIELDS, headerAddressValue);
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
