package gov.cms.bfd.server.war;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.servlet.InstrumentedFilter;
import com.codahale.metrics.servlets.HealthCheckServlet;
import com.codahale.metrics.servlets.MetricsServlet;
import com.google.common.base.Strings;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.ConfigurableEnvironment;
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

  /** Checks for BFD v2 Flag */
  private static boolean isV2Enabled() {
    if (!Strings.isNullOrEmpty(System.getProperty("bfdServer.v2.enabled"))) {
      return Boolean.parseBoolean(System.getProperty("bfdServer.v2.enabled"));
    }
    return false;
  }

  /**
   * @see
   *     org.springframework.web.WebApplicationInitializer#onStartup(jakarta.servlet.ServletContext)
   */
  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    LOGGER.info("Initializing Blue Button API backend server...");

    // Create the Spring application context.
    AnnotationConfigWebApplicationContext springContext =
        new AnnotationConfigWebApplicationContext();
    springContext.register(SpringConfiguration.class);
    springContext.refresh();

    // Set the Spring PRODUCTION profile as the default.
    ConfigurableEnvironment springEnv = springContext.getEnvironment();
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

    // Register the Blue Button R4 Server/Servlet if v2 is ENABLED!!

    if (isV2Enabled()) {

      V2Server r4Servlet = new V2Server();
      cxfServletReg = servletContext.addServlet("r4Servlet", r4Servlet);
      cxfServletReg.setLoadOnStartup(1);
      cxfServletReg.addMapping("/v2/fhir/*");
    }

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
