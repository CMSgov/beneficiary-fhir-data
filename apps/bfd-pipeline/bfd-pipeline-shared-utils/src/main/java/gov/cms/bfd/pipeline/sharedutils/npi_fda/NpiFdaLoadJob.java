package gov.cms.bfd.pipeline.sharedutils.npi_fda;

import static gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome.NOTHING_TO_DO;
import static gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome.WORK_DONE;

import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Pipeline job to download the NPI and FDA data, then persist it to a database table. */
public class NpiFdaLoadJob implements PipelineJob {
  /** Sempaphore to ensure that only one instance of this job is running. */
  private final Semaphore runningSemaphore;

  /** The ApplicationState. */
  PipelineApplicationState appState;

  /** The number of records to save before commiting the transaction. */
  int batchSize;

  /** How often to run the job, in days. */
  int runInterval;

  /** The logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(NpiFdaLoadJob.class);

  /**
   * Constructor.
   *
   * @param appState The PipelineApplicationState.
   * @param batchSize The number of records to save before committing a transaction.
   * @param runInterval How often to run the job, in days.
   */
  public NpiFdaLoadJob(PipelineApplicationState appState, int batchSize, int runInterval) {
    this.appState = appState;
    runningSemaphore = new Semaphore(1);
    this.batchSize = batchSize;
    this.runInterval = runInterval;
  }

  /** {@inheritDoc} */
  @Override
  public PipelineJobType getType() {
    return PipelineJob.super.getType();
  }

  /** {@inheritDoc} */
  @Override
  public Optional<PipelineJobSchedule> getSchedule() {
    // Run the service to see if the data should be loaded once per day.
    // If we relied on this schedule by itself to check if the data should be loaded,
    // the data load would happen everytime the Pipeline restarted.
    // instead, we will check a database last_updated value once inside of the service.

    return Optional.of(new PipelineJobSchedule(1, ChronoUnit.DAYS));
  }

  /** {@inheritDoc} */
  @Override
  public boolean isInterruptible() {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isSmokeTestSuccessful() throws Exception {
    return PipelineJob.super.isSmokeTestSuccessful();
  }

  /** {@inheritDoc} */
  @Override
  public PipelineJobOutcome call() throws Exception {
    if (!runningSemaphore.tryAcquire()) {
      LOGGER.warn("job is already running");
      return NOTHING_TO_DO;
    }
    LOGGER.info("Starting LoadNpiFdaDataJob.");
    int total;
    try (EntityManager entityManager = appState.getEntityManagerFactory().createEntityManager()) {
      Instant start = Instant.now();
      LoadNpiDataFiles loadNpiDataFiles =
          new LoadNpiDataFiles(entityManager, batchSize, runInterval);
      try (ExecutorService executor = Executors.newFixedThreadPool(1)) {
        Future<Integer> npiTotal = executor.submit(loadNpiDataFiles);
        try {
          total = npiTotal.get();
        } catch (InterruptedException | ExecutionException ex) {
          npiTotal.cancel(true);
          throw new Exception(ex);
        }
      }
      Instant finish = Instant.now();
      Long totalTime = ChronoUnit.SECONDS.between(start, finish);
      if (total > -1) {
        LOGGER.info(
            "LoadNpiFdaDataJob finished in {} seconds. Loaded {} records.", totalTime, total);
        return WORK_DONE;
      } else {
        return NOTHING_TO_DO;
      }
    } finally {
      runningSemaphore.release();
    }
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws Exception {
    PipelineJob.super.close();
  }
}
