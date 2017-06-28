package gov.hhs.cms.bluebutton.data.pipeline.rif.schema;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.internal.dbsupport.DbSupport;
import org.flywaydb.core.internal.dbsupport.DbSupportFactory;
import org.flywaydb.core.internal.dbsupport.SqlScript;
import org.flywaydb.core.internal.util.PlaceholderReplacer;
import org.flywaydb.core.internal.util.jdbc.JdbcUtils;
import org.flywaydb.core.internal.util.scanner.classpath.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.justdavis.karl.misc.exceptions.unchecked.UncheckedSqlException;

/**
 * <p>
 * Manages the schema of the database being used to store the Blue Button API
 * backend's data.
 * </p>
 * <p>
 * This uses <a href="http://www.liquibase.org/">Liquibase</a> to manage the
 * schema. The main Liquibase changelog is in
 * <code>src/main/resources/db-schema.xml</code>.
 * </p>
 */
public final class DatabaseSchemaManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSchemaManager.class);

	/**
	 * Creates or updates, as appropriate, the Blue Button API backend database
	 * schema for the specified database. The Flyway migration scripts are
	 * stored in <code>src/main/resources/db/migration</code>.
	 * 
	 * @param dataSource
	 *            the JDBC {@link DataSource} for the database whose schema
	 *            should be created or updated
	 */
	public static void createOrUpdateSchema(DataSource dataSource) {
		LOGGER.info("Schema create/upgrade: running...");

		Flyway flyway = new Flyway();

		// Trying to prevent career-limiting mistakes.
		flyway.setCleanDisabled(true);

		flyway.setDataSource(dataSource);
		Map<String, String> placeholders = new HashMap<>();
		try (Connection connection = dataSource.getConnection()) {
			if (connection.getMetaData().getDatabaseProductName().equals("HSQL Database Engine")) {
				placeholders.put("type.int4", "integer");
				placeholders.put("logic.if-exists", "");
				placeholders.put("logic.tablespaces-escape", "--");
			} else {
				placeholders.put("type.int4", "int4");
				placeholders.put("logic.if-exists", "if exists");
				placeholders.put("logic.tablespaces-escape", "--");
			}
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		}
		flyway.setPlaceholders(placeholders);
		flyway.migrate();

		Connection connectionMetaDataTable = JdbcUtils.openConnection(dataSource);
		DbSupport dbSupport = DbSupportFactory.createDbSupport(connectionMetaDataTable, true);
		String source = new ClassPathResource("db/scripts/Drop_all_constraints.sql",
				DatabaseSchemaManager.class.getClassLoader()).loadAsString("UTF-8");
		source = new PlaceholderReplacer(placeholders, "${", "}").replacePlaceholders(source);
		SqlScript disableConstraintsScript = new SqlScript(source, dbSupport);
		disableConstraintsScript.execute(dbSupport.getJdbcTemplate());

		LOGGER.info("Schema create/upgrade: complete.");
	}
}
