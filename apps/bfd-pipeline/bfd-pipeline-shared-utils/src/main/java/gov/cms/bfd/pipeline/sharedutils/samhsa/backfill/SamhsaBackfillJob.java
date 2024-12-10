package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import static gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome.NOTHING_TO_DO;

import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.PipelineJob;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobOutcome;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobSchedule;
import gov.cms.bfd.pipeline.sharedutils.PipelineJobType;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Pipeline job for the SAMHSA Backfill. */
public class SamhsaBackfillJob implements PipelineJob {
  /** Sempaphore to ensure that only one instance of this job is running. */
  private final Semaphore runningSemaphore;

  /** The PipelineApplicationState. */
  PipelineApplicationState appState;

  /** Batch size. */
  int batchSize;

  /** The logger. */
  private static final Logger LOGGER = LoggerFactory.getLogger(SamhsaBackfillJob.class);

  /**
   * Constructor.
   *
   * @param appState The PipelineApplicationState
   * @param batchSize the query batch size.
   */
  public SamhsaBackfillJob(PipelineApplicationState appState, int batchSize) {
    this.appState = appState;
    this.batchSize = batchSize;
    runningSemaphore = new Semaphore(1);
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
    LOGGER.info("Starting SamhsaBackfillJob.");
    Long processedCount = 0L;
    try {
      processedCount += callBackfillService();
      LOGGER.info(
          String.format("SamhsaBackfillJob created total Tags for %d claims.", processedCount));
      return processedCount == 0 ? NOTHING_TO_DO : PipelineJobOutcome.WORK_DONE;
    } finally {
      runningSemaphore.release();
    }
  }

  /**
   * Calls the backfill service.
   *
   * @return The number of SAMHSA tags created.
   */
  Long callBackfillService() {
    SamhsaBackfillService backfillService =
        SamhsaBackfillService.createBackfillService(appState, batchSize);
    Long processedCount = backfillService.startBackFill(true, true);
    return processedCount;
  }

  /** {@inheritDoc} */
  @Override
  public void close() throws Exception {
    PipelineJob.super.close();
  }
}
