package gov.cms.bfd.pipeline.ccw.rif.load;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.zaxxer.hikari.pool.HikariProxyConnection;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.BeneficiaryCsvWriter;
import gov.cms.bfd.model.rif.BeneficiaryHistory;
import gov.cms.bfd.model.rif.BeneficiaryMonthly;
import gov.cms.bfd.model.rif.Beneficiary_;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.CarrierClaimCsvWriter;
import gov.cms.bfd.model.rif.CarrierClaimLine;
import gov.cms.bfd.model.rif.LoadedBatch;
import gov.cms.bfd.model.rif.LoadedBatchBuilder;
import gov.cms.bfd.model.rif.LoadedFile;
import gov.cms.bfd.model.rif.RecordAction;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileRecords;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.RifRecordBase;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.model.rif.SkippedRifRecord;
import gov.cms.bfd.model.rif.SkippedRifRecord.SkipReasonCode;
import gov.cms.bfd.model.rif.parse.RifParsingUtils;
import gov.cms.bfd.pipeline.ccw.rif.load.RifRecordLoadResult.LoadAction;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import gov.cms.bfd.pipeline.sharedutils.PipelineApplicationState;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Pushes CCW beneficiary and claims data from {@link RifRecordEvent}s into the Blue Button API's
 * database.
 */
public final class RifLoader {
  /**
   * The number of {@link RifRecordEvent}s that will be included in each processing batch. Note that
   * larger batch sizes mean that more {@link RifRecordEvent}s will be held in memory
   * simultaneously.
   */
  private static final int RECORD_BATCH_SIZE = 100;

  private static final Period MAX_FILE_AGE_DAYS = Period.ofDays(40);

  private static final Logger LOGGER = LoggerFactory.getLogger(RifLoader.class);
  private static final Logger LOGGER_RECORD_COUNTS =
      LoggerFactory.getLogger(RifLoader.class.getName() + ".recordCounts");

  private final LoadAppOptions options;
  private final IdHasher idHasher;
  private final PipelineApplicationState appState;

  /**
   * Constructs a new {@link RifLoader} instance.
   *
   * @param options the {@link LoadAppOptions} to use
   * @param appState the {@link PipelineApplicationState} to use
   */
  public RifLoader(LoadAppOptions options, PipelineApplicationState appState) {
    this.options = options;
    this.appState = appState;

    /*
     * We are re-using the same hash configuration for HICNs and MBIs so we only need one idHasher.
     */
    this.idHasher = new IdHasher(options.getIdHasherConfig());
  }

  /**
   * @param options the {@link LoadAppOptions} to use
   * @return the {@link BlockingThreadPoolExecutor} to use for asynchronous load tasks
   */
  private static BlockingThreadPoolExecutor createLoadExecutor(LoadAppOptions options) {
    /*
     * A 16 vCPU ETL server can handle 400 loader threads at less than 30%
     * CPU usage (once a steady state is hit). The biggest limit here is
     * what the DB will allow.
     */
    int threadPoolSize = options.getLoaderThreads();

    /*
     * It's tempting to think that a large queue will improve performance,
     * but in reality: nope. Once the ETL hits a steady state, the queue
     * will almost always be empty, so about all it accomplishes is
     * unnecessarily eating up a bunch of RAM when the ETL happens to be
     * running more slowly (for whatever reason).
     */
    int taskQueueSize = 10 * threadPoolSize;

    LOGGER.info(
        "Configured to load with '{}' threads, a queue of '{}', and a batch size of '{}'.",
        options.getLoaderThreads(),
        taskQueueSize,
        RECORD_BATCH_SIZE);

    /*
     * I feel like a hipster using "found" code like
     * BlockingThreadPoolExecutor: this really cool (and old) class supports
     * our use case beautifully. It hands out tasks to multiple consumers,
     * and allows a single producer to feed it, blocking that producer when
     * the task queue is full.
     */
    BlockingThreadPoolExecutor loadExecutor =
        new BlockingThreadPoolExecutor(threadPoolSize, taskQueueSize, 100, TimeUnit.MILLISECONDS);
    return loadExecutor;
  }

  /**
   * @param recordAction the {@link RecordAction} of the specific record being processed
   * @return the {@link LoadStrategy} that should be used for the record being processed
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
   * @return <code>true</code> if {@link #entityManagerFactory} is connected to a PostgreSQL
   *     database, <code>false</code> if it is not
   */
  private boolean isDatabasePostgreSql() {
    AtomicBoolean result = new AtomicBoolean(false);

    EntityManager entityManager = null;
    try {
      entityManager = appState.getEntityManagerFactory().createEntityManager();
      Session session = entityManager.unwrap(Session.class);
      session.doWork(
          new Work() {
            /** @see org.hibernate.jdbc.Work#execute(java.sql.Connection) */
            @Override
            public void execute(Connection connection) throws SQLException {
              String databaseName = connection.getMetaData().getDatabaseProductName();
              if (databaseName.equals("PostgreSQL")) result.set(true);
            }
          });
    } finally {
      if (entityManager != null) entityManager.close();
    }

    return result.get();
  }

  /**
   * Consumes the input {@link Stream} of {@link RifRecordEvent}s, pushing each {@link
   * RifRecordEvent}'s record to the database, and passing the result for each of those bundles to
   * the specified error handler and result handler, as appropriate.
   *
   * <p>This is a <a href=
   * "https://docs.oracle.com/javase/8/docs/api/java/util/stream/package-summary.html#StreamOps">
   * terminal operation</a>.
   *
   * @param dataToLoad the FHIR {@link RifRecordEvent}s to be loaded
   * @param errorHandler the {@link Consumer} to pass each error that occurs to (possibly one error
   *     per {@link RifRecordEvent}, if every input element fails to load), which will be run on the
   *     caller's thread
   * @param resultHandler the {@link Consumer} to pass each the {@link RifRecordLoadResult} for each
   *     of the successfully-processed input {@link RifRecordEvent}s, which will be run on the
   *     caller's thread
   */
  public void process(
      RifFileRecords dataToLoad,
      Consumer<Throwable> errorHandler,
      Consumer<RifRecordLoadResult> resultHandler) {
    BlockingThreadPoolExecutor loadExecutor = createLoadExecutor(options);

    MetricRegistry fileEventMetrics = dataToLoad.getSourceEvent().getEventMetrics();
    Timer.Context timerDataSetFile =
        appState
            .getMetrics()
            .timer(MetricRegistry.name(getClass().getSimpleName(), "dataSet", "file", "processed"))
            .time();
    LOGGER.info("Processing '{}'...", dataToLoad);

    dataToLoad
        .getSourceEvent()
        .getEventMetrics()
        .register(
            MetricRegistry.name(getClass().getSimpleName(), "loadExecutorService", "queueSize"),
            new Gauge<Integer>() {
              /** @see com.codahale.metrics.Gauge#getValue() */
              @Override
              public Integer getValue() {
                return loadExecutor.getQueue().size();
              }
            });
    dataToLoad
        .getSourceEvent()
        .getEventMetrics()
        .register(
            MetricRegistry.name(getClass().getSimpleName(), "loadExecutorService", "activeBatches"),
            new Gauge<Integer>() {
              /** @see com.codahale.metrics.Gauge#getValue() */
              @Override
              public Integer getValue() {
                return loadExecutor.getActiveCount();
              }
            });

    // Trim the LoadedFiles & LoadedBatches table
    trimLoadedFiles(errorHandler);

    // Insert a LoadedFiles entry
    final long loadedFileId = insertLoadedFile(dataToLoad.getSourceEvent(), errorHandler);
    if (loadedFileId < 0) {
      return; // Something went wrong, the error handler was called.
    }

    /*
     * Design history note: Initially, this function just returned a stream
     * of CompleteableFutures, which seems like the obvious choice.
     * Unfortunately, that ends up being rather hard to use correctly. Had
     * some tests that were misusing it and ended up unintentionally forcing
     * the processing back to being serial. Also, it leads to a ton of
     * copy-pasted code. Thus, we just return void, and instead accept
     * handlers that folks can do whatever they want with. It makes things
     * harder for the tests to inspect, but also ensures that the loading is
     * always run in a consistent manner.
     */

    try (PostgreSqlCopyInserter postgresBatch =
        new PostgreSqlCopyInserter(appState.getEntityManagerFactory(), fileEventMetrics)) {
      // Define the Consumer that will handle each batch.
      Consumer<List<RifRecordEvent<?>>> batchProcessor =
          recordsBatch -> {
            /*
             * Submit the RifRecordEvent for asynchronous processing. Note
             * that, due to the ExecutorService's configuration (see in
             * constructor), this will block if too many tasks are already
             * pending. That's desirable behavior, as it prevents
             * OutOfMemoryErrors.
             */
            processAsync(
                loadExecutor,
                recordsBatch,
                loadedFileId,
                postgresBatch,
                resultHandler,
                errorHandler);
          };

      // Collect records into batches and submit each to batchProcessor.
      if (RECORD_BATCH_SIZE > 1)
        BatchSpliterator.batches(dataToLoad.getRecords(), RECORD_BATCH_SIZE)
            .forEach(batchProcessor);
      else
        dataToLoad
            .getRecords()
            .map(
                record -> {
                  List<RifRecordEvent<?>> ittyBittyBatch = new LinkedList<>();
                  ittyBittyBatch.add(record);
                  return ittyBittyBatch;
                })
            .forEach(batchProcessor);

      // Wait for all submitted batches to complete.
      try {
        loadExecutor.shutdown();
        boolean terminatedSuccessfully = loadExecutor.awaitTermination(72, TimeUnit.HOURS);
        if (!terminatedSuccessfully)
          throw new IllegalStateException(
              String.format(
                  "%s failed to complete processing the records in time: '%s'.",
                  this.getClass().getSimpleName(), dataToLoad));
      } catch (InterruptedException e) {
        // Interrupts should not be used on this thread, so go boom.
        throw new RuntimeException(e);
      }

      // Submit the queued PostgreSQL COPY operations, if any.
      if (!postgresBatch.isEmpty()) {
        postgresBatch.submit();
      }
    }

    LOGGER.info("Processed '{}'.", dataToLoad);
    timerDataSetFile.stop();

    logRecordCounts();
  }

  /**
   * @param loadExecutor the {@link BlockingThreadPoolExecutor} to use for asynchronous load tasks
   * @param recordsBatch the {@link RifRecordEvent}s to process
   * @param loadedFileBuilder the builder for the {@LoadedFiled} associated with this batch
   * @param postgresBatch the {@link PostgreSqlCopyInserter} for the current set of {@link
   *     RifFilesEvent}s being processed
   * @param resultHandler the {@link Consumer} to notify when the batch completes successfully
   * @param errorHandler the {@link Consumer} to notify when the batch fails for any reason
   */
  private void processAsync(
      BlockingThreadPoolExecutor loadExecutor,
      List<RifRecordEvent<?>> recordsBatch,
      long loadedFileId,
      PostgreSqlCopyInserter postgresBatch,
      Consumer<RifRecordLoadResult> resultHandler,
      Consumer<Throwable> errorHandler) {
    loadExecutor.submit(
        () -> {
          try {
            List<RifRecordLoadResult> processResults =
                process(recordsBatch, loadedFileId, postgresBatch);
            processResults.forEach(resultHandler::accept);
          } catch (Throwable e) {
            errorHandler.accept(e);
          }
        });
  }

  /**
   * @param recordsBatch the {@link RifRecordEvent}s to process
   * @param loadedFileBuilder the builder for the {@LoadedFile} associated with this batch
   * @param postgresBatch the {@link PostgreSqlCopyInserter} for the current set of {@link
   *     RifFilesEvent}s being processed
   * @return the {@link RifRecordLoadResult}s that model the results of the operation
   */
  private List<RifRecordLoadResult> process(
      List<RifRecordEvent<?>> recordsBatch,
      long loadedFileId,
      PostgreSqlCopyInserter postgresBatch) {
    RifFileEvent fileEvent = recordsBatch.get(0).getFileEvent();
    MetricRegistry fileEventMetrics = fileEvent.getEventMetrics();

    RifFileType rifFileType = fileEvent.getFile().getFileType();

    if (rifFileType == RifFileType.BENEFICIARY_HISTORY) {
      for (RifRecordEvent<?> rifRecordEvent : recordsBatch) {
        hashBeneficiaryHistoryHicn(rifRecordEvent);
        hashBeneficiaryHistoryMbi(rifRecordEvent);
      }
    }

    // Only one of each failure/success Timer.Contexts will be applied.
    Timer.Context timerBatchSuccess =
        appState
            .getMetrics()
            .timer(MetricRegistry.name(getClass().getSimpleName(), "recordBatches"))
            .time();
    Timer.Context timerBatchTypeSuccess =
        fileEventMetrics
            .timer(
                MetricRegistry.name(
                    getClass().getSimpleName(), "recordBatches", rifFileType.name()))
            .time();
    Timer.Context timerBundleFailure =
        appState
            .getMetrics()
            .timer(MetricRegistry.name(getClass().getSimpleName(), "recordBatches", "failed"))
            .time();

    EntityManager entityManager = null;
    EntityTransaction txn = null;

    // TODO: refactor the following to be less of an indented mess
    try {
      entityManager = appState.getEntityManagerFactory().createEntityManager();
      txn = entityManager.getTransaction();
      txn.begin();
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

          /* Blow up the data load if we try to insert a record that has a non 2022 year.
           * See {@link LoadAppOptions.isFilteringNonNullAndNon2022Benes}
           */
          if (options.isFilteringNonNullAndNon2022Benes()
              && !isBeneficiaryWithNullOr2022Year(rifRecordEvent)) {
            throw new IllegalArgumentException(
                "Cannot INSERT beneficiary with non-2022 enrollment year; investigate this data load.");
          }

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

            /* Blow up the data load if we try to insert a record that has a non 2022 year.
             * See {@link LoadAppOptions.isFilteringNonNullAndNon2022Benes}
             */
            if (options.isFilteringNonNullAndNon2022Benes()
                && !isBeneficiaryWithNullOr2022Year(rifRecordEvent)) {
              throw new IllegalArgumentException(
                  "Cannot INSERT beneficiary with non-2022 enrollment year; investigate this data load.");
            }
            tweakIfBeneficiary(entityManager, loadedBatchBuilder, rifRecordEvent);
            entityManager.persist(record);

          } else if (rifRecordEvent.getRecordAction().equals(RecordAction.UPDATE)) {
            loadAction = LoadAction.UPDATED;
            // Skip this record if the year is not 2022 and its an update.
            if (options.isFilteringNonNullAndNon2022Benes()
                && !isBeneficiaryWithNullOr2022Year(rifRecordEvent)) {
              /*
               * Serialize the record's CSV data back to actual RIF/CSV, as that's how we'll store
               * it in the DB.
               */
              StringBuffer rifData = new StringBuffer();
              try (CSVPrinter csvPrinter = new CSVPrinter(rifData, RifParsingUtils.CSV_FORMAT)) {
                for (CSVRecord csvRow : rifRecordEvent.getRawCsvRecords()) {
                  csvPrinter.printRecord(csvRow);
                }
              }

              // Save the skipped record to the DB.
              SkippedRifRecord skippedRifRecord =
                  new SkippedRifRecord(
                      rifRecordEvent.getFileEvent().getParentFilesEvent().getTimestamp(),
                      SkipReasonCode.DELAYED_BACKDATED_ENROLLMENT_BFD_1566,
                      rifRecordEvent.getFileEvent().getFile().getFileType().name(),
                      rifRecordEvent.getRecordAction(),
                      ((Beneficiary) record).getBeneficiaryId(),
                      rifData.toString());
              entityManager.persist(skippedRifRecord);
            } else {
              tweakIfBeneficiary(entityManager, loadedBatchBuilder, rifRecordEvent);
              entityManager.merge(record);
            }
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

      txn.commit();

      // Update the metrics now that things have been pushed.
      timerBatchSuccess.stop();
      timerBatchTypeSuccess.stop();

      return loadResults;
    } catch (Throwable t) {
      timerBundleFailure.stop();
      fileEventMetrics
          .meter(MetricRegistry.name(getClass().getSimpleName(), "recordBatches", "failed"))
          .mark(1);
      LOGGER.warn("Failed to load '{}' record.", rifFileType, t);

      throw new RifLoadFailure(recordsBatch, t);
    } finally {
      /*
       * Some errors (e.g. HSQL constraint violations) seem to cause the
       * rollback to fail. Extra error handling is needed here, too, to
       * ensure that the failing data is captured.
       */
      try {
        if (txn != null && txn.isActive()) txn.rollback();
      } catch (Throwable t) {
        timerBundleFailure.stop();
        fileEventMetrics
            .meter(MetricRegistry.name(getClass().getSimpleName(), "recordBatches", "failed"))
            .mark(1);
        LOGGER.warn("Failed to load '{}' record.", rifFileType, t);

        throw new RifLoadFailure(recordsBatch, t);
      }

      if (entityManager != null) entityManager.close();
    }
  }

  /**
   * Checks if the record is a beneficiary and has a enrollment reference year that is either <code>
   * null</code> or is from 2022. This is to handle special filtering while CCW fixes an issue and
   * should be temporary.
   *
   * @param rifRecordEvent the {@link RifRecordEvent} to check
   * @return {@code true} if the record is a beneficiary and has an enrollment year that is either
   *     <code>null</code> or 2022
   */
  private boolean isBeneficiaryWithNullOr2022Year(RifRecordEvent<?> rifRecordEvent) {
    if (rifRecordEvent.getRecord() instanceof Beneficiary) {
      Beneficiary bene = (Beneficiary) rifRecordEvent.getRecord();
      if (bene.getBeneEnrollmentReferenceYear().isPresent()) {
        return BigDecimal.valueOf(2022).equals(bene.getBeneEnrollmentReferenceYear().get());
      } else {
        return true;
      }
    }

    return false;
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
     * Grab the the previous/current version of the Beneficiary (if any, as it exists in the
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
      updateBeneficaryHistory(
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
  private static void updateBeneficaryHistory(
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
    // BFD-1308: removed check for getMbiObsoleteDate; this value is derived from the Beneficiary
    // EFCTV_END_DT but that field has never had a value in the beneficiary.rif file received from
    // CCW.

    // last but not least...these probably won't ever change
    return (Objects.equals(newBeneficiaryRecord.getBirthDate(), oldBeneficiaryRecord.getBirthDate())
        && Objects.equals(newBeneficiaryRecord.getSex(), oldBeneficiaryRecord.getSex()));
  }

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
   * Insert the LoadedFile into the database
   *
   * @param fileEvent to base this new LoadedFile
   * @param errorHandler to call if something bad happens
   * @return the loadedFileId of the new LoadedFile record
   */
  private long insertLoadedFile(RifFileEvent fileEvent, Consumer<Throwable> errorHandler) {
    if (fileEvent == null || fileEvent.getFile().getFileType() == null) {
      throw new IllegalArgumentException();
    }

    final LoadedFile loadedFile = new LoadedFile();
    loadedFile.setRifType(fileEvent.getFile().getFileType().toString());
    loadedFile.setCreated(Instant.now());

    try {
      EntityManager em = appState.getEntityManagerFactory().createEntityManager();
      EntityTransaction txn = null;
      try {
        // Insert the passed in loaded file
        txn = em.getTransaction();
        txn.begin();
        em.persist(loadedFile);
        txn.commit();
        LOGGER.info(
            "Inserting LoadedFile {} of type {} created at {}",
            loadedFile.getLoadedFileId(),
            loadedFile.getRifType(),
            loadedFile.getCreated());

        return loadedFile.getLoadedFileId();
      } finally {
        if (em != null && em.isOpen()) {
          if (txn != null && txn.isActive()) {
            txn.rollback();
          }
          em.close();
        }
      }
    } catch (Exception ex) {
      errorHandler.accept(ex);
      return -1;
    }
  }

  /**
   * Trim the LoadedFiles and LoadedBatches tables if necessary
   *
   * @param errorHandler is called on exceptions
   */
  private void trimLoadedFiles(Consumer<Throwable> errorHandler) {
    try {
      EntityManager em = appState.getEntityManagerFactory().createEntityManager();
      EntityTransaction txn = null;
      try {
        txn = em.getTransaction();
        txn.begin();
        final Instant oldDate = Instant.now().minus(MAX_FILE_AGE_DAYS);

        em.clear(); // Must be done before JPQL statements
        em.flush();
        List<Long> oldIds =
            em.createQuery("select f.loadedFileId from LoadedFile f where created < :oldDate")
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
    } catch (Exception ex) {
      errorHandler.accept(ex);
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
   * @param entityType the JPA {@link Entity} type to count instances of
   * @return a count of the number of instances of the specified JPA {@link Entity} type that are
   *     currently in the database
   */
  private long queryForEntityCount(Class<?> entityType) {
    EntityManager entityManager = null;
    try {
      entityManager = appState.getEntityManagerFactory().createEntityManager();

      CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
      CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
      criteriaQuery.select(criteriaBuilder.count(criteriaQuery.from(entityType)));

      return entityManager.createQuery(criteriaQuery).getSingleResult();
    } finally {
      if (entityManager != null) entityManager.close();
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

  /**
   * Provides the state tracking and logic needed for {@link RifLoader} to handle PostgreSQL {@link
   * RecordAction#INSERT}s via the use of PostgreSQL's non-standard {@link CopyManager} APIs.
   *
   * <p>In <a href="https://www.postgresql.org/docs/9.6/static/populate.html">PostgreSQL 9.6 Manual:
   * Populating a Database</a>, this is recommended as the fastest way to insert large amounts of
   * data. However, real-world testing with Blue Button data has shown that to be not be exactly
   * true: highly parallelized <code>INSERT</code>s (e.g. hundreds of simultaneous connections) can
   * actually be about 18% faster. Even still, this code may eventually be useful for some
   * situations, so we'll keep it around.
   */
  private static final class PostgreSqlCopyInserter implements AutoCloseable {
    private final EntityManagerFactory entityManagerFactory;
    private final MetricRegistry metrics;
    private final List<CsvPrinterBundle> csvPrinterBundles;

    /**
     * Constructs a new {@link PostgreSqlCopyInserter} instance.
     *
     * @param entityManagerFactory the {@link EntityManagerFactory} to use
     * @param metrics the {@link MetricRegistry} to use
     */
    public PostgreSqlCopyInserter(
        EntityManagerFactory entityManagerFactory, MetricRegistry metrics) {
      this.entityManagerFactory = entityManagerFactory;
      this.metrics = metrics;

      List<CsvPrinterBundle> csvPrinters = new LinkedList<>();
      csvPrinters.add(createCsvPrinter(Beneficiary.class));
      csvPrinters.add(createCsvPrinter(CarrierClaim.class));
      csvPrinters.add(createCsvPrinter(CarrierClaimLine.class));
      this.csvPrinterBundles = csvPrinters;
    }

    /**
     * @param entityType the JPA {@link Entity} to create a {@link CSVPrinter} for
     * @return the {@link CSVPrinter} for the specified SQL table
     */
    private CsvPrinterBundle createCsvPrinter(Class<?> entityType) {
      Table tableAnnotation = entityType.getAnnotation(Table.class);
      String tableName = tableAnnotation.name().replaceAll("`", "");

      CSVFormat baseCsvFormat = CSVFormat.DEFAULT;

      try {
        CsvPrinterBundle csvPrinterBundle = new CsvPrinterBundle();
        csvPrinterBundle.tableName = tableName;
        csvPrinterBundle.backingTempFile = File.createTempFile(tableName, ".postgreSqlCsv");
        csvPrinterBundle.csvPrinter =
            new CSVPrinter(new FileWriter(csvPrinterBundle.backingTempFile), baseCsvFormat);
        return csvPrinterBundle;
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      } finally {
      }
    }

    /**
     * Queues the specified {@link RifRecordEvent#getRecord()} top-level entity instance (e.g. a
     * {@link Beneficiary}, {@link CarrierClaim}, etc.) for insertion when {@link #submit()} is
     * called.
     *
     * @param record the {@link RifRecordEvent#getRecord()} top-level entity instance (e.g. a {@link
     *     Beneficiary}, {@link CarrierClaim} , etc.) to queue for insertion
     */
    public void add(Object record) {
      /*
       * Use the auto-generated *CsvWriter helpers to convert the JPA
       * entity to its raw field values, in a format suitable for use with
       * PostgreSQL's CopyManager. Each Map entry will represent a single
       * JPA table, and each Object[] in there represents a single entity
       * instance, with the first Object[] containing the (correctly
       * ordered) SQL column names. So, for a CarrierClaim, there will be
       * two "CarrerClaims" Object[]s: one column header and one with the
       * claim header values. In addition, there will be multiple
       * "CarrierClaimLines" Objects[]: one for the column header and then
       * one for each CarrierClaim.getLines() entry.
       */
      Map<String, Object[][]> csvRecordsByTable;
      if (record instanceof Beneficiary) {
        csvRecordsByTable = BeneficiaryCsvWriter.toCsvRecordsByTable((Beneficiary) record);
      } else if (record instanceof CarrierClaim) {
        csvRecordsByTable = CarrierClaimCsvWriter.toCsvRecordsByTable((CarrierClaim) record);
      } else throw new BadCodeMonkeyException();

      /*
       * Hand off the *CsvWriter results to the appropriate
       * CsvPrinterBundle entries.
       */
      for (Entry<String, Object[][]> tableRecordsEntry : csvRecordsByTable.entrySet()) {
        String tableName = tableRecordsEntry.getKey();
        CsvPrinterBundle tablePrinterBundle =
            csvPrinterBundles.stream().filter(b -> tableName.equals(b.tableName)).findAny().get();

        // Set the column header if it hasn't been yet.
        if (tablePrinterBundle.columnNames == null) {
          Object[] columnNamesAsObjects = tableRecordsEntry.getValue()[0];
          String[] columnNames =
              Arrays.copyOf(columnNamesAsObjects, columnNamesAsObjects.length, String[].class);
          tablePrinterBundle.columnNames = columnNames;
        }

        /*
         * Write out each Object[] entity row to the temp CSV for the
         * appropriate SQL table. These HAVE to be written out now, as
         * there isn't enough RAM to store all of them in memory until
         * submit() gets called.
         */
        for (int recordIndex = 1;
            recordIndex < tableRecordsEntry.getValue().length;
            recordIndex++) {
          Object[] csvRecord = tableRecordsEntry.getValue()[recordIndex];
          tablePrinterBundle.recordsPrinted.getAndIncrement();

          try {
            /*
             * This will be called by multiple loader threads
             * (possibly hundreds), so it must be synchronized to
             * ensure that writes aren't corrupted. This isn't the
             * most efficient possible strategy, but has proven to
             * not be a bottleneck.
             */
            synchronized (tablePrinterBundle.csvPrinter) {
              tablePrinterBundle.csvPrinter.printRecord(csvRecord);
            }
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }
      }
    }

    /**
     * @return <code>true</code> if {@link #add(Object)} hasn't been called yet, <code>false</code>
     *     if it has
     */
    public boolean isEmpty() {
      return !csvPrinterBundles.stream()
          .filter(b -> b.recordsPrinted.get() > 0)
          .findAny()
          .isPresent();
    }

    /**
     * Uses PostgreSQL's {@link CopyManager} API to bulk-insert all of the JPA entities that have
     * been queued via {@link #add(Object)}.
     *
     * <p>Note: this is an <em>efficient</em> operation, but still not necessarily a <em>fast</em>
     * one: it can take hours to run for large amounts of data.
     */
    public void submit() {
      Timer.Context submitTimer =
          metrics
              .timer(
                  MetricRegistry.name(
                      getClass().getSimpleName(), "postgresSqlBatches", "submitted"))
              .time();

      EntityManager entityManager = null;
      try {
        entityManager = entityManagerFactory.createEntityManager();

        /*
         * PostgreSQL's CopyManager needs a raw PostgreSQL
         * BaseConnection. So here we unwrap one from the EntityManager.
         */
        Session session = entityManager.unwrap(Session.class);
        session.doWork(
            new Work() {
              /** @see org.hibernate.jdbc.Work#execute(java.sql.Connection) */
              @Override
              public void execute(Connection connection) throws SQLException {
                /*
                 * Further connection unwrapping: go from a pooled
                 * Hikari connection to a raw PostgreSQL one.
                 */
                HikariProxyConnection pooledConnection = (HikariProxyConnection) connection;
                BaseConnection postgreSqlConnection = pooledConnection.unwrap(BaseConnection.class);

                /*
                 * Use that PostgreSQL connection to construct a
                 * CopyManager instance. Finally!
                 */
                CopyManager copyManager = new CopyManager(postgreSqlConnection);

                /*
                 * Run the CopyManager against each CsvPrinterBundle
                 * with queued records.
                 */
                csvPrinterBundles.stream()
                    .filter(b -> b.recordsPrinted.get() > 0)
                    .forEach(
                        b -> {
                          try {
                            LOGGER.debug(
                                "Flushing PostgreSQL COPY queue: '{}'.", b.backingTempFile);
                            /*
                             * First, flush the CSVPrinter to ensure that
                             * all queued records are available on disk.
                             */
                            b.csvPrinter.flush();

                            /*
                             * Crack open the queued CSV records and feed
                             * them into the CopyManager.
                             */
                            Timer.Context postgresCopyTimer =
                                metrics
                                    .timer(
                                        MetricRegistry.name(
                                            getClass().getSimpleName(),
                                            "postgresCopy",
                                            "completed"))
                                    .time();
                            LOGGER.info("Submitting PostgreSQL COPY queue: '{}'...", b.tableName);
                            FileReader reader = new FileReader(b.backingTempFile);
                            String columnsList =
                                Arrays.stream(b.columnNames)
                                    .map(c -> "\"" + c + "\"")
                                    .collect(Collectors.joining(", "));
                            copyManager.copyIn(
                                String.format(
                                    "COPY \"%s\" (%s) FROM STDIN DELIMITERS ',' CSV ENCODING 'UTF8'",
                                    b.tableName, columnsList),
                                reader);
                            LOGGER.info("Submitted PostgreSQL COPY queue: '{}'.", b.tableName);
                            postgresCopyTimer.stop();
                          } catch (Exception e) {
                            throw new RifLoadFailure(e);
                          }
                        });
              }
            });
      } finally {
        if (entityManager != null) entityManager.close();
      }

      submitTimer.stop();
    }

    /** @see java.lang.AutoCloseable#close() */
    @Override
    public void close() {
      csvPrinterBundles.stream()
          .forEach(
              b -> {
                try {
                  b.csvPrinter.close();
                  b.backingTempFile.delete();
                } catch (IOException e) {
                  throw new UncheckedIOException(e);
                }
              });
    }

    /**
     * A simple struct for storing all of the state and tracking information for each SQL table's
     * {@link CSVPrinter}.
     */
    private static final class CsvPrinterBundle {
      String tableName = null;
      CSVPrinter csvPrinter = null;
      File backingTempFile = null;
      String[] columnNames = null;
      AtomicInteger recordsPrinted = new AtomicInteger(0);
    }
  }

  /** Enumerates the {@link RifLoader} record handling strategies. */
  private static enum LoadStrategy {
    INSERT_IDEMPOTENT,

    INSERT_UPDATE_NON_IDEMPOTENT;
  }

  /** Encapsulates the {@link RifLoader} record handling preferences. */
  private static final class LoadFeatures {
    private final boolean idempotencyRequired;
    private final boolean copyDesired;

    /**
     * Constructs a new {@link LoadFeatures} instance.
     *
     * @param idempotencyRequired the value to use for {@link #isIdempotencyRequired()}
     * @param copyDesired the value to use for {@link #isCopyDesired()}
     */
    public LoadFeatures(boolean idempotencyRequired, boolean copyDesired) {
      this.idempotencyRequired = idempotencyRequired;
      this.copyDesired = copyDesired;
    }

    /**
     * @return <code>true</code> if record inserts must be idempotent, <code>false</code> if that's
     *     not required
     */
    public boolean isIdempotencyRequired() {
      return idempotencyRequired;
    }

    /**
     * @return <code>true</code> if PostgreSQL's {@link CopyManager} APIs should be used to load
     *     data when possible, <code>false</code> if not
     */
    public boolean isCopyDesired() {
      return copyDesired;
    }
  }
}
