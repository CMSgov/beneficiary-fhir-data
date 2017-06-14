package gov.hhs.cms.bluebutton.data.pipeline.rif.load;

import java.nio.charset.StandardCharsets;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimLine;

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
	 * The value to use for {@link LoadAppOptions#getDatabaseUrl()}.
	 */
	public static final String DB_URL = "jdbc:hsqldb:mem:test";

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
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void cleanDatabaseServer() {
		// Before disabling this check, please go and update your resume.
		if (!DB_URL.contains("hsql"))
			throw new BadCodeMonkeyException("Saving you from a career-changing event.");

		LOGGER.info("Deleting all resources...");

		EntityManagerFactory entityManagerFactory = createEntityManagerFactory();
		EntityManager entityManager = entityManagerFactory.createEntityManager();

		// TODO add other entity classes
		entityManager.getTransaction().begin();
		for (Class<?> entityClass : new Class[] { CarrierClaimLine.class, CarrierClaim.class, Beneficiary.class }) {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaDelete query = builder.createCriteriaDelete(entityClass);
			query.from(entityClass);
			entityManager.createQuery(query).executeUpdate();
		}
		entityManager.getTransaction().commit();
		LOGGER.info("Deleted all resources.");
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
	 * @return a JPA {@link EntityManagerFactory} for the database server used
	 *         in tests
	 */
	public static EntityManagerFactory createEntityManagerFactory() {
		return RifLoader.createEntityManagerFactory(getLoadOptions(), new MetricRegistry());
	}
}
