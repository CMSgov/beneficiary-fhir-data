package gov.cms.bfd.pipeline.rda.grpc;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.slf4j.Logger;

/**
 * An abstract class that encapsulates the common code for executing a job to clean up old
 * pre-adjudicated claims from the RDA pipeline tables.
 */
@Getter
@AllArgsConstructor
public abstract class AbstractCleanupJob implements CleanupJob {

  /** maximum age of claims in days from current date. */
  private static final int OLDEST_CLAIM_AGE_IN_DAYS = 60;

  /** EntityManagerFactory to use to generate the EntityManager. */
  private final EntityManagerFactory entityManagerFactory;

  /** the number of claims to include in a single delete transaction. */
  private final int cleanupTransactionSize;

  /** the number of claims to delete in a single run of the cleanup job. */
  private final int cleanupRunSize;

  /** when true the cleanup job should run, false otherwise. */
  private final boolean enabled;

  /** Logger provided from each subclass. */
  private final Logger logger;

  /**
   * Returns a list of child entity class names for use in jpql.
   *
   * @return list of child entity class names.
   */
  abstract List<String> getChildEntityNames();

  /**
   * Returns the principal entity class name for use in jpql.
   *
   * @return the class name of the principal entity.
   */
  abstract String getEntityName();

  /**
   * Returns the name of the database table mapped to the principal entity class.
   *
   * @return the database table name for the principal entity.
   */
  abstract String getEntityTableName();

  /**
   * Executes the job if enabled. Calculates the number of transactions from the cleanupRunSize /
   * cleanupTransactionSize then for each transaction it determines the maximum last updated date
   * for claims that will be removed then deletes them.
   *
   * <p>If any iteration fails to find claims to delete or the total number of claims deleted
   * already exceeds the cleanupRunSize then processing stops.
   *
   * @return the number of deleted claims, or zero if not enabled.
   */
  public int run() throws ProcessingException {
    int claimsDeleted = 0;

    if (enabled) {
      var entityManager = entityManagerFactory.createEntityManager();

      try {
        var maxDate = Instant.now().minus(OLDEST_CLAIM_AGE_IN_DAYS, ChronoUnit.DAYS);
        var transactionMaxDateQuery = getTransactionMaxDateQuery(maxDate, entityManager);
        var numberOfTransactions = Math.ceilDiv(cleanupRunSize, cleanupTransactionSize);

        for (int i = 0; i < numberOfTransactions; i++) {
          var result = executeDeleteTransaction(transactionMaxDateQuery, entityManager);
          claimsDeleted += result;

          // If no claims deleted this iteration, or total claims deleted
          // exceeds the cleanupRunSize, then stop

          if (result == 0 || claimsDeleted >= cleanupRunSize) {
            break;
          }
        }
      } catch (Exception ex) {
        logger.error("pre-processing aborted by an exception: message={}", ex.getMessage(), ex);
        throw new ProcessingException(ex, 0);
      } finally {
        entityManager.close();
      }
    }

    return claimsDeleted;
  }

  /**
   * Creates a query object. Used to determine the maximum last updated date for a single
   * transaction. Created as a native sql query because it is not easy to use limits with a subquery
   * using jpql.
   *
   * @param maxDate the maximum last_updated date.
   * @param em the entity manager to use to create the query.
   * @return a query to retrieve the maximum last_update date.
   */
  private Query getTransactionMaxDateQuery(Instant maxDate, EntityManager em) {

    // This native query finds the most recent last update date
    // for the group of oldest claims eligible for removal,
    // limited by the batch size for a transaction.

    var transactionMaxDateQuery =
        em.createNativeQuery(
            "select max(t.last_updated) from ("
                + " select last_updated from "
                + getEntityTableName()
                + " where last_updated < ?1"
                + " order by last_updated "
                + " limit "
                + cleanupTransactionSize
                + ") as t");

    transactionMaxDateQuery.setParameter(1, maxDate);

    return transactionMaxDateQuery;
  }

  /**
   * Executes a single delete transaction. Used the transactionMaxDateQuery to determine the cutoff
   * last_updated date for claims to be removed.
   *
   * @param transactionMaxDateQuery the query used to determine the cutoff date
   * @param em the entitymanager to use with the deletion.
   * @return the number of claims deleted by the transaction.
   */
  private int executeDeleteTransaction(Query transactionMaxDateQuery, EntityManager em) {
    var claimsDeleted = 0;

    // Find the maximum last_updated date for this transaction
    var transactionMaxDateResult = transactionMaxDateQuery.getResultList();

    // If date found create delete query and execute in a transaction
    if (transactionMaxDateResult.size() == 1) {
      Timestamp transactionMaxDate = (Timestamp) transactionMaxDateResult.getFirst();
      if (transactionMaxDate != null) {
        em.getTransaction().begin();
        getChildEntityNames()
            .forEach(
                entityName -> {
                  Query childQuery =
                      em.createQuery(
                          "delete from "
                              + entityName
                              + " e "
                              + "where e.claimId in (select claimId from "
                              + getEntityName()
                              + "  claim where claim.lastUpdated <= ?1)");
                  childQuery.setParameter(1, transactionMaxDate.toInstant());
                  childQuery.executeUpdate();
                });

        Query parentQuery =
            em.createQuery("delete from " + getEntityName() + " where lastUpdated <= ?1");
        parentQuery.setParameter(1, transactionMaxDate.toInstant());
        claimsDeleted = parentQuery.executeUpdate();
        em.getTransaction().commit();
      }
    }

    return claimsDeleted;
  }
}
