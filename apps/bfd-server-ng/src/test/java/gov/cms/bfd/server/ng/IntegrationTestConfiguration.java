package gov.cms.bfd.server.ng;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class IntegrationTestConfiguration {
  @Value("${project.basedir}")
  private String baseDir;

  // Container lifecycle is managed by Spring,
  // so the resource closing warning is not applicable here
  @SuppressWarnings("resource")
  @Bean
  @ServiceConnection
  public PostgreSQLContainer<?> postgres() throws IOException, InterruptedException {
    // Provides an implementation of JdbcConnectionDetails that will be injected into the Spring
    // context
    var databaseImage = System.getProperty("its.testcontainer.db.image");

    var container =
        new PostgreSQLContainer<>(
                DockerImageName.parse(databaseImage).asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("testdb")
            .withUsername("bfdtest")
            .withPassword("bfdtest")
            .waitingFor(Wait.forListeningPort())
            .withInitScript(new ClassPathResource("mock-idr.sql").getPath());
    container.start();
    var flyway =
        Flyway.configure()
            .configuration(
                Map.of(
                    "flyway.schemas",
                    "idr",
                    "flyway.url",
                    container.getJdbcUrl(),
                    "flyway.user",
                    container.getUsername(),
                    "flyway.password",
                    container.getPassword(),
                    "flyway.locations",
                    "filesystem:" + Paths.get(baseDir, "../bfd-db-migrator-ng/migrations")))
            .load();
    var result = flyway.migrate();
    if (!result.success) {
      throw new RuntimeException(result.exceptionObject);
    }
    runPython(container, "uv", "sync");
    runPython(container, "uv", "run", "load_synthetic.py", "./test_samples2");
    runPython(container, "uv", "run", "pipeline.py", "synthetic");

    return container;
  }

  private void runPython(PostgreSQLContainer<?> container, String... args)
      throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder(args);
    processBuilder.redirectErrorStream(true);
    var env = processBuilder.environment();
    env.put("BFD_DB_ENDPOINT", container.getHost());
    env.put("BFD_DB_PORT", container.getMappedPort(5432).toString());
    env.put("BFD_DB_USERNAME", container.getUsername());
    env.put("BFD_DB_PASSWORD", container.getPassword());
    env.put("BFD_DB_NAME", container.getDatabaseName());

    processBuilder.directory(
        new File(Paths.get(baseDir, "../bfd-pipeline/bfd-pipeline-idr").toString()));
    var process = processBuilder.start();
    var code = process.waitFor();
    if (code > 0) {
      var out = new String(process.getInputStream().readAllBytes());
      throw new RuntimeException(out);
    }
  }
}
