package gov.cms.bfd.server.ng;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@TestConfiguration
public class IntegrationTestConfiguration {
  // Container lifecycle is managed by Spring,
  // so the resource closing warning is not applicable here
  @SuppressWarnings("resource")
  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgres() {
    // Provides an implementation of JdbcConnectionDetails that will be injected into the Spring
    // context
    var databaseImage = System.getProperty("its.testcontainer.db.image");
    return new PostgreSQLContainer<>(databaseImage)
        .withDatabaseName("testdb")
        .withUsername("bfdtest")
        .withPassword("bfdtest")
        .waitingFor(Wait.forListeningPort())
        .withInitScript(new ClassPathResource("bfd.sql").getPath());
  }
}
