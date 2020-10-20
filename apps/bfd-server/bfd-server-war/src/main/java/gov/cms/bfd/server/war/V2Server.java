package gov.cms.bfd.server.war;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.ApacheProxyAddressStrategy;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.hl7.fhir.r4.hapi.rest.server.ServerCapabilityStatementProvider;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

/**
 * The primary {@link Servlet} for this web application. Uses the <a href="http://hapifhir.io/">HAPI
 * FHIR</a> framework to provide a fully functional FHIR API server that queries stored RIF data
 * from the CCW and converts it to the proper FHIR format "on the fly".
 */
public class V2Server extends RestfulServer {

  private static final long serialVersionUID = 1L;

  static final String CAPABILITIES_PUBLISHER = "Centers for Medicare & Medicaid Services";
  static final String CAPABILITIES_SERVER_NAME = "Blue Button API: Direct";

  /** Constructs a new {@link V2Server} instance. */
  public V2Server() {
    super(FhirContext.forR4());
    setServerAddressStrategy(ApacheProxyAddressStrategy.forHttp());
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

  /** @see ca.uhn.fhir.rest.server.RestfulServer#initialize() */
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

    /*
     * Each "plain" provider has one or more annotated methods that provides
     * support for non-resource-type methods, such as transaction, and
     * global history.
     */
    List<Object> plainProviders = new ArrayList<>();
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
  }
}
