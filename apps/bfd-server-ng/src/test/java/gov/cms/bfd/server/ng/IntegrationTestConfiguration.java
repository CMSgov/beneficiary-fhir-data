package gov.cms.bfd.server.ng;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.platform.commons.util.StringUtils;
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
  public PostgreSQLContainer<?> postgres(Instant date) throws IOException, InterruptedException {
    if (StringUtils.isNotBlank(System.getenv("PGPASSWORD"))
        || StringUtils.isNotBlank(System.getenv("BFD_SENSITIVE_DB_PASSWORD"))) {
      // Postgres environment variables can interfere with the database configuration and should
      // not be used here.
      throw new RuntimeException("Database variables should not be set while running tests");
    }
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
    runMigrator(container);

    runPython(container, date, "uv", "sync");
    runPython(container, date, "uv", "run", "load_synthetic.py", "./test_samples2");

    // Update CLM_IDR_LD_DT to CURRENT_DATE before pipeline.py
    // Reason: PAC data older than 60 days is filtered by coalescing
    // (idr_updt_ts, idr_insrt_ts, clm_idr_ld_dt). Synthetic data has
    // outdated clm_idr_ld_dt value and empty idr_updt_ts, idr_insrt_ts.
    container.execInContainer(
        "psql",
        "-U",
        "bfdtest",
        "-d",
        "testdb",
        "-c",
        "UPDATE cms_vdm_view_mdcr_prd.v2_mdcr_clm "
            + "SET \"clm_idr_ld_dt\" = '"
            + date
            + "',"
            + "\"idr_insrt_ts\" = '"
            + date
            + "',"
            + "\"idr_updt_ts\" = '"
            + date
            + "';");

    runPython(container, date, "uv", "run", "pipeline.py", "synthetic");

    return container;
  }

  private void runMigrator(PostgreSQLContainer<?> container) {
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
  }

  private void runPython(PostgreSQLContainer<?> container, Instant date, String... args)
      throws IOException, InterruptedException {
    ProcessBuilder processBuilder = new ProcessBuilder(args);
    var env = processBuilder.environment();
    env.put("BFD_DB_ENDPOINT", container.getHost());
    env.put("BFD_DB_PORT", container.getMappedPort(5432).toString());
    env.put("BFD_DB_USERNAME", container.getUsername());
    env.put("BFD_DB_PASSWORD", container.getPassword());
    env.put("BFD_DB_NAME", container.getDatabaseName());
    // Makes the pipeline go slightly faster by increasing the number of tasks that run in parallel.
    env.put("IDR_LOAD_TYPE", "initial");
    // Partitions are necessary for massive amounts of prod data, but will cause our modestly-sized
    // test data to load significantly slower.
    env.put("IDR_ENABLE_PARTITIONS", "0");
    env.put("BFD_TEST_DATE", date.toString());

    processBuilder
        .directory(new File(Paths.get(baseDir, "../bfd-pipeline-idr").toString()))
        // Redirect streams to prevent excessive logs from filling up the internal buffer and
        // causing the process to hang
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT);
    var process = processBuilder.start();
    var code = process.waitFor();
    if (code > 0) {
      throw new RuntimeException("Command failed. See logs for details.");
    }
  }
}
