package gov.cms.bfd.pipeline.ccw.rif.load;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import gov.cms.bfd.model.rif.LoadedBatch;
import gov.cms.bfd.model.rif.LoadedBatchBuilder;
import gov.cms.bfd.model.rif.LoadedFile;
import gov.cms.bfd.model.rif.RecordAction;
import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifRecordBase;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.model.rif.entities.Beneficiary;
import gov.cms.bfd.model.rif.entities.BeneficiaryHistory;
import gov.cms.bfd.model.rif.entities.BeneficiaryMonthly;
import gov.cms.bfd.model.rif.entities.Beneficiary_;
import gov.cms.bfd.pipeline.ccw.rif.extract.RifFileRecords;
import gov.cms.bfd.pipeline.ccw.rif.load.RifRecordLoadResult.LoadAction;
import gov.cms.bfd.pipeline.sharedutils.FluxUtils;
import gov.cms.bfd.pipeline.sharedutils.FluxWaiter;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Pushes CCW beneficiary and claims data from {@link RifRecordEvent}s into the Blue Button API's
 * database.
 */
public final class RifLoader {
  /**
   * How old a file can be in days before it is deleted from the loaded files table in the database.
   */
  private static final Period MAX_FILE_AGE_DAYS = Period.ofDays(40);

  private static final Logger LOGGER = LoggerFactory.getLogger(RifLoader.class);

  /**
   * Logger to count the number of records loaded; only logs when the log level is allowing debug
   * messages.
   */
  private static final Logger LOGGER_RECORD_COUNTS =
      LoggerFactory.getLogger(RifLoader.class.getName() + ".recordCounts");

  /** The load options. */
  private final LoadAppOptions options;

  /** The hasher for ids. */
  private final IdHasher idHasher;

  /** The shared application state. */
  private final PipelineApplicationState appState;

  /** Used to wait for flux completion. */
  private final FluxWaiter fluxWaiter;

  /** The maximum amount of time we will wait for a job to complete loading its batches. */
  private static final Duration MAX_FILE_WAIT_TIME = Duration.ofHours(72);

  /**
   * The maximum amount of time we will wait for a job to quit when an interrupt has been triggered.
   */
  private static final Duration MAX_INTERRUPTED_WAIT_TIME = Duration.ofMinutes(5);

  /** Query for refreshing the current beneficiaries materialized view. */
  private static final String REFRESH_CURRENT_BENEFICIARIES_VIEW_SQL =
      "SELECT ccw.refresh_current_beneficiaries()";

  /**
   * Constructs a new {@link RifLoader} instance.
   *
   * @param options the {@link LoadAppOptions} to use
   * @param appState the {@link PipelineApplicationState} to use
   */
  public RifLoader(LoadAppOptions options, PipelineApplicationState appState) {
    this.options = options;
    this.appState = appState;

    idHasher = new IdHasher(options.getIdHasherConfig());
    fluxWaiter = new FluxWaiter(MAX_FILE_WAIT_TIME, MAX_INTERRUPTED_WAIT_TIME);
  }

  /**
   * Creates the load executor and add metrics to track its queue and batch sizes.
   *
   * @param settings the {@link LoadAppOptions.PerformanceSettings} to use
   * @return the {@link Scheduler} to use for asynchronous load tasks
   */
  private Scheduler createScheduler(LoadAppOptions.PerformanceSettings settings) {
    // allow one extra thread for parsing and another for background progress updates
    final int threadPoolSize = settings.getLoaderThreads() + 2;
    final int batchSize = settings.getRecordBatchSize();
    final int taskQueueSize = settings.getLoaderThreads() * settings.getTaskQueueSizeMultiple();

    LOGGER.info(
        "Configured to load with '{}' threads, a queue of '{}' batches, and a batch size of '{}'.",
        threadPoolSize,
        taskQueueSize,
        batchSize);

    // We allow a little extra capacity in the pool since we require a thread from the scheduler to
    // parse records in addition to the ones used to write batches to the database.
    return Schedulers.newBoundedElastic(2 + threadPoolSize, taskQueueSize, "RifLoader", 60);
  }

  /**
   * Selects the {@link LoadStrategy} that should be used for the record being processed.
   *
   * @param recordAction the {@link RecordAction} of the specific record being processed
   * @return the {@link LoadStrategy} to use
   */
  private LoadStrategy selectStrategy(RecordAction recordAction) {
    if (recordAction == RecordAction.INSERT) {
      if (options.isIdempotencyRequired()) return LoadStrategy.INSERT_IDEMPOTENT;
      else return LoadStrategy.INSERT_UPDATE_NON_IDEMPOTENT;
    } else {
      return LoadStrategy.INSERT_UPDATE_NON_IDEMPOTENT;
    }
  }

  /**
   * Consumes the {@link RifRecordEvent}s from the file, pushing each {@link RifRecordEvent}'s
   * entity objects to the database. Blocks for up to {@link #MAX_FILE_WAIT_TIME} to allow
   * processing to be completed. All {@link RifRecordLoadResult}s are discarded but a count of the
   * number of them is returned. If the flux terminates with an exception that is passed through to
   * the caller instead.
   *
   * @param dataToLoad the {@link RifFileRecords} containing FHIR {@link RifRecordEvent}s to be
   *     loaded
   * @return count of {@link RifRecordLoadResult}s encountered during processing
   * @throws Exception passed through from processing
   */
  public long processBlocking(RifFileRecords dataToLoad) throws Exception {
    final var interrupted = new AtomicBoolean();
    return fluxWaiter.waitForCompletion(processAsync(dataToLoad, interrupted), interrupted);
  }

  /**
   * Produces a {@link Flux} that, when subscribed to, consumes the {@link RifRecordEvent}s from the
   * file, pushing each {@link RifRecordEvent}'s record to the database, and publishing the result
   * for each record. Any exception thrown during processing terminates the flux with an error.
   *
   * @param dataToLoad the {@link RifFileRecords} containing FHIR {@link RifRecordEvent}s to be
   *     loaded
   * @param interrupted flag that will be trigger a clean shutdown if set to true by {@link
   *     FluxWaiter}
   * @return a {@link Flux} that processes all records asynchronously
   */
  public Flux<RifRecordLoadResult> processAsync(
      RifFileRecords dataToLoad, AtomicBoolean interrupted) {
    return FluxUtils.fromFluxFunction(
        () -> {
          final RifFile rifFile = dataToLoad.getSourceEvent().getFile();
          final RifFileType fileType = rifFile.getFileType();
          final LoadAppOptions.PerformanceSettings performanceSettings =
              options.selectPerformanceSettingsForFileType(fileType);
          final int batchPrefetch =
              performanceSettings.getLoaderThreads()
                  * performanceSettings.getTaskQueueSizeMultiple();

          final Timer.Context timerDataSetFile =
              appState
                  .getMetrics()
                  .timer(
                      MetricRegistry.name(
                          getClass().getSimpleName(), "dataSet", "file", "processed"))
                  .time();
          LOGGER.info("Processing '{}'...", dataToLoad);

          // Trim the LoadedFiles & LoadedBatches table (throws on failure)
          trimLoadedFiles();

          // Insert a LoadedFiles entry (throws on failure)
          final long loadedFileId = insertLoadedFile(dataToLoad.getSourceEvent());

          // Create and return a flux that asynchronously loads records in batches using a custom
          // scheduler.
          final Scheduler scheduler = createScheduler(performanceSettings);
          final var progressTracker = new RifFileProgressTracker(rifFile);
          final long startingRecordNumber = progressTracker.getStartingRecordNumber();
          if (startingRecordNumber > 0) {
            LOGGER.info("skipping to record number {} before processing", startingRecordNumber);
          }
          // Schedule task to update progress in database once per second.
          final var progressUpdateSchedule =
              scheduler.schedulePeriodically(
                  progressTracker::writeProgress, 1L, 1L, TimeUnit.SECONDS);
          return dataToLoad
              .getRecords()
              // Skip any records that we know have been processed before.
              .filter(event -> event.getRecordNumber() > startingRecordNumber)
              // Add active record number to progress tracker.
              .doOnNext(event -> progressTracker.recordActive(event.getRecordNumber()))
              // Parse records on a thread from our scheduler.
              .subscribeOn(scheduler)
              // collect records into batches
              .buffer(performanceSettings.getRecordBatchSize())
              // Set the number of batches we want to keep ready for processing.  The actual amount
              // will vary between 75% and 100% of the requested value as the flux manages the
              // queue.
              .limitRate(batchPrefetch)
              // Stop processing if we have received an interrupt
              .takeUntil(ignored -> interrupted.get())
              // process batches in parallel using threads from our scheduler
              .flatMap(
                  batch ->
                      processBatch(batch, loadedFileId)
                          .subscribeOn(scheduler)
                          // Stop processing if we have received an interrupt
                          .takeUntil(ignored -> interrupted.get()),
                  performanceSettings.getLoaderThreads())
              // Mark active record as complete so progress can be updated.
              .doOnNext(result -> progressTracker.recordComplete(result.getRecordNumber()))
              // Update progress with final result when all records have been processed
              .doOnComplete(
                  () -> {
                    progressTracker.writeProgress();
                    if (fileType == RifFileType.BENEFICIARY) {
                      refreshCurrentBeneficiariesView();
                    }
                  })
              // clean up when the flux terminates (either by error or completion)
              .doFinally(
                  ignored -> {
                    progressUpdateSchedule.dispose();
                    timerDataSetFile.stop();
                    logRecordCounts();
                    scheduler.dispose();
                  })
              // log success or failure events before they are published to subscriber
              .doOnComplete(() -> LOGGER.info("Processed '{}'.", dataToLoad))
              .doOnError(
                  ex -> LOGGER.error("terminated by exception: message={}", ex.getMessage(), ex));
        });
  }

  /** Refreshes the current beneficiaries materialized view. */
  private void refreshCurrentBeneficiariesView() {
    try (EntityManager entityManager = appState.getEntityManagerFactory().createEntityManager()) {
      try (final Timer.Context timerRefreshCurrentBeneficiaries =
          appState
              .getMetrics()
              .timer(
                  MetricRegistry.name(
                      getClass().getSimpleName(), "refreshCurrentBeneficiariesView"))
              .time()) {
        entityManager.createNativeQuery(REFRESH_CURRENT_BENEFICIARIES_VIEW_SQL).getResultList();
      }
    }
  }

  /**
   * Creates a {@link Flux} that, when subscribed to in a scheduler, processes a batch of records
   * and publishes the result for each record.
   *
   * @param recordsBatch the {@link RifRecordEvent}s to process
   * @param loadedFileId the loaded file id
   * @return the flux
   */
  private Flux<RifRecordLoadResult> processBatch(
      List<RifRecordEvent<?>> recordsBatch, long loadedFileId) {
    return FluxUtils.fromIterableFunction(
        () -> {
          final RifFileEvent fileEvent = recordsBatch.get(0).getFileEvent();
          final MetricRegistry fileEventMetrics = fileEvent.getEventMetrics();
          final RifFileType rifFileType = fileEvent.getFile().getFileType();

          // Only one of each failure/success Timer.Contexts will be applied.
          final Timer.Context timerBatchSuccess =
              appState
                  .getMetrics()
                  .timer(MetricRegistry.name(getClass().getSimpleName(), "recordBatches"))
                  .time();
          final Timer.Context timerBatchTypeSuccess =
              fileEventMetrics
                  .timer(
                      MetricRegistry.name(
                          getClass().getSimpleName(), "recordBatches", rifFileType.name()))
                  .time();
          final Timer.Context timerBundleFailure =
              appState
                  .getMetrics()
                  .timer(MetricRegistry.name(getClass().getSimpleName(), "recordBatches", "failed"))
                  .time();
          // Execute the transaction and capture any exception it might throw as a RifLoadFailure.
          // Catching it here lets us rethrow it after cleaning up our state farther down in the
          // method.
          RifLoadFailure failure = null;
          List<RifRecordLoadResult> processResults = List.of();
          try (TransactionManager transactionManager =
              new TransactionManager(appState.getEntityManagerFactory())) {
            processResults =
                transactionManager.executeFunction(
                    entityManager -> processBatchImpl(recordsBatch, loadedFileId, entityManager));
          } catch (Exception e) {
            LOGGER.warn("Failed to load '{}' batch.", rifFileType, e);
            failure = new RifLoadFailure(recordsBatch, e);
          }

          if (failure == null) {
            // Update the metrics now that things have been pushed.
            timerBatchSuccess.stop();
            timerBatchTypeSuccess.stop();
            return processResults;
          } else {
            // Update metrics for a failure and halt the pipeline.
            timerBundleFailure.stop();
            fileEventMetrics
                .meter(MetricRegistry.name(getClass().getSimpleName(), "recordBatches", "failed"))
                .mark(1);

            LOGGER.error("Error caught when processing async batch!", failure);
            throw failure;
          }
        });
  }

  /**
   * Loads a batch of records into the database and returns the load result for each record.
   *
   * @param recordsBatch the {@link RifRecordEvent}s to process
   * @param loadedFileId the loaded file id
   * @param entityManager the {@link EntityManager} for the current transaction
   * @return the {@link RifRecordLoadResult}s that model the results of the operation
   * @throws IOException can be thrown by {@link org.apache.commons.csv.CSVPrinter}
   */
  private List<RifRecordLoadResult> processBatchImpl(
      List<RifRecordEvent<?>> recordsBatch, long loadedFileId, EntityManager entityManager)
      throws IOException {
    RifFileEvent fileEvent = recordsBatch.get(0).getFileEvent();
    MetricRegistry fileEventMetrics = fileEvent.getEventMetrics();
    RifFileType rifFileType = fileEvent.getFile().getFileType();

    if (rifFileType == RifFileType.BENEFICIARY_HISTORY) {
      for (RifRecordEvent<?> rifRecordEvent : recordsBatch) {
        hashBeneficiaryHistoryHicn(rifRecordEvent);
        hashBeneficiaryHistoryMbi(rifRecordEvent);
      }
    }

    List<RifRecordLoadResult> loadResults = new ArrayList<>(recordsBatch.size());

    /*
     * Dev Note: All timestamps of records in the batch and the LoadedBatch must be the same for data consistency.
     * The timestamp from the LoadedBatchBuilder is used.
     */
    LoadedBatchBuilder loadedBatchBuilder =
        new LoadedBatchBuilder(loadedFileId, recordsBatch.size());

    for (RifRecordEvent<?> rifRecordEvent : recordsBatch) {
      RecordAction recordAction = rifRecordEvent.getRecordAction();
      RifRecordBase record = rifRecordEvent.getRecord();

      LOGGER.trace("Loading '{}' record.", rifFileType);

      // Set lastUpdated to the same value for the whole batch
      record.setLastUpdated(Optional.of(loadedBatchBuilder.getTimestamp()));

      // Associate the beneficiary with this file loaded
      loadedBatchBuilder.associateBeneficiary(rifRecordEvent.getBeneficiaryId());

      LoadStrategy strategy = selectStrategy(recordAction);
      LoadAction loadAction;

      if (strategy == LoadStrategy.INSERT_IDEMPOTENT) {
        // Check to see if record already exists.
        Timer.Context timerIdempotencyQuery =
            fileEventMetrics
                .timer(MetricRegistry.name(getClass().getSimpleName(), "idempotencyQueries"))
                .time();
        Object recordId =
            appState.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(record);
        Objects.requireNonNull(recordId);
        Object recordInDb = entityManager.find(record.getClass(), recordId);
        timerIdempotencyQuery.close();

        if (recordInDb == null) {
          loadAction = LoadAction.INSERTED;
          tweakIfBeneficiary(entityManager, loadedBatchBuilder, rifRecordEvent);
          entityManager.persist(record);
          // FIXME Object recordInDbAfterUpdate = entityManager.find(record.getClass(), recordId);
        } else {
          loadAction = LoadAction.DID_NOTHING;
        }
      } else if (strategy == LoadStrategy.INSERT_UPDATE_NON_IDEMPOTENT) {
        if (rifRecordEvent.getRecordAction().equals(RecordAction.INSERT)) {
          loadAction = LoadAction.INSERTED;
          tweakIfBeneficiary(entityManager, loadedBatchBuilder, rifRecordEvent);
          entityManager.persist(record);
        } else if (rifRecordEvent.getRecordAction().equals(RecordAction.UPDATE)) {
          loadAction = LoadAction.UPDATED;
          tweakIfBeneficiary(entityManager, loadedBatchBuilder, rifRecordEvent);
          entityManager.merge(record);
        } else {
          throw new BadCodeMonkeyException(
              String.format(
                  "Unhandled %s: '%s'.", RecordAction.class, rifRecordEvent.getRecordAction()));
        }
      } else throw new BadCodeMonkeyException();

      LOGGER.trace("Loaded '{}' record.", rifFileType);

      fileEventMetrics
          .meter(MetricRegistry.name(getClass().getSimpleName(), "records", loadAction.name()))
          .mark(1);

      loadResults.add(new RifRecordLoadResult(rifRecordEvent, loadAction));
    }
    LoadedBatch loadedBatch = loadedBatchBuilder.build();
    entityManager.persist(loadedBatch);

    return loadResults;
  }

  /**
   * Applies various "tweaks" to the {@link Beneficiary} (if any) in the specified {@link
   * RifRecordEvent}:
   *
   * <ul>
   *   <li>Hashes any MBIs or HICNs in it.
   *   <li>Updates its {@link Beneficiary#getBeneficiaryMonthlys()} records, as needed.
   *   <li>Adds a {@link BeneficiaryHistory} record for previous the {@link Beneficiary}, as needed.
   * </ul>
   *
   * @param entityManager the {@link EntityManager} to use
   * @param loadedBatchBuilder the {@link LoadedBatchBuilder} to use
   * @param rifRecordEvent the {@link RifRecordEvent} to handle the {@link Beneficiary} (if any) for
   */
  private void tweakIfBeneficiary(
      EntityManager entityManager,
      LoadedBatchBuilder loadedBatchBuilder,
      RifRecordEvent<?> rifRecordEvent) {
    RifRecordBase record = rifRecordEvent.getRecord();

    // Nothing to do here unless it's a Beneficiary record.
    if (!(record instanceof Beneficiary)) {
      return;
    }

    Beneficiary newBeneficiaryRecord = (Beneficiary) record;
    Optional<Beneficiary> oldBeneficiaryRecord = Optional.empty();

    /*
     * Grab the previous/current version of the Beneficiary (if any, as it exists in the
     * database before applying the specified RifRecordEvent).
     */
    if (rifRecordEvent.getRecordAction() == RecordAction.UPDATE) {
      /*
       * FIXME We need to enforce a new invariant on the incoming RIF files: no repeats of same
       * record/PK in same RIF file allowed. Otherwise, we're running the risk of data race bugs and
       * out-of-order application due to the asynchronous nature of this processing.
       */
      CriteriaBuilder builder = entityManager.getCriteriaBuilder();
      CriteriaQuery<Beneficiary> criteria = builder.createQuery(Beneficiary.class);
      Root<Beneficiary> root = criteria.from(Beneficiary.class);
      root.fetch(Beneficiary_.beneficiaryMonthlys, JoinType.LEFT);
      criteria.select(root);
      criteria.where(
          builder.equal(
              root.get(Beneficiary_.beneficiaryId), newBeneficiaryRecord.getBeneficiaryId()));

      oldBeneficiaryRecord =
          Optional.ofNullable(entityManager.createQuery(criteria).getSingleResult());
    }

    /*
     * Ensure that hashes are added for the secrets in bene records (HICNs and MBIs). When the
     * secret value hasn't changed (for UPDATE records), we copy the old hash over, to avoid
     * unnecessary hashing. Otherwise, we can waste hours recomputing hashes that don't need
     * recomputing.
     */
    if (oldBeneficiaryRecord.isPresent()
        && Objects.equals(
            newBeneficiaryRecord.getHicnUnhashed(), oldBeneficiaryRecord.get().getHicnUnhashed())) {
      newBeneficiaryRecord.setHicn(oldBeneficiaryRecord.get().getHicn());
    } else {
      hashBeneficiaryHicn(rifRecordEvent);
    }
    if (oldBeneficiaryRecord.isPresent()
        && Objects.equals(
            newBeneficiaryRecord.getMedicareBeneficiaryId(),
            oldBeneficiaryRecord.get().getMedicareBeneficiaryId())) {
      newBeneficiaryRecord.setMbiHash(oldBeneficiaryRecord.get().getMbiHash());
    } else {
      hashBeneficiaryMbi(rifRecordEvent);
    }

    if (rifRecordEvent.getRecordAction() == RecordAction.UPDATE) {
      /*
       * When beneficiaries are updated, we need to be careful to capture their current/previous
       * state as a BeneficiaryHistory record. (Note: this has to be done AFTER the secret hashes
       * have been updated, as per above.
       */
      updateBeneficiaryHistory(
          entityManager,
          newBeneficiaryRecord,
          oldBeneficiaryRecord,
          loadedBatchBuilder.getTimestamp());
    }
    updateBeneficiaryMonthly(newBeneficiaryRecord, oldBeneficiaryRecord);
  }

  /**
   * Ensures that a {@link BeneficiaryMonthly} record is created or updated for the specified {@link
   * Beneficiary}, if that {@link Beneficiary} already exists and is just being updated.
   *
   * @param newBeneficiaryRecord the {@link Beneficiary} record being processed
   * @param oldBeneficiaryRecord the previous/current version of the {@link Beneficiary} (as it
   *     exists in the database before applying the specified {@link RifRecordEvent})
   */
  private static void updateBeneficiaryMonthly(
      Beneficiary newBeneficiaryRecord, Optional<Beneficiary> oldBeneficiaryRecord) {

    if (newBeneficiaryRecord.getBeneEnrollmentReferenceYear().isPresent()) {

      int year = newBeneficiaryRecord.getBeneEnrollmentReferenceYear().get().intValue();
      List<BeneficiaryMonthly> currentYearBeneficiaryMonthly = new ArrayList<BeneficiaryMonthly>();

      BeneficiaryMonthly beneficiaryMonthly =
          getBeneficiaryMonthly(
              newBeneficiaryRecord,
              LocalDate.of(year, 1, 1),
              newBeneficiaryRecord.getEntitlementBuyInJanInd(),
              newBeneficiaryRecord.getFipsStateCntyJanCode(),
              newBeneficiaryRecord.getHmoIndicatorJanInd(),
              newBeneficiaryRecord.getMedicaidDualEligibilityJanCode(),
              newBeneficiaryRecord.getMedicareStatusJanCode(),
              newBeneficiaryRecord.getPartCContractNumberJanId(),
              newBeneficiaryRecord.getPartCPbpNumberJanId(),
              newBeneficiaryRecord.getPartCPlanTypeJanCode(),
              newBeneficiaryRecord.getPartDContractNumberJanId(),
              newBeneficiaryRecord.getPartDLowIncomeCostShareGroupJanCode(),
              newBeneficiaryRecord.getPartDPbpNumberJanId(),
              newBeneficiaryRecord.getPartDRetireeDrugSubsidyJanInd(),
              newBeneficiaryRecord.getPartDSegmentNumberJanId());

      if (beneficiaryMonthly != null) {
        currentYearBeneficiaryMonthly.add(beneficiaryMonthly);
      }

      beneficiaryMonthly =
          getBeneficiaryMonthly(
              newBeneficiaryRecord,
              LocalDate.of(year, 2, 1),
              newBeneficiaryRecord.getEntitlementBuyInFebInd(),
              newBeneficiaryRecord.getFipsStateCntyFebCode(),
              newBeneficiaryRecord.getHmoIndicatorFebInd(),
              newBeneficiaryRecord.getMedicaidDualEligibilityFebCode(),
              newBeneficiaryRecord.getMedicareStatusFebCode(),
              newBeneficiaryRecord.getPartCContractNumberFebId(),
              newBeneficiaryRecord.getPartCPbpNumberFebId(),
              newBeneficiaryRecord.getPartCPlanTypeFebCode(),
              newBeneficiaryRecord.getPartDContractNumberFebId(),
              newBeneficiaryRecord.getPartDLowIncomeCostShareGroupFebCode(),
              newBeneficiaryRecord.getPartDPbpNumberFebId(),
              newBeneficiaryRecord.getPartDRetireeDrugSubsidyFebInd(),
              newBeneficiaryRecord.getPartDSegmentNumberFebId());

      if (beneficiaryMonthly != null) {
        currentYearBeneficiaryMonthly.add(beneficiaryMonthly);
      }

      beneficiaryMonthly =
          getBeneficiaryMonthly(
              newBeneficiaryRecord,
              LocalDate.of(year, 3, 1),
              newBeneficiaryRecord.getEntitlementBuyInMarInd(),
              newBeneficiaryRecord.getFipsStateCntyMarCode(),
              newBeneficiaryRecord.getHmoIndicatorMarInd(),
              newBeneficiaryRecord.getMedicaidDualEligibilityMarCode(),
              newBeneficiaryRecord.getMedicareStatusMarCode(),
              newBeneficiaryRecord.getPartCContractNumberMarId(),
              newBeneficiaryRecord.getPartCPbpNumberMarId(),
              newBeneficiaryRecord.getPartCPlanTypeMarCode(),
              newBeneficiaryRecord.getPartDContractNumberMarId(),
              newBeneficiaryRecord.getPartDLowIncomeCostShareGroupMarCode(),
              newBeneficiaryRecord.getPartDPbpNumberMarId(),
              newBeneficiaryRecord.getPartDRetireeDrugSubsidyMarInd(),
              newBeneficiaryRecord.getPartDSegmentNumberMarId());

      if (beneficiaryMonthly != null) {
        currentYearBeneficiaryMonthly.add(beneficiaryMonthly);
      }

      beneficiaryMonthly =
          getBeneficiaryMonthly(
              newBeneficiaryRecord,
              LocalDate.of(year, 4, 1),
              newBeneficiaryRecord.getEntitlementBuyInAprInd(),
              newBeneficiaryRecord.getFipsStateCntyAprCode(),
              newBeneficiaryRecord.getHmoIndicatorAprInd(),
              newBeneficiaryRecord.getMedicaidDualEligibilityAprCode(),
              newBeneficiaryRecord.getMedicareStatusAprCode(),
              newBeneficiaryRecord.getPartCContractNumberAprId(),
              newBeneficiaryRecord.getPartCPbpNumberAprId(),
              newBeneficiaryRecord.getPartCPlanTypeAprCode(),
              newBeneficiaryRecord.getPartDContractNumberAprId(),
              newBeneficiaryRecord.getPartDLowIncomeCostShareGroupAprCode(),
              newBeneficiaryRecord.getPartDPbpNumberAprId(),
              newBeneficiaryRecord.getPartDRetireeDrugSubsidyAprInd(),
              newBeneficiaryRecord.getPartDSegmentNumberAprId());

      if (beneficiaryMonthly != null) {
        currentYearBeneficiaryMonthly.add(beneficiaryMonthly);
      }

      beneficiaryMonthly =
          getBeneficiaryMonthly(
              newBeneficiaryRecord,
              LocalDate.of(year, 5, 1),
              newBeneficiaryRecord.getEntitlementBuyInMayInd(),
              newBeneficiaryRecord.getFipsStateCntyMayCode(),
              newBeneficiaryRecord.getHmoIndicatorMayInd(),
              newBeneficiaryRecord.getMedicaidDualEligibilityMayCode(),
              newBeneficiaryRecord.getMedicareStatusMayCode(),
              newBeneficiaryRecord.getPartCContractNumberMayId(),
              newBeneficiaryRecord.getPartCPbpNumberMayId(),
              newBeneficiaryRecord.getPartCPlanTypeMayCode(),
              newBeneficiaryRecord.getPartDContractNumberMayId(),
              newBeneficiaryRecord.getPartDLowIncomeCostShareGroupMayCode(),
              newBeneficiaryRecord.getPartDPbpNumberMayId(),
              newBeneficiaryRecord.getPartDRetireeDrugSubsidyMayInd(),
              newBeneficiaryRecord.getPartDSegmentNumberMayId());

      if (beneficiaryMonthly != null) {
        currentYearBeneficiaryMonthly.add(beneficiaryMonthly);
      }
      beneficiaryMonthly =
          getBeneficiaryMonthly(
              newBeneficiaryRecord,
              LocalDate.of(year, 6, 1),
              newBeneficiaryRecord.getEntitlementBuyInJunInd(),
              newBeneficiaryRecord.getFipsStateCntyJunCode(),
              newBeneficiaryRecord.getHmoIndicatorJunInd(),
              newBeneficiaryRecord.getMedicaidDualEligibilityJunCode(),
              newBeneficiaryRecord.getMedicareStatusJunCode(),
              newBeneficiaryRecord.getPartCContractNumberJunId(),
              newBeneficiaryRecord.getPartCPbpNumberJunId(),
              newBeneficiaryRecord.getPartCPlanTypeJunCode(),
              newBeneficiaryRecord.getPartDContractNumberJunId(),
              newBeneficiaryRecord.getPartDLowIncomeCostShareGroupJunCode(),
              newBeneficiaryRecord.getPartDPbpNumberJunId(),
              newBeneficiaryRecord.getPartDRetireeDrugSubsidyJunInd(),
              newBeneficiaryRecord.getPartDSegmentNumberJunId());

      if (beneficiaryMonthly != null) {
        currentYearBeneficiaryMonthly.add(beneficiaryMonthly);
      }

      beneficiaryMonthly =
          getBeneficiaryMonthly(
              newBeneficiaryRecord,
              LocalDate.of(year, 7, 1),
              newBeneficiaryRecord.getEntitlementBuyInJulInd(),
              newBeneficiaryRecord.getFipsStateCntyJulCode(),
              newBeneficiaryRecord.getHmoIndicatorJulInd(),
              newBeneficiaryRecord.getMedicaidDualEligibilityJulCode(),
              newBeneficiaryRecord.getMedicareStatusJulCode(),
              newBeneficiaryRecord.getPartCContractNumberJulId(),
              newBeneficiaryRecord.getPartCPbpNumberJulId(),
              newBeneficiaryRecord.getPartCPlanTypeJulCode(),
              newBeneficiaryRecord.getPartDContractNumberJulId(),
              newBeneficiaryRecord.getPartDLowIncomeCostShareGroupJulCode(),
              newBeneficiaryRecord.getPartDPbpNumberJulId(),
              newBeneficiaryRecord.getPartDRetireeDrugSubsidyJulInd(),
              newBeneficiaryRecord.getPartDSegmentNumberJulId());

      if (beneficiaryMonthly != null) {
        currentYearBeneficiaryMonthly.add(beneficiaryMonthly);
      }

      beneficiaryMonthly =
          getBeneficiaryMonthly(
              newBeneficiaryRecord,
              LocalDate.of(year, 8, 1),
              newBeneficiaryRecord.getEntitlementBuyInAugInd(),
              newBeneficiaryRecord.getFipsStateCntyAugCode(),
              newBeneficiaryRecord.getHmoIndicatorAugInd(),
              newBeneficiaryRecord.getMedicaidDualEligibilityAugCode(),
              newBeneficiaryRecord.getMedicareStatusAugCode(),
              newBeneficiaryRecord.getPartCContractNumberAugId(),
              newBeneficiaryRecord.getPartCPbpNumberAugId(),
              newBeneficiaryRecord.getPartCPlanTypeAugCode(),
              newBeneficiaryRecord.getPartDContractNumberAugId(),
              newBeneficiaryRecord.getPartDLowIncomeCostShareGroupAugCode(),
              newBeneficiaryRecord.getPartDPbpNumberAugId(),
              newBeneficiaryRecord.getPartDRetireeDrugSubsidyAugInd(),
              newBeneficiaryRecord.getPartDSegmentNumberAugId());

      if (beneficiaryMonthly != null) {
        currentYearBeneficiaryMonthly.add(beneficiaryMonthly);
      }

      beneficiaryMonthly =
          getBeneficiaryMonthly(
              newBeneficiaryRecord,
              LocalDate.of(year, 9, 1),
              newBeneficiaryRecord.getEntitlementBuyInSeptInd(),
              newBeneficiaryRecord.getFipsStateCntySeptCode(),
              newBeneficiaryRecord.getHmoIndicatorSeptInd(),
              newBeneficiaryRecord.getMedicaidDualEligibilitySeptCode(),
              newBeneficiaryRecord.getMedicareStatusSeptCode(),
              newBeneficiaryRecord.getPartCContractNumberSeptId(),
              newBeneficiaryRecord.getPartCPbpNumberSeptId(),
              newBeneficiaryRecord.getPartCPlanTypeSeptCode(),
              newBeneficiaryRecord.getPartDContractNumberSeptId(),
              newBeneficiaryRecord.getPartDLowIncomeCostShareGroupSeptCode(),
              newBeneficiaryRecord.getPartDPbpNumberSeptId(),
              newBeneficiaryRecord.getPartDRetireeDrugSubsidySeptInd(),
              newBeneficiaryRecord.getPartDSegmentNumberSeptId());

      if (beneficiaryMonthly != null) {
        currentYearBeneficiaryMonthly.add(beneficiaryMonthly);
      }

      beneficiaryMonthly =
          getBeneficiaryMonthly(
              newBeneficiaryRecord,
              LocalDate.of(year, 10, 1),
              newBeneficiaryRecord.getEntitlementBuyInOctInd(),
              newBeneficiaryRecord.getFipsStateCntyOctCode(),
              newBeneficiaryRecord.getHmoIndicatorOctInd(),
              newBeneficiaryRecord.getMedicaidDualEligibilityOctCode(),
              newBeneficiaryRecord.getMedicareStatusOctCode(),
              newBeneficiaryRecord.getPartCContractNumberOctId(),
              newBeneficiaryRecord.getPartCPbpNumberOctId(),
              newBeneficiaryRecord.getPartCPlanTypeOctCode(),
              newBeneficiaryRecord.getPartDContractNumberOctId(),
              newBeneficiaryRecord.getPartDLowIncomeCostShareGroupOctCode(),
              newBeneficiaryRecord.getPartDPbpNumberOctId(),
              newBeneficiaryRecord.getPartDRetireeDrugSubsidyOctInd(),
              newBeneficiaryRecord.getPartDSegmentNumberOctId());

      if (beneficiaryMonthly != null) {
        currentYearBeneficiaryMonthly.add(beneficiaryMonthly);
      }

      beneficiaryMonthly =
          getBeneficiaryMonthly(
              newBeneficiaryRecord,
              LocalDate.of(year, 11, 1),
              newBeneficiaryRecord.getEntitlementBuyInNovInd(),
              newBeneficiaryRecord.getFipsStateCntyNovCode(),
              newBeneficiaryRecord.getHmoIndicatorNovInd(),
              newBeneficiaryRecord.getMedicaidDualEligibilityNovCode(),
              newBeneficiaryRecord.getMedicareStatusNovCode(),
              newBeneficiaryRecord.getPartCContractNumberNovId(),
              newBeneficiaryRecord.getPartCPbpNumberNovId(),
              newBeneficiaryRecord.getPartCPlanTypeNovCode(),
              newBeneficiaryRecord.getPartDContractNumberNovId(),
              newBeneficiaryRecord.getPartDLowIncomeCostShareGroupNovCode(),
              newBeneficiaryRecord.getPartDPbpNumberNovId(),
              newBeneficiaryRecord.getPartDRetireeDrugSubsidyNovInd(),
              newBeneficiaryRecord.getPartDSegmentNumberNovId());

      if (beneficiaryMonthly != null) {
        currentYearBeneficiaryMonthly.add(beneficiaryMonthly);
      }

      beneficiaryMonthly =
          getBeneficiaryMonthly(
              newBeneficiaryRecord,
              LocalDate.of(year, 12, 1),
              newBeneficiaryRecord.getEntitlementBuyInDecInd(),
              newBeneficiaryRecord.getFipsStateCntyDecCode(),
              newBeneficiaryRecord.getHmoIndicatorDecInd(),
              newBeneficiaryRecord.getMedicaidDualEligibilityDecCode(),
              newBeneficiaryRecord.getMedicareStatusDecCode(),
              newBeneficiaryRecord.getPartCContractNumberDecId(),
              newBeneficiaryRecord.getPartCPbpNumberDecId(),
              newBeneficiaryRecord.getPartCPlanTypeDecCode(),
              newBeneficiaryRecord.getPartDContractNumberDecId(),
              newBeneficiaryRecord.getPartDLowIncomeCostShareGroupDecCode(),
              newBeneficiaryRecord.getPartDPbpNumberDecId(),
              newBeneficiaryRecord.getPartDRetireeDrugSubsidyDecInd(),
              newBeneficiaryRecord.getPartDSegmentNumberDecId());

      if (beneficiaryMonthly != null) {
        currentYearBeneficiaryMonthly.add(beneficiaryMonthly);
      }

      if (currentYearBeneficiaryMonthly.size() > 0) {
        List<BeneficiaryMonthly> currentBeneficiaryMonthlyWithUpdates;

        if (oldBeneficiaryRecord.isPresent()
            && oldBeneficiaryRecord.get().getBeneficiaryMonthlys().size() > 0) {
          currentBeneficiaryMonthlyWithUpdates =
              oldBeneficiaryRecord.get().getBeneficiaryMonthlys();
          List<BeneficiaryMonthly> currentYearBeneficiaryMonthlyPrevious =
              oldBeneficiaryRecord.get().getBeneficiaryMonthlys().stream()
                  .filter(e -> year == e.getYearMonth().getYear())
                  .collect(Collectors.toList());

          for (BeneficiaryMonthly previousEnrollment : currentYearBeneficiaryMonthlyPrevious) {
            currentBeneficiaryMonthlyWithUpdates.remove(previousEnrollment);
          }
        } else {
          currentBeneficiaryMonthlyWithUpdates = new LinkedList<BeneficiaryMonthly>();
        }

        currentBeneficiaryMonthlyWithUpdates.addAll(currentYearBeneficiaryMonthly);
        newBeneficiaryRecord.setBeneficiaryMonthlys(currentBeneficiaryMonthlyWithUpdates);
      }
    }
  }

  /**
   * Ensures that a {@link BeneficiaryHistory} record is created for the specified {@link
   * Beneficiary}, if that {@link Beneficiary} already exists and is just being updated.
   *
   * @param entityManager the {@link EntityManager} to use
   * @param newBeneficiaryRecord the {@link Beneficiary} record being processed
   * @param oldBeneficiaryRecord the previous/current version of the {@link Beneficiary} (as it
   *     exists in the database before applying the specified {@link RifRecordEvent})
   * @param batchTimestamp the timestamp of the batch
   */
  private static void updateBeneficiaryHistory(
      EntityManager entityManager,
      Beneficiary newBeneficiaryRecord,
      Optional<Beneficiary> oldBeneficiaryRecord,
      Instant batchTimestamp) {
    if (oldBeneficiaryRecord.isPresent()
        && !isBeneficiaryHistoryEqual(newBeneficiaryRecord, oldBeneficiaryRecord.get())) {
      BeneficiaryHistory oldBeneCopy = new BeneficiaryHistory();
      oldBeneCopy.setBeneficiaryId(oldBeneficiaryRecord.get().getBeneficiaryId());
      oldBeneCopy.setBirthDate(oldBeneficiaryRecord.get().getBirthDate());
      oldBeneCopy.setHicn(oldBeneficiaryRecord.get().getHicn());
      oldBeneCopy.setHicnUnhashed(oldBeneficiaryRecord.get().getHicnUnhashed());
      oldBeneCopy.setSex(oldBeneficiaryRecord.get().getSex());
      oldBeneCopy.setMedicareBeneficiaryId(oldBeneficiaryRecord.get().getMedicareBeneficiaryId());
      oldBeneCopy.setMbiHash(oldBeneficiaryRecord.get().getMbiHash());
      oldBeneCopy.setMbiEffectiveDate(oldBeneficiaryRecord.get().getMbiEffectiveDate());
      oldBeneCopy.setMbiObsoleteDate(oldBeneficiaryRecord.get().getMbiObsoleteDate());
      oldBeneCopy.setXrefGroupId(oldBeneficiaryRecord.get().getXrefGroupId());
      oldBeneCopy.setXrefSwitch(oldBeneficiaryRecord.get().getXrefSwitch());
      oldBeneCopy.setLastUpdated(Optional.of(batchTimestamp));

      entityManager.persist(oldBeneCopy);
    }
  }

  /**
   * Ensures that a {@link Beneficiary} records for old vs. new Benificiarie record(s) are equal.
   * Implemented using a 'fail fast' paradigm; it is best to put those tests that have a higher
   * possibility of change at the beginning (i.e., it's hard to change your DOB)
   *
   * @param newBeneficiaryRecord the {@link Beneficiary} new record being processed
   * @param oldBeneficiaryRecord the {@link Beneficiary} old record that was processed
   * @return {@code true} if the two beneficiary records are equal
   */
  static boolean isBeneficiaryHistoryEqual(
      Beneficiary newBeneficiaryRecord, Beneficiary oldBeneficiaryRecord) {
    if (!Objects.equals(
        newBeneficiaryRecord.getMedicareBeneficiaryId(),
        oldBeneficiaryRecord.getMedicareBeneficiaryId())) {
      return false;
    }
    if (!Objects.equals(newBeneficiaryRecord.getHicn(), oldBeneficiaryRecord.getHicn())) {
      return false;
    }
    if (!Objects.equals(
        newBeneficiaryRecord.getHicnUnhashed(), oldBeneficiaryRecord.getHicnUnhashed())) {
      return false;
    }
    if (!Objects.equals(newBeneficiaryRecord.getMbiHash(), oldBeneficiaryRecord.getMbiHash())) {
      return false;
    }
    if (!Objects.equals(
        newBeneficiaryRecord.getMbiEffectiveDate(), oldBeneficiaryRecord.getMbiEffectiveDate())) {
      return false;
    }

    if (!Objects.equals(
        newBeneficiaryRecord.getXrefGroupId(), oldBeneficiaryRecord.getXrefGroupId())) {
      return false;
    }

    if (!Objects.equals(
        newBeneficiaryRecord.getXrefSwitch(), oldBeneficiaryRecord.getXrefSwitch())) {
      return false;
    }

    // BFD-1308: removed check for getMbiObsoleteDate; this value is derived from the Beneficiary
    // EFCTV_END_DT but that field has never had a value in the beneficiary.rif file received from
    // CCW.

    // last but not least...these probably won't ever change
    return (Objects.equals(newBeneficiaryRecord.getBirthDate(), oldBeneficiaryRecord.getBirthDate())
        && Objects.equals(newBeneficiaryRecord.getSex(), oldBeneficiaryRecord.getSex()));
  }

  /**
   * Creates a beneficiary monthly data with the supplied data.
   *
   * <p>TODO: Rename this method; it's really creating not getting something
   *
   * @param parentBeneficiary the parent beneficiary
   * @param yearMonth the year month
   * @param entitlementBuyInInd the entitlement buy in ind
   * @param fipsStateCntyCode the fips state cnty code
   * @param hmoIndicatorInd the hmo indicator ind
   * @param medicaidDualEligibilityCode the medicaid dual eligibility code
   * @param medicareStatusCode the medicare status code
   * @param partCContractNumberId the part c contract number id
   * @param partCPbpNumberId the part c pbp number id
   * @param partCPlanTypeCode the part c plan type code
   * @param partDContractNumberId the part d contract number id
   * @param partDLowIncomeCostShareGroupCode the part d low income cost share group code
   * @param partDPbpNumberId the part d pbp number id
   * @param partDRetireeDrugSubsidyInd the part d retiree drug subsidy ind
   * @param partDSegmentNumberId the part d segment number id
   * @return the beneficiary monthly object, or {@code null}
   */
  public static BeneficiaryMonthly getBeneficiaryMonthly(
      Beneficiary parentBeneficiary,
      LocalDate yearMonth,
      Optional<Character> entitlementBuyInInd,
      Optional<String> fipsStateCntyCode,
      Optional<Character> hmoIndicatorInd,
      Optional<String> medicaidDualEligibilityCode,
      Optional<String> medicareStatusCode,
      Optional<String> partCContractNumberId,
      Optional<String> partCPbpNumberId,
      Optional<String> partCPlanTypeCode,
      Optional<String> partDContractNumberId,
      Optional<String> partDLowIncomeCostShareGroupCode,
      Optional<String> partDPbpNumberId,
      Optional<Character> partDRetireeDrugSubsidyInd,
      Optional<String> partDSegmentNumberId) {

    BeneficiaryMonthly beneficiaryMonthly = null;

    if (entitlementBuyInInd.isPresent()
        || fipsStateCntyCode.isPresent()
        || hmoIndicatorInd.isPresent()
        || medicaidDualEligibilityCode.isPresent()
        || medicareStatusCode.isPresent()
        || partCContractNumberId.isPresent()
        || partCPbpNumberId.isPresent()
        || partCPlanTypeCode.isPresent()
        || partDContractNumberId.isPresent()
        || partDLowIncomeCostShareGroupCode.isPresent()
        || partDPbpNumberId.isPresent()
        || partDRetireeDrugSubsidyInd.isPresent()
        || partDSegmentNumberId.isPresent()) {
      beneficiaryMonthly = new BeneficiaryMonthly();
      beneficiaryMonthly.setParentBeneficiary(parentBeneficiary);
      beneficiaryMonthly.setYearMonth(yearMonth);
      beneficiaryMonthly.setEntitlementBuyInInd(entitlementBuyInInd);
      beneficiaryMonthly.setFipsStateCntyCode(fipsStateCntyCode);
      beneficiaryMonthly.setHmoIndicatorInd(hmoIndicatorInd);
      beneficiaryMonthly.setMedicaidDualEligibilityCode(medicaidDualEligibilityCode);
      beneficiaryMonthly.setMedicareStatusCode(medicareStatusCode);
      beneficiaryMonthly.setPartCContractNumberId(partCContractNumberId);
      beneficiaryMonthly.setPartCPbpNumberId(partCPbpNumberId);
      beneficiaryMonthly.setPartCPlanTypeCode(partCPlanTypeCode);
      beneficiaryMonthly.setPartDContractNumberId(partDContractNumberId);
      beneficiaryMonthly.setPartDLowIncomeCostShareGroupCode(partDLowIncomeCostShareGroupCode);
      beneficiaryMonthly.setPartDPbpNumberId(partDPbpNumberId);
      beneficiaryMonthly.setPartDRetireeDrugSubsidyInd(partDRetireeDrugSubsidyInd);
      beneficiaryMonthly.setPartDSegmentNumberId(partDSegmentNumberId);
    }

    return beneficiaryMonthly;
  }

  /**
   * Insert the LoadedFile into the database.
   *
   * @param fileEvent to base this new LoadedFile
   * @return the loadedFileId of the new LoadedFile record
   */
  private long insertLoadedFile(RifFileEvent fileEvent) {
    if (fileEvent == null || fileEvent.getFile().getFileType() == null) {
      throw new IllegalArgumentException();
    }

    final LoadedFile loadedFile = new LoadedFile();
    RifFile.RecordId dataFileId = fileEvent.getFile().getRecordId();
    if (dataFileId != null) {
      loadedFile.setS3ManifestId(dataFileId.manifestId());
      loadedFile.setS3FileIndex(dataFileId.index());
    }
    loadedFile.setRifType(fileEvent.getFile().getFileType().toString());
    loadedFile.setCreated(Instant.now());

    try (TransactionManager transactionManager =
        new TransactionManager(appState.getEntityManagerFactory())) {
      long loadedFileId =
          transactionManager.executeFunction(
              entityManager -> {
                // Insert the passed in loaded file
                entityManager.persist(loadedFile);
                LOGGER.info(
                    "Inserting LoadedFile {} of type {} created at {}",
                    loadedFile.getLoadedFileId(),
                    loadedFile.getRifType(),
                    loadedFile.getCreated());

                return loadedFile.getLoadedFileId();
              });
      return loadedFileId;
    }
  }

  /** Trim the LoadedFiles and LoadedBatches tables if necessary. */
  private void trimLoadedFiles() {
    EntityManager em = appState.getEntityManagerFactory().createEntityManager();
    EntityTransaction txn = null;
    try {
      txn = em.getTransaction();
      txn.begin();
      final Instant oldDate = Instant.now().minus(MAX_FILE_AGE_DAYS);

      em.clear(); // Must be done before JPQL statements
      em.flush();
      List<Long> oldIds =
          em.createQuery(
                  "select f.loadedFileId from LoadedFile f where created < :oldDate", Long.class)
              .setParameter("oldDate", oldDate)
              .getResultList();

      if (oldIds.size() > 0) {
        LOGGER.info("Deleting old files: {}", oldIds.size());
        em.createQuery("delete from LoadedBatch where loadedFileId in :ids")
            .setParameter("ids", oldIds)
            .executeUpdate();
        em.createQuery("delete from LoadedFile where loadedFileId in :ids")
            .setParameter("ids", oldIds)
            .executeUpdate();
        txn.commit();
      } else {
        txn.rollback();
      }
    } finally {
      if (em != null && em.isOpen()) {
        if (txn != null && txn.isActive()) {
          txn.rollback();
        }
        em.close();
      }
    }
  }

  /** Computes and logs a count for all record types. */
  private void logRecordCounts() {
    if (!LOGGER_RECORD_COUNTS.isDebugEnabled()) return;

    Timer.Context timerCounting =
        appState
            .getMetrics()
            .timer(MetricRegistry.name(getClass().getSimpleName(), "recordCounting"))
            .time();
    LOGGER.debug("Counting records...");
    String entityTypeCounts =
        appState.getEntityManagerFactory().getMetamodel().getManagedTypes().stream()
            .map(t -> t.getJavaType())
            .sorted(Comparator.comparing(Class::getName))
            .map(
                t -> {
                  long entityTypeRecordCount = queryForEntityCount(t);
                  return String.format("%s: %d", t.getSimpleName(), entityTypeRecordCount);
                })
            .collect(Collectors.joining(", "));
    LOGGER.debug("Record counts by entity type: '{}'.", entityTypeCounts);
    timerCounting.stop();
  }

  /**
   * Gets a count of the number of instances of the specified JPA {@link Entity} type that are
   * currently in the database.
   *
   * @param entityType the JPA {@link Entity} type to count instances of
   * @return the number of instances in the db
   */
  private long queryForEntityCount(Class<?> entityType) {
    try (TransactionManager transactionManager =
        new TransactionManager(appState.getEntityManagerFactory())) {
      return transactionManager.executeFunction(
          entityManager -> {
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
            criteriaQuery.select(criteriaBuilder.count(criteriaQuery.from(entityType)));

            return entityManager.createQuery(criteriaQuery).getSingleResult();
          });
    }
  }

  /**
   * For {@link RifRecordEvent}s where the {@link RifRecordEvent#getRecord()} is a {@link
   * Beneficiary}, switches the {@link Beneficiary#getHicn()} property to a cryptographic hash of
   * its current value. This is done for security purposes, and the Blue Button API frontend
   * applications know how to compute the exact same hash, which allows the two halves of the system
   * to interoperate.
   *
   * <p>All other {@link RifRecordEvent}s are left unmodified.
   *
   * @param rifRecordEvent the {@link RifRecordEvent} to (possibly) modify
   */
  private void hashBeneficiaryHicn(RifRecordEvent<?> rifRecordEvent) {
    if (rifRecordEvent.getFileEvent().getFile().getFileType() != RifFileType.BENEFICIARY) return;

    Timer.Context timerHashing =
        rifRecordEvent
            .getFileEvent()
            .getEventMetrics()
            .timer(MetricRegistry.name(getClass().getSimpleName(), "hicnsHashed"))
            .time();

    Beneficiary beneficiary = (Beneficiary) rifRecordEvent.getRecord();
    if (beneficiary.getHicnUnhashed().isPresent()) {
      String hicnHash = computeHicnHash(idHasher, beneficiary.getHicnUnhashed().get());
      beneficiary.setHicn(hicnHash);
    } else {
      beneficiary.setHicn(null);
    }

    timerHashing.stop();
  }

  /**
   * For {@link RifRecordEvent}s where the {@link RifRecordEvent#getRecord()} is a {@link
   * Beneficiary}, computes the {@link Beneficiary#getMedicareBeneficiaryId()} ()} property to a
   * cryptographic hash of its current value. This is done for security purposes, and the Blue
   * Button API frontend applications know how to compute the exact same hash, which allows the two
   * halves of the system to interoperate.
   *
   * <p>All other {@link RifRecordEvent}s are left unmodified.
   *
   * @param rifRecordEvent the {@link RifRecordEvent} to (possibly) modify
   */
  private void hashBeneficiaryMbi(RifRecordEvent<?> rifRecordEvent) {
    if (rifRecordEvent.getFileEvent().getFile().getFileType() != RifFileType.BENEFICIARY) return;

    Timer.Context timerHashing =
        rifRecordEvent
            .getFileEvent()
            .getEventMetrics()
            .timer(MetricRegistry.name(getClass().getSimpleName(), "mbisHashed"))
            .time();

    Beneficiary beneficiary = (Beneficiary) rifRecordEvent.getRecord();
    if (beneficiary.getMedicareBeneficiaryId().isPresent()) {
      String mbiHash = computeMbiHash(idHasher, beneficiary.getMedicareBeneficiaryId().get());
      beneficiary.setMbiHash(Optional.of(mbiHash));
    } else {
      beneficiary.setMbiHash(Optional.empty());
    }

    timerHashing.stop();
  }

  /**
   * For {@link RifRecordEvent}s where the {@link RifRecordEvent#getRecord()} is a {@link
   * BeneficiaryHistory}, switches the {@link BeneficiaryHistory#getHicn()} property to a
   * cryptographic hash of its current value. This is done for security purposes, and the Blue
   * Button API frontend applications know how to compute the exact same hash, which allows the two
   * halves of the system to interoperate.
   *
   * <p>All other {@link RifRecordEvent}s are left unmodified.
   *
   * @param rifRecordEvent the {@link RifRecordEvent} to (possibly) modify
   */
  private void hashBeneficiaryHistoryHicn(RifRecordEvent<?> rifRecordEvent) {
    if (rifRecordEvent.getFileEvent().getFile().getFileType() != RifFileType.BENEFICIARY_HISTORY)
      return;

    Timer.Context timerHashing =
        rifRecordEvent
            .getFileEvent()
            .getEventMetrics()
            .timer(MetricRegistry.name(getClass().getSimpleName(), "hicnsHashed"))
            .time();

    BeneficiaryHistory beneficiaryHistory = (BeneficiaryHistory) rifRecordEvent.getRecord();

    // set the unhashed Hicn
    beneficiaryHistory.setHicnUnhashed(Optional.of(beneficiaryHistory.getHicn()));

    // set the hashed Hicn
    beneficiaryHistory.setHicn(computeHicnHash(idHasher, beneficiaryHistory.getHicn()));

    timerHashing.stop();
  }

  /**
   * For {@link RifRecordEvent}s where the {@link RifRecordEvent#getRecord()} is a {@link
   * BeneficiaryHistory}, switches the {@link BeneficiaryHistory#getHicn()} property to a
   * cryptographic hash of its current value. This is done for security purposes, and the Blue
   * Button API frontend applications know how to compute the exact same hash, which allows the two
   * halves of the system to interoperate.
   *
   * <p>All other {@link RifRecordEvent}s are left unmodified.
   *
   * @param rifRecordEvent the {@link RifRecordEvent} to (possibly) modify
   */
  private void hashBeneficiaryHistoryMbi(RifRecordEvent<?> rifRecordEvent) {
    if (rifRecordEvent.getFileEvent().getFile().getFileType() != RifFileType.BENEFICIARY_HISTORY)
      return;

    Timer.Context timerHashing =
        rifRecordEvent
            .getFileEvent()
            .getEventMetrics()
            .timer(MetricRegistry.name(getClass().getSimpleName(), "mbisHashed"))
            .time();

    BeneficiaryHistory beneficiaryHistory = (BeneficiaryHistory) rifRecordEvent.getRecord();

    // set the hashed MBI
    beneficiaryHistory
        .getMedicareBeneficiaryId()
        .ifPresent(
            mbi -> {
              String mbiHash = computeMbiHash(idHasher, mbi);
              beneficiaryHistory.setMbiHash(Optional.of(mbiHash));
            });

    timerHashing.stop();
  }

  /**
   * Computes a one-way cryptographic hash of the specified HICN value. This is used as a secure
   * means of identifying Medicare beneficiaries between the Blue Button API frontend and backend
   * systems: the HICN is the only unique beneficiary identifier shared between those two systems.
   *
   * @param idHasher the {@link IdHasher} to use
   * @param hicn the Medicare beneficiary HICN to be hashed
   * @return a one-way cryptographic hash of the specified HICN value, exactly 64 characters long
   */
  static String computeHicnHash(IdHasher idHasher, String hicn) {
    return idHasher.computeIdentifierHash(hicn);
  }

  /**
   * Computes a one-way cryptographic hash of the specified MBI value.
   *
   * @param idHasher the {@link IdHasher} to use
   * @param mbi the Medicare beneficiary id to be hashed
   * @return a one-way cryptographic hash of the specified MBI value, exactly 64 characters long
   */
  static String computeMbiHash(IdHasher idHasher, String mbi) {
    return idHasher.computeIdentifierHash(mbi);
  }

  /** Enumerates the {@link RifLoader} record handling strategies. */
  private enum LoadStrategy {
    /** Represents if the inserts should be treated as updates if the unique keys already exist. */
    INSERT_IDEMPOTENT,
    /**
     * Represents if the inserts and updates should be strictly treated as labelled (meaning we blow
     * up if the unique constraints are violated).
     */
    INSERT_UPDATE_NON_IDEMPOTENT;
  }
}
