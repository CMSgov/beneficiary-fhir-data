package gov.cms.bfd.server.ng;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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
            .withInitScripts(
                new ClassPathResource("mock-idr.sql").getPath(),
                new ClassPathResource("bfd.sql").getPath());
    container.start();
    runPython(container, "uv", "sync");
    runPython(container, "uv", "run", "load_synthetic.py", "./test_samples2");

    container.execInContainer(
        "psql",
        "-U",
        "bfdtest",
        "-d",
        "testdb",
        "-c",
        "UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_clm SET \"clm_idr_ld_dt\" = CURRENT_DATE;");

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
