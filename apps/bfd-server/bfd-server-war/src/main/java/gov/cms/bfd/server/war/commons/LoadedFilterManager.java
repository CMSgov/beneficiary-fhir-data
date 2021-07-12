package gov.cms.bfd.server.war.commons;

import ca.uhn.fhir.rest.param.DateRangeParam;
import gov.cms.bfd.model.rif.LoadedBatch;
import gov.cms.bfd.model.rif.LoadedFile;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import org.apache.spark.util.sketch.BloomFilter;
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

  // A date before the lastUpdate feature was rolled out
  private static final Instant BEFORE_LAST_UPDATED_FEATURE = Instant.parse("2020-01-01T00:00:00Z");

  // The size of the beneficiaryId column
  private static final int BENE_ID_SIZE = 15;

  // The connection to the DB
  private EntityManager entityManager;

  // The filter set
  private List<LoadedFileFilter> filters;

  // The latest transaction time from the LoadedBatch files
  private Instant transactionTime;

  // The last LoadedBatch.created in the filter set
  private Instant lastBatchCreated;

  // The first LoadedBatch.created in the filter set
  private Instant firstBatchCreated;

  /**
   * A tuple of values: LoadedFile.loadedFileid, LoadedFile.created, max(LoadedBatch.created). Used
   * for an optimized query that includes only what is needed to refresh filters
   */
  public static class LoadedTuple {
    private long loadedFileId;
    private Instant firstUpdated;
    private Instant lastUpdated;

    public LoadedTuple(long loadedFileId, Instant firstUpdated, Instant lastUpdated) {
      this.loadedFileId = loadedFileId;
      this.firstUpdated = firstUpdated;
      this.lastUpdated = lastUpdated;
    }

    public long getLoadedFileId() {
      return loadedFileId;
    }

    public Instant getFirstUpdated() {
      return firstUpdated;
    }

    public Instant getLastUpdated() {
      return lastUpdated;
    }
  }

  /** Create a manager for {@link LoadedFileFilter}s. */
  public LoadedFilterManager() {
    this.filters = new ArrayList<>();
  }

  /** @return the list of current filters. Newest first. */
  public List<LoadedFileFilter> getFilters() {
    return filters;
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
   * The return the first batch that the filter manager knows about
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
   * The return the first batch that the filter manager knows about
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
   * Setup the JPA entityManager for the database to query
   *
   * @param entityManager to use
   */
  @PersistenceContext
  public void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /** Called to finish initialization of the manager */
  @PostConstruct
  public synchronized void init() {
    // The transaction time will either the last LoadedBatch or some earlier time
    transactionTime = fetchLastLoadedBatchCreated().orElse(BEFORE_LAST_UPDATED_FEATURE);
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
      String beneficiaryId, DateRangeParam lastUpdatedRange) {
    if (beneficiaryId == null || beneficiaryId.isEmpty()) throw new IllegalArgumentException();

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
      } else if (filter.getLastUpdated().toEpochMilli()
          < lastUpdatedRange.getLowerBoundAsInstant().toInstant().toEpochMilli()) {
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
      // If new batches are present, then build new filters for the affected files
      final Instant currentLastBatchCreated =
          fetchLastLoadedBatchCreated().orElse(BEFORE_LAST_UPDATED_FEATURE);

      if (this.lastBatchCreated == null
          || this.lastBatchCreated.isBefore(currentLastBatchCreated)) {
        LOGGER.info(
            "Refreshing LoadedFile filters with new filters from {} to {}",
            lastBatchCreated,
            currentLastBatchCreated);

        List<LoadedTuple> loadedTuples = fetchLoadedTuples(this.lastBatchCreated);
        List<LoadedFileFilter> newFilters =
            updateFilters(this.filters, loadedTuples, this::fetchLoadedBatches);

        // If batches been trimmed, then remove filters which are no longer present
        final Instant currentFirstBatchUpdate =
            fetchFirstLoadedBatchCreated().orElse(BEFORE_LAST_UPDATED_FEATURE);

        if (this.firstBatchCreated == null
            || this.firstBatchCreated.isBefore(currentFirstBatchUpdate)) {
          LOGGER.info("Trimmed LoadedFile filters before {}", currentFirstBatchUpdate);
          List<LoadedFile> loadedFiles = fetchLoadedFiles();
          newFilters = trimFilters(newFilters, loadedFiles);
        }

        LOGGER.info(
            "Updating timestamps. currentFirstBatchUpdate={} currentLastBatchCreated={}",
            currentFirstBatchUpdate,
            currentLastBatchCreated);

        set(newFilters, currentFirstBatchUpdate, currentLastBatchCreated);
      }
    } catch (Throwable ex) {
      LOGGER.error("Error found refreshing LoadedFile filters", ex);
    }
  }

  /**
   * Set the current state in consistent fashion
   *
   * @param filters to use
   * @param firstBatchCreated to use
   * @param lastBatchCreated to use
   */
  public synchronized void set(
      List<LoadedFileFilter> filters, Instant firstBatchCreated, Instant lastBatchCreated) {
    this.filters = filters;
    this.firstBatchCreated = firstBatchCreated;
    this.lastBatchCreated = lastBatchCreated;
    this.transactionTime = lastBatchCreated;
  }

  /** @return a info about the filter manager state */
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
   * Create an updated {@link LoadedFileFilter} list from existing filters and newly loaded files
   * and batches
   *
   * @param existingFilters that should be included
   * @param loadedTuples that come from new LoadedBatch
   * @param fetchById to use retrieve list of LoadedBatch by id
   * @return a new filter list
   */
  public static List<LoadedFileFilter> updateFilters(
      List<LoadedFileFilter> existingFilters,
      List<LoadedTuple> loadedTuples,
      Function<Long, List<LoadedBatch>> fetchById) {
    List<LoadedFileFilter> result = new ArrayList<>(existingFilters);
    List<LoadedFileFilter> newFilters = buildFilters(loadedTuples, fetchById);
    newFilters.forEach(
        filter -> {
          result.removeIf(f -> f.getLoadedFileId() == filter.getLoadedFileId());
          result.add(filter);
        });
    result.sort((a, b) -> b.getFirstUpdated().compareTo(a.getFirstUpdated())); // Descending
    return result;
  }

  /**
   * Build a new {@link LoadedFileFilter} list
   *
   * @param loadedTuples that come from new LoadedBatch
   * @param fetchById to use retrieve list of LoadedBatch by id
   * @return a new filter list
   */
  public static List<LoadedFileFilter> buildFilters(
      List<LoadedTuple> loadedTuples, Function<Long, List<LoadedBatch>> fetchById) {
    return loadedTuples.stream()
        .map(t -> buildFilter(t.getLoadedFileId(), t.getFirstUpdated(), fetchById))
        .collect(Collectors.toList());
  }

  /**
   * Trim filters to match current {@link LoadedFile} list. Only deletes filters.
   *
   * @param existingFilters to reuse if possible
   * @param loadedFiles list of current loaded files
   * @return a new filter list
   */
  public static List<LoadedFileFilter> trimFilters(
      List<LoadedFileFilter> existingFilters, List<LoadedFile> loadedFiles) {
    List<LoadedFileFilter> newFilters = new ArrayList<>(existingFilters);
    newFilters.removeIf(
        filter ->
            loadedFiles.stream().noneMatch(f -> f.getLoadedFileId() == filter.getLoadedFileId()));
    return newFilters;
  }

  /**
   * Build a filter for this loaded file. Should be a pure function.
   *
   * @param fileId to build a filter for
   * @param firstUpdated time stamp
   * @param fetchById a function which returns a list of batches
   * @return a new filter
   */
  public static LoadedFileFilter buildFilter(
      long fileId, Instant firstUpdated, Function<Long, List<LoadedBatch>> fetchById) {
    final List<LoadedBatch> loadedBatches = fetchById.apply(fileId);
    final int batchCount = loadedBatches.size();
    if (batchCount == 0) {
      throw new IllegalArgumentException("Batches cannot be empty for a filter");
    }
    final int batchSize =
        (loadedBatches.get(0).getBeneficiaries().length() + BENE_ID_SIZE) / BENE_ID_SIZE;

    // It is important to get a good estimate of the number of entries for
    // an accurate FFP and minimal memory size. This one assumes that all batches are of equal size.
    final BloomFilter bloomFilter = LoadedFileFilter.createFilter(batchSize * batchCount);

    // Loop through all batches, filling the bloom filter and finding the lastUpdated
    Instant lastUpdated = firstUpdated;
    for (LoadedBatch batch : loadedBatches) {
      for (String beneficiary : batch.getBeneficiariesAsList()) {
        bloomFilter.putString(beneficiary);
      }
      if (batch.getCreated().isAfter(lastUpdated)) {
        lastUpdated = batch.getCreated();
      }
    }

    LOGGER.info("Built a filter for {} with {} batches", fileId, loadedBatches.size());
    return new LoadedFileFilter(fileId, batchCount, firstUpdated, lastUpdated, bloomFilter);
  }

  /* DB Operations */

  /**
   * Return the max date from the LoadedBatch table. If no batches are present, then the schema
   * migration time which will be a timestamp before the first loaded batch
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
   * Return the min date from the LoadedBatch table
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
   * Fetch the tuple of (loadedFileId, LoadedFile.created, max(LoadedBatch.created))
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
   * Fetch all the batches associated with LoadedFile.
   *
   * @param loadedFileId of the LoadedFile
   * @return a list of LoadedBatches or an empty list
   */
  private List<LoadedBatch> fetchLoadedBatches(long loadedFileId) {
    return entityManager
        .createQuery(
            "select b from LoadedBatch b where b.loadedFileId = :loadedFileId", LoadedBatch.class)
        .setParameter("loadedFileId", loadedFileId)
        .getResultList();
  }
}
