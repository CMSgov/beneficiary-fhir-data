package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import static gov.cms.bfd.pipeline.sharedutils.samhsa.backfill.AbstractSamhsaBackfill.COLUMN_TYPE.SAMHSA_CODE;
import static gov.cms.bfd.pipeline.sharedutils.samhsa.backfill.QueryConstants.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.bfd.pipeline.sharedutils.SamhsaUtil;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.pipeline.sharedutils.model.BackfillProgress;
import gov.cms.bfd.pipeline.sharedutils.model.TableEntry;
import gov.cms.bfd.pipeline.sharedutils.model.TagDetails;
import gov.cms.bfd.sharedutils.TagCode;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;

/**
 * Abstract class to backfill the SAMHSA tags. This will iterate through the claims in a table, and
 * create SAMHSA tags for any claims that do not already have them. It stores progress in the
 * ccw.samhsa_backfill_progress table, so that if the job is stopped, it can pick up where it left
 * off on its next run. In order to start the job over from the beginning, the
 * ccw.samhsa_backfill_progress table must be cleared of all rows (or just the rows for the tables
 * to start from the beginning).
 */
public abstract class AbstractSamhsaBackfill implements Callable {
  /** The Logger. */
  @Getter private final Logger logger;

  /** The table to use for this thread. */
  @Getter protected final TableEntry tableEntry;

  /** The transaction manager. */
  TransactionManager transactionManager;

  /**
   * SamhsaUtil class. This will be used to check the claims for SAMHSA data, and create the tags if
   * necessary.
   */
  @Getter @Setter SamhsaUtil samhsaUtil;

  /** query batch size. */
  @Getter @Setter int batchSize;

  /** The log interval. */
  @Getter Long logInterval;

  /** The size of the list of claims pulled in a particular iteration. */
  @Getter @Setter int claimSize = 0;

  /** total claims saved to the database. */
  @Getter @Setter long totalSaved;

  /** Total number of claims processed, overall. */
  @Getter @Setter long totalProcessed;

  /** Last claim id processed in a particular iteration. */
  @Getter @Setter Optional<String> lastClaimId;

  /** The start time of a logging interval. */
  @Getter @Setter Instant startTime;

  /** Total claims processed in a particular logging interval. */
  @Getter @Setter Long totalProcessedInInterval;

  /** The query to use. */
  @Getter @Setter String query;

  @Getter @Setter int fromDatePosition;
  @Getter @Setter int toDatePosition;
  @Getter @Setter int claimIdPosition;
  @Getter @Setter List<Integer> samhsaColumnPositions;
  @Getter @Setter int lineNumPosition;
  @Getter @Setter String[] columnNames;

  /** The column type for a column. */
  public enum COLUMN_TYPE {
    /** From date column type. */
    DATE_FROM,
    /** To date column type. */
    DATE_TO,
    /** Line num column type. */
    LINE_NUM,
    /** Claim ID column type. */
    CLAIM_ID,
    /** SAMHSA code column type. */
    SAMHSA_CODE
  }

  /**
   * Constructor.
   *
   * @param transactionManager The transaction manager.
   * @param batchSize the query batch size. This is the limit of claims to be pulled with each
   *     query.
   * @param logger The logger.
   * @param logInterval The log reporting interval, in seconds.
   * @param tableEntry the table Entry for this thread.
   */
  public AbstractSamhsaBackfill(
      TransactionManager transactionManager,
      int batchSize,
      Logger logger,
      Long logInterval,
      TableEntry tableEntry) {
    this.logger = logger;
    this.logInterval = logInterval;
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
    return query.getResultList();
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
  Query buildQuery(
      Optional<String> startingClaim,
      TableEntry tableEntry,
      int limit,
      EntityManager entityManager) {
    StringSubstitutor strSub;
    // GT_CLAIM_LINE allows the query to start at a given claim number.
    Map<String, String> params =
        Map.of(
            "gtClaimLine",
            startingClaim.isPresent() ? GT_CLAIM_LINE : "",
            "claimField",
            tableEntry.getClaimField());
    strSub = new StringSubstitutor(params);
    String queryStr = strSub.replace(getQuery());
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
    Long tableTotal = executeForTable();
    logger.info("Created tags for {} claims in table {}", tableTotal, tableEntry.getClaimTable());
    total += tableTotal;
    return total;
  }

  /**
   * Gets the positions of all columns of the specified type.
   *
   * @param columnType The column type.
   * @param queryColumns Map of columns to their types.
   * @param uniqueColumnType True if there should only be one column of this type.
   * @param required True if there is required to be at least one column of this type.
   * @return The List of column positions.
   */
  public static List<Integer> getColumnPositions(
      COLUMN_TYPE columnType,
      Map<String, COLUMN_TYPE> queryColumns,
      boolean uniqueColumnType,
      boolean required) {
    List<Integer> results = new ArrayList<>();
    int i = 0;
    for (Map.Entry<String, COLUMN_TYPE> entry : queryColumns.entrySet()) {
      if (entry.getValue().equals(columnType)) {
        results.add(i);
      }
      i++;
    }
    if (uniqueColumnType && results.size() > 1) {
      throw new BadCodeMonkeyException(
          String.format(
              "Query should have a single column for %s, but it has multiple.", columnType.name()));
    } else if (required && results.isEmpty()) {
      throw new BadCodeMonkeyException(
          String.format("Query should have a column for %s, but does not.", columnType.name()));
    }
    return results;
  }

  /**
   * Processes a claim with SamhsaUtil to check for SAMHSA codes. Does not use Entities, instead
   * using the positions in the array to determine the column type. This obviously relies on the
   * queries being constructed in a particular way, so it's not ideal -- but, since it does not have
   * to construct an entity, it is the fastest way to query.
   *
   * @param claim the claim to process
   * @param datesMap Contains previously fetched claim dates for this claim id. This is useful if a
   *     claim has more than one lineitem.
   * @param entityManager The entity manager.
   * @return The total number of tags saved.
   */
  protected int processClaim(
      Object[] claim, HashMap<String, Object[]> datesMap, EntityManager entityManager) {
    Object claimId = claim[claimIdPosition];
    Optional<Object[]> dates = Optional.empty();
    // Line item tables pull the active dates with a separate query, while parent tables use the
    // original query.
    if (fromDatePosition >= 0 && toDatePosition >= 0) {
      dates = Optional.of(new Object[] {claim[fromDatePosition], claim[toDatePosition]});
    } else if (datesMap.containsKey(claimId.toString())) {
      // The active dates for this claim were previously saved for a different record with the same
      // claim id.
      dates = Optional.of(datesMap.get(claimId.toString()));
    }
    // Create an array that contains only the SAMHSA codes for this record.
    List<TagDetails> tagDetailsList =
        samhsaUtil.processCodeList(
            claim,
            tableEntry,
            claimId,
            dates,
            datesMap,
            entityManager,
            samhsaColumnPositions,
            columnNames,
            lineNumPosition);
    if (!tagDetailsList.isEmpty()) {
      return writeEntry(claimId, tableEntry.getTagTable(), tagDetailsList, entityManager);
    }
    return 0;
  }

  abstract COLUMN_TYPE getEntryType(String entry);

  /**
   * Puts the columns in a map, along with an enum describing their purpose.
   *
   * @param columns The list of columns, in order.
   * @return a map containing the columns mapped to their purpose.
   */
  Map<String, COLUMN_TYPE> markSamhsaColumns(List<String> columns) {
    // Must be a LinkedHashMap here, to preserve the order.
    HashMap<String, COLUMN_TYPE> map = new LinkedHashMap<>();

    for (String column : columns) {
      map.put(column, getEntryType(column));
    }
    return map;
  }

  /**
   * Builds the SAMHSA query string template, and also builds the queryColumns object, which will
   * keep track of the column positions and their purpose.
   *
   * @param table The table.
   * @param claimField The claim id field.
   * @param columns The columns to check.
   * @return The query string for a particular table.
   */
  protected String buildQueryStringTemplate(String table, String claimField, String... columns) {
    String concatColumns = String.join(", ", columns);
    List<String> samhsaColumnOrder = new ArrayList<>();
    samhsaColumnOrder.add(claimField);
    samhsaColumnOrder.addAll(splitColumnCsvToList(columns));
    Map<String, COLUMN_TYPE> queryColumns = markSamhsaColumns(samhsaColumnOrder);
    setColumnPositions(queryColumns);
    StringBuilder builder = new StringBuilder();
    builder.append("SELECT ");
    builder.append(claimField);
    builder.append(", ");
    builder.append(concatColumns);
    builder.append(" FROM ");
    builder.append(table);
    builder.append(
        " ${gtClaimLine} ORDER BY "); // ${gtClaimLine} is used to insert the last processed claim
    // id into the query. If there is no last processed claim id,
    // ${gtClaimLine} will be evaluated to an empty string.
    builder.append(claimField);
    builder.append(" limit :limit"); // limit will be the batch size set in the configuration.
    return builder.toString();
  }

  private void setColumnPositions(Map<String, AbstractSamhsaBackfill.COLUMN_TYPE> queryColumns) {
    claimIdPosition = getColumnPositions(COLUMN_TYPE.CLAIM_ID, queryColumns, true, true).getFirst();
    fromDatePosition =
        getColumnPositions(COLUMN_TYPE.DATE_FROM, queryColumns, true, false).stream()
            .findFirst()
            .orElse(-1);
    toDatePosition =
        getColumnPositions(COLUMN_TYPE.DATE_TO, queryColumns, true, false).stream()
            .findFirst()
            .orElse(-1);
    samhsaColumnPositions = getColumnPositions(SAMHSA_CODE, queryColumns, false, true);
    lineNumPosition =
        getColumnPositions(COLUMN_TYPE.LINE_NUM, queryColumns, true, false).stream()
            .findFirst()
            .orElse(-1);
    columnNames = queryColumns.keySet().toArray(new String[0]);
  }

  /**
   * The columns come in as a list of strings, some of which may be a comma separated list of
   * columns. This is due to the way the enumerateColumns method builds the list of columns.
   *
   * @param columns An array containing the columns.
   * @return a list of the columns.
   */
  private List<String> splitColumnCsvToList(String[] columns) {
    List columnsList = new ArrayList();
    for (String column : columns) {
      List newColumns =
          Arrays.stream(column.split(",")).map(String::trim).collect(Collectors.toList());
      columnsList.addAll(newColumns);
    }
    return columnsList;
  }

  /**
   * Contents of the main loop to process the table.
   *
   * @param entityManager The entityManager
   */
  void executeQueryLoop(EntityManager entityManager) {
    Query query = buildQuery(getLastClaimId(), getTableEntry(), getBatchSize(), entityManager);
    List<Object[]> claims = executeQuery(query);
    int savedInBatch = 0;
    // This Map will allow us to save the active dates for a claim to be used in multiple
    // records with the same claim id.
    HashMap<String, Object[]> datesMap = new HashMap<>();
    // Iterate over the batch of claims that were just pulled, and process them for SAMHSA
    // codes. */
    for (Object[] claim : claims) {
      savedInBatch += processClaim(claim, datesMap, entityManager);
    }
    incrementTotalSaved(savedInBatch);
    incrementTotalProcessedInInterval(claims.size());
    incrementTotalProcessed(claims.size());
    // Checks if a progress message should be logged.
    checkTimeIntervalForLogging();
    setLastClaimId(
        !claims.isEmpty() ? Optional.of(String.valueOf(claims.getLast()[0])) : Optional.empty());
    // Write progress to the progress table, so that we can restart at the last processed
    // claim id if interrupted.
    saveProgress(
        getTableEntry().getClaimTable(),
        getLastClaimId(),
        getTotalProcessed(),
        getTotalSaved(),
        entityManager);
    setClaimSize(claims.size());
  }

  /** Logs a progress message if enough time has passed. */
  private void checkTimeIntervalForLogging() {
    // Only write progress to the log at a given interval.
    if (getStartTime().plus(getLogInterval(), ChronoUnit.SECONDS).isBefore(Instant.now())) {
      getLogger()
          .info(
              "Processed {} claims from table {}, {} in the last {} seconds. {} SAMHSA tags saved total.",
              getTotalProcessed(),
              getTableEntry().getClaimTable(),
              getTotalProcessedInInterval(),
              Duration.between(getStartTime(), Instant.now()).getSeconds(),
              getTotalSaved());
      setStartTime(Instant.now());
      setTotalProcessedInInterval(0L);
    }
  }

  /**
   * increments the totalProcessed value.
   *
   * @param amount The amount to increment by.
   */
  private void incrementTotalProcessed(int amount) {
    totalProcessed += amount;
  }

  /**
   * Increments the totalProcessedInInterval value.
   *
   * @param amount The amount to increment by.
   */
  private void incrementTotalProcessedInInterval(int amount) {
    totalProcessedInInterval += amount;
  }

  /**
   * Increments the totalSaved value.
   *
   * @param amount The amount to increment by.
   */
  private void incrementTotalSaved(int amount) {
    totalSaved += amount;
  }

  /**
   * Iterates over all of the claims in a table, and checks for SAMHSA data.
   *
   * @return The number of claims for which tags were created.
   */
  private Long executeForTable() {
    setupExecution();
    do {
      try {
        transactionManager.executeProcedure(this::executeQueryLoop);

      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
      // If the number of returned claims is not equal to the requested batch size, then the
      // table
      // is done being processed.
    } while (getClaimSize() == getBatchSize());

    logger.info(
        "Finished processing table {}. Processed {} claims, and {} of them had SAMHSA codes.",
        tableEntry.getClaimTable(),
        getTotalProcessed(),
        getTotalSaved());
    return getTotalSaved();
  }

  /** Sets up the fields needed to begin processing the table. */
  private void setupExecution() {
    Optional<BackfillProgress> progress = getLastClaimId(getTableEntry().getClaimTable());
    setTotalSaved(
        progress.isPresent() && progress.get().getTotalTags() != null
            ? progress.get().getTotalTags()
            : 0L);
    setTotalProcessed(
        progress.isPresent() && progress.get().getTotalProcessed() != null
            ? progress.get().getTotalProcessed()
            : 0L);
    setLastClaimId(progress.map(BackfillProgress::getLastClaimId));

    // lastClaimId will only be present if this is the second or later run of the job. */
    if (getLastClaimId().isPresent()) {
      logger.info(
          "Starting processing of table {} at claim {}",
          getTableEntry().getClaimTable(),
          getLastClaimId().get());
    } else {
      logger.info(
          "Starting processing of table {} from the beginning.", getTableEntry().getClaimTable());
    }
    setStartTime(Instant.now());
    setTotalProcessedInInterval(0L);
  }

  /**
   * Writes the tag entry to the database. Due to a performance trade-off, the details column will
   * not be written to.
   *
   * @param claimId claim id -- Could be a Long or a String.
   * @param table tag table to use.
   * @param detailsList List containing TagDetails for this claim.
   * @param entityManager The entity manager.
   * @return the total number of records saved.
   */
  protected int writeEntry(
      Object claimId, String table, List<TagDetails> detailsList, EntityManager entityManager) {
    ObjectMapper mapper = new ObjectMapper();
    String detailsJson;
    try {
      detailsJson = mapper.writeValueAsString(detailsList);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    Map<String, String> params = Map.of("tagTable", table);
    StringSubstitutor strSub = new StringSubstitutor(params);
    String queryStr = strSub.replace(TAG_UPSERT_QUERY);
    Query query = entityManager.createNativeQuery(queryStr);
    query.setParameter("code", TagCode.R.toString());
    query.setParameter("claimId", claimId);
    query.setParameter("details", detailsJson);
    int total = query.executeUpdate();
    query.setParameter("code", TagCode._42CFRPart2.toString());
    total += query.executeUpdate();
    return total;
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
  void saveProgress(
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
   * Executes a query to get the last claim id processed.
   *
   * @param table The claim table in question.
   * @return The last claim id processed for the given table.
   */
  Optional<BackfillProgress> getLastClaimId(String table) {
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
