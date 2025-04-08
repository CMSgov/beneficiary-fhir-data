package gov.cms.bfd.pipeline.sharedutils.npi_fda;

import static gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome.NOTHING_TO_DO;
import static gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome.WORK_DONE;

import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import jakarta.persistence.EntityManager;
import java.time.temporal.ChronoUnit;
import java.util.List;
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

  /** Column delimiter. */
  static final String DELIMITER = ",";

  /** The logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(NpiFdaLoadJob.class);

  /**
   * Constructor.
   *
   * @param appStates The PipelineApplicationStates.
   * @param batchSize The number of records to save before committing a transaction.
   * @param runInterval How often to run the job, in days.
   */
  public NpiFdaLoadJob(List<PipelineApplicationState> appStates, int batchSize, int runInterval)
      throws Exception {
    if (appStates != null && appStates.size() > 1) {
      this.npiAppState = appStates.get(0);
      this.fdaAppState = appStates.get(1);
    } else {
      throw new Exception("Not enough instances of PipelineApplicationState");
    }
    runningSemaphore = new Semaphore(1);
    this.batchSize = batchSize;
    this.runInterval = runInterval;
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
      return (npiTotal == -1 && fdaTotal == -1) ? NOTHING_TO_DO : WORK_DONE;
    } finally {
      runningSemaphore.release();
    }
  }
}
