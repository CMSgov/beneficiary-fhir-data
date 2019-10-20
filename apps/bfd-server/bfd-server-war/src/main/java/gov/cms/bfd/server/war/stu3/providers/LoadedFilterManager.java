package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.rest.param.DateRangeParam;
import gov.cms.bfd.model.rif.LoadedBatch;
import gov.cms.bfd.model.rif.LoadedFile;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
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

  // The connection to the DB
  private EntityManager entityManager;

  // The filter set
  private List<LoadedFileFilter> filters;

  // Estimate of the time it takes for a write to the DB to replicate to have a read.
  private int replicaDelay;

  // The limit of the time interval that the this filter set can know about
  private Date knownUpperBound;
  private Date knownLowerBound;

  // The loadBatchIds in the filters
  private long minBatchId;
  private long maxBatchId;

  /**
   * A tuple of values: LoadedFile.loadedFileid, LoadedFile.created, max(LoadedBatch.created). Used
   * for an optimized query that includes only what is needed to refresh filters
   */
  public static class LoadedTuple {
    private long loadedFileId;
    private Date firstUpdated;
    private Date lastUpdated;

    public LoadedTuple() {}

    public LoadedTuple(long loadedFileId, Date firstUpdated, Date lastUpdated) {
      this.loadedFileId = loadedFileId;
      this.firstUpdated = firstUpdated;
      this.lastUpdated = lastUpdated;
    }

    public long getLoadedFileId() {
      return loadedFileId;
    }

    public Date getFirstUpdated() {
      return firstUpdated;
    }

    public Date getLastUpdated() {
      return lastUpdated;
    }
  }

  /** Create a manager for {@link LoadedFileFilter}s. */
  public LoadedFilterManager() {
    this.replicaDelay = 5; // Default estimate
    this.knownUpperBound = new Date();
    this.knownLowerBound = this.knownUpperBound;
    this.filters = new ArrayList<>();
    this.minBatchId = 0;
    this.maxBatchId = 0;
  }

  /**
   * Create a manager for {@link LoadedFileFilter}s.
   *
   * @param replicaDelay is an estimate of the time it takes for pipeline write to propogate.
   */
  public LoadedFilterManager(int replicaDelay) {
    this();
    this.replicaDelay = replicaDelay;
  }

  /** @return the list of current filters. Newest first. */
  public List<LoadedFileFilter> getFilters() {
    return filters;
  }

  /**
   * An estimate of the delay between master and replica DB.
   *
   * @return replication delay estimate
   */
  public int getReplicaDelay() {
    return replicaDelay;
  }

  /**
   * The upper bound of the interval that the manager has information about
   *
   * @return the upper bound of the known interval
   */
  public Date getKnownUpperBound() {
    return knownUpperBound;
  }

  /**
   * The lower bound of the interval that the manager has information about
   *
   * @return the lower bound of the known interval
   */
  public Date getKnownLowerBound() {
    return knownLowerBound;
  }

  /**
   * The minimum batchId currently in the filter set
   *
   * @return the min
   */
  public long getMinBatchId() {
    return minBatchId;
  }

  /**
   * The maximum batchId currently in the filter set
   *
   * @return the max
   */
  public long getMaxBatchId() {
    return maxBatchId;
  }

  /**
   * Setup the JPA entityManager for the database to query
   *
   * @param entityManager to use
   */
  @PersistenceContext
  void setEntityManager(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  /**
   * Is the result set going to be empty for this beneficiary and time period? Will return false
   * negatives, never false positives.
   *
   * @param beneficiaryId to test
   * @param lastUpdatedRange to test
   * @return true if the results set is empty. false if the result set *may* contain items.
   */
  public synchronized boolean isResultSetEmpty(
      String beneficiaryId, DateRangeParam lastUpdatedRange) {
    if (beneficiaryId == null || beneficiaryId.isEmpty()) throw new IllegalArgumentException();
    if (!isInKnownBounds(lastUpdatedRange)) {
      // Out of bounds has to be treated as unknown result
      return false;
    }

    // Within the known interval that search for matching filters
    for (LoadedFileFilter filter : filters) {
      if (filter.matchesDateRange(lastUpdatedRange)) {
        if (filter.mightContain(beneficiaryId)) {
          return false;
        }
      } else if (filter.getLastUpdated().getTime()
          < lastUpdatedRange.getLowerBoundAsInstant().getTime()) {
        // filters are sorted in descending by lastUpdated time, so we can exit early from this
        // loop
        return true;
      }
    }
    return true;
  }

  /**
   * Test the passed in range against the upper and lower bounds of the filter set.
   *
   * @param range to test against
   * @return true iff the range is within the bounds of the filters
   */
  public synchronized boolean isInKnownBounds(DateRangeParam range) {
    if (range == null || knownUpperBound == null || getFilters().size() == 0) return false;

    // The manager has "known" interval which it has information about.
    final Date upperBound = range.getUpperBoundAsInstant();
    if (upperBound == null || upperBound.getTime() > knownUpperBound.getTime()) return false;
    final Date lowerBound = range.getLowerBoundAsInstant();
    return lowerBound != null && lowerBound.getTime() >= knownLowerBound.getTime();
  }

  /** Called periodically to build and refresh the filters list from the entityManager. */
  @Scheduled(fixedDelay = 10000, initialDelay = 2000)
  public synchronized void refreshFilters() {
    /*
     * Dev note: the pipeline has a process to trim the files list. Nevertheless, building a set of
     * bloom filters may take a while. This method is expected to be called on it's own thread, so
     * this filter building process can happen without interfering with serving. Also, this refresh
     * time will be proportional to the number of files which have been loaded in the past refresh
     * period. If no files have been loaded, this refresh should take about a millisecond.
     */
    try {
      // If new batches are present, then build new filters for the affected files
      final long updatedMaxBatchId = getMaxLoadedBatchId();
      if (updatedMaxBatchId != this.maxBatchId) {
        // Fetch the files that have been updated
        List<LoadedTuple> loadedTuples = fetchLoadedTuples(maxBatchId);

        // Update the filter list
        this.filters = updateFilters(this.filters, loadedTuples, this::fetchLoadedBatches);

        /*
         * Dev note: knownUpperBound should be calculated on the pipeline's clock as other dates,
         * since there is the possibility of clock skew and db replication delay between the
         * pipeline and the data server. However, this code is running on the data server, so we
         * have to estimate the refresh time. To do so, subtract a few seconds a safety margin for
         * the possibility of these effects. A wrong delay estimate will never affect the
         * correctness of the BFD's results. The replicaDelay estimate can be set to 0 for
         * testing.
         */
        final Date refreshTime = Date.from(Instant.now().minusSeconds(replicaDelay));
        this.knownUpperBound = calcUpperBound(loadedTuples, refreshTime);
        this.maxBatchId = updatedMaxBatchId;
        LOGGER.info("Refreshed LoadedFile filters till batchId {}", this.maxBatchId);
      }

      // If batches been trimmed, then remove filters which are no longer present
      final long updatedMinBatchId = getMinLoadedBatchId();
      if (updatedMinBatchId != this.minBatchId) {
        List<LoadedFile> loadedFiles = fetchLoadedFiles();
        this.filters = trimFilters(this.filters, loadedFiles);
        this.knownLowerBound = calcLowerBound(loadedFiles, this.knownLowerBound);
        this.minBatchId = updatedMinBatchId;
        LOGGER.info("Trimmed LoadedFile filters before batchId {}", this.minBatchId);
      }
    } catch (Exception ex) {
      LOGGER.error("Error found refreshing LoadedFile filters", ex);
    }
  }

  /**
   * Set the current state. Used in tests.
   *
   * @param filters to use
   * @param knownLowerBound to use
   * @param knownUpperBound to use
   * @param minBatchId to use
   * @param maxBatchId to use
   */
  public void set(
      List<LoadedFileFilter> filters,
      Date knownLowerBound,
      Date knownUpperBound,
      long minBatchId,
      long maxBatchId) {
    this.filters = filters;
    this.knownLowerBound = knownLowerBound;
    this.knownUpperBound = knownUpperBound;
    this.minBatchId = minBatchId;
    this.maxBatchId = maxBatchId;
  }

  /** @return a info about the filter manager state */
  @Override
  public String toString() {
    return "LoadedFilterManager [filters.size="
        + (filters != null ? String.valueOf(filters.size()) : "null")
        + ", knownLowerBound="
        + knownLowerBound
        + ", knownUpperBound="
        + knownUpperBound
        + ", replicaDelay="
        + replicaDelay
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
   * Build a filter for this loaded file. Should be called in a synchronized context.
   *
   * @param fileId to build a filter for
   * @param firstUpdated time stamp
   * @param fetchById a function which returns a list of batches
   * @return a new filter
   */
  public static LoadedFileFilter buildFilter(
      long fileId, Date firstUpdated, Function<Long, List<LoadedBatch>> fetchById) {
    final List<LoadedBatch> loadedBatches = fetchById.apply(fileId);
    final int batchCount = loadedBatches.size();
    final int batchSize = loadedBatches.get(0).getBeneficiaries().size();
    if (batchCount == 0) {
      throw new IllegalArgumentException("Batches cannot be empty for a filter");
    }
    // It is important to get a good estimate of the number of entries for
    // an accurate FFP and minimal memory size. This one assumes that all batches are of equal size.
    final BloomFilter bloomFilter = LoadedFileFilter.createFilter(batchSize * batchCount);

    // Loop through all batches, filling the bloom filter and finding the lastUpdated
    Date lastUpdated = firstUpdated;
    for (LoadedBatch batch : loadedBatches) {
      for (String beneficiary : batch.getBeneficiaries()) {
        bloomFilter.putString(beneficiary);
      }
      if (batch.getCreated().after(lastUpdated)) {
        lastUpdated = batch.getCreated();
      }
    }

    LOGGER.info("Built a filter for {} with {} batches", fileId, loadedBatches.size());
    return new LoadedFileFilter(fileId, batchCount, firstUpdated, lastUpdated, bloomFilter);
  }

  /**
   * Calculate the upper bound based on unfinished loaded files and the passed upper bound
   *
   * @param loadedTuples from the database
   * @param refreshTime the current time when the fileRows was fetched
   * @return calculated upper bound date
   */
  public static Date calcUpperBound(List<LoadedTuple> loadedTuples, Date refreshTime) {
    Optional<Date> maxLastUpdated =
        loadedTuples.stream().map(LoadedTuple::getLastUpdated).max(Date::compareTo);
    return maxLastUpdated.filter(d -> d.after(refreshTime)).orElse(refreshTime);
  }

  /**
   * Calculate the lower bound based on the loaded file information
   *
   * @param loadedFiles Tuples of loadedFileId, LoadedFile.created, max(LoadedBatch.created)
   * @param lowerBound is bound to use if no rows are present
   * @return new calculated bound or lowerBound for empty loadedFiles
   */
  public static Date calcLowerBound(List<LoadedFile> loadedFiles, Date lowerBound) {
    Optional<LoadedFile> min =
        loadedFiles.stream().min(Comparator.comparing(LoadedFile::getCreated));
    return min.map(LoadedFile::getCreated).orElse(lowerBound);
  }

  /* DB Operations */

  /**
   * Return the max loaded batch id
   *
   * @return the max loaded batch id
   */
  private long getMaxLoadedBatchId() {
    Long maxBatchId =
        entityManager
            .createQuery("select max(b.loadedBatchId) from LoadedBatch b", Long.class)
            .getSingleResult();
    return Optional.ofNullable(maxBatchId).orElse(0L);
  }

  /**
   * Return the min loaded batch id
   *
   * @return the min id
   */
  private long getMinLoadedBatchId() {
    Long minBatchId =
        entityManager
            .createQuery("select min(b.loadedBatchId) from LoadedBatch b", Long.class)
            .getSingleResult();
    return Optional.ofNullable(minBatchId).orElse(0L);
  }

  /**
   * Fetch the tuple of (loadedFileId, LoadedFile.created, max(LoadedBatch.created))
   *
   * @param afterBatchId limits the query to include batches after this loadedBatchId
   * @return tuples that meet the afterBatchId criteria or an empty list
   */
  private List<LoadedTuple> fetchLoadedTuples(long afterBatchId) {
    final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    final CriteriaQuery<LoadedTuple> query = cb.createQuery(LoadedTuple.class);
    final Root<LoadedFile> f = query.from(LoadedFile.class);
    Join<LoadedFile, LoadedBatch> b = f.join("batches");
    query
        .select(
            cb.construct(
                LoadedTuple.class,
                f.get("loadedFileId"),
                f.get("created"),
                cb.max(b.get("created"))))
        .where(cb.gt(b.get("loadedBatchId"), afterBatchId))
        .groupBy(f.get("loadedFileId"), f.get("created"))
        .orderBy(cb.desc(f.get("created")));
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
