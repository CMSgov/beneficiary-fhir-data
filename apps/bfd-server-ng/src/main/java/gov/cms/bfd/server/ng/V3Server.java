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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Coverage;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** FHIR server for V3 operations. */
@RequiredArgsConstructor
@Component
@WebServlet(urlPatterns = {"/v3/fhir*"})
public class V3Server extends RestfulServer {
  private final List<IResourceProvider> resourceProviders;

  private static final Logger LOGGER = LoggerFactory.getLogger(V3Server.class);

  private final Configuration configuration;

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

    List<IResourceProvider> filteredProviders =
        resourceProviders.stream()
            .filter(this::isResourceProviderEnabled)
            .collect(Collectors.toList());

    this.registerProviders(filteredProviders); // Register the filtered list
    this.registerInterceptor(new LoggingInterceptor());
    this.registerInterceptor(new BanUnsupportedHttpMethodsInterceptor());
    this.registerInterceptor(new ExceptionHandlingInterceptor());
    this.registerInterceptor(new OpenApiInterceptor());
  }

  private boolean isResourceProviderEnabled(IResourceProvider provider) {
    Class<? extends IBaseResource> resourceTypeClass = provider.getResourceType();
    boolean keepProvider = true; // Default to keeping the provider

    Configuration.Nonsensitive nonsensitiveConfig = configuration.getNonsensitive();

    if (resourceTypeClass.equals(ExplanationOfBenefit.class)) {
      keepProvider = nonsensitiveConfig.isEobEnabled();
      if (!keepProvider) {
        LOGGER.info("Disabling ExplanationOfBenefit endpoint based on config.");
      }
    } else if (resourceTypeClass.equals(Patient.class)) {
      keepProvider = nonsensitiveConfig.isPatientEnabled();
      if (!keepProvider) {
        LOGGER.info("Disabling Patient endpoint based on config.");
      }
    } else if (resourceTypeClass.equals(Coverage.class)) {
      keepProvider = nonsensitiveConfig.isCoverageEnabled();
      if (!keepProvider) {
        LOGGER.info("Disabling Coverage endpoint based on config.");
      }
    } else {
      LOGGER.debug(
          "Keeping {} endpoint by default (no specific flag configured).",
          resourceTypeClass.getSimpleName());
    }

    return keepProvider;
  }
}
