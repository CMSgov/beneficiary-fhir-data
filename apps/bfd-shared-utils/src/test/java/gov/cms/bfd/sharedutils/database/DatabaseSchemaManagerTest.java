package gov.cms.bfd.sharedutils.database;

import gov.cms.bfd.DatabaseTestUtils;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DatabaseSchemaManager}. */
public final class DatabaseSchemaManagerTest {
  /** Verifies that {@link DatabaseSchemaManager} runs correctly against an HSQL database. */
  @Test
  public void createOrUpdateSchemaOnHsql() {
    // Ensure that this runs without errors.
    DatabaseSchemaManager.createOrUpdateSchema(
        DatabaseTestUtils.getUnpooledUnmigratedHsqlDataSource());
  }
}
