package gov.cms.bfd.server.war;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.openapi.OpenApiInterceptor;
import ca.uhn.fhir.rest.server.ApacheProxyAddressStrategy;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.consent.ConsentInterceptor;
import ca.uhn.fhir.rest.server.provider.ServerCapabilityStatementProvider;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

/**
 * The primary {@link Servlet} for this web application. Uses the <a href="http://hapifhir.io/">HAPI
 * FHIR</a> framework to provide a fully functional FHIR API server that queries stored RIF data
 * from the CCW and converts it to the proper FHIR format "on the fly".
 */
@WebServlet(
    urlPatterns = {"/v2/fhir/*"},
    displayName = "Beneficiary FHIR Data v2")
public class V2Server extends RestfulServer {

  private static final long serialVersionUID = 1L;

  /** Represents the capabilities publisher value. */
  static final String CAPABILITIES_PUBLISHER = "Centers for Medicare & Medicaid Services";

  /** Represents the capabilities server name. */
  static final String CAPABILITIES_SERVER_NAME = "Blue Button API: Direct";

  /** Constructs a new {@link V2Server} instance. */
  public V2Server() {
    super(FhirContext.forR4());
    setServerAddressStrategy(ApacheProxyAddressStrategy.forHttp());
    // HAPI FHIR, by default, does not trust the parameters (from both the query string and content
    // body, for POSTs) that are automatically extracted by the web framework in the request to be
    // properly encoded. Due to this, it attempts to extract parameters on its own, but fails to do
    // so in our case due to Jetty already exhausting the input stream of the body. We need to set
    // this, so it does not do its own parameter extraction and simply takes the parameter map from
    // the Jetty request object. Note that this is NOT respected if any "Content-Encoding" header is
    // specified in the request; then it will ONLY extract query string parameters
    // See:
    // https://hapifhir.io/hapi-fhir/apidocs/hapi-fhir-server/ca/uhn/fhir/rest/server/RestfulServer.html#isIgnoreServerParsedRequestParameters()
    setIgnoreServerParsedRequestParameters(false);
    configureServerInfoMetadata();
  }

  /**
   * Configures various metadata fields that will be included in this server's {@link
   * CapabilityStatement}.
   */
  private void configureServerInfoMetadata() {
    setServerName(CAPABILITIES_SERVER_NAME);

    /*
     * Read in some of the project metadata from a Maven-filtered properties
     * file, which ensures that it's always up to date.
     */
    Properties projectProps = new Properties();
    try (InputStream projectPropsStream =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("project.properties"); ) {
      projectProps.load(projectPropsStream);

      setImplementationDescription(projectProps.getProperty("project.id"));
      setServerVersion(projectProps.getProperty("project.version"));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    // Lightly customize the capability provider to set publisher name.
    ServerCapabilityStatementProvider capabilityStatementProvider =
        new ServerCapabilityStatementProvider(this);
    capabilityStatementProvider.setPublisher(CAPABILITIES_PUBLISHER);
    setServerConformanceProvider(capabilityStatementProvider);
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  protected void initialize() throws ServletException {
    /*
     * Grab the application's Spring WebApplicationContext from the web
     * container. We can use this to retrieve beans (and anything that needs
     * Spring injection/autowiring, e.g. anything that accesses the DB, must
     * be a bean).
     */
    WebApplicationContext springContext = ContextLoaderListener.getCurrentWebApplicationContext();

    // Each IResourceProvider adds support for a specific FHIR resource.
    List<IResourceProvider> resourceProviders =
        springContext.getBean(SpringConfiguration.BLUEBUTTON_R4_RESOURCE_PROVIDERS, List.class);
    setResourceProviders(resourceProviders);

    ConfigLoader configLoader = springContext.getBean(ConfigLoader.class);
    boolean samhsaV2Enabled =
        configLoader.booleanValue(SpringConfiguration.SSM_PATH_SAMHSA_V2_ENABLED);

    /*
     * Each "plain" provider has one or more annotated methods that provides
     * support for non-resource-type methods, such as transaction, and
     * global history.
     */
    List<Object> plainProviders = new ArrayList<>();
    // TODO: Refactor to use registerProviders. The current method is deprecated.
    setPlainProviders(plainProviders);

    /*
     * Register the HAPI server interceptors that have been configured in
     * Spring.
     */
    Collection<IServerInterceptor> hapiInterceptors =
        springContext.getBeansOfType(IServerInterceptor.class).values();
    for (IServerInterceptor hapiInterceptor : hapiInterceptors) {
      this.registerInterceptor(hapiInterceptor);
    }

    // Enable ETag Support (this is already the default)
    setETagSupport(ETagSupportEnum.ENABLED);

    // Default to XML and pretty printing.
    setDefaultResponseEncoding(EncodingEnum.JSON);
    setDefaultPrettyPrint(false);

    // Registers HAPI interceptors to capture request/response time metrics when BFD handlers are
    // executed
    registerInterceptor(new TimerInterceptor());
    if (samhsaV2Enabled) {
      registerInterceptor(new ConsentInterceptor(new V2SamhsaConsentInterceptor()));
    }
    // OpenAPI
    OpenApiInterceptor openApiInterceptor = new OpenApiInterceptor();
    registerInterceptor(openApiInterceptor);
  }
}
