package gov.hhs.cms.bluebutton.data.pipeline.rif.schema;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.justdavis.karl.misc.exceptions.unchecked.UncheckedLiquibaseException;
import com.justdavis.karl.misc.exceptions.unchecked.UncheckedSqlException;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.diff.DiffGeneratorFactory;
import liquibase.diff.DiffResult;
import liquibase.diff.compare.CompareControl;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.DiffToChangeLog;
import liquibase.exception.LiquibaseException;
import liquibase.integration.commandline.CommandLineUtils;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import liquibase.resource.ResourceAccessor;

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

	private static final String CHANGE_LOG = "db-schema.xml";

	private static final ResourceAccessor RESOURCE_ACCESSOR = new CompositeResourceAccessor(
			new ClassLoaderResourceAccessor(Thread.currentThread().getContextClassLoader()),
			new FileSystemResourceAccessor("."));

	/**
	 * Creates or updates, as appropriate, the Blue Button API backend database
	 * schema for the specified database.
	 * 
	 * @param dataSource
	 *            the JDBC {@link DataSource} for the database whose schema
	 *            should be created or updated
	 */
	public void createOrUpdateSchema(DataSource dataSource) {
		LOGGER.info("Liquibase schema create/upgrade: running...");
		runAgainstLiquibase(dataSource, liquibase -> {
			/*
			 * Run Liquibase to apply the entire schema change log.
			 */
			try {
				liquibase.update(new Contexts("", "primaryKeys"));
			} catch (LiquibaseException e) {
				throw new UncheckedLiquibaseException(e);
			}

			return null;
		});
		LOGGER.info("Liquibase schema create/upgrade: complete.");
	}

	/**
	 * <p>
	 * Prints an auto-generated Liquibase change log to {@link System#out},
	 * generated based on the JPA metadata for PostgreSQL.
	 * </p>
	 * <p>
	 * NOTE: In order for this to work, the
	 * <code>jpa:persistence:META-INF/persistence.xml</code> file must have the
	 * following property temporarily added to it:
	 * <code>&lt;property name="hibernate.dialect" value="org.hibernate.dialect.PostgreSQL95Dialect" /&gt;</code>
	 * .
	 * </p>
	 */
	public static void generateChangeLogFromJpa() {
		try {
			// Create a "connection" to the offline JPA data.
			String url = "jpa:persistence:META-INF/persistence.xml";
			Database jpaDatabase = CommandLineUtils.createDatabaseObject(RESOURCE_ACCESSOR, url, null, null, null, null,
					null, false, false, null, null, null, null, null, null, null);

			DiffResult schemaDiff = DiffGeneratorFactory.getInstance().compare(jpaDatabase, null,
					CompareControl.STANDARD);
			DiffToChangeLog diffChangeLogProducer = new DiffToChangeLog(schemaDiff, new DiffOutputControl());
			diffChangeLogProducer.print(System.out);
		} catch (LiquibaseException e) {
			throw new UncheckedLiquibaseException(e);
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException(e);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * A simple app that just runs {@link #generateChangeLogFromJpa()}.
	 * 
	 * @param args
	 *            (unused)
	 */
	public static void main(String[] args) {
		DatabaseSchemaManager.generateChangeLogFromJpa();
	}

	/**
	 * Runs the specified {@link Function} against Liquibase, handling all of
	 * the boilerplate connection management that's needed.
	 * 
	 * @param dataSource
	 *            the JDBC {@link DataSource} for the database that Liquibase
	 *            will operate against
	 * @param liquibaseAction
	 *            the {@link Function} to run against the {@link Liquibase}
	 *            instance that will be supplied
	 */
	private <R> R runAgainstLiquibase(DataSource dataSource, Function<Liquibase, R> liquibaseAction) {
		try (Connection dbConnection = dataSource.getConnection()) {
			/*
			 * Create the Liquibase Database instance, which is really more of a
			 * connection with some metadata. Liquibase needs this.
			 */
			Database database = DatabaseFactory.getInstance()
					.findCorrectDatabaseImplementation(new JdbcConnection(dbConnection));

			/*
			 * Create the Liquibase instance, which is the main handle for the
			 * Liquibase API.
			 */
			Liquibase liquibase = new Liquibase(CHANGE_LOG, RESOURCE_ACCESSOR, database);

			return liquibaseAction.apply(liquibase);
		} catch (SQLException e) {
			throw new UncheckedSqlException(e);
		} catch (LiquibaseException e) {
			throw new UncheckedLiquibaseException(e);
		}
	}
}
