package gov.cms.bfd.migrator.app;

import gov.cms.bfd.sharedutils.database.DatabaseUtils;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import gov.cms.bfd.sharedutils.exceptions.UncheckedSqlException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the schema of the database being used to store the Blue Button API backend's data.
 *
 * <p>This uses <a href="http://www.liquibase.org/">Liquibase</a> to manage the schema. The main
 * Liquibase changelog is in <code>src/main/resources/db-schema.xml</code>.
 */
public final class DatabaseSchemaManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSchemaManager.class);

  /**
   * Creates or updates, as appropriate, the Blue Button API backend database schema for the
   * specified database. The Flyway migration scripts are stored in <code>
   *  src/main/resources/db/migration</code>.
   *
   * @param dataSource the JDBC {@link DataSource} for the database whose schema should be created
   *     or updated
   * @param appConfiguration the app configuration
   * @return {@code true} if the migration was successful
   */
  public static boolean createOrUpdateSchema(
      DataSource dataSource, AppConfiguration appConfiguration) {
    LOGGER.info("Schema create/upgrade: running...");

    Flyway flyway;
    try {
      flyway = createFlyway(dataSource, appConfiguration);
      flyway.migrate();
    } catch (FlywaySqlScriptException sqlException) {
      LOGGER.error("SQL Exception when running migration: ", sqlException);
      return false;
    } catch (FlywayException flywayException) {
      LOGGER.error("Flyway Exception when running migration: ", flywayException);
      return false;
    } catch (Exception ex) {
      LOGGER.error("Unexpected Exception when running migration: ", ex);
      return false;
    }

    LOGGER.info("Schema create/upgrade: complete.");
    // Ensure the final migration was a success to return true
    MigrationInfoService flywayInfo = flyway.info();
    return flywayInfo != null
        && flywayInfo.current() != null
        && flywayInfo.current().getState() == MigrationState.SUCCESS;
  }

  /**
   * Create flyway using the specified parameters.
   *
   * @param dataSource the {@link DataSource} to run {@link Flyway} against
   * @return a {@link Flyway} instance that can be used for the specified {@link DataSource}
   */
  private static Flyway createFlyway(DataSource dataSource, AppConfiguration appConfiguration) {
    FluentConfiguration flywayBuilder = Flyway.configure().dataSource(dataSource);
    flywayBuilder.placeholders(createScriptPlaceholdersMap(dataSource));

    // Seems to be required in our tests for some reason that I couldn't debug.
    flywayBuilder.connectRetries(5);

    // Trying to prevent career-limiting mistakes.
    flywayBuilder.cleanDisabled(true);

    // The default name for the schema table changed in Flyway 5.
    // We need to specify the original table name for backwards compatibility.
    flywayBuilder.table("schema_version");

    // If we want to point at a specific location for the migration scripts
    // Useful for testing
    String scriptLocationOverride = appConfiguration.getFlywayScriptLocationOverride();
    if (scriptLocationOverride != null && scriptLocationOverride.length() > 0) {
      flywayBuilder.locations(scriptLocationOverride);
    }

    return flywayBuilder.load();
  }

  /**
   * @param dataSource the {@link DataSource} that the replacements will be used for
   * @return the {@link Map} of key-value replacements to use for {@link
   *     FluentConfiguration#placeholders(Map)}
   */
  private static Map<String, String> createScriptPlaceholdersMap(DataSource dataSource) {
    Map<String, String> placeholders = new HashMap<>();
    try (Connection connection = dataSource.getConnection()) {
      if (DatabaseUtils.isHsqlConnection(connection)) {
        placeholders.put("type.int4", "integer");
        placeholders.put("type.text", "longvarchar");
        placeholders.put("logic.tablespaces-escape", "--");
        placeholders.put("logic.drop-tablespaces-escape", "--");
        placeholders.put("logic.alter-column-type", "");
        placeholders.put("logic.hsql-only-alter", "alter");
        placeholders.put("logic.psql-only-alter", "-- alter");
        placeholders.put("logic.alter-rename-column", "alter column");
        placeholders.put("logic.alter-rename-constraint", "alter constraint");
        placeholders.put("logic.rename-to", "rename to");
        placeholders.put("logic.index-create-concurrently", "");
        placeholders.put("logic.sequence-start", "start with");
        placeholders.put("logic.sequence-increment", "increment by");
        placeholders.put("logic.perms", "--");
      } else if (DatabaseUtils.isPostgresConnection(connection)) {
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
      } else {
        throw new BadCodeMonkeyException(
            String.format(
                "Unknown database vendor: %s", DatabaseUtils.extractVendorName(connection)));
      }
    } catch (SQLException e) {
      throw new UncheckedSqlException(e);
    }
    return placeholders;
  }
}
