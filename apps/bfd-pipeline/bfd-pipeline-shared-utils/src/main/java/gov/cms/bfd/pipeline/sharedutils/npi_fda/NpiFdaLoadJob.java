package gov.cms.bfd.pipeline.sharedutils.npi_fda;

import static gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome.NOTHING_TO_DO;
import static gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome.SHOULD_TERMINATE;

import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import jakarta.persistence.EntityManager;
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
  PipelineApplicationState npiAppState;

  PipelineApplicationState fdaAppState;

  /** The number of records to save before commiting the transaction. */
  int batchSize;

  /** How often to run the job, in days. */
  int runInterval;

  /** The logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(NpiFdaLoadJob.class);

  /**
   * Constructor.
   *
   * @param fdaAppState PipelineApplicationState to use for FDA Drug Enrichment.
   * @param npiAppState PipelineApplicationState to use for NPI Enrichment.
   * @param batchSize The number of records to save before committing a transaction.
   * @param runInterval How often to run the job, in days.
   */
  public NpiFdaLoadJob(
      PipelineApplicationState npiAppState,
      PipelineApplicationState fdaAppState,
      int batchSize,
      int runInterval)
      throws Exception {
    this.npiAppState = npiAppState;
    this.fdaAppState = fdaAppState;
    runningSemaphore = new Semaphore(1);
    this.batchSize = batchSize;
    this.runInterval = runInterval;
  }

  /** {@inheritDoc} */
  @Override
  public Optional<PipelineJobSchedule> getSchedule() {
    // With the switch to ECS, we no longer rely on any of the Pipeline's complex scheduling
    // mechanisms to schedule jobs; instead, we run purpose-built Tasks for each of the various
    // Pipeline Jobs and define their schedules in our infrastructure. As such, we want this Job to
    // run right after the Pipeline process starts, so setting the schedule to 1 second ensures the
    // Job starts near-immediately. Additionally, the Job returns SHOULD_TERMINATE on completion, so
    // the Pipeline process will exit after Job completion, ensuring we do not run this Job again.
    return Optional.of(new PipelineJobSchedule(1, ChronoUnit.SECONDS));
  }

  /** {@inheritDoc} */
  @Override
  public boolean isInterruptible() {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public PipelineJobOutcome call() throws Exception {
    if (!runningSemaphore.tryAcquire()) {
      LOGGER.warn("job is already running");
      return NOTHING_TO_DO;
    }
    LOGGER.info("Starting LoadNpiFdaDataJob.");
    int npiTotal;
    int fdaTotal;
    try (EntityManager npiEntityManager =
            npiAppState.getEntityManagerFactory().createEntityManager();
        EntityManager fdaEntityManager =
            fdaAppState.getEntityManagerFactory().createEntityManager()) {
      LoadNpiDataFiles loadNpiDataFiles =
          new LoadNpiDataFiles(npiEntityManager, batchSize, runInterval);
      LoadFdaDataFiles loadFdaDataFiles =
          new LoadFdaDataFiles(fdaEntityManager, batchSize, runInterval);
      try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
        Future<Integer> npiTotalFuture = executor.submit(loadNpiDataFiles);
        Future<Integer> fdaTotalFuture = executor.submit(loadFdaDataFiles);
        try {
          npiTotal = npiTotalFuture.get();
          fdaTotal = fdaTotalFuture.get();
        } catch (InterruptedException | ExecutionException ex) {
          npiTotalFuture.cancel(true);
          fdaTotalFuture.cancel(true);
          throw new Exception(ex);
        }
      }
      return (npiTotal == -1 && fdaTotal == -1) ? NOTHING_TO_DO : SHOULD_TERMINATE;
    } finally {
      runningSemaphore.release();
    }
  }
}
