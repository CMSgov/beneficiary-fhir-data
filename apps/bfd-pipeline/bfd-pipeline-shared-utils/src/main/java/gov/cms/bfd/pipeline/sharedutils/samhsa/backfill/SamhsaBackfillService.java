package gov.cms.bfd.pipeline.sharedutils.samhsa.backfill;

import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.pipeline.sharedutils.samhsa.backfill.CCWSamhsaBackfill.CCW_TABLES;
import gov.cms.bfd.pipeline.sharedutils.samhsa.backfill.RDASamhsaBackfill.RDA_TABLES;
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
public class SamhsaBackfillService {
  /** List of RDA tables. */
  private final List<RDA_TABLES> rdaTables = List.of(RDA_TABLES.FISS_CLAIMS, RDA_TABLES.MCS_CLAIMS);

  /** List of CCW tables. */
  private final List<CCW_TABLES> ccwTables =
      List.of(
          CCW_TABLES.CARRIER_CLAIMS,
          CCW_TABLES.DME_CLAIMS,
          CCW_TABLES.HHA_CLAIMS,
          CCW_TABLES.HOSPICE_CLAIMS,
          CCW_TABLES.INPATIENT_CLAIMS,
          CCW_TABLES.OUTPATIENT_CLAIMS,
          CCW_TABLES.SNF_CLAIMS);

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
  private List<Callable> createRdaCallables(List<RDA_TABLES> tables) {
    List<Callable> callables = new ArrayList<>();
    for (RDASamhsaBackfill.RDA_TABLES table : tables) {
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
  private List<Callable> createCcwCallables(List<CCW_TABLES> tables) {
    List<Callable> callables = new ArrayList<>();
    for (CCW_TABLES table : tables) {
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
    rdaCallables = createRdaCallables(rdaTables);
    ccwCallables = createCcwCallables(ccwTables);
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
    total =
        totals.stream()
            .mapToLong(
                f -> {
                  try {
                    return f.get();
                  } catch (InterruptedException | ExecutionException ex) {
                    throw new RuntimeException(ex);
                  }
                })
            .sum();
    return total;
  }
}
