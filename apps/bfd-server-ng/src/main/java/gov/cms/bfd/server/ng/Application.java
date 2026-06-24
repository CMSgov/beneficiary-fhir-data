package gov.cms.bfd.server.ng;

import ca.uhn.fhir.rest.server.RestfulServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.server.ng.audit.AuditEventRepository;
import gov.cms.bfd.server.ng.log.AuditLogger;
import jakarta.servlet.Servlet;
import java.time.Clock;
import javax.sql.DataSource;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.SimpleAsyncTaskExecutorBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/** BFD Server startup class. */
@ServletComponentScan(basePackageClasses = {RestfulServer.class})
@SpringBootApplication(scanBasePackages = "gov.cms.bfd.server.ng")
@EnableConfigurationProperties(Configuration.class)
@EnableTransactionManagement
@EnableAsync
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
   * Configures a date that propagates throughout the application that is overridable in test
   * configurations.
   *
   * @return clock
   */
  @Bean
  public Clock systemClock() {
    return Clock.systemUTC();
  }

  /**
   * Creates the audit logger(s).
   *
   * @param configuration app configuration
   * @param auditEventRepository Audit Event Repository
   * @param objectMapper object mapper
   * @return audit logger
   */
  @Bean
  public AuditLogger auditLogger(
      Configuration configuration,
      AuditEventRepository auditEventRepository,
      ObjectMapper objectMapper) {
    return configuration.getAuditLogger(auditEventRepository, objectMapper);
  }

  /**
   * Creates the DynamoDbClient.
   *
   * @param configuration app configuration
   * @return DynamoDbClient
   */
  @Bean
  public DynamoDbClient dynamoDbClient(Configuration configuration) {
    return configuration.getDynamoDbClient();
  }

  /**
   * Creates the DynamoDB table mapping for audit events. Built once in the application classloader
   * to avoid ClassCastException with DevTools restart.
   *
   * @param dynamoDbClient DynamoDB client
   * @return configured DynamoDB table for AuditEventDynamoItem
   */
  @Bean
  public DynamoDbEnhancedClient auditEventTable(DynamoDbClient dynamoDbClient) {
    return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
  }

  /**
   * Creates the custom task executor.
   *
   * @param builder builder
   * @return task executor
   */
  @Bean
  public SimpleAsyncTaskExecutor taskExecutor(SimpleAsyncTaskExecutorBuilder builder) {
    return builder.taskDecorator(Application::wrapWithMdcContext).build();
  }

  private static Runnable wrapWithMdcContext(Runnable task) {
    // save the current MDC context
    var contextMap = MDC.getCopyOfContextMap();
    return () -> {
      MDC.clear();
      MDC.setContextMap(contextMap);
      try {
        task.run();
      } finally {
        // once the task is complete, clear MDC
        MDC.clear();
      }
    };
  }
}
