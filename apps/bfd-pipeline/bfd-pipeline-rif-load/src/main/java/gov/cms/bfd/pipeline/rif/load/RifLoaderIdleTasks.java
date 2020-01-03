/** */
package gov.cms.bfd.pipeline.rif.load;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import javax.crypto.SecretKeyFactory;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the work that is done during the idle time of a RifLoader. This is a helper
 * class for the RifLoader. It is expected to have a 1-to-1 relationship to a RifLoader.
 */
public class RifLoaderIdleTasks {
  /** Parameters for the post startup tasks. Tuned for throughput. */

  /** Time slice that a task can take before returning/yielding to the main pipeline */
  private static final int TASK_TIME_LIMIT_MILLIS = 9800;

  private static final Duration TASK_TIME_LIMIT = Duration.ofMillis(TASK_TIME_LIMIT_MILLIS);

  /** The record count of a db update batch */
  private static final int BATCH_COUNT = 100;

  /** One thread for each query type */
  private static final int THREAD_COUNT = 2;

  /** JPQL queries */
  private static final String SELECT_UNHASHED_BENFICIARIES =
      "select b from Beneficiary b "
          + "where b.mbiHash is null and b.medicareBeneficiaryId is not null and "
          + "b.medicareBeneficiaryId is not empty";

  /** JPQL query for a count */
  private static final String COUNT_UNHASHED_BENFICIARIES =
      "select count(b) from Beneficiary b "
          + "where b.mbiHash is null and b.medicareBeneficiaryId is not null and "
          + "b.medicareBeneficiaryId is not empty";

  /** JPQL query for a list */
  private static final String SELECT_UNHASHED_HISTORIES =
      "select b from BeneficiaryHistory b "
          + "where b.mbiHash is null and b.medicareBeneficiaryId is not null and "
          + "b.medicareBeneficiaryId is not empty";

  /** JPQL query for a count */
  private static final String COUNT_UNHASHED_HISTORIES =
      "select count(b) from BeneficiaryHistory b "
          + "where b.mbiHash is null and b.medicareBeneficiaryId is not null and "
          + "b.medicareBeneficiaryId is not empty";

  private static final Logger LOGGER = LoggerFactory.getLogger(RifLoaderIdleTasks.class);

  /** Enum to tell what the current task is being executed. */
  public enum Task {
    /** The initial task which is executed after startup */
    INITIAL,

    /** After the initial task, this task is run. */
    POST_STARTUP,

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

    this.executorService = Executors.newFixedThreadPool(THREAD_COUNT);
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
    boolean isTaskDone;
    switch (currentTask) {
      case INITIAL:
        isTaskDone = doInitialTask();
        break;
      case POST_STARTUP:
        isTaskDone = doPostStartupTask();
        break;
      case NORMAL:
        isTaskDone = doNormalTask();
        break;
      default:
        throw new RuntimeException("Unexpected idle task");
    }
    if (isTaskDone) {
      setNextTask();
    }
  }

  /** Set the currentTask to the next task after current task. */
  private void setNextTask() {
    switch (currentTask) {
      case INITIAL:
        currentTask = Task.POST_STARTUP;
        break;

      case POST_STARTUP:
      case NORMAL:
      default:
        currentTask = Task.NORMAL;
        break;
    }
  }

  /**
   * Run this task as the first idle task.
   *
   * @return true if done with this task.
   */
  public boolean doInitialTask() {
    // For the log count the work that we have to do.
    final EntityManager em = entityManagerFactory.createEntityManager();
    final Long beneficiaryCount =
        em.createQuery(COUNT_UNHASHED_BENFICIARIES, Long.class).getSingleResult();
    final Long historyCount =
        em.createQuery(COUNT_UNHASHED_HISTORIES, Long.class).getSingleResult();

    LOGGER.info(
        "Starting idle task processing with null mbiHash for: {} Beneficaries and {} Benficiary Histories",
        beneficiaryCount,
        historyCount);

    return true;
  }

  /**
   * Run this task after the initial task. Respect the TASK_TIME_LIMIT.
   *
   * @return true if done with the current task.
   */
  public boolean doPostStartupTask() {
    final Instant startTime = Instant.now();
    LOGGER.debug("Started a PostStartup time slice");

    // Execute batches in parallel
    List<Future<Boolean>> batchFutures = new ArrayList<Future<Boolean>>();
    batchFutures.add(
        executorService.submit(() -> doTransaction(startTime, this::fixupBeneficiaryBatch)));
    batchFutures.add(
        executorService.submit(() -> doTransaction(startTime, this::fixupHistoryBatch)));

    final boolean isDone = waitUntilDone(batchFutures);
    LOGGER.debug("Finished a PostStartup time slice");
    if (isDone) {
      LOGGER.info("Finished idle startup tasks");
    }
    return isDone;
  }

  /**
   * Do the normal idle task
   *
   * @return true if this task is complete
   */
  public boolean doNormalTask() {
    // Nothing to do normally
    return false;
  }

  /**
   * Wait until all tasks are done or time out
   *
   * @param tasks to wait on
   */
  private boolean waitUntilDone(List<Future<Boolean>> tasks) {
    boolean isDone = true;
    try {
      // Use a 2x longer value than the expected termination.
      for (Future<Boolean> task : tasks) {
        isDone = task.get(2 * TASK_TIME_LIMIT_MILLIS, TimeUnit.MILLISECONDS) && isDone;
      }
    } catch (TimeoutException ex) {
    } catch (ExecutionException ex) {
    } catch (InterruptedException ex) {
    }
    return isDone;
  }

  /**
   * Fixup a batch of Beneficiaries. Update the beneficary metrics.
   *
   * @param em a {@link EntityManager} setup for a transaction
   * @return true if done with all fixups
   */
  public Boolean fixupBeneficiaryBatch(final EntityManager em, final Instant startTime) {
    LOGGER.debug("Start fixing up a Beneficiary batch");
    boolean isDone = true;
    // Use a cursor, measures slightly faster than
    try (ScrollableResults itemCursor =
        em.unwrap(Session.class)
            .createQuery(SELECT_UNHASHED_BENFICIARIES)
            .setFetchSize(BATCH_COUNT)
            .scroll(ScrollMode.SCROLL_INSENSITIVE)) {
      int count = 0;
      while (inPeriod(startTime, TASK_TIME_LIMIT) && itemCursor.next()) {
        final Beneficiary beneficiary = (Beneficiary) itemCursor.get(0);
        beneficiary
            .getMedicareBeneficiaryId()
            .ifPresent(
                mbi -> {
                  final String mbiHash = RifLoader.computeMbiHash(options, secretKeyFactory, mbi);
                  beneficiary.setMbiHash(Optional.of(mbiHash));
                });
        isDone = itemCursor.isLast();
        // Write to the DB in batches
        if (++count % BATCH_COUNT == 0) {
          em.flush();
          em.clear();
        }
      }
      beneficaryMeter.mark(count);
      LOGGER.debug("Finished fixing up a Beneficiary batch: {}", count);
    }
    return isDone;
  }

  /**
   * Fixup a batch of BeneficiaryHistory. Update the history metrics.
   *
   * @param em a {@link EntityManager} setup for a transaction
   * @return true if done with all fixups
   */
  public Boolean fixupHistoryBatch(final EntityManager em, final Instant startTime) {
    LOGGER.debug("Start fixing up a History batch");
    boolean isDone = true;
    try (ScrollableResults itemCursor =
        em.unwrap(Session.class)
            .createQuery(SELECT_UNHASHED_HISTORIES)
            .setFetchSize(BATCH_COUNT)
            .scroll(ScrollMode.SCROLL_INSENSITIVE)) {
      int count = 0;
      while (inPeriod(startTime, TASK_TIME_LIMIT) && itemCursor.next()) {
        final BeneficiaryHistory beneficiary = (BeneficiaryHistory) itemCursor.get(0);
        beneficiary
            .getMedicareBeneficiaryId()
            .ifPresent(
                mbi -> {
                  final String mbiHash = RifLoader.computeMbiHash(options, secretKeyFactory, mbi);
                  beneficiary.setMbiHash(Optional.of(mbiHash));
                });
        isDone = itemCursor.isLast();
        // Write to the DB in batches
        if (++count % BATCH_COUNT == 0) {
          em.flush();
          em.clear();
        }
      }
      historyMeter.mark(count);
      LOGGER.debug("Finished fixing up a History batch: {}", count);
    }
    return isDone;
  }

  /**
   * Any time left in this time slice?
   *
   * @param start of the period
   * @param period duration
   * @return true iff current period is less than the passed in period duration;
   */
  public static boolean inPeriod(final Instant start, final Duration period) {
    Instant nowInstant = Instant.now();
    return start.isBefore(nowInstant) && start.plus(period).isAfter(nowInstant);
  }

  /**
   * Setup a DB transaction and call the executor to do the work.
   *
   * @param startTime context of the work
   * @param executor does the work. Return the value from the executor
   * @return the return value from the executor
   */
  public Boolean doTransaction(
      final Instant startTime, final BiFunction<EntityManager, Instant, Boolean> executor) {
    Objects.requireNonNull(executor);
    try {
      final EntityManager em = entityManagerFactory.createEntityManager();
      EntityTransaction txn = null;
      try {
        txn = em.getTransaction();
        txn.begin();
        final Boolean result = executor.apply(em, startTime);
        txn.commit();
        return result;
      } finally {
        if (em != null && em.isOpen()) {
          if (txn != null && txn.isActive()) {
            txn.rollback();
          }
          em.close();
        }
      }
    } catch (final Exception ex) {
      LOGGER.error("Error while doing a idle task", ex);
      return true;
    }
  }
}
