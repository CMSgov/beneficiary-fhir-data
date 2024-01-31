package gov.cms.bfd.pipeline.rda.grpc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

  /** template for delete query. */
  private static final String DELETE_QUERY_TEMPLATE =
      "delete from %s t where t.%s in ("
          + "  select %s "
          + "  from %s "
          + "  where last_updated < '%s' "
          + "  order by last_updated desc "
          + "  limit %d"
          + ")";

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
   * Returns a list of table names for use in native queries.
   *
   * @return list of database table names.
   */
  abstract List<String> getTableNames();

  /**
   * Returns the name of the database table mapped to the parent entity class.
   *
   * @return the database table name for the parent entity.
   */
  abstract String getParentTableName();

  /**
   * Returns the key column of the database table mapped to the parent entity class.
   *
   * @return the key column name for the parent entity.
   */
  abstract String getParentTableKey();

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
      final long startMillis = System.currentTimeMillis();
      var entityManager = entityManagerFactory.createEntityManager();

      try {
        Instant cutoffDate = Instant.now().minus(OLDEST_CLAIM_AGE_IN_DAYS, ChronoUnit.DAYS);
        List<Query> queries = buildDeleteQueries(cutoffDate, entityManager);
        var numberOfTransactions = Math.ceilDiv(cleanupRunSize, cleanupTransactionSize);

        for (int i = 0; i < numberOfTransactions; i++) {
          var result = executeDeleteTransaction(queries, entityManager);
          claimsDeleted += result;

          // If no claims deleted this iteration, or total claims deleted
          // exceeds the cleanupRunSize, then stop

          if (result == 0 || claimsDeleted >= cleanupRunSize) {
            break;
          }
        }
        final long endMillis = System.currentTimeMillis();
        logger.info(
            "cleanup job removed {} claims in {}ms", claimsDeleted, endMillis - startMillis);
      } catch (Exception ex) {
        logger.error("cleanup job aborted by an exception: message={}", ex.getMessage(), ex);
        throw new ProcessingException(ex, 0);
      } finally {
        entityManager.close();
      }
    }

    return claimsDeleted;
  }

  /**
   * Executes a single delete transaction. The list of queries are passed in execution order, last
   * query must delete from the parent table, all other queries are child table deletes.
   *
   * @param queries the queries to execute in each transaction.
   * @param em the entitymanager to use with the deletion.
   * @return the number of claims deleted by the transaction.
   */
  private int executeDeleteTransaction(List<Query> queries, EntityManager em) {
    var count = 0;
    em.getTransaction().begin();

    // execute all queries in order, use the count from the last query (parent table)
    // to track the number of claims deleted.
    for (Query query : queries) {
      count = query.executeUpdate();
    }
    em.getTransaction().commit();
    return count;
  }

  /**
   * Build a list of native sql delete queries to remove claims.
   *
   * @param cutoffDate the Instant used to identify queries for deletion.
   * @param em the EntityManager to use.
   * @return the list of queries.
   */
  private List<Query> buildDeleteQueries(Instant cutoffDate, EntityManager em) {
    List<Query> queries = new ArrayList<>();
    for (String tableName : getTableNames()) {
      String queryStr =
          String.format(
              DELETE_QUERY_TEMPLATE,
              tableName,
              getParentTableKey(),
              getParentTableKey(),
              getParentTableName(),
              cutoffDate.toString(),
              cleanupTransactionSize);
      queries.add(em.createNativeQuery(queryStr));
    }
    return queries;
  }
}
