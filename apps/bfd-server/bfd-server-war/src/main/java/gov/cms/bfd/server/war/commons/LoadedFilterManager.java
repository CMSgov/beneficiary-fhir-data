package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.param.DateRangeParam;
import gov.cms.bfd.model.rif.LoadedBatch;
import gov.cms.bfd.model.rif.LoadedFile;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Monitors the loaded files and their associated batches in the database. Creates Bloom filters to
 * match these files.
 */
@Component
public class LoadedFilterManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoadedFilterManager.class);

  /** A date before the lastUpdate feature was rolled out. */
  @VisibleForTesting // Confusingly, unit tests are in a different package, thus public visibility
  public static final Instant BEFORE_LAST_UPDATED_FEATURE = Instant.parse("2020-01-01T00:00:00Z");

  /** The connection to the DB. */
  private EntityManager entityManager;

  /** The direct data source connection to the DB. */
  private DataSource dataSource;

  /** The filter set. */
  @Getter private List<LoadedFileFilter> filters;

  /** The latest transaction time from the LoadedBatch files. */
  private Instant transactionTime;

  /** The last LoadedBatch.created in the filter set. */
  private Instant lastBatchCreated;

  /** The first LoadedBatch.created in the filter set. */
  private Instant firstBatchCreated;

  /**
   * A tuple of values: LoadedFile.loadedFileid, LoadedFile.created, max(LoadedBatch.created). Used
   * for an optimized query that includes only what is needed to refresh filters.
   */
  @Getter
  @AllArgsConstructor
  public static class LoadedTuple {
    /** The id for the loaded file. */
    private long loadedFileId;

    /** The load start time for the loaded file. */
    private Instant firstUpdated;

    /** The load end time for the loaded file. */
    private Instant lastUpdated;
  }

  /**
   * Create a manager for {@link LoadedFileFilter}s.
   *
   * @param dataSource the {@link DataSource} provided by {@link
   *     gov.cms.bfd.server.war.SpringConfiguration}
   */
  public LoadedFilterManager(DataSource dataSource) {
    this.filters = new ArrayList<>();
    this.dataSource = dataSource;
  }

  /**
   * The last time that the filter manager knows that database has been updated.
   *
   * @return the last batch's created timestamp
   */
  public Instant getTransactionTime() {
    if (transactionTime == null) {
      throw new RuntimeException("LoadedFilterManager has not been initialized.");
    }
    return transactionTime;
  }

  /**
   * The return the first batch that the filter manager knows about.
   *
   * @return the first batch's created timestamp
   */
  public Instant getLastBatchCreated() {
    if (lastBatchCreated == null) {
      throw new RuntimeException("LoadedFilterManager has not been refreshed.");
    }
    return lastBatchCreated;
  }

  /**
   * The return the first batch that the filter manager knows about.
   *
   * @return the first batch's created timestamp
   */
  public Instant getFirstBatchCreated() {
    if (firstBatchCreated == null) {
      throw new RuntimeException("LoadedFilterManager has not been refreshed.");
    }
    return firstBatchCreated;
  }

  /**
   * Set up the JPA entityManager for the database to query.
   *
   * @param entityManager to use
   */
  @PersistenceContext
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /** Called to finish initialization of the manager. */
  @PostConstruct
  public synchronized void init() {
    try {
      // The transaction time will either the last LoadedBatch or some earlier time
      this.transactionTime = fetchLastLoadedBatchCreated().orElse(BEFORE_LAST_UPDATED_FEATURE);
    } catch (Exception ex) {
      this.transactionTime = BEFORE_LAST_UPDATED_FEATURE;
      LOGGER.warn("Unable to query for transactionTime on init. Will be set in refreshFilters", ex);
    }
  }

  /**
   * Is the result set going to be empty for this beneficiary and time period?
   *
   * <p>This result is eventually consistent with the state of the BFD database. The FilterManager's
   * knowledge of the state of the database lags the writes to the database by as much as a second.
   *
   * @param beneficiaryId to test
   * @param lastUpdatedRange to test
   * @return true if the results set is empty. false if the result set *may* contain items.
   */
  public synchronized boolean isResultSetEmpty(
      Long beneficiaryId, DateRangeParam lastUpdatedRange) {
    if (beneficiaryId == null) {
      // This case should not happen and be caught/validated before this, therefore if we get here
      // it is a code error
      throw new IllegalArgumentException("Beneficiary id cannot be null/empty");
    }

    if (!isInBounds(lastUpdatedRange)) {
      // Out of bounds has to be treated as unknown result
      return false;
    }

    // Within the known interval that search for matching filters
    for (LoadedFileFilter filter : filters) {
      if (filter.matchesDateRange(lastUpdatedRange)) {
        if (filter.mightContain(beneficiaryId)) {
          return false;
        }
      } else if (filter
          .getLastUpdated()
          .isBefore(lastUpdatedRange.getLowerBoundAsInstant().toInstant())) {
        // filters are sorted in descending by lastUpdated time, so we can exit early from this
        // loop
        return true;
      }
    }
    return true;
  }

  /**
   * Test the passed in range against the range of information that filter manager knows about.
   *
   * <p>This result is eventually consistent with the state of the BFD database. The FilterManager's
   * knowledge of the state of the database lags the writes to the database by as much as a second.
   *
   * @param range to test against
   * @return true iff the range is within the bounds of the filters
   */
  public synchronized boolean isInBounds(DateRangeParam range) {
    if (range == null || getFilters().size() == 0) return false;

    // The manager has a "known" interval which it has information about. The known range
    // is from the firstFilterUpdate to the future.
    final Instant lowerBound =
        range.getLowerBoundAsInstant() != null ? range.getLowerBoundAsInstant().toInstant() : null;
    return lowerBound != null && lowerBound.toEpochMilli() >= getFirstBatchCreated().toEpochMilli();
  }

  /**
   * Called periodically to build and refresh the filters list from the entityManager.
   *
   * <p>The {@link #lastBatchCreated} and {@link #firstBatchCreated} fields are updated by this
   * call.
   */
  @Scheduled(fixedDelay = 1000, initialDelay = 2000)
  public void refreshFilters() {
    /*
     * Dev note: the pipeline has a process to trim the files list. Nevertheless, building a set of
     * bloom filters may take a while. This method is expected to be called on it's own thread by
     * the the Spring framework. In addition, it doesn't lock this object until the end of the
     * process, so this filter building process can happen without interfering with serving. Also,
     * this refresh time will be proportional to the number of files which have been loaded in the
     * past refresh period. If no files have been loaded, this refresh should take less than a
     * millisecond.
     */
    try {
      // If transactionTime remains unset we should try to set it again in case requests are being
      // made while the bloom filters are loading.
      if (this.transactionTime.equals(BEFORE_LAST_UPDATED_FEATURE)) {
        this.transactionTime = fetchLastLoadedBatchCreated().orElse(BEFORE_LAST_UPDATED_FEATURE);
      }

      // If new batches are present, then build new filters for the affected files
      final Instant currentLastBatchCreated =
          fetchLastLoadedBatchCreated().orElse(BEFORE_LAST_UPDATED_FEATURE);

      if (this.lastBatchCreated == null
          || this.lastBatchCreated.isBefore(currentLastBatchCreated)) {
        LOGGER.info(
            "Refreshing LoadedFile filters with new filters from {} to {}",
            this.lastBatchCreated,
            currentLastBatchCreated);

        List<LoadedTuple> loadedTuples = fetchLoadedTuples(this.lastBatchCreated);
        Stream<LoadedFileFilter> updatedFilters =
            buildMergedFilters(
                this.filters,
                loadedTuples,
                this::fetchLoadedBatches,
                this::fetchBatchSizeByFileId,
                this::fetchEstimatedBeneficiariesCountByFileId);

        // If batches been trimmed, then remove filters which are no longer present
        final Instant currentFirstBatchUpdate =
            fetchFirstLoadedBatchCreated().orElse(BEFORE_LAST_UPDATED_FEATURE);

        if (this.firstBatchCreated == null
            || this.firstBatchCreated.isBefore(currentFirstBatchUpdate)) {
          LOGGER.info("Trimmed LoadedFile filters before {}", currentFirstBatchUpdate);
          List<LoadedFile> loadedFiles = fetchLoadedFiles();
          updatedFilters = buildTrimmedFilters(updatedFilters, loadedFiles);
        }

        LOGGER.info(
            "Updating timestamps. currentFirstBatchUpdate={} currentLastBatchCreated={}",
            currentFirstBatchUpdate,
            currentLastBatchCreated);

        set(updatedFilters.toList(), currentFirstBatchUpdate, currentLastBatchCreated);
      }
    } catch (Throwable ex) {
      LOGGER.error("Error found refreshing LoadedFile filters", ex);
    }
  }

  /**
   * Set the current state in consistent fashion.
   *
   * @param filters the filters
   * @param firstBatchCreated the first batch created
   * @param lastBatchCreated the last batch created
   */
  public synchronized void set(
      List<LoadedFileFilter> filters, Instant firstBatchCreated, Instant lastBatchCreated) {
    this.filters = filters;
    this.firstBatchCreated = firstBatchCreated;
    this.lastBatchCreated = lastBatchCreated;
    this.transactionTime = lastBatchCreated;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return "LoadedFilterManager [filters.size="
        + (filters != null ? String.valueOf(filters.size()) : "null")
        + ", transactionTime="
        + transactionTime
        + ", firstBatchCreated="
        + firstBatchCreated
        + ", lastBatchCreated="
        + lastBatchCreated
        + "]";
  }

  /*
   * Dev Note: The following static methods encapsulate the logic of the manager. They are separated
   * from the state of the manager to allow for easy testing. They should be considered private to
   * the class, but they are made public for tests.
   *
   * <p>If you are interested, the idea comes from:
   * https://www.mokacoding.com/blog/functional-core-reactive-shell/
   */

  /**
   * Create an updated, merged {@link LoadedFileFilter} {@link Stream} from existing filters and
   * newly loaded files and batches.
   *
   * @param existingFilters that should be included
   * @param loadedTuples that come from new LoadedBatch
   * @param fetchById to use retrieve list of LoadedBatch by id
   * @param fetchBatchSizeByFileId used to retrieve number of {@link LoadedBatch} per {@link
   *     LoadedTuple}
   * @param fetchEstimatedBenesCountByFileId a function that returns an estimated count of
   *     beneficiaries per-{@link LoadedBatch} for the given {@link LoadedFile} ID
   * @return a new filter {@link Stream}
   */
  public static Stream<LoadedFileFilter> buildMergedFilters(
      List<LoadedFileFilter> existingFilters,
      List<LoadedTuple> loadedTuples,
      BiFunction<Long, Integer, Stream<LoadedBatch>> fetchById,
      LongFunction<Long> fetchBatchSizeByFileId,
      LongFunction<Long> fetchEstimatedBenesCountByFileId) {
    return Stream.concat(
            existingFilters.stream()
                .filter(
                    f ->
                        loadedTuples.stream()
                            .noneMatch(t -> t.getLoadedFileId() == f.getLoadedFileId())),
            buildNewFilters(
                loadedTuples, fetchById, fetchBatchSizeByFileId, fetchEstimatedBenesCountByFileId))
        // Sort each filter in descending order to optimize search time when determining if a result
        // would be empty
        .sorted((a, b) -> b.getFirstUpdated().compareTo(a.getFirstUpdated()));
  }

  /**
   * Build a new {@link LoadedFileFilter} {@link Stream}.
   *
   * @param loadedTuples that come from new LoadedBatch
   * @param fetchById to use retrieve list of LoadedBatch by id
   * @param fetchBatchSizeByFileId used to retrieve number of {@link LoadedBatch} per {@link
   *     LoadedTuple}
   * @param fetchEstimatedBenesCountByFileId a function that returns an estimated count of
   *     beneficiaries per-{@link LoadedBatch} for the given {@link LoadedFile} ID
   * @return a new filter {@link Stream}
   */
  public static Stream<LoadedFileFilter> buildNewFilters(
      List<LoadedTuple> loadedTuples,
      BiFunction<Long, Integer, Stream<LoadedBatch>> fetchById,
      LongFunction<Long> fetchBatchSizeByFileId,
      LongFunction<Long> fetchEstimatedBenesCountByFileId) {
    return loadedTuples.stream()
        .map(
            t ->
                buildFilter(
                    t, fetchById, fetchBatchSizeByFileId, fetchEstimatedBenesCountByFileId));
  }

  /**
   * Trim filters to match current {@link LoadedFile} {@link Stream}. Filters out any {@link
   * LoadedFileFilter}s that are not in the {@code loaded_files} table.
   *
   * @param existingFilters {@link Stream} of current {@link LoadedFileFilter}s
   * @param loadedFiles {@link Stream} of current {@link LoadedFile}s
   * @return a new filter {@link Stream}
   */
  public static Stream<LoadedFileFilter> buildTrimmedFilters(
      Stream<LoadedFileFilter> existingFilters, List<LoadedFile> loadedFiles) {
    return existingFilters.filter(
        filter ->
            loadedFiles.stream()
                .anyMatch(file -> file.getLoadedFileId() == filter.getLoadedFileId()));
  }

  /**
   * Build a filter for this loaded file. Should be a pure function.
   *
   * @param tuple the {@link LoadedTuple} for a given file
   * @param fetchById a function which returns a list of batches
   * @param fetchBatchSizeByFileId a function that returns the batch size of the file
   * @param fetchEstimatedBenesCountByFileId a function that returns an estimated count of
   *     beneficiaries per-{@link LoadedBatch} for the given {@link LoadedTuple} file ID
   * @return a new filter
   */
  public static LoadedFileFilter buildFilter(
      LoadedTuple tuple,
      BiFunction<Long, Integer, Stream<LoadedBatch>> fetchById,
      LongFunction<Long> fetchBatchSizeByFileId,
      LongFunction<Long> fetchEstimatedBenesCountByFileId) {
    final var fileId = tuple.getLoadedFileId();
    final var batchCount = fetchBatchSizeByFileId.apply(fileId).intValue();
    if (batchCount == 0) {
      throw new IllegalArgumentException("Batches cannot be empty for a filter");
    }

    final var estimatedBeneficiaryCount = fetchEstimatedBenesCountByFileId.apply(fileId).intValue();

    // It is important to get a good estimate of the number of entries for
    // an accurate FFP and minimal memory size. This one assumes that all batches are equally-sized
    // with respect to their beneficiaries
    final var bloomFilter = LoadedFileFilter.createFilter(batchCount * estimatedBeneficiaryCount);
    // Loop through all batches, filling the bloom filter based upon each beneficiary ID
    fetchById
        .apply(fileId, batchCount)
        .flatMap(b -> b.getBeneficiaries().stream())
        .forEach(bloomFilter::putLong);
    LOGGER.info(
        "Built a filter for {} with {} batches; BloomFilter size {}, BloomFilter cardinality {}",
        fileId,
        batchCount,
        bloomFilter.bitSize(),
        bloomFilter.cardinality());

    return new LoadedFileFilter(
        fileId, batchCount, tuple.getFirstUpdated(), tuple.getLastUpdated(), bloomFilter);
  }

  /* DB Operations */

  /**
   * Return the max date from the LoadedBatch table. If no batches are present, then the schema
   * migration time which will be a timestamp before the first loaded batch.
   *
   * @return the max date
   */
  private Optional<Instant> fetchLastLoadedBatchCreated() {
    Instant maxCreated =
        entityManager
            .createQuery("select max(b.created) from LoadedBatch b", Instant.class)
            .getSingleResult();
    return Optional.ofNullable(maxCreated);
  }

  /**
   * Return the min date from the LoadedBatch table.
   *
   * @return the min date
   */
  private Optional<Instant> fetchFirstLoadedBatchCreated() {
    Instant minBatchId =
        entityManager
            .createQuery("select min(b.created) from LoadedBatch b", Instant.class)
            .getSingleResult();
    return Optional.ofNullable(minBatchId);
  }

  /**
   * Fetch the tuple of (loadedFileId, LoadedFile.created, max(LoadedBatch.created)).
   *
   * @param after limits the query to include batches created after this timestamp
   * @return tuples that meet the after criteria or an empty list
   */
  private List<LoadedTuple> fetchLoadedTuples(Instant after) {
    final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<LoadedTuple> query = cb.createQuery(LoadedTuple.class);
    final Root<LoadedFile> f = query.from(LoadedFile.class);
    Join<LoadedFile, LoadedBatch> b = f.join("batches");
    query =
        query.select(
            cb.construct(
                LoadedTuple.class,
                f.get("loadedFileId"),
                f.get("created"),
                cb.max(b.get("created"))));
    if (after != null) {
      query = query.where(cb.greaterThan(b.get("created"), after));
    }
    query =
        query.groupBy(f.get("loadedFileId"), f.get("created")).orderBy(cb.desc(f.get("created")));
    return entityManager.createQuery(query).getResultList();
  }

  /**
   * Fetch all the files that are currently loaded.
   *
   * @return the LoadedFiles or an empty list
   */
  private List<LoadedFile> fetchLoadedFiles() {
    return entityManager
        .createQuery("select f from LoadedFile f", LoadedFile.class)
        .getResultList();
  }

  /**
   * Fetch the number of {@link LoadedBatch}s associated with the given {@link LoadedFile}.
   *
   * @param fileId id of the {@link LoadedFile}
   * @return the number of {@link LoadedBatch}s associated with the given {@link LoadedFile} ID
   */
  private long fetchBatchSizeByFileId(long fileId) {
    return entityManager
        .createQuery("select count(b) from LoadedBatch b where loadedFileId = :fileId", Long.class)
        .setParameter("fileId", fileId)
        .getSingleResult();
  }

  /**
   * Fetch the number of {@link LoadedBatch}s associated with the given {@link LoadedFile}.
   *
   * @param fileId id of the {@link LoadedFile}
   * @return the number of {@link LoadedBatch}s associated with the given {@link LoadedFile} ID
   */
  private long fetchEstimatedBeneficiariesCountByFileId(long fileId) {
    try (final var conn = dataSource.getConnection();
        final var stm =
            conn.prepareStatement(
                "select array_length(string_to_array(beneficiaries, ','), 1) as bene_count from ccw.loaded_batches where loaded_file_id = ? limit 1")) {
      stm.setLong(1, fileId);

      final var result = stm.executeQuery();

      result.next();
      return result.getLong("bene_count");
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Fetch all the batches associated with LoadedFile.
   *
   * @param loadedFileId of the {@link LoadedFile}
   * @param batchCount number of batches associated with the {@link LoadedFile}
   * @return a list of LoadedBatches or an empty list
   * @implNote We avoid using the {@link EntityManager}/JPA in order to use a cursor to load the
   *     {@link LoadedBatch}s as otherwise upto 10 million {@link LoadedBatch}s could be loaded into
   *     memory for a single {@link LoadedFile} in prod. This causes immense memory utilization, and
   *     has led to multiple instances of running Server containers falling over due to OOM.
   */
  // We suppress the S2095 warning (unclosed closeable) because we close them outside
  // the scope of this method via Stream::onClose once the Stream is exhausted.
  @SuppressWarnings("java:S2095")
  private Stream<LoadedBatch> fetchLoadedBatches(long loadedFileId, int batchCount) {
    try {
      final var conn = dataSource.getConnection();
      conn.setAutoCommit(false);

      final var stm =
          conn.prepareStatement(
              "select loaded_batch_id, loaded_file_id, beneficiaries, created from ccw.loaded_batches where loaded_file_id = ?");
      stm.setLong(1, loadedFileId);
      // Turn use of the cursor on.
      stm.setFetchSize(100_000);

      final var rs = stm.executeQuery();

      // enable parallelism only if it's worth the overhead
      final var isParallel = batchCount > 100_000;
      // turn iterator into a stream
      return StreamSupport.stream(
          new LoadedBatchSpliterator(batchCount, rs, conn, stm, loadedFileId), isParallel);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private static class LoadedBatchSpliterator extends AbstractSpliterator<LoadedBatch> {
    private final ResultSet rs;
    private final Connection conn;
    private final PreparedStatement stm;
    private final long loadedFileId;

    public LoadedBatchSpliterator(
        int batchCount, ResultSet rs, Connection conn, PreparedStatement stm, long loadedFileId) {
      super(batchCount, Spliterator.ORDERED);
      this.rs = rs;
      this.conn = conn;
      this.stm = stm;
      this.loadedFileId = loadedFileId;
    }

    @Override
    public boolean tryAdvance(Consumer<? super LoadedBatch> action) {
      try {
        if (!rs.next()) {
          conn.close();
          stm.close();
          rs.close();
          LOGGER.debug(
              "Closed connection, statement, and result set for loading batches from {}",
              loadedFileId);

          return false;
        }

        action.accept(
            new LoadedBatch(
                rs.getLong("loaded_batch_id"),
                rs.getLong("loaded_file_id"),
                rs.getString("beneficiaries"),
                rs.getObject("created", OffsetDateTime.class).toInstant()));
        return true;
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
