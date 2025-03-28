package gov.cms.bfd.server.ng;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.openapi.OpenApiInterceptor;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import java.util.List;
import org.springframework.stereotype.Component;

/** FHIR server for V3 operations. */
@Component
@WebServlet(urlPatterns = {"/v3/fhir*"})
public class V3Server extends RestfulServer {
  private final List<IResourceProvider> resourceProviders;

  /**
   * Creates a new {@link V3Server}.
   *
   * @param resourceProviders FHIR resource providers
   */
  public V3Server(List<IResourceProvider> resourceProviders) {
    super();
    this.resourceProviders = resourceProviders;
  }

  @Override
  public void initialize() throws ServletException {
    this.setFhirContext(FhirContext.forR4());
    this.registerProviders(resourceProviders);
    OpenApiInterceptor openApiInterceptor = new OpenApiInterceptor();

    this.registerInterceptor(openApiInterceptor);
  }
}
