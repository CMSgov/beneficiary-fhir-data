package gov.cms.bfd.server.ng;

import ca.uhn.fhir.rest.server.RestfulServer;
import jakarta.servlet.Servlet;
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
}
