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

  /**
   * Creates the Singleton for this service.
   *
   * @param appState The PipelineApplicationState
   * @return the service singleton.
   */
  public static SamhsaBackfillService createBackfillService(PipelineApplicationState appState) {
    if (service == null) {
      service = new SamhsaBackfillService(appState);
    }
    return service;
  }

  /**
   * Constructor.
   *
   * @param appState The PipelineApplicationState.
   */
  private SamhsaBackfillService(PipelineApplicationState appState) {
    transactionManager = new TransactionManager(appState.getEntityManagerFactory());
    rdaSamhsaBackfill = new RDASamhsaBackfill(transactionManager);
    ccwSamhsaBackfill = new CCWSamhsaBackfill(transactionManager);
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
