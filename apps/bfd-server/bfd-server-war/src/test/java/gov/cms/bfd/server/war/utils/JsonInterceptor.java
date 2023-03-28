package gov.cms.bfd.server.war.utils;

import ca.uhn.fhir.rest.client.api.IClientInterceptor;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.api.IHttpResponse;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;

/** An interceptor class to save endpoint responses from requests to a fhirClient. */
public class JsonInterceptor implements IClientInterceptor {
  /** The Json response. */
  private String response;

  /** {@inheritDoc} */
  @Override
  public void interceptRequest(IHttpRequest theRequest) {
    // TODO Auto-generated method stub

  }

  /** {@inheritDoc} */
  @Override
  public void interceptResponse(IHttpResponse theResponse) throws IOException {
    /*
     * The following code comes from {@link LoggingInterceptor} and has been
     * re-purposed and used here to save responses from the fhirClient.
     */
    theResponse.bufferEntity();
    try (InputStream respEntity = theResponse.readEntity(); ) {
      final byte[] bytes;
      bytes = IOUtils.toByteArray(respEntity);
      response = new String(bytes, "UTF-8");
    } catch (IllegalStateException e) {
      throw new InternalErrorException(e);
    }
  }

  /**
   * Gets the response.
   *
   * @return the response
   */
  public String getResponse() {
    return response;
  }
}
