package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import gov.cms.bfd.pipeline.sharedutils.SamhsaUtil;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.pipeline.sharedutils.model.TableEntry;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class to backfill the SAMHSA tags. This will iterate through each of the tables, pull
 * any claims that don't already have SAMHSA tags, and checks them for SAMHSA codes, using
 * SamhsaUtil.
 */
public abstract class AbstractSamhsaBackfill {
  /** The Logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSamhsaBackfill.class);

  /**
   * Query to retrieve a list of claims objects, ignoring claims that already have SAMHSA tags. Will
   * start at a given claim id, and limit the results to a given limit.
   */
  protected String QUERY_WITH_STARTING_CLAIM =
      " SELECT * FROM ${tableName} t "
          + " WHERE ${claimColumn} > ${startingClaim} "
          + " AND NOT EXISTS "
          + " (SELECT 1 FROM ${tagTableName} g where t.${claimColumn} = g.clm_id) "
          + " ORDER BY ${claimColumn} ASC "
          + " LIMIT ${limit};";

  /**
   * Query to retrieve a list of claims objects, ignoring claims that already have SAMHSA tags.
   * Starts at the beginning of the sorted list of claims. This query will be run on the first
   * iteration on a table when we have no original claim to continue off of.
   */
  protected String QUERY_WITH_NO_STARTING_CLAIM =
      " SELECT * FROM ${tableName} t "
          + " WHERE NOT EXISTS "
          + " (SELECT 1 FROM ${tagTableName} g where t.${claimColumn} = g.clm_id) "
          + " ORDER BY ${claimColumn} ASC "
          + " LIMIT ${limit};";

  /** Query to perform upsert on the backfill progress table for a given claim table. */
  protected String UPSERT_PROGRESS_QUERY =
      " INSERT INTO ccw.samhsa_backfill_progress "
          + " (claim_table, last_processed_claim) "
          + " VALUES (:tableName, :lastClaim) "
          + " ON CONFLICT (claim_table) "
          + " DO UPDATE SET "
          + "   last_processed_claim = :lastClaim ";

  /** Query to get the backfill progress from the database for a given claim table. */
  protected String GET_PROGRESS_QUERY =
      " SELECT last_processed_claim FROM ccw.samhsa_backfill_progress "
          + " WHERE claim_table = :tableName ";

  /** The transaction manager. */
  TransactionManager transactionManager;

  /**
   * SamhsaUtil class. This will be used to check the claims for SAMHSA data, and create the tags if
   * necessary.
   */
  SamhsaUtil samhsaUtil;

  /** query batch size. */
  int batchSize;

  /**
   * Constructor.
   *
   * @param transactionManager The transaction manager.
   * @param batchSize the query batch size. This is the limit of claims to be pulled with each
   *     query.
   */
  public AbstractSamhsaBackfill(TransactionManager transactionManager, int batchSize) {
    this.transactionManager = transactionManager;
    this.batchSize = batchSize;
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
        entityManager -> (List<TClaim>) query.getResultList());
  }

  /**
   * Builds a query object by taking a SQL query string and replacing the parameter placeholders
   * with appropriate values.
   *
   * @param startingClaim The claim to start at. If this is empty, the version of the query with no
   *     starting claim will be used.
   * @param tableEntry The TableEntry pojo for this table.
   * @param limit the number of claims to pull at a time.
   * @return Query to run.
   */
  private Query buildQuery(Optional<String> startingClaim, TableEntry tableEntry, int limit) {
    return transactionManager.executeFunction(
        entityManager -> {
          Map<String, String> params =
              Map.of(
                  "tableName",
                  tableEntry.getClaimTable(),
                  "claimColumn",
                  tableEntry.getClaimColumnName(),
                  "startingClaim",
                  startingClaim.orElse(""),
                  "tagTableName",
                  tableEntry.getTagTable(),
                  "limit",
                  String.valueOf(limit));
          StringSubstitutor strSub = new StringSubstitutor(params);
          String queryStr =
              strSub.replace(
                  startingClaim.isPresent()
                      ? QUERY_WITH_STARTING_CLAIM
                      : QUERY_WITH_NO_STARTING_CLAIM);
          return entityManager.createNativeQuery(queryStr, tableEntry.getClaimClass());
        });
  }

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
  protected abstract List<TableEntry> getTables();

  /**
   * Entry point.
   *
   * @return The total number of claims for which tags were created.
   */
  public Long execute() {
    long total = 0L;
    for (TableEntry tableEntry : getTables()) {
      Long tableTotal = executeForTable(tableEntry);
      LOGGER.info(
          String.format(
              "Created tags for %d claims in table %s", tableTotal, tableEntry.getClaimTable()));
      total += tableTotal;
    }
    return total;
  }

  /**
   * Processes a claim with SamhsaUtil to check for SAMHSA codes.
   *
   * @param claim the claim to process
   * @return true if a claim was persisted.
   * @param <TClaim> Type of the claim.
   */
  protected abstract <TClaim> boolean processClaim(TClaim claim);

  /**
   * Iterates over all of the claims in a table, and checks for SAMHSA data.
   *
   * @param tableEntry Contains information about the tables and entities for this claim type.
   * @return The number of claims for which tags were created.
   * @param <TClaim> The type of the claim.
   */
  private <TClaim> Long executeForTable(TableEntry tableEntry) {
    long totalSaved = 0L;
    long totalProcessed = 0L;
    Optional<String> lastClaimId = getLastClaimId(tableEntry.getClaimTable());
    if (lastClaimId.isPresent()) {
      LOGGER.info(
          String.format(
              "Starting processing of table %s at claim %s",
              tableEntry.getClaimTable(), lastClaimId.get()));
    } else {
      LOGGER.info(
          String.format(
              "Starting processing of table %s from the beginning.", tableEntry.getClaimTable()));
    }
    List<TClaim> claims;
    do {
      Query query = buildQuery(lastClaimId, tableEntry, batchSize);
      claims = executeQuery(query);
      int savedInBatch = 0;
      for (TClaim claim : claims) {
        boolean persisted = processClaim(claim);
        if (persisted) {
          savedInBatch++;
        }
      }
      totalSaved += savedInBatch;
      LOGGER.info(
          String.format(
              "Processed Batch of %d claims from table %s. %d of them had SAMHSA codes.",
              batchSize, tableEntry.getClaimTable(), savedInBatch));
      totalProcessed += claims.size();
      lastClaimId =
          !claims.isEmpty() ? Optional.of(getClaimId(claims.getLast())) : Optional.empty();
      saveProgress(tableEntry.getClaimTable(), lastClaimId);
      // If the number of returned claims is not equal to the requested batch size, then the table
      // is done being processed.
    } while (claims.size() == batchSize);
    LOGGER.info(
        String.format(
            "Finished processing table %s. Processed %d claims, and %d of them had SAMHSA codes.",
            tableEntry.getClaimTable(), totalProcessed, totalSaved));

    return totalSaved;
  }

  /**
   * Saves the progress in processing the given table by writing the last processed claim id to the
   * database.
   *
   * @param table The table in question.
   * @param lastClaimId The last claim id processed.
   */
  private void saveProgress(String table, Optional<String> lastClaimId) {
    if (lastClaimId.isPresent()) {
      transactionManager.executeProcedure(
          entityManager -> {
            Query query =
                Objects.requireNonNull(entityManager).createNativeQuery(UPSERT_PROGRESS_QUERY);
            query.setParameter("tableName", table);
            query.setParameter("lastClaim", lastClaimId.get());
            query.executeUpdate();
          });
    }
  }

  /**
   * Rims a query to get the last claim id processed.
   *
   * @param table The claim table in question.
   * @return The last claim id processed for the given table.
   */
  private Optional<String> getLastClaimId(String table) {
    return transactionManager.executeFunction(
        entityManager -> {
          Query query = Objects.requireNonNull(entityManager).createNativeQuery(GET_PROGRESS_QUERY);
          query.setParameter("tableName", table);
          try {
            String claimId = (String) query.getSingleResult();
            return Optional.of(claimId);
          } catch (NoResultException e) {
            return Optional.empty();
          }
        });
  }
}
