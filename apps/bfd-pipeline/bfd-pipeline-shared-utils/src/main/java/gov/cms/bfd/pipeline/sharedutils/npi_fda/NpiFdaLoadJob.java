package gov.cms.bfd.pipeline.sharedutils.npi_fda;

import static gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome.NOTHING_TO_DO;

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

/** Pipeline job for the SAMHSA Backfill. */
public class NpiFdaLoadJob implements PipelineJob {
  /** Sempaphore to ensure that only one instance of this job is running. */
  private final Semaphore runningSemaphore;

  /** The PipelineApplicationState. */
  EntityManager entityManager;

  /** The number of records to save before commiting the transasction. */
  int batchSize;

  /** The logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(NpiFdaLoadJob.class);

  /**
   * Constructor.
   *
   * @param appState The PipelineApplicationState.
   * @param batchSize The number of records to save before committing a transaction.
   */
  public NpiFdaLoadJob(PipelineApplicationState appState, int batchSize) {
    entityManager = appState.getEntityManagerFactory().createEntityManager();
    runningSemaphore = new Semaphore(1);
    this.batchSize = batchSize;
  }

  /** {@inheritDoc} */
  @Override
  public PipelineJobType getType() {
    return PipelineJob.super.getType();
  }

  /** {@inheritDoc} */
  @Override
  public Optional<PipelineJobSchedule> getSchedule() {
    return Optional.empty();
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
    int total = 0;
    try {
      Instant start = Instant.now();
      LoadNpiDataFiles loadNpiDataFiles = new LoadNpiDataFiles(entityManager, batchSize);
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
      LOGGER.info("LoadNpiFdaDataJob finished in {} seconds. Loaded {} records.", totalTime, total);
      return PipelineJobOutcome.WORK_DONE;
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
