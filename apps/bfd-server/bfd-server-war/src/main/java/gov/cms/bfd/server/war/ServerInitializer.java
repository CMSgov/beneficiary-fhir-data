package gov.cms.bfd.server.war;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.server.war.commons.CommonTransformerUtils;
import gov.cms.bfd.sharedutils.config.ConfigLoader;
import gov.cms.bfd.sharedutils.config.ConfigLoaderSource;
import io.dropwizard.metrics.servlet.InstrumentedFilter;
import io.dropwizard.metrics.servlets.HealthCheckServlet;
import io.dropwizard.metrics.servlets.MetricsServlet;
import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
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
    initializeSpringConfiguration(
        springContext, servletContext, SpringConfiguration.class, ConfigLoaderSource.fromEnv());

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
    CommonTransformerUtils.setNpiOrgLookup(springContext.getBean(NPIOrgLookup.class));

    LOGGER.info("Initialized Blue Button API backend server.");
  }

  /**
   * Creates a {@link ConfigLoader} using the provided function to access environment variables and
   * initializes the provided spring and servlet contexts with the {@link ConfigLoader}. Configures
   * the spring environment to use a {@link ConfigPropertySource} to access properties so that the
   * {@link SpringConfiguration} can use properties defined in SSM. Adds a {@link
   * SpringConfiguration} and refreshes the context.
   *
   * @param springContext spring context to initialize
   * @param servletContext servlet context to initialize
   * @param configurationClass {@link Configuration} annotated class for our config (parameterized
   *     for testing)
   * @param getenv function used to access environment variables (parameterized for testing)
   */
  @VisibleForTesting
  static void initializeSpringConfiguration(
      AnnotationConfigWebApplicationContext springContext,
      ServletContext servletContext,
      Class<?> configurationClass,
      ConfigLoaderSource getenv) {
    springContext.setServletContext(servletContext);
    final ConfigLoader config;
    if (servletContext.getAttribute(SpringConfiguration.CONFIG_LOADER_CONTEXT_NAME) != null) {
      // ServerExecutor passes in an appropriate ConfigLoader that we can use as-is.
      config =
          (ConfigLoader)
              servletContext.getAttribute(SpringConfiguration.CONFIG_LOADER_CONTEXT_NAME);
    } else {
      // In real life we create our own ConfigLoader and add it to the context.
      config = SpringConfiguration.createConfigLoader(getenv);
      servletContext.setAttribute(SpringConfiguration.CONFIG_LOADER_CONTEXT_NAME, config);
    }

    ConfigurableEnvironment springEnv = springContext.getEnvironment();
    MutablePropertySources sources = springEnv.getPropertySources();
    sources.addFirst(new ConfigPropertySource("configLoader", config));
    springContext.register(configurationClass);
    springContext.refresh();
  }
}
