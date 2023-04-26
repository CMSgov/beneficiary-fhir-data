package gov.cms.bfd.server.war;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import javax.annotation.Nonnull;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.web.SpringServletContainerInitializer;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

/**
 * Initializes the Blue Button API FHIR backend application.
 *
 * <p>If deployed in a Servlet 3.0 container, this application will be found and loaded
 * automagically, as this project includes Spring's {@link SpringServletContainerInitializer} in its
 * dependencies. That class is a registered {@link ServletContainerInitializer} SPI and will in turn
 * search for and enable any {@link WebApplicationInitializer} implementations (such as this class).
 */
public final class ServerInitializer implements WebApplicationInitializer {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerInitializer.class);

  @Override
  public void onStartup(@Nonnull ServletContext servletContext) throws ServletException {
    LOGGER.info("Initializing Blue Button API backend server...");

    // Create the Spring application context and configure it with our ConfigLoader
    // and SpringConfiguration.  We store the ConfigLoader as a ServletContext
    // attribute so that it can be reused in SpringConfiguration rather than
    // creating a static field.
    var springContext = new AnnotationConfigWebApplicationContext();
    springContext.setServletContext(servletContext);
    ConfigLoader config = SpringConfiguration.createConfigLoader(System::getenv);
    servletContext.setAttribute(SpringConfiguration.CONFIG_LOADER_CONTEXT_NAME, config);

    ConfigurableEnvironment springEnv = springContext.getEnvironment();
    MutablePropertySources sources = springEnv.getPropertySources();
    sources.addFirst(new ConfigPropertySource("configLoader", config));
    springContext.register(SpringConfiguration.class);
    springContext.refresh();

    // Set the Spring PRODUCTION profile as the default.
    springEnv.setDefaultProfiles(SpringProfile.PRODUCTION);

    // Manage the lifecycle of the root application context.
    servletContext.addListener(new ContextLoaderListener(springContext));

    // Ensure that request-scoped resource beans work correctly.
    servletContext.addListener(RequestContextListener.class);

    // Register the Blue Button STU3 Server/Servlet.
    V1Server stu3Servlet = new V1Server();
    ServletRegistration.Dynamic cxfServletReg =
        servletContext.addServlet("fhirStu3Servlet", stu3Servlet);
    cxfServletReg.setLoadOnStartup(1);
    cxfServletReg.addMapping("/v1/fhir/*");

    // Register the Blue Button R4 Server/Servlet.
    V2Server r4Servlet = new V2Server();
    cxfServletReg = servletContext.addServlet("r4Servlet", r4Servlet);
    cxfServletReg.setLoadOnStartup(1);
    cxfServletReg.addMapping("/v2/fhir/*");

    /*
     * Register the MetricRegistry and HealthCheckRegistry into the ServletContext,
     * so that InstrumentedFilter and AdminServlet (configured in web.xml) can work.
     */
    servletContext.setAttribute(
        InstrumentedFilter.REGISTRY_ATTRIBUTE, springContext.getBean(MetricRegistry.class));
    servletContext.setAttribute(
        MetricsServlet.METRICS_REGISTRY, springContext.getBean(MetricRegistry.class));
    servletContext.setAttribute(
        HealthCheckServlet.HEALTH_CHECK_REGISTRY, springContext.getBean(HealthCheckRegistry.class));

    LOGGER.info("Initialized Blue Button API backend server.");
  }
}
