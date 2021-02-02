package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import java.io.IOException;
import java.util.Optional;

/** A HAPI {@link IClientInterceptor} that allows us to add HTTP headers to our requests. */
public class ExtraParamsInterceptor implements IClientInterceptor {
  private Optional<String> includeIdentifiersValue = Optional.empty();

  /**
   * @see
   *     ca.uhn.fhir.rest.client.api.IClientInterceptor#interceptRequest(ca.uhn.fhir.rest.client.api.IHttpRequest)
   */
  @Override
  public void interceptRequest(IHttpRequest theRequest) {
    if (includeIdentifiersValue.isPresent())
      theRequest.addHeader(
          PatientResourceProvider.HEADER_NAME_INCLUDE_IDENTIFIERS, includeIdentifiersValue.get());
  }

  /**
   * @see
   *     ca.uhn.fhir.rest.client.api.IClientInterceptor#interceptResponse(ca.uhn.fhir.rest.client.api.IHttpResponse)
   */
  @Override
  public void interceptResponse(IHttpResponse theResponse) throws IOException {
    // nothing needed here
  }

  /**
   * Sets the {@link PatientResourceProvider#HEADER_NAME_INCLUDE_IDENTIFIERS} header for any/all
   * requests using this {@link ExtraParamsInterceptor}.
   *
   * @param includeIdentifiersValue the value to supply in the HTTP header
   */
  public void setIncludeIdentifiers(String includeIdentifiersValue) {
    this.includeIdentifiersValue = Optional.of(includeIdentifiersValue);
  }
}
