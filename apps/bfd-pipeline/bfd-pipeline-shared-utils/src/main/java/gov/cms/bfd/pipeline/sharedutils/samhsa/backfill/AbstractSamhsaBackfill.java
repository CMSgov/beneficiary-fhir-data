package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import static gov.cms.bfd.pipeline.sharedutils.samhsa.backfill.QueryConstants.GT_CLAIM_LINE;
import static gov.cms.bfd.pipeline.sharedutils.samhsa.backfill.QueryConstants.TAG_UPSERT_QUERY;

import gov.cms.bfd.pipeline.sharedutils.SamhsaUtil;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.pipeline.sharedutils.model.BackfillProgress;
import gov.cms.bfd.pipeline.sharedutils.model.TableEntry;
import gov.cms.bfd.pipeline.sharedutils.model.TagCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;

/**
 * Abstract class to backfill the SAMHSA tags. This will iterate through the claims in a table, and
 * create SAMHSA tags for any claims that do not already have them.
 */
public abstract class AbstractSamhsaBackfill implements Callable {
  /** The Logger. */
  private final Logger logger;

  /** The table to use for this thread. */
  protected final TableEntry tableEntry;

  /** Query to perform upsert on the backfill progress table for a given claim table. */
  protected String UPSERT_PROGRESS_QUERY =
      " INSERT INTO ccw.samhsa_backfill_progress "
          + " (claim_table, last_processed_claim, total_processed, total_tags) "
          + " VALUES (:tableName, :lastClaim, :totalProcessed, :totalTags) "
          + " ON CONFLICT (claim_table) "
          + " DO UPDATE SET "
          + "   last_processed_claim = :lastClaim, total_processed = :totalProcessed, total_tags = :totalTags ";

  /** Query to get the backfill progress from the database for a given claim table. */
  protected String GET_PROGRESS_QUERY =
      " SELECT claim_table, last_processed_claim, total_processed, total_tags FROM ccw.samhsa_backfill_progress "
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
   * @param logger The logger.
   * @param tableEntry the table Entry for this thread.
   */
  public AbstractSamhsaBackfill(
      TransactionManager transactionManager, int batchSize, Logger logger, TableEntry tableEntry) {
    this.logger = logger;
    this.transactionManager = transactionManager;
    this.batchSize = batchSize;
    this.tableEntry = tableEntry;
    samhsaUtil = SamhsaUtil.getSamhsaUtil();
  }

  /**
   * Executes the query using the transaction manager.
   *
   * @param query The query to execute.
   * @return a list of claims.
   */
  private List<Object[]> executeQuery(Query query) {
    return (List<Object[]>) query.getResultList();
  }

  /**
   * Builds a query object by taking a SQL query string and replacing the parameter placeholders
   * with appropriate values.
   *
   * @param startingClaim The claim to start at. If this is empty, the version of the query with no
   *     starting claim will be used.
   * @param tableEntry The TableEntry pojo for this table.
   * @param limit the number of claims to pull at a time.
   * @param entityManager The entity manager.
   * @return Query to run.
   */
  private Query buildQuery(
      Optional<String> startingClaim,
      TableEntry tableEntry,
      int limit,
      EntityManager entityManager) {
    StringSubstitutor strSub;
    Map<String, String> params =
        Map.of(
            "gtClaimLine",
            startingClaim.isPresent() ? GT_CLAIM_LINE : "",
            "claimField",
            tableEntry.getClaimField());
    strSub = new StringSubstitutor(params);
    String queryStr = strSub.replace(tableEntry.getQuery());
    Query query = entityManager.createNativeQuery(queryStr);
    startingClaim.ifPresent(s -> query.setParameter("startingClaim", convertClaimId(s)));
    query.setParameter("limit", limit);
    return query;
  }

  /**
   * Converts the String value of the claimId to the correct type for the table.
   *
   * @param claim The Claim to check.
   * @param <TClaimId> Type of the claimId. Will be Long or String.
   * @return the claim id.
   */
  protected abstract <TClaimId> TClaimId convertClaimId(String claim);

  /**
   * Entry point.
   *
   * @return The total number of claims for which tags were created.
   */
  @Override
  public Long call() {
    long total = 0L;
    Long tableTotal = executeForTable(tableEntry);
    logger.info(
        String.format(
            "Created tags for %d claims in table %s", tableTotal, tableEntry.getClaimTable()));
    total += tableTotal;
    return total;
  }

  /**
   * Processes a claim with SamhsaUtil to check for SAMHSA codes.
   *
   * @param claim the claim to process
   * @param entityManager The entity manager.
   * @return true if a claim was persisted.
   */
  protected boolean processClaim(Object[] claim, EntityManager entityManager) {
    Object claimId = claim[0];
    LocalDate coverageStartDate =
        claim[1] == null ? LocalDate.parse("1970-01-01") : ((Date) claim[1]).toLocalDate();
    LocalDate coverageEndDate =
        claim[2] == null ? LocalDate.now() : ((Date) claim[2]).toLocalDate();
    List<String> codes =
        Arrays.asList(claim).subList(3, claim.length).stream()
            .filter(Objects::nonNull)
            .map(code -> (String) code)
            .collect(Collectors.toList());
    boolean persisted = samhsaUtil.processCodeList(codes, coverageStartDate, coverageEndDate);
    if (persisted) {
      writeEntry(claimId, tableEntry.getTagTable(), entityManager);
      return true;
    }
    return false;
  }

  /**
   * Iterates over all of the claims in a table, and checks for SAMHSA data.
   *
   * @param tableEntry Contains information about the tables and entities for this claim type.
   * @return The number of claims for which tags were created.
   * @param <TClaim> The type of the claim.
   */
  private <TClaim> Long executeForTable(TableEntry tableEntry) {
    // making these final Atomic objects allow us to use them inside of executeProcedure lambda.
    final AtomicLong claimsSize = new AtomicLong(0L);
    Optional<BackfillProgress> progress = getLastClaimId(tableEntry.getClaimTable());
    final AtomicLong totalSaved =
        new AtomicLong(
            progress.isPresent() && progress.get().getTotalTags() != null
                ? progress.get().getTotalTags()
                : 0L);
    final AtomicLong totalProcessed =
        new AtomicLong(
            progress.isPresent() && progress.get().getTotalProcessed() != null
                ? progress.get().getTotalProcessed()
                : 0L);
    Optional<String> id = progress.map(BackfillProgress::getLastClaimId);
    final AtomicReference<Optional<String>> lastClaimId = new AtomicReference<>(id);
    if (lastClaimId.get().isPresent()) {
      logger.info(
          String.format(
              "Starting processing of table %s at claim %s",
              tableEntry.getClaimTable(), lastClaimId.get().get()));
    } else {
      logger.info(
          String.format(
              "Starting processing of table %s from the beginning.", tableEntry.getClaimTable()));
    }

    do {
      try {
        transactionManager.executeProcedure(
            entityManager -> {
              Query query = buildQuery(lastClaimId.get(), tableEntry, batchSize, entityManager);
              List<Object[]> claims = executeQuery(query);
              int savedInBatch = 0;
              for (Object[] claim : claims) {
                boolean persisted = processClaim(claim, entityManager);
                if (persisted) {
                  savedInBatch++;
                }
              }
              totalSaved.accumulateAndGet(savedInBatch, Long::sum);
              totalProcessed.accumulateAndGet(claims.size(), Long::sum);
              logger.info(
                  String.format(
                      "Processed Batch of %d claims from table %s. %d of them had SAMHSA codes. Total processed so far: %d",
                      batchSize, tableEntry.getClaimTable(), savedInBatch, totalProcessed.get()));

              lastClaimId.set(
                  !claims.isEmpty()
                      ? Optional.of(String.valueOf(claims.getLast()[0]))
                      : Optional.empty());
              saveProgress(
                  tableEntry.getClaimTable(),
                  lastClaimId.get(),
                  totalProcessed.get(),
                  totalSaved.get(),
                  entityManager);

              claimsSize.set(claims.size());
            });
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
      // If the number of returned claims is not equal to the requested batch size, then the
      // table
      // is done being processed.
    } while (claimsSize.get() == batchSize);

    logger.info(
        String.format(
            "Finished processing table %s. Processed %d claims, and %d of them had SAMHSA codes.",
            tableEntry.getClaimTable(), totalProcessed.get(), totalSaved.get()));
    return totalSaved.get();
  }

  /**
   * Writes the tag entry to the database.
   *
   * @param claimId claim id -- Could be a Long or a String.
   * @param table tag table to use.
   * @param entityManager The entity manager.
   */
  protected void writeEntry(Object claimId, String table, EntityManager entityManager) {
    Map<String, String> params = Map.of("tagTable", table);
    StringSubstitutor strSub = new StringSubstitutor(params);
    String queryStr = strSub.replace(TAG_UPSERT_QUERY);
    Query query = entityManager.createNativeQuery(queryStr);
    query.setParameter("code", TagCode.R.toString());
    query.setParameter("claimId", claimId);
    query.executeUpdate();
    query.setParameter("code", TagCode._42CFRPart2.toString());
    query.executeUpdate();
  }

  /**
   * Saves the progress in processing the given table by writing the last processed claim id to the
   * database.
   *
   * @param table The table in question.
   * @param lastClaimId The last claim id processed.
   * @param totalProcessed Total progress so far.
   * @param totalTags The total number of tags created so far.
   * @param entityManager The entity manager.
   */
  private void saveProgress(
      String table,
      Optional<String> lastClaimId,
      Long totalProcessed,
      Long totalTags,
      EntityManager entityManager) {
    // In some cases, it's possible for lastClaimId to be empty. This isn't an error, it just means
    // that no results were returned in the last query.
    if (lastClaimId.isPresent()) {
      Query query = Objects.requireNonNull(entityManager).createNativeQuery(UPSERT_PROGRESS_QUERY);
      query.setParameter("tableName", table);
      query.setParameter("lastClaim", lastClaimId.get());
      query.setParameter("totalProcessed", totalProcessed);
      query.setParameter("totalTags", totalTags);
      query.executeUpdate();
    }
  }

  /**
   * executes a query to get the last claim id processed.
   *
   * @param table The claim table in question.
   * @return The last claim id processed for the given table.
   */
  private Optional<BackfillProgress> getLastClaimId(String table) {
    return transactionManager.executeFunction(
        entityManager -> {
          Query query =
              Objects.requireNonNull(entityManager)
                  .createNativeQuery(GET_PROGRESS_QUERY, BackfillProgress.class);
          query.setParameter("tableName", table);
          try {
            BackfillProgress progress = (BackfillProgress) query.getSingleResult();
            return Optional.of(progress);
          } catch (NoResultException e) {
            return Optional.empty();
          }
        });
  }
}
