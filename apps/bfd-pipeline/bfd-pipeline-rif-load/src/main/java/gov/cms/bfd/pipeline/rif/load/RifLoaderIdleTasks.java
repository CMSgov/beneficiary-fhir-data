/** */
package gov.cms.bfd.pipeline.rif.load;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.crypto.SecretKeyFactory;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the work that is done during the idle time of RifLoader. This is a helper
 * class for the RifLoader.
 */
public class RifLoaderIdleTasks {
  /** Statics */
  private static final Duration TASK_TIME_LIMIT = Duration.ofMillis(9800);

  private static final int BATCH_COUNT = 1000;

  private static final String SELECT_UNHASHED_BENFICIARIES =
      "select b from Beneficiary b "
          + "where b.mbiHash is null and "
          + "b.medicareBeneficiaryId is not null and "
          + "b.medicareBeneficiaryId is not empty";
  private static final String COUNT_UNHASHED_BENFICIARIES =
      "select count(b) from Beneficiary b "
          + "where b.mbiHash is null and "
          + "b.medicareBeneficiaryId is not null and "
          + "b.medicareBeneficiaryId is not empty";
  private static final String SELECT_UNHASHED_HISTORIES =
      "select b from BeneficiaryHistory b "
          + "where b.mbiHash is null and "
          + "b.medicareBeneficiaryId is not null and "
          + "b.medicareBeneficiaryId is not empty";
  private static final String COUNT_UNHASHED_HISTORIES =
      "select count(b) from BeneficiaryHistory b "
          + "where b.mbiHash is null and "
          + "b.medicareBeneficiaryId is not null and "
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

  /* Hashing entities*/
  private final LoadAppOptions options;
  private final EntityManagerFactory entityManagerFactory;
  private final SecretKeyFactory secretKeyFactory;

  /* Metrics */
  private final Meter beneficaryMeter;
  private final Meter historyMeter;
  private final Counter beneficiaryCounter;
  private final Counter historyCounter;

  /* The tasks that is going to execute next */
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

    this.beneficaryMeter = appMetrics.meter("Beneficiary_fixup_rate");
    this.historyMeter = appMetrics.meter("BeneficiaryHistory_fixup_rate");
    this.beneficiaryCounter = appMetrics.counter("Beneficiary_fixups_remaining");
    this.historyCounter = appMetrics.counter("BeneficiaryHistory_fixups_remaining");
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
    switch (currentTask) {
      case INITIAL:
        if (doInitialTask()) {
          currentTask = Task.POST_STARTUP;
        }
        break;
      case POST_STARTUP:
        if (doPostStartupTask()) {
          currentTask = Task.NORMAL;
        }
        break;
      case NORMAL:
        if (doNormalTask()) {
          currentTask = Task.NORMAL;
        }
        break;
      default:
        throw new RuntimeException("Unexcpected idle task");
    }
  }

  /**
   * Run this task as the first idle task.
   *
   * @return true if done with this task.
   */
  public boolean doInitialTask() {
    // Setup the counters
    EntityManager em = entityManagerFactory.createEntityManager();
    Long beneficiaryCount =
        em.createQuery(COUNT_UNHASHED_BENFICIARIES, Long.class).getSingleResult();
    Long historyCount = em.createQuery(COUNT_UNHASHED_HISTORIES, Long.class).getSingleResult();

    beneficiaryCounter.inc(beneficiaryCount);
    historyCounter.inc(historyCount);

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
    Instant start = Instant.now();
    /*
     * Do mbiHash fixups
     * This is a long running startup tasks. The TASK_TIME_LIMIT comes into play.
     */
    while (inPeriod(start, TASK_TIME_LIMIT) && !doTransaction(this::fixupBeneficiaryBatch)) {}
    while (inPeriod(start, TASK_TIME_LIMIT) && !doTransaction(this::fixupHistoryBatch)) {}

    boolean isDone = beneficiaryCounter.getCount() <= 0 && historyCounter.getCount() <= 0;
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
    return true;
  }

  /**
   * Fixup a batch of Beneficiaries. Update the beneficary metrics.
   *
   * @param em a {@link EntityManager} setup for a transaction
   * @return true if done with all fixups
   */
  public Boolean fixupBeneficiaryBatch(final EntityManager em) {
    final List<Beneficiary> beneficiaries =
        em.createQuery(SELECT_UNHASHED_BENFICIARIES, Beneficiary.class)
            .setMaxResults(BATCH_COUNT)
            .getResultList();

    for (final Beneficiary beneficiary : beneficiaries) {
      beneficiary
          .getMedicareBeneficiaryId()
          .ifPresent(
              mbi -> {
                if (mbi.isEmpty()) return;
                final String mbiHash = RifLoader.computeMbiHash(options, secretKeyFactory, mbi);
                beneficiary.setMbiHash(Optional.of(mbiHash));
              });
    }

    beneficaryMeter.mark(beneficiaries.size());
    beneficiaryCounter.dec(beneficiaries.size());
    return beneficiaries.size() < BATCH_COUNT;
  }

  /**
   * Fixup a batch of BeneficiaryHistory. Update the history metrics.
   *
   * @param em a {@link EntityManager} setup for a transaction
   * @return true if done with all fixups
   */
  public Boolean fixupHistoryBatch(final EntityManager em) {
    final List<BeneficiaryHistory> histories =
        em.createQuery(SELECT_UNHASHED_HISTORIES, BeneficiaryHistory.class)
            .setMaxResults(BATCH_COUNT)
            .getResultList();

    for (final BeneficiaryHistory history : histories) {
      history
          .getMedicareBeneficiaryId()
          .ifPresent(
              mbi -> {
                if (mbi.isEmpty()) return;
                final String mbiHash = RifLoader.computeMbiHash(options, secretKeyFactory, mbi);
                history.setMbiHash(Optional.of(mbiHash));
              });
    }

    historyMeter.mark(histories.size());
    historyCounter.dec(histories.size());
    return histories.size() < BATCH_COUNT;
  }

  /**
   * Any time left in this time slice?
   *
   * @param start of the period
   * @param period duration
   * @return true iff current period is less than the passed in period duration;
   */
  public static boolean inPeriod(final Instant start, final Duration period) {
    return Duration.between(start, Instant.now()).compareTo(period) <= 0;
  }

  /**
   * Setup a DB transaction and call the executor to do the work.
   *
   * @param executor does the work. Return the value from the executor
   * @return the return value from the executor
   */
  public boolean doTransaction(final Function<EntityManager, Boolean> executor) {
    try {
      final EntityManager em = entityManagerFactory.createEntityManager();
      EntityTransaction txn = null;
      try {
        txn = em.getTransaction();
        txn.begin();
        final Boolean result = executor.apply(em);
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
