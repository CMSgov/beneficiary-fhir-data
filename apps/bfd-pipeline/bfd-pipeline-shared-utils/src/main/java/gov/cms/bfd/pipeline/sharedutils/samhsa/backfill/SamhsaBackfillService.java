package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;

/**
 * Service to start RDASamhsaBackfill and CCWSamhsaBackfill, which will create the SAMHSA tags for
 * existing data.
 */
public class SamhsaBackfillService {
  /** The transaction manager. */
  TransactionManager transactionManager;

  /** This service. Will be a singleton. */
  static SamhsaBackfillService service;

  /** Class to backfill RDA data. */
  AbstractSamhsaBackfill rdaSamhsaBackfill;

  /** Class to backfill CCW data. */
  AbstractSamhsaBackfill ccwSamhsaBackfill;

  /** Batch size. */
  int batchSize;

  /**
   * Creates the Singleton for this service.
   *
   * @param appState The PipelineApplicationState.
   * @param batchSize The query batch size.
   * @return the service singleton.
   */
  public static SamhsaBackfillService createBackfillService(
      PipelineApplicationState appState, int batchSize) {
    if (service == null) {
      service = new SamhsaBackfillService(appState, batchSize);
    }
    return service;
  }

  /**
   * Constructor.
   *
   * @param appState The PipelineApplicationState.
   * @param batchSize The query batch size.
   */
  private SamhsaBackfillService(PipelineApplicationState appState, int batchSize) {
    transactionManager = new TransactionManager(appState.getEntityManagerFactory());
    rdaSamhsaBackfill = new RDASamhsaBackfill(transactionManager, batchSize);
    ccwSamhsaBackfill = new CCWSamhsaBackfill(transactionManager, batchSize);
  }

  /**
   * Starts the backfill.
   *
   * @return the total number of SAMHSA tags created.
   */
  public Long startBackFill() {
    Long total = rdaSamhsaBackfill.execute();
    total += ccwSamhsaBackfill.execute();
    return total;
  }
}
