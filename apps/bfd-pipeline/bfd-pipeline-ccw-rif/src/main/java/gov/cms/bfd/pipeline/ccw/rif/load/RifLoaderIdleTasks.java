package gov.cms.bfd.pipeline.ccw.rif.load;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.crypto.SecretKeyFactory;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the work that is done during the idle time of a RifLoader. This is a helper
 * class for the RifLoader. It is expected to have a 1-to-1 relationship to a RifLoader.
 */
public class RifLoaderIdleTasks {
  /*
   * Developer Notes:
   *
   * This class was created to fixup the mbiHash field, but with idea there may be other
   * maintenance tasks in future.
   *
   * There are about a billion mbiHash fields to fill. So time was spent to come up with
   * a fast method to fixup fields. In the end, batching large number of updates and doing these
   * batches in parallel worked well enough.
   *
   * There is a hierarchy in the names and concepts used in this class.
   *
   *  Task - the overall state and goal of the manager.
   *  Time Slice - A fixed amount of time that the task can execute before it must return to the main thread.
   *    A task can take many time slices to complete.
   *  Executor - Within a time slice, work is given to an executor.
   *    Instead of another interface, a generic Callable interface is used.
   *  Partition - Fixup work is divide among partitions of a table.
   *    Partitions are numbered 0 ... PARTITION_COUNT-1;
   *    Each partition is given its own executor.
   *  Batch - A group of records to update in one DB transaction. All records come from a single partition.
   */

  /** Time slice that a task can take before returning/yielding to the main pipeline */
  private static final int TIME_SLICE_LIMIT_MILLIS = 20000;

  /** Time slice that a task can take before returning/yielding to the main pipeline */
  private static final Duration TIME_SLICE_LIMIT = Duration.ofMillis(TIME_SLICE_LIMIT_MILLIS);

  /** Max amount of time before a timeout occurs. */
  private static final int MAX_EXECUTOR_TIME_SECONDS = 300; // Allow for large table scans

  /** The record count of a db update batch */
  private static final int BATCH_COUNT = 100;

  /** An executor list that does no work and always completes */
  private static final List<Callable<Boolean>> NULL_EXECUTORS = Collections.emptyList();

  private static final Logger LOGGER = LoggerFactory.getLogger(RifLoaderIdleTasks.class);

  /** The number of partitions to run by default. It is an option. */
  public static final int DEFAULT_PARTITION_COUNT = 20;

  /** Enum to tell what the current task is being executed. */
  public enum Task {
    /** The initial task which is executed after startup */
    INITIAL,

    /** After the initial task, this general fixup is run. */
    POST_STARTUP,

    /** A sub-task to fixup Beneficiaries table */
    POST_STARTUP_FIXUP_BENEFICIARIES,

    /** A sub-task to fixup BeneficiariesHistory table */
    POST_STARTUP_FIXUP_BENEFICIARY_HISTORY,

    /** Run the normal task */
    NORMAL,
  }

  /* Hashing entities */
  private final LoadAppOptions options;
  private final EntityManagerFactory entityManagerFactory;
  private final SecretKeyFactory secretKeyFactory;

  /* Metrics */
  private final Meter beneficaryMeter;
  private final Meter historyMeter;

  /* Thread pool for post startup tasks */
  private final ExecutorService executorService;

  /* The task that is going to execute next */
  private Task currentTask = Task.INITIAL;

  /**
   * Create a helper to manage the idle time tasks.
   *
   * @param options pipeline options
   * @param appMetrics pipeline metrics
   * @param entityManagerFactory a connection to the database of the pipeline
   * @param secretKeyFactory for hashing
   */
  public RifLoaderIdleTasks(
      final LoadAppOptions options,
      final MetricRegistry appMetrics,
      final EntityManagerFactory entityManagerFactory,
      final SecretKeyFactory secretKeyFactory) {
    this.options = options;
    this.entityManagerFactory = entityManagerFactory;
    this.secretKeyFactory = secretKeyFactory;

    this.beneficaryMeter = appMetrics.meter("fixups.beneficiary.rate");
    this.historyMeter = appMetrics.meter("fixups.beneficiary_history.rate");

    this.executorService = Executors.newFixedThreadPool(options.getFixupThreads());
  }

  /**
   * What task will be executed in the next idle time slot
   *
   * @return the current task
   */
  public Task getCurrentTask() {
    return currentTask;
  }

  /**
   * Run the current idle task. This method is expected to be called whenever no RIF files are
   * present for process. It will respect the TASK_TIME_LIMIT to allow checking of RIF files and to
   * not interfer with RIF file processing.
   */
  public void doIdleTask() {
    List<Callable<Boolean>> executors = getTaskExecutors(currentTask);
    boolean isTaskDone = doExecutors(executors);
    if (isTaskDone) {
      currentTask = getNextTask();
    }
  }

  /**
   * Form a list of callables to execute.
   *
   * @return the callables associated with the current task
   */
  private List<Callable<Boolean>> getTaskExecutors(Task currentTask) {
    switch (currentTask) {
      case INITIAL:
        return Collections.singletonList(this::doInitialTask);
      case POST_STARTUP:
        return NULL_EXECUTORS;
      case POST_STARTUP_FIXUP_BENEFICIARIES:
        return makeExecutorsForPartitions(this::fixupBeneficiaryExecutor);
      case POST_STARTUP_FIXUP_BENEFICIARY_HISTORY:
        return makeExecutorsForPartitions(this::fixupHistoryExecutor);
      case NORMAL:
        return NULL_EXECUTORS;
      default:
        throw new RuntimeException("Unexpected idle task state");
    }
  }

  /**
   * Form a list of executors, one for each partition
   *
   * @param executor for a single partition
   * @return the list of executors
   */
  private List<Callable<Boolean>> makeExecutorsForPartitions(Function<Integer, Boolean> executor) {
    return IntStream.range(0, options.getFixupThreads())
        .mapToObj((partition) -> (Callable<Boolean>) () -> executor.apply(partition))
        .collect(Collectors.toList());
  }

  /**
   * Execute the work in parallel threads. Wait for all executors to complete.
   *
   * @param executors Callables to that do the work
   */
  private boolean doExecutors(List<Callable<Boolean>> executors) {
    if (executors.size() == 0) return true;
    final Instant startTime = Instant.now();
    LOGGER.debug("Started a time slice: {}", currentTask);

    List<Future<Boolean>> futures =
        executors.stream().map(executorService::submit).collect(Collectors.toList());

    final boolean isTaskDone =
        futures.stream()
            .map(
                future -> {
                  try {
                    return future.get(MAX_EXECUTOR_TIME_SECONDS, TimeUnit.SECONDS);
                  } catch (TimeoutException | ExecutionException | InterruptedException ex) {
                    LOGGER.error("Error executing in sub-task {}", getCurrentTask());
                    LOGGER.error("Exception executing a task", ex);
                    return false;
                  }
                })
            .reduce((a, b) -> a && b)
            .orElse(true);

    LOGGER.debug(
        "Finished a time slice: {}, {} millis",
        currentTask,
        startTime.until(Instant.now(), ChronoUnit.MILLIS));
    if (isTaskDone) {
      LOGGER.info("Finished idle task: {}", currentTask);
    }
    return isTaskDone;
  }

  /** Return the next task given the current task. */
  private Task getNextTask() {
    switch (currentTask) {
      case INITIAL:
        return Task.POST_STARTUP;
      case POST_STARTUP:
        if (options.isFixupsEnabled()) {
          return Task.POST_STARTUP_FIXUP_BENEFICIARIES;
        } else {
          LOGGER.info("PostStartup fixups are not enabled.");
          return Task.NORMAL;
        }
      case POST_STARTUP_FIXUP_BENEFICIARIES:
        return Task.POST_STARTUP_FIXUP_BENEFICIARY_HISTORY;
      case POST_STARTUP_FIXUP_BENEFICIARY_HISTORY:
        return Task.NORMAL;
      case NORMAL:
      default:
        return Task.NORMAL;
    }
  }

  /**
   * Run this task as the first idle task.
   *
   * @return true if done with this task.
   */
  public boolean doInitialTask() {
    if (!options.isFixupsEnabled()) return true;

    final EntityManager em = entityManagerFactory.createEntityManager();

    final Long beneficiaryCount =
        em.createQuery(
                "select count(*) from Beneficiary where mbiHash is null and medicareBeneficiaryId is not null",
                Long.class)
            .getSingleResult();

    final Long historyCount =
        em.createQuery(
                "select count(*) from BeneficiaryHistory where mbiHash is null and medicareBeneficiaryId is not null",
                Long.class)
            .getSingleResult();

    LOGGER.info(
        "Missing mbiHash for: {} Beneficaries and {} Benficiary Histories",
        beneficiaryCount,
        historyCount);

    return true;
  }

  /**
   * Executor for the Beneficiaries table.
   *
   * @param partition to work on
   * @return true if done with the work on this partition
   */
  public Boolean fixupBeneficiaryExecutor(final Integer partition) {
    LOGGER.debug("Start a Beneficiary executor: partition {}", partition);
    final AtomicInteger counter = new AtomicInteger(0);
    final Boolean isDone =
        doBatches(
            session ->
                fixupBatch(session, "Beneficiaries", "beneficiaryId", true, partition, counter));
    beneficaryMeter.mark(counter.get());
    LOGGER.debug("Finished a Beneficiary executor: {}, count {}", partition, counter.get());
    return isDone;
  }

  /**
   * Executor for the BeneficiariesHistory table.
   *
   * @param partition to work on
   * @return true if done with the work on this partition
   */
  public Boolean fixupHistoryExecutor(final Integer partition) {
    LOGGER.debug("Start a BeneficiariesHistory partition {}", partition);
    final AtomicInteger counter = new AtomicInteger(0);
    final Boolean isDone =
        doBatches(
            session ->
                fixupBatch(
                    session,
                    "BeneficiariesHistory",
                    "beneficiaryHistoryId",
                    false,
                    partition,
                    counter));
    historyMeter.mark(counter.get());
    LOGGER.debug("Finished a History executor: {}, count {}", partition, counter.get());
    return isDone;
  }

  /**
   * Break up the work into a series of batches of record to update. Each batch is done in a
   * transaction. After each batch, check the amount of time taken. Return after the
   * TIME_SLICE_LIMIT is reached or the work is done.
   *
   * @param batchWorker does the work. Return the isDone value from the worker.
   * @return the return value from the last batchWorker.
   */
  public boolean doBatches(final Function<StatelessSession, Boolean> batchWorker) {
    // Use the stateless sessions to avoid the overhead of Hibernates caches which are not needed in
    // this bulk update use-case
    final Instant startTime = Instant.now();
    final SessionFactory sf = entityManagerFactory.unwrap(SessionFactory.class);
    final StatelessSession statelessSession = sf.openStatelessSession();
    Transaction txn = null;
    try {
      boolean isDone = false;
      while (!isDone && inPeriod(startTime, TIME_SLICE_LIMIT)) {
        txn = statelessSession.beginTransaction();
        isDone = batchWorker.apply(statelessSession);
        txn.commit();
      }
      return isDone;
    } finally {
      if (statelessSession.isOpen()) {
        if (txn != null && txn.isActive()) {
          txn.rollback();
        }
        statelessSession.close();
      }
    }
  }

  /**
   * Fixup a batch of records. Executed in the context of a transaction.
   *
   * @param session to use
   * @param tableName to fetch from
   * @param idName of the table
   * @param hasTextId is true if the id is a varchar, false if the id is a bigint
   * @param partition to fetch from
   * @param counter to increment with the record count
   * @return the number of records fixed up;
   */
  private boolean fixupBatch(
      final StatelessSession session,
      final String tableName,
      final String idName,
      final boolean hasTextId,
      final int partition,
      final AtomicInteger counter) {
    List<Object[]> rows = fetchBatchRows(session, tableName, idName, hasTextId, partition);
    if (rows.size() == 0) return true;
    updateBatchMbiHash(session, rows, tableName, idName, hasTextId);
    counter.addAndGet(rows.size());
    return false;
  }

  /**
   * Fetch the rows of a batch. Each row contains the tableId and a medicareBeneficiaryId.
   *
   * @param session to use
   * @param tableName to fetch from
   * @param idName of the table
   * @param hasTextId is true if the id is a varchar, false if the id is a bigint
   * @param partition to fetch from
   * @return a list of rows
   */
  @SuppressWarnings("unchecked")
  private List<Object[]> fetchBatchRows(
      final StatelessSession session,
      final String tableName,
      final String idName,
      final boolean hasTextId,
      final int partition) {
    final String select =
        "SELECT b.\""
            + idName
            + "\", b.\"medicareBeneficiaryId\" "
            + "FROM \""
            + tableName
            + "\" b "
            + "WHERE b.\"mbiHash\" IS NULL AND b.\"medicareBeneficiaryId\" IS NOT NULL AND "
            + (hasTextId
                ? "MOD(CAST(b.\""
                    + idName
                    + "\" AS numeric), "
                    + options.getFixupThreads()
                    + ") = "
                    + partition
                : "MOD(b.\"" + idName + "\", " + options.getFixupThreads() + ") = " + partition);

    return session.createNativeQuery(select).setMaxResults(BATCH_COUNT).getResultList();
  }

  /**
   * Update the mbiHash field of the batch.
   *
   * @param session to use
   * @param rows rows of id and medicareBeneficiaryId tuples
   * @param tableName to update
   * @param idName of the table
   * @param hasTextId is true if the id is a varchar, false if the id is a bigint
   */
  private void updateBatchMbiHash(
      final StatelessSession session,
      final List<Object[]> rows,
      String tableName,
      String idName,
      boolean hasTextId) {
    final int rowSize = 100; // 64 for hash and 14 for MBI
    StringBuilder update = new StringBuilder(rows.size() * rowSize + rowSize);

    /*
     * Developer Note:
     * VALUES is a allows single statement update statement to update mulitple values.
     * It's a common operator, but does not work on HSQLDB.
     */
    update
        .append("UPDATE \"")
        .append(tableName)
        .append("\" b SET \"mbiHash\" = mbi_hash FROM (VALUES ");
    for (int i = 0; i < rows.size(); i++) {
      Object[] row = rows.get(i);
      String mbi = (String) row[1];
      String mbiHash = RifLoader.computeMbiHash(options, secretKeyFactory, mbi);
      if (i > 0) update.append(",");
      update
          .append(hasTextId ? "('" : "(")
          .append(row[0].toString())
          .append(hasTextId ? "','" : ",'")
          .append(mbiHash)
          .append("')");
    }
    update.append(") AS t(id, mbi_hash) WHERE b.\"").append(idName).append("\" = id");

    session.createNativeQuery(update.toString()).executeUpdate();
  }

  /**
   * Any time left in this time slice?
   *
   * @param start of the period
   * @param period duration
   * @return true iff current period is less than the passed in period duration;
   */
  private static boolean inPeriod(final Instant start, final Duration period) {
    Instant nowInstant = Instant.now();
    return start.compareTo(nowInstant) <= 0 && start.plus(period).isAfter(nowInstant);
  }
}
