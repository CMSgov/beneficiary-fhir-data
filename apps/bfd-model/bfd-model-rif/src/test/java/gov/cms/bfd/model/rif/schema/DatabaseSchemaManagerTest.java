package gov.cms.bfd.model.rif.schema;

import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.Test;

import gov.cms.bfd.model.rif.schema.DatabaseSchemaManager;

/**
 * Unit tests for {@link DatabaseSchemaManager}.
 */
public final class DatabaseSchemaManagerTest {
	/**
	 * Verifies that {@link DatabaseSchemaManager} runs correctly against an
	 * HSQL database.
	 */
	@Test
	public void createOrUpdateSchemaOnHsql() {
		JDBCDataSource hsqlDataSource = new JDBCDataSource();
		hsqlDataSource.setUrl("jdbc:hsqldb:mem:test");

		// Ensure that this runs without errors.
		DatabaseSchemaManager.createOrUpdateSchema(hsqlDataSource);
	}
}
