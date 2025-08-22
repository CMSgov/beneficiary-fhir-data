package gov.cms.bfd.server.ng;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import gov.cms.bfd.server.ng.interceptor.BanUnsupportedHttpMethodsInterceptor;
import gov.cms.bfd.server.ng.interceptor.ExceptionHandlingInterceptor;
import gov.cms.bfd.server.ng.interceptor.LoggingInterceptor;
import gov.cms.bfd.server.openapi.OpenApiInterceptor;
import jakarta.servlet.annotation.WebServlet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** FHIR server for V3 operations. */
@RequiredArgsConstructor
@Component
@WebServlet(urlPatterns = {"/v3/fhir*"})
public class V3Server extends RestfulServer {
  private final List<IResourceProvider> resourceProviders;

  @Override
  public void initialize() {
    // HAPI FHIR, by default, does not trust the parameters (from both the query string and content
    // body, for POSTs) that are automatically extracted by the web framework in the request to be
    // properly encoded. Due to this, it attempts to extract parameters on its own, but fails to do
    // so in our case due to Jetty already exhausting the input stream of the body. We need to set
    // this, so it does not do its own parameter extraction and simply takes the parameter map from
    // the Jetty request object. Note that this is NOT respected if any "Content-Encoding" header is
    // specified in the request; then it will ONLY extract query string parameters
    // See:
    // https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-server/ca/uhn/fhir/rest/server/RestfulServer.html#isIgnoreServerParsedRequestParameters()
    this.setIgnoreServerParsedRequestParameters(false);

    this.setFhirContext(FhirContext.forR4());
    this.registerProviders(resourceProviders);

    this.registerInterceptor(new LoggingInterceptor());
    this.registerInterceptor(new BanUnsupportedHttpMethodsInterceptor());
    this.registerInterceptor(new ExceptionHandlingInterceptor());
    this.registerInterceptor(new OpenApiInterceptor());
  }
}
