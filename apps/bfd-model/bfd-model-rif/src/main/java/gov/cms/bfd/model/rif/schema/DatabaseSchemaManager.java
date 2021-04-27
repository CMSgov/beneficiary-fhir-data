package gov.cms.bfd.model.rif.schema;

import gov.cms.bfd.sharedutils.exceptions.UncheckedSqlException;
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
   * src/main/resources/db/migration</code>.
   *
   * @param dataSource the JDBC {@link DataSource} for the database whose schema should be created
   *     or updated
   */
  public static void createOrUpdateSchema(DataSource dataSource) {
    LOGGER.info("Schema create/upgrade: running...");

    Flyway flyway = new Flyway();

    // Trying to prevent career-limiting mistakes.
    flyway.setCleanDisabled(true);

    flyway.setDataSource(dataSource);
    flyway.setPlaceholders(createScriptPlaceholdersMap(dataSource));
    flyway.migrate();

    LOGGER.info("Schema create/upgrade: complete.");
  }

  /**
   * <strong>WARNING:</strong> This method should never be run against a production database that is
   * in-service. Any queries against the database will take over ten minutes to complete. This
   * method is only intended for (very careful) use when initially loading an empty database. Drops
   * all indexes in the specified database.
   *
   * @param dataSource the JDBC {@link DataSource} for the database whose schema should be modified
   */
  public static void dropIndexes(DataSource dataSource) {
    Connection connectionMetaDataTable = JdbcUtils.openConnection(dataSource);
    DbSupport dbSupport = DbSupportFactory.createDbSupport(connectionMetaDataTable, true);

    String source =
        new ClassPathResource(
                "db/scripts/Drop_all_constraints.sql", DatabaseSchemaManager.class.getClassLoader())
            .loadAsString("UTF-8");
    source =
        new PlaceholderReplacer(createScriptPlaceholdersMap(dataSource), "${", "}")
            .replacePlaceholders(source);

    SqlScript disableConstraintsScript = new SqlScript(source, dbSupport);

    disableConstraintsScript.execute(dbSupport.getJdbcTemplate());
  }

  /**
   * @param dataSource the {@link DataSource} that the replacements will be used for
   * @return the {@link Map} of key-value replacements to use for {@link
   *     Flyway#setPlaceholders(Map)}
   */
  private static Map<String, String> createScriptPlaceholdersMap(DataSource dataSource) {
    Map<String, String> placeholders = new HashMap<>();
    try (Connection connection = dataSource.getConnection()) {
      if (connection.getMetaData().getDatabaseProductName().equals("HSQL Database Engine")) {
        placeholders.put("type.int4", "integer");
        placeholders.put("logic.tablespaces-escape", "--");
        placeholders.put("logic.drop-tablespaces-escape", "--");
        placeholders.put("logic.alter-column-type", "");
        placeholders.put("logic.index-create-concurrently", "");
        placeholders.put("logic.sequence-start", "start with");
        placeholders.put("logic.sequence-increment", "increment by");
      } else {
        placeholders.put("type.int4", "int4");
        placeholders.put("logic.tablespaces-escape", "--");
        placeholders.put("logic.drop-tablespaces-escape", "");
        placeholders.put("logic.alter-column-type", "type");
        placeholders.put("logic.index-create-concurrently", "concurrently");
        placeholders.put("logic.sequence-start", "start");
        placeholders.put("logic.sequence-increment", "increment");
      }
    } catch (SQLException e) {
      throw new UncheckedSqlException(e);
    }
    return placeholders;
  }
}
