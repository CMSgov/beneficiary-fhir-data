package gov.cms.bfd.pipeline.sharedutils.npi_fda;

import static gov.cms.bfd.pipeline.sharedutils.npi_fda.LoadNpiDataFilesTest.TEST_CSV;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.codahale.metrics.MetricRegistry;
import com.zaxxer.hikari.HikariDataSource;
import gov.cms.bfd.DatabaseTestUtils;
import gov.cms.bfd.model.rif.npi_fda.NPIData;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LoadNpiDataFilesIT {
  /**
   * Ensures that each test case here starts with a clean/empty database, with the right schema.
   *
   * @param testInfo the test info
   */
  private static final int DEFAULT_MAX_POOL_SIZE =
      Math.max(1, (Runtime.getRuntime().availableProcessors() - 1)) * 2 * 2;

  PipelineApplicationState applicationState;
  EntityManager entityManager;
  static String NPI_COUNT_QUERY = "select count(*) from ccw.npi_data";
  static String SELECT_RECORD_QUERY = "select n from NPIData n where n.npi = :npi";

  @BeforeEach
  public void setup() {
    HikariDataSource pooledDataSource = new HikariDataSource();
    pooledDataSource.setDataSource(DatabaseTestUtils.get().getUnpooledDataSource());
    pooledDataSource.setMaximumPoolSize(DEFAULT_MAX_POOL_SIZE);
    pooledDataSource.setRegisterMbeans(true);
    // By default, the pool would immediately open all allowed connections. This is excessive for
    // unit/IT testing, so we lower it to avoid exceeding max connections in postgresql.
    pooledDataSource.setMinimumIdle(3);
    pooledDataSource.setIdleTimeout(30_000);

    this.applicationState =
        new PipelineApplicationState(
            new SimpleMeterRegistry(),
            new MetricRegistry(),
            pooledDataSource,
            PipelineApplicationState.PERSISTENCE_UNIT_NAME,
            Clock.systemUTC());
    this.entityManager = applicationState.getEntityManagerFactory().createEntityManager();
  }

  @Test
  void shouldSaveDataFiles() throws IOException {
    LoadNpiDataFiles loadNpiDataFiles = new LoadNpiDataFiles(entityManager, 1, 30);
    loadNpiDataFiles.saveDataFile(
        new InputStreamReader(new ByteArrayInputStream(TEST_CSV.getBytes())));
    Long recordCount = (Long) entityManager.createNativeQuery(NPI_COUNT_QUERY).getSingleResult();
    assertEquals(8L, recordCount);
    Query query = entityManager.createQuery(SELECT_RECORD_QUERY, NPIData.class);
    query.setParameter("npi", "1588667638");
    NPIData npiData = (NPIData) query.getSingleResult();
    assertEquals(
        "DR. WILLIAM C PILCHER MD",
        String.format(
            "%s %s %s %s %s",
            npiData.getProviderNamePrefix(),
            npiData.getProviderFirstName(),
            npiData.getProviderMiddleName(),
            npiData.getProviderLastName(),
            npiData.getProviderCredential()));
    assertEquals("207RC0000X", npiData.getTaxonomyCode());
    assertEquals("Cardiovascular Disease Physician", npiData.getTaxonomyDisplay());
  }
}
