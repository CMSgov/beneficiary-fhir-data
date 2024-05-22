package gov.cms.bfd;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.internal.sqlscript.FlywaySqlScriptException;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A copy of DatabaseSchemaManager for testing purposes, to eliminate dependencies between util
 * packages.
 */
public class DatabaseTestSchemaManager {

  /** Logger for writing information out. */
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseTestSchemaManager.class);

  /**
   * Creates or updates, as appropriate, the backend database schema for the specified database.
   *
   * @param dataSource the JDBC {@link DataSource} for the database whose schema should be created
   *     or updated
   * @return {@code true} if the migration was successful
   */
  public static boolean createOrUpdateSchema(DataSource dataSource) {
    return createOrUpdateSchema(dataSource, null);
  }

  /**
   * Creates or updates, as appropriate, the Blue Button API backend database schema for the
   * specified database.
   *
   * @param dataSource the JDBC {@link DataSource} for the database whose schema should be created
   *     or updated
   * @param flywayScriptLocationOverride the flyway script location override, can be null if no
   *     override
   * @return {@code true} if the migration was successful
   */
  public static boolean createOrUpdateSchema(
      DataSource dataSource, String flywayScriptLocationOverride) {
    Flyway flyway;
    try {
      flyway = createFlyway(dataSource, flywayScriptLocationOverride);
      flyway.migrate();
    } catch (FlywaySqlScriptException sqlException) {
      handleException("SQL Exception when running migration: ", sqlException);
      return false;
    } catch (FlywayException flywayException) {
      handleException("Flyway Exception when running migration: ", flywayException);
      return false;
    } catch (Exception ex) {
      handleException("Unexpected Exception when running migration: ", ex);
      return false;
    }

    // Ensure the final migration was a success to return true
    MigrationInfoService flywayInfo = flyway.info();
    return flywayInfo != null
        && flywayInfo.current() != null
        && flywayInfo.current().getState() == MigrationState.SUCCESS;
  }

  /**
   * Private method to handle Exceptions that are thrown.
   *
   * @param message Error message for exception.
   * @param exception Exception that is being logged.
   */
  private static void handleException(String message, Exception exception) {
    LOGGER.error(message, exception);
  }

  /**
   * Creates a {@link Flyway} instance that can be used for the specified {@link DataSource}.
   *
   * @param dataSource the {@link DataSource} to run {@link Flyway} against
   * @param flywayScriptLocationOverride the flyway script location override
   * @return a {@link Flyway} instance
   */
  private static Flyway createFlyway(DataSource dataSource, String flywayScriptLocationOverride) {
    FluentConfiguration flywayBuilder = Flyway.configure().dataSource(dataSource);
    flywayBuilder.placeholders(createScriptPlaceholdersMap(dataSource));

    // Seems to be required in our tests for some reason that I couldn't debug.
    flywayBuilder.connectRetries(5);

    // Trying to prevent career-limiting mistakes.
    flywayBuilder.cleanDisabled(true);

    // Apply a baseline for non-empty databases, start at version 0
    flywayBuilder.baselineOnMigrate(true);
    flywayBuilder.baselineVersion("0");

    // The default name for the schema table changed in Flyway 5.
    // We need to specify the original table name for backwards compatibility.
    // flywayBuilder.table("schema_version");
    flywayBuilder.defaultSchema("ccw");

    // If we want to point at a specific location for the migration scripts
    // Useful for testing
    if (flywayScriptLocationOverride != null && flywayScriptLocationOverride.length() > 0) {
      flywayBuilder.locations(flywayScriptLocationOverride);
    }

    // Transactional locks default to `true` as of Flyway 9.1.2 and better
    // See https://github.com/flyway/flyway/issues/3497
    // See https://github.com/flyway/flyway/commit/022a646b7959aa7a9a11760d8e93e5e238fbd6ec
    flywayBuilder
        .getPluginRegister()
        .getPlugin(PostgreSQLConfigurationExtension.class)
        .setTransactionalLock(false);

    return flywayBuilder.load();
  }

  /**
   * Creates the script placeholders map.
   *
   * @param dataSource the {@link DataSource} that the replacements will be used for
   * @return the {@link Map} of key-value replacements to use for {@link
   *     FluentConfiguration#placeholders(Map)}
   */
  public static Map<String, String> createScriptPlaceholdersMap(DataSource dataSource) {
    Map<String, String> placeholders = new HashMap<>();
    try (Connection connection = dataSource.getConnection()) {
      if (connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")) {
        placeholders.put("type.int4", "int4");
        placeholders.put("type.text", "text");
        placeholders.put("logic.tablespaces-escape", "--");
        placeholders.put("logic.drop-tablespaces-escape", "");
        placeholders.put("logic.alter-column-type", "type");
        placeholders.put("logic.hsql-only-alter", "-- alter");
        placeholders.put("logic.psql-only-alter", "alter");
        placeholders.put("logic.alter-rename-column", "rename column");
        placeholders.put("logic.alter-rename-constraint", "rename constraint");
        placeholders.put("logic.rename-to", "to");
        placeholders.put("logic.index-create-concurrently", "concurrently");
        placeholders.put("logic.sequence-start", "start");
        placeholders.put("logic.sequence-increment", "increment");
        placeholders.put("logic.perms", "");
        placeholders.put("logic.psql-only", "");
        placeholders.put("logic.hsql-only", "-- ");
      } else {
        throw new RuntimeException("Unsupported database vendor");
      }
    } catch (SQLException e) {
      throw new RuntimeException("SQL Exception while running migrations: ", e);
    }
    return placeholders;
  }
}
