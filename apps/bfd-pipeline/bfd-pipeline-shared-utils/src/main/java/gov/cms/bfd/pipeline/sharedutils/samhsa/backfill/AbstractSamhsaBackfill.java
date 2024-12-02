package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import gov.cms.bfd.pipeline.sharedutils.SamhsaUtil;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Does the actual work of backfilling the SAMHSA tags. */
public abstract class AbstractSamhsaBackfill {
  /** The Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSamhsaBackfill.class);

  /**
   * Query to retrieve a list of claims objects. Will start at a given claim id, and limit the
   * results to a given limit.
   */
  protected String QUERY_WITH_STARTING_CLAIM =
      " SELECT * FROM ${tableName} t "
          + " WHERE ${claimColumn} > ${startingClaim} "
          + " ORDER BY ${claimColumn} ASC "
          + " LIMIT ${limit};";

  /**
   * Query to retrieve a list of claims objects. Starts at the beginning of the sorted list of
   * claims. This query will be run on the first iteration on a table when we have no original claim
   * to continue off of.
   */
  protected String QUERY_WITH_NO_STARTING_CLAIM =
      " SELECT * FROM ${tableName} t " + " ORDER BY ${claimColumn} ASC " + " LIMIT ${limit};";

  /** The transaction manager. */
  TransactionManager transactionManager;

  /**
   * SamhsaUtil class. This will be used to check the claims for SAMHSA data, and create the tags if
   * necessary.
   */
  SamhsaUtil samhsaUtil;

  /**
   * Constructor.
   *
   * @param transactionManager The transaction manager.
   */
  public AbstractSamhsaBackfill(TransactionManager transactionManager) {
    this.transactionManager = transactionManager;
    samhsaUtil = SamhsaUtil.getSamhsaUtil();
  }

  /**
   * Executes the query using the trnasaction manager.
   *
   * @param query The query to execute.
   * @return a list of claims.
   * @param <TClaim> The type of the claim.
   */
  private <TClaim> List<TClaim> executeQuery(Query query) {
    return transactionManager.executeFunction(
        entityManager -> {
          // execute all queries in order, use the count from the last query (parent table)
          // to track the number of claims deleted.
          var count = 0;
          return (List<TClaim>) query.getResultList();
        });
  }

  /**
   * Builds a query.
   *
   * @param startingClaim The claim to start at. If this is empty, the version of the query with no
   *     starting claim will be used.
   * @param table The table to query.
   * @param claimColumn The column for the claim id.
   * @param limit the number of claims to pull at a time.
   * @return Query to run.
   */
  private Query buildQuery(
      Optional<String> startingClaim, String table, String claimColumn, int limit) {
    AtomicReference<Query> atomicQuery = new AtomicReference<>();

    transactionManager.executeProcedure(
        entityManager -> {
          Map<String, String> params =
              Map.of(
                  "tableName",
                  table,
                  "claimColumn",
                  claimColumn,
                  "startingClaim",
                  startingClaim.isPresent() ? String.valueOf(startingClaim.get()) : "",
                  "limit",
                  String.valueOf(limit));
          StringSubstitutor strSub = new StringSubstitutor(params);
          String queryStr =
              strSub.replace(
                  startingClaim.isPresent()
                      ? QUERY_WITH_STARTING_CLAIM
                      : QUERY_WITH_NO_STARTING_CLAIM);
          atomicQuery.set(entityManager.createNativeQuery(queryStr, getTableClass(table)));
        });
    return atomicQuery.get();
  }

  /**
   * Gets the claim id column for a given table.
   *
   * @param table The table.
   * @return The name of the claim id table.
   */
  protected abstract String getClaimIdColumnName(String table);

  /**
   * Gets the record limit for each iteration.
   *
   * @return The record limit for each iteration.
   */
  protected abstract Integer getRecordLimit();

  /**
   * Gets the claim id of a claim.
   *
   * @param claim The Claim to check.
   * @return the claim id.
   */
  protected abstract String getClaimId(Object claim);

  /**
   * Gets the list of tables to iterate over.
   *
   * @return The list of tables.
   */
  protected abstract List<String> getTables();

  /**
   * Gets the class for a claim table.
   *
   * @param table the claim table.
   * @return the table's class.
   */
  protected abstract Class getTableClass(String table);

  /**
   * Entry point.
   *
   * @return The total number of claims for which tags were created.
   */
  public Long execute() {
    long total = 0L;
    for (String table : getTables()) {
      Long tableTotal = executeForTable(table);
      LOGGER.info(String.format("Created tags for %d claims in table %s", tableTotal, table));
      total += tableTotal;
    }
    return total;
  }

  /**
   * Iterates over all of the claims in a table, and checks for SAMHSA data.
   *
   * @param table The table to iterate over.
   * @return The number of claims for which tags were created.
   * @param <TClaim> The type of the claim.
   */
  private <TClaim> Long executeForTable(String table) {
    Query query =
        buildQuery(Optional.empty(), table, getClaimIdColumnName(table), getRecordLimit());
    List<TClaim> claims = executeQuery(query);
    long totalSaved = 0L;
    long totalProcessed = 0L;
    for (TClaim claim : claims) {
      boolean persisted = samhsaUtil.processClaim(claim, transactionManager.getEntityManager());
      if (persisted) {
        totalSaved++;
      }
    }
    totalProcessed += claims.size();

    while (claims.size() == getRecordLimit()) {
      String lastClaimId = getClaimId(claims.getLast());
      query =
          buildQuery(
              Optional.of(lastClaimId), table, getClaimIdColumnName(table), getRecordLimit());
      claims = executeQuery(query);
      for (TClaim claim : claims) {
        boolean persisted = samhsaUtil.processClaim(claim, transactionManager.getEntityManager());
        if (persisted) {
          totalSaved++;
        }
      }
      totalProcessed += claims.size();
    }
    LOGGER.info(
        String.format(
            "Processed %d claims from table %s. %d of them had SAMHSA codes.",
            totalProcessed, table, totalSaved));

    return totalSaved;
  }
}
