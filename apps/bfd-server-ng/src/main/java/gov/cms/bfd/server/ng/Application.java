package gov.cms.bfd.server.ng;

import ca.uhn.fhir.rest.server.RestfulServer;
import io.micrometer.common.annotation.ValueResolver;
import io.micrometer.core.aop.MeterTagAnnotationHandler;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.Servlet;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import javax.sql.DataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** BFD Server startup class. */
@ServletComponentScan(basePackageClasses = {RestfulServer.class})
@SpringBootApplication(scanBasePackages = "gov.cms.bfd.server.ng")
@EnableConfigurationProperties(Configuration.class)
@EnableTransactionManagement
public class Application {
  /**
   * Server entrypoint.
   *
   * @param args args
   */
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  /**
   * Creates the SQL datasource.
   *
   * @param configuration app configuration
   * @return datasource
   */
  @Bean
  public DataSource dataSource(Configuration configuration) {
    return configuration.getDataSourceFactory().createDataSource();
  }

  /**
   * Configures the routes for the FHIR server.
   *
   * @param restfulServer FHIR server
   * @return configured servlet
   */
  @Bean
  public ServletRegistrationBean<Servlet> hapiServletRegistration(V3Server restfulServer) {
    ServletRegistrationBean<Servlet> servletRegistrationBean = new ServletRegistrationBean<>();
    servletRegistrationBean.setServlet(restfulServer);

    servletRegistrationBean.addUrlMappings("/v3/fhir/*");

    return servletRegistrationBean;
  }

  /**
   * Configures Micrometer to support @MeterTag annotations with SPEL expressions.
   *
   * <p>IMPORTANT: @Timed annotations only work on public methods for classes that are registered
   * beans. Attempting to use this for private methods or non-bean classes will not work since it
   * relies on Spring AOP's proxies.
   *
   * @param registry meter registry
   * @return timed aspect
   */
  @Bean
  public TimedAspect timedAspect(MeterRegistry registry) {
    var timedAspect = new TimedAspect(registry);
    ValueResolver valueResolver = Object::toString;
    var valueExpressionResolver = new SpelValueExpressionResolver();
    timedAspect.setMeterTagAnnotationHandler(
        new MeterTagAnnotationHandler(aClass -> valueResolver, aClass -> valueExpressionResolver));
    return timedAspect;
  }

  /**
   * Configures a date that propagates throughout the application that is overridable in test
   * configurations.
   *
   * @return clock instant
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
