package gov.cms.bfd.model.rif.schema;

import org.junit.Test;

/** Unit tests for {@link gov.cms.bfd.model.rif.schema.DatabaseSchemaManager}. */
public final class DatabaseSchemaManagerTest {
  /**
   * Verifies that {@link gov.cms.bfd.model.rif.schema.DatabaseSchemaManager} runs correctly against
   * an HSQL database.
   */
  @Test
  public void createOrUpdateSchemaOnHsql() {
    // Ensure that this runs without errors.
    DatabaseSchemaManager.createOrUpdateSchema(DatabaseTestUtils.get().getUnpooledDataSource());
  }
}
