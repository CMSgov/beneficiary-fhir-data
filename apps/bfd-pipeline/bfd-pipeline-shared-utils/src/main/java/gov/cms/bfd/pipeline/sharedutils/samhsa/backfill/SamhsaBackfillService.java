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
  public static final List<RDA_TABLES> rdaTables =
      List.of(
          RDA_TABLES.FISS_CLAIMS,
          RDA_TABLES.FISS_REVENUE_LINES,
          RDA_TABLES.FISS_PROC_CODES,
          RDA_TABLES.FISS_DIAGNOSIS_CODES,
          RDA_TABLES.MCS_DIAGNOSIS_CODES,
          RDA_TABLES.MCS_DETAILS);

  /** List of CCW tables. */
  public static final List<CCW_TABLES> ccwTables =
      List.of(
          CCW_TABLES.CARRIER_CLAIMS,
          CCW_TABLES.CARRIER_CLAIM_LINES,
          CCW_TABLES.DME_CLAIMS,
          CCW_TABLES.DME_CLAIM_LINES,
          CCW_TABLES.HHA_CLAIMS,
          CCW_TABLES.HHA_CLAIM_LINES,
          CCW_TABLES.HOSPICE_CLAIMS,
          CCW_TABLES.HOSPICE_CLAIM_LINES,
          CCW_TABLES.INPATIENT_CLAIMS,
          CCW_TABLES.INPATIENT_CLAIM_LINES,
          CCW_TABLES.OUTPATIENT_CLAIMS,
          CCW_TABLES.OUTPATIENT_CLAIM_LINES,
          CCW_TABLES.SNF_CLAIMS,
          CCW_TABLES.SNF_CLAIM_LINES);

  /** Contains the list of callables for RDA. Each callable will be a different table. */
  List<Callable> rdaCallables;

  /** Contains the list of callables for CCW. Each callable will be a different table. */
  List<Callable> ccwCallables;

  /** The CCW transaction manager. */
  TransactionManager transactionManagerCcw;

  /** The RDA transaction manager. */
  TransactionManager transactionManagerRda;

  /** This service. Will be a singleton. */
  static SamhsaBackfillService service;

  /** Batch size. */
  int batchSize;

  /** The log interval. */
  Long logInterval;

  /**
   * Creates the Singleton for this service.
   *
   * @param appStateCcw The CCW PipelineApplicationState.
   * @param appStateRda The RDA PipelineApplicationState.
   * @param batchSize The query batch size.
   * @param logInterval The log interval.
   * @return the service singleton.
   */
  public static SamhsaBackfillService createBackfillService(
      List<PipelineApplicationState> appStateCcw,
      List<PipelineApplicationState> appStateRda,
      int batchSize,
      Long logInterval) {
    if (service == null) {
      service = new SamhsaBackfillService(appStateCcw, appStateRda, batchSize, logInterval);
    }
    return service;
  }

  /**
   * Creates the Singleton for this service.
   *
   * @param appStateCcw The CCW PipelineApplicationState.
   * @param appStateRda The RDA PipelineApplicationState.
   * @param batchSize The query batch size.
   * @param logInterval The log interval.
   * @return the service singleton.
   */
  public static SamhsaBackfillService createBackfillService(
      PipelineApplicationState appStateCcw,
      PipelineApplicationState appStateRda,
      int batchSize,
      Long logInterval) {
    List<PipelineApplicationState> ccwStates = new ArrayList<>();
    for (int i = 0; i < ccwTables.size(); i++) {
      ccwStates.add(appStateCcw);
    }
    List<PipelineApplicationState> rdaStates = new ArrayList<>();
    for (int i = 0; i < rdaTables.size(); i++) {
      rdaStates.add(appStateRda);
    }
    return createBackfillService(ccwStates, rdaStates, batchSize, logInterval);
  }

  /**
   * Creates callables for RDA.
   *
   * @param tables The RDA tables to use.
   * @param tm The TransactionManager
   * @return A list of callables
   */
  private List<Callable> createRdaCallables(List<RDA_TABLES> tables, List<TransactionManager> tm) {
    List<Callable> callables = new ArrayList<>();
    int i = 0;
    for (RDASamhsaBackfill.RDA_TABLES table : tables) {
      callables.add(new RDASamhsaBackfill(tm.get(i), batchSize, logInterval, table));
      i++;
    }
    return callables;
  }

  /**
   * Creates callables for CCW.
   *
   * @param tables The CCW tables to use.
   * @param tm The TransactionManager.
   * @return A list of callables
   */
  private List<Callable> createCcwCallables(List<CCW_TABLES> tables, List<TransactionManager> tm) {
    List<Callable> callables = new ArrayList<>();
    int i = 0;
    for (CCW_TABLES table : tables) {
      callables.add(new CCWSamhsaBackfill(tm.get(i), batchSize, logInterval, table));
      i++;
    }
    return callables;
  }

  /**
   * Constructor.
   *
   * @param appStateCcw The CCW PipelineApplicationState.
   * @param appStateRda The RDA PipelineApplicationState.
   * @param batchSize The query batch size.
   * @param logInterval The log interval.
   */
  private SamhsaBackfillService(
      List<PipelineApplicationState> appStateCcw,
      List<PipelineApplicationState> appStateRda,
      int batchSize,
      Long logInterval) {
    this.batchSize = batchSize;
    this.logInterval = logInterval;
    if (appStateCcw != null) {
      List<TransactionManager> transactionManagerCcw = new ArrayList<>();
      for (PipelineApplicationState appState : appStateCcw) {
        transactionManagerCcw.add(new TransactionManager(appState.getEntityManagerFactory()));
      }
      ccwCallables = createCcwCallables(ccwTables, transactionManagerCcw);
    }
    if (appStateRda != null) {
      List<TransactionManager> transactionManagerRda = new ArrayList<>();
      for (PipelineApplicationState appState : appStateCcw) {
        transactionManagerRda.add(new TransactionManager(appState.getEntityManagerFactory()));
      }

      rdaCallables = createRdaCallables(rdaTables, transactionManagerRda);
    }
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
    Integer threadPoolSize = 0;
    ccw = ccw && ccwCallables != null;
    rda = rda && rdaCallables != null;
    if (ccw) {
      threadPoolSize += ccwCallables.size();
    }
    if (rda) {
      threadPoolSize += rdaCallables.size();
    }
    ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
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
