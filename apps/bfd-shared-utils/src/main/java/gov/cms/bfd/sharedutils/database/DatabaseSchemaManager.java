package gov.cms.bfd.sharedutils.database;

import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import gov.cms.bfd.sharedutils.exceptions.UncheckedSqlException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
 * Manages the schema of the database being used to store the Blue Button API backend's data.
 *
 * <p>This uses <a href="http://www.liquibase.org/">Liquibase</a> to manage the schema. The main
 * Liquibase changelog is in {@code src/main/resources/db-schema.xml}.
 */
public final class DatabaseSchemaManager {
  /** Logger for writing messages. */
  private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSchemaManager.class);

  /** Baseline version of migration scripts. */
  public static final String BASELINE_VERSION = "20240522164906244";

  /** List of schemas. */
  public static final List<String> SCHEMAS = List.of("ccw", "rda");

  /**
   * Creates or updates, as appropriate, the backend database schema for the specified database.
   * Does not report any progress.
   *
   * @param dataSource the JDBC {@link DataSource} for the database whose schema should be created
   *     or updated
   * @return {@code true} if the migration was successful
   */
  public static boolean createOrUpdateSchema(DataSource dataSource) {
    return createOrUpdateSchema(dataSource, null, ignored -> {});
  }

  /**
   * Creates or updates, as appropriate, the Blue Button API backend database schema for the
   * specified database.
   *
   * @param dataSource the JDBC {@link DataSource} for the database whose schema should be created
   *     or updated
   * @param flywayScriptLocationOverride the flyway script location override, can be null if no
   *     override
   * @param progressConsumer function to receive migration status updates
   * @return {@code true} if the migration was successful
   */
  public static boolean createOrUpdateSchema(
      DataSource dataSource,
      String flywayScriptLocationOverride,
      Consumer<DatabaseMigrationProgress> progressConsumer) {
    LOGGER.info("Schema create/upgrade: running...");

    Flyway flyway;
    try {
      flyway = createFlyway(dataSource, flywayScriptLocationOverride, progressConsumer);
      progressConsumer.accept(
          new DatabaseMigrationProgress(
              DatabaseMigrationProgress.Stage.BeforeMigration, flyway.info().current()));
      flyway.migrate();
      progressConsumer.accept(
          new DatabaseMigrationProgress(
              DatabaseMigrationProgress.Stage.AfterMigration, flyway.info().current()));
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
        && (flywayInfo.current().getState() == MigrationState.SUCCESS
            || flywayInfo.current().getState() == MigrationState.BASELINE);
  }

  /**
   * Create flyway using the specified parameters.
   *
   * @param dataSource the {@link DataSource} to run {@link Flyway} against
   * @param flywayScriptLocationOverride the flyway script location override, can be null if no
   *     override
   * @param progressConsumer function to receive migration status updates
   * @return a {@link Flyway} instance that can be used for the specified {@link DataSource}
   */
  private static Flyway createFlyway(
      DataSource dataSource,
      String flywayScriptLocationOverride,
      Consumer<DatabaseMigrationProgress> progressConsumer) {
    FluentConfiguration flywayBuilder = Flyway.configure().dataSource(dataSource);
    flywayBuilder.placeholders(createScriptPlaceholdersMap(dataSource));

    // Seems to be required in our tests for some reason that I couldn't debug.
    flywayBuilder.connectRetries(5);

    // Trying to prevent career-limiting mistakes.
    flywayBuilder.cleanDisabled(true);

    // Apply a baseline for non-empty databases.
    // FIXME: Official documentation warns against these settings for
    // production environments. As of flyway 9, this option needs to
    // be set explicitly in order to remain consistent with the way
    // BFD used flyway 8 and the existing IT strategy. Until BFD adopts
    // a better IT strategy, this must be set to true.
    flywayBuilder.baselineOnMigrate(true);
    flywayBuilder.baselineVersion(BASELINE_VERSION);

    // We want to allow the scripts to be executed out of order, in the case of concurrent
    // development.
    flywayBuilder.outOfOrder(true);
    // Let flyway know which schemas it will be working with.
    flywayBuilder.schemas(SCHEMAS.toArray(new String[0]));
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

    // Set up a callback to submit progress updates.
    flywayBuilder.callbacks(new FlywayProgressCallback(progressConsumer));

    return flywayBuilder.load();
  }

  /**
   * Creates the script placeholders map.
   *
   * @param dataSource the {@link DataSource} that the replacements will be used for
   * @return the {@link Map} of key-value replacements to use for {@link
   *     FluentConfiguration#placeholders(Map)}
   */
  private static Map<String, String> createScriptPlaceholdersMap(DataSource dataSource) {
    Map<String, String> placeholders = new HashMap<>();
    try (Connection connection = dataSource.getConnection()) {
      if (DatabaseUtils.isPostgresConnection(connection)) {
        placeholders.put("type.int4", "int4");
        placeholders.put("type.text", "text");
        placeholders.put("logic.tablespaces-escape", "--");
        placeholders.put("logic.drop-tablespaces-escape", "");
        placeholders.put("logic.alter-column-type", "type");
        placeholders.put("logic.psql-only-alter", "alter");
        placeholders.put("logic.alter-rename-column", "rename column");
        placeholders.put("logic.alter-rename-constraint", "rename constraint");
        placeholders.put("logic.rename-to", "to");
        placeholders.put("logic.index-create-concurrently", "concurrently");
        placeholders.put("logic.sequence-start", "start");
        placeholders.put("logic.sequence-increment", "increment");
        placeholders.put("logic.perms", "");
        placeholders.put("logic.psql-only", "");
      } else {
        throw new BadCodeMonkeyException(
            String.format(
                "Unsupported database vendor: %s", DatabaseUtils.extractVendorName(connection)));
      }
    } catch (SQLException e) {
      throw new UncheckedSqlException(e);
    }
    return placeholders;
  }
}
