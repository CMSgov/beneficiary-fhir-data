package gov.hhs.cms.bluebutton.data.pipeline.rif.load;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;

/**
 * <p>
 * Contains utilities that are useful when running the {@link RifLoader}.
 * </p>
 * <p>
 * This is being left in <code>src/main</code> so that it can be used from other
 * modules' tests, without having to delve into classpath dark arts.
 * </p>
 */
public final class RifLoaderTestUtils {
	/**
	 * The value to use for {@link LoadAppOptions#getHicnHashIterations()} in
	 * tests.
	 */
	public static final int HICN_HASH_ITERATIONS = 2;

	/**
	 * The value to use for {@link LoadAppOptions#getHicnHashPepper()} in tests.
	 */
	public static final byte[] HICN_HASH_PEPPER = "nottherealpepper".getBytes(StandardCharsets.UTF_8);

	/**
	 * <p>
	 * The value to use for {@link LoadAppOptions#getDatabaseUrl()}. It's
	 * occasionally useful for devs to manually change this to one of these
	 * values:
	 * </p>
	 * <ul>
	 * <li>In-memory HSQL DB (this is the default):
	 * <code>jdbc:hsqldb:mem:test;hsqldb.tx=mvcc</code></li>
	 * <li>On-disk HSQL DB (useful when the in-memory DB is running out of
	 * memory):
	 * <code>jdbc:hsqldb:file:target/hsql-db-for-its;hsqldb.tx=mvcc</code></li>
	 * </ul>
	 * <p>
	 * Note: The <code>hsqldb.tx=mvcc</code> option included in the URL is
	 * needed to avoid locking problems with some concurrent tests that access
	 * the DB. See
	 * <a href="http://hsqldb.org/doc/guide/sessions-chapt.html#snc_tx_mvcc">
	 * HSQL DB: MVCC</a> for details.
	 * </p>
	 */
	public static final String DB_URL = "jdbc:hsqldb:mem:test;hsqldb.tx=mvcc";

	/**
	 * The value to use for {@link LoadAppOptions#getDatabaseUsername()}.
	 */
	public static final String DB_USERNAME = "";

	/**
	 * The value to use for {@link LoadAppOptions#getDatabasePassword()}.
	 */
	public static final char[] DB_PASSWORD = "".toCharArray();

	private static final Logger LOGGER = LoggerFactory.getLogger(RifLoaderTestUtils.class);

	/**
	 * <strong>Serious Business:</strong> deletes all resources from the
	 * database server used in tests.
	 * 
	 * @param options
	 *            the {@link LoadAppOptions} specifying the DB to clean
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void cleanDatabaseServerViaDeletes(LoadAppOptions options) {
		// Before disabling this check, please go and update your resume.
		if (!DB_URL.contains("hsql"))
			throw new BadCodeMonkeyException("Saving you from a career-changing event.");

		EntityManagerFactory entityManagerFactory = createEntityManagerFactory(options);
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		// Determine the entity types to delete, and the order to do so in.
		Comparator<Class<?>> entityDeletionSorter = (t1, t2) -> {
			if (t1.equals(Beneficiary.class))
				return 1;
			if (t2.equals(Beneficiary.class))
				return -1;
			if (t1.getSimpleName().endsWith("Line"))
				return -1;
			if (t2.getSimpleName().endsWith("Line"))
				return 1;
			return 0;
		};
		List<Class<?>> entityTypesInDeletionOrder = entityManagerFactory.getMetamodel().getEntities().stream()
				.map(t -> t.getJavaType()).sorted(entityDeletionSorter).collect(Collectors.toList());

		LOGGER.info("Deleting all resources...");
		entityManager.getTransaction().begin();
		for (Class<?> entityClass : entityTypesInDeletionOrder) {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaDelete query = builder.createCriteriaDelete(entityClass);
			query.from(entityClass);
			entityManager.createQuery(query).executeUpdate();
		}
		entityManager.getTransaction().commit();
		LOGGER.info("Deleted all resources.");
	}

	/**
	 * <strong>Serious Business:</strong> deletes all resources from the
	 * database server used in tests.
	 * 
	 * @param options
	 *            the {@link LoadAppOptions} specifying the DB to clean
	 */
	public static void cleanDatabaseServer(LoadAppOptions options) {
		// Before disabling this check, please go and update your resume.
		if (!options.getDatabaseUrl().contains("hsql"))
			throw new BadCodeMonkeyException("Saving you from a career-changing event.");

		Flyway flyway = new Flyway();
		flyway.setDataSource(RifLoader.createDataSource(options, new MetricRegistry()));
		flyway.clean();
	}

	/**
	 * @return the {@link LoadAppOptions} that should be used in tests, which
	 *         specifies how to connect to the database server that tests should
	 *         be run against
	 */
	public static LoadAppOptions getLoadOptions() {
		return new LoadAppOptions(HICN_HASH_ITERATIONS, HICN_HASH_PEPPER, DB_URL, DB_USERNAME, DB_PASSWORD,
				LoadAppOptions.DEFAULT_LOADER_THREADS);
	}

	/**
	 * @param options
	 *            the {@link LoadAppOptions} specifying the DB to use
	 * @return a JDBC {@link DataSource} for the database server used in tests
	 */
	public static DataSource createDataSouce(LoadAppOptions options) {
		DataSource jdbcDataSource = RifLoader.createDataSource(options, new MetricRegistry());
		return jdbcDataSource;
	}

	/**
	 * @param options
	 *            the {@link LoadAppOptions} specifying the DB to use
	 * @return a JPA {@link EntityManagerFactory} for the database server used
	 *         in tests
	 */
	public static EntityManagerFactory createEntityManagerFactory(LoadAppOptions options) {
		DataSource jdbcDataSource = createDataSouce(options);
		return RifLoader.createEntityManagerFactory(jdbcDataSource);
	}
}
