package gov.cms.bfd.sharedutils.database;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link DatabaseSchemaManager}. */
public final class DatabaseSchemaManagerTest {

  /** Verifies that {@link DatabaseSchemaManager} runs correctly against a postgres database. */
  @Test
  public void createOrUpdateSchema() {
    // Ensure that this runs without errors.
    // TODO: Fix this by using a mock and ensuring flyway got called against the data source
    // DatabaseSchemaManager.createOrUpdateSchema(DatabaseTestUtils.get().getUnpooledDataSource());
  }
}
