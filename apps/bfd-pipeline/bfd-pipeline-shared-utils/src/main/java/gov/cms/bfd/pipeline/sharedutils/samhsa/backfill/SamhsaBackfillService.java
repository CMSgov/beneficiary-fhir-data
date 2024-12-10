package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Service to start RDASamhsaBackfill and CCWSamhsaBackfill, which will create the SAMHSA tags for
 * existing data. This class will create a Callable for each claim table, and run each
 * simultaneously in a different thread.
 */
public class SamhsaBackfillService implements Callable {
  /** List of RDA tables. */
  private final List<String> RDA_TABLES = List.of("rda.fiss_claims", "rda.mcs_claims");

  /** List of CCW tables. */
  private final List<String> CCW_TABLES =
      List.of(
          "ccw.carrier_claims",
          "ccw.dme_claims",
          "ccw.hha_claims",
          "ccw.hospice_claims",
          "ccw.inpatient_claims",
          "ccw.outpatient_claims",
          "ccw.snf_claims");

  /** Contains the list of callables for RDA. Each callable will be a different table. */
  List<Callable> rdaCallables;

  /** Contains the list of callables for CCW. Each callable will be a different table. */
  List<Callable> ccwCallables;

  /** The transaction manager. */
  TransactionManager transactionManager;

  /** This service. Will be a singleton. */
  static SamhsaBackfillService service;

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
   * Creates callables for RDA.
   *
   * @param tables The RDA tables to use.
   * @return A list of callables
   */
  private List<Callable> createRdaCallables(List<String> tables) {
    List<Callable> callables = new ArrayList<>();
    for (String table : tables) {
      callables.add(new RDASamhsaBackfill(transactionManager, batchSize, table));
    }
    return callables;
  }

  /**
   * Creates callables for CCW.
   *
   * @param tables The CCW tables to use.
   * @return A list of callables
   */
  private List<Callable> createCcwCallables(List<String> tables) {
    List<Callable> callables = new ArrayList<>();
    for (String table : tables) {
      callables.add(new CCWSamhsaBackfill(transactionManager, batchSize, table));
    }
    return callables;
  }

  /**
   * Constructor.
   *
   * @param appState The PipelineApplicationState.
   * @param batchSize The query batch size.
   */
  private SamhsaBackfillService(PipelineApplicationState appState, int batchSize) {
    transactionManager = new TransactionManager(appState.getEntityManagerFactory());
    this.batchSize = batchSize;
    rdaCallables = createRdaCallables(RDA_TABLES);
    ccwCallables = createCcwCallables(CCW_TABLES);
  }

  /**
   * Starts the backfill.
   *
   * @param ccw Backfill CCW claims
   * @param rda Backfill RDA claims
   * @return the total number of SAMHSA tags created.
   */
  public Long startBackFill(boolean ccw, boolean rda) {
    Future<Long> totalRda = null;
    Future<Long> totalCcw = null;
    Long total = 0L;
    ExecutorService executor =
        Executors.newFixedThreadPool(rdaCallables.size() + ccwCallables.size());
    List<Future<Long>> totals = new ArrayList<>();
    if (rda) {

      for (Callable callable : rdaCallables) {
        totals.add(executor.submit(callable));
      }
    }
    if (ccw) {
      for (Callable callable : ccwCallables) {
        totals.add(executor.submit(callable));
      }
    }
    try {
      for (Future<Long> futureTotal : totals) {
        total += futureTotal.get();
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
    return total;
  }

  @Override
  public Object call() throws Exception {
    return null;
  }
}
