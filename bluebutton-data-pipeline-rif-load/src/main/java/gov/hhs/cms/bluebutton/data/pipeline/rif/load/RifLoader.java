package gov.hhs.cms.bluebutton.data.pipeline.rif.load;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Table;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.sql.DataSource;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.hibernate.tool.schema.Action;
import org.postgresql.copy.CopyManager;
import org.postgresql.core.BaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariProxyConnection;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryCsvWriter;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryHistory;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimCsvWriter;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.RecordAction;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileEvent;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileRecords;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileType;
import gov.hhs.cms.bluebutton.data.model.rif.RifFilesEvent;
import gov.hhs.cms.bluebutton.data.model.rif.RifRecordEvent;
import gov.hhs.cms.bluebutton.data.model.rif.schema.DatabaseSchemaManager;
import gov.hhs.cms.bluebutton.data.pipeline.rif.load.RifRecordLoadResult.LoadAction;

/**
 * Pushes CCW beneficiary and claims data from {@link RifRecordEvent}s into the
 * Blue Button API's database.
 */
public final class RifLoader {
	/**
	 * The number of {@link RifRecordEvent}s that will be included in each
	 * processing batch. Note that larger batch sizes mean that more
	 * {@link RifRecordEvent}s will be held in memory simultaneously.
	 */
	private static final int RECORD_BATCH_SIZE = 100;

	private static final Logger LOGGER = LoggerFactory.getLogger(RifLoader.class);
	private static final Logger LOGGER_RECORD_COUNTS = LoggerFactory
			.getLogger(RifLoader.class.getName() + ".recordCounts");

	private final MetricRegistry appMetrics;
	private final LoadAppOptions options;
	private final EntityManagerFactory entityManagerFactory;
	private final SecretKeyFactory secretKeyFactory;

	/**
	 * Constructs a new {@link RifLoader} instance.
	 * 
	 * @param appMetrics
	 *            the {@link MetricRegistry} being used for the overall
	 *            application (as opposed to a specific data set)
	 * @param options
	 *            the {@link LoadAppOptions} to use
	 */
	public RifLoader(MetricRegistry appMetrics, LoadAppOptions options) {
		this.appMetrics = appMetrics;
		this.options = options;

		DataSource jdbcDataSource = createDataSource(options, appMetrics);
		DatabaseSchemaManager.createOrUpdateSchema(jdbcDataSource);
		this.entityManagerFactory = createEntityManagerFactory(jdbcDataSource);

		this.secretKeyFactory = createSecretKeyFactory();
	}

	/**
	 * @param options
	 *            the {@link LoadAppOptions} to use
	 * @param metrics
	 *            the {@link MetricRegistry} to use
	 * @return a JDBC {@link DataSource} for the Blue Button API backend
	 *         database
	 */
	static DataSource createDataSource(LoadAppOptions options, MetricRegistry metrics) {
		HikariDataSource dataSource = new HikariDataSource();

		/*
		 * FIXME The pool size needs to be double the number of loader threads
		 * when idempotent loads are being used. Apparently, the queries need a
		 * separate Connection?
		 */
		dataSource.setMaximumPoolSize(options.getLoaderThreads());

		dataSource.setJdbcUrl(options.getDatabaseUrl());
		dataSource.setUsername(options.getDatabaseUsername());
		dataSource.setPassword(String.valueOf(options.getDatabasePassword()));
		dataSource.setRegisterMbeans(true);
		dataSource.setMetricRegistry(metrics);

		return dataSource;
	}

	/**
	 * @param jdbcDataSource
	 *            the JDBC {@link DataSource} for the Blue Button API backend
	 *            database
	 * @param metrics
	 *            the {@link MetricRegistry} to use
	 * @return a JPA {@link EntityManagerFactory} for the Blue Button API
	 *         backend database
	 */
	static EntityManagerFactory createEntityManagerFactory(DataSource jdbcDataSource) {
		/*
		 * The number of JDBC statements that will be queued/batched within a
		 * single transaction. Most recommendations suggest this should be 5-30.
		 * Paradoxically, setting it higher seems to actually slow things down.
		 * Presumably, it's delaying work that could be done earlier in a batch,
		 * and that starts to cost more than the extra network roundtrips.
		 */
		int jdbcBatchSize = 10;

		Map<String, Object> hibernateProperties = new HashMap<>();
		hibernateProperties.put(org.hibernate.cfg.AvailableSettings.DATASOURCE, jdbcDataSource);
		hibernateProperties.put(org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, Action.VALIDATE);
		hibernateProperties.put(org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, jdbcBatchSize);

		EntityManagerFactory entityManagerFactory = Persistence
				.createEntityManagerFactory("gov.hhs.cms.bluebutton.data", hibernateProperties);
		return entityManagerFactory;
	}

	/**
	 * @param options
	 *            the {@link LoadAppOptions} to use
	 * @return the {@link BlockingThreadPoolExecutor} to use for asynchronous
	 *         load tasks
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

		LOGGER.info("Configured to load with '{}' threads, a queue of '{}', and a batch size of '{}'.",
				options.getLoaderThreads(), taskQueueSize, RECORD_BATCH_SIZE);

		/*
		 * I feel like a hipster using "found" code like
		 * BlockingThreadPoolExecutor: this really cool (and old) class supports
		 * our use case beautifully. It hands out tasks to multiple consumers,
		 * and allows a single producer to feed it, blocking that producer when
		 * the task queue is full.
		 */
		BlockingThreadPoolExecutor loadExecutor = new BlockingThreadPoolExecutor(threadPoolSize, taskQueueSize, 100,
				TimeUnit.MILLISECONDS);
		return loadExecutor;
	}

	/**
	 * @param recordAction
	 *            the {@link RecordAction} of the specific record being processed
	 * @return the {@link LoadStrategy} that should be used for the record being
	 *         processed
	 */
	private LoadStrategy selectStrategy(RecordAction recordAction) {
		if (recordAction == RecordAction.INSERT) {
			if (options.isIdempotencyRequired())
				return LoadStrategy.INSERT_IDEMPOTENT;
			else
				return LoadStrategy.INSERT_UPDATE_NON_IDEMPOTENT;
		} else {
			return LoadStrategy.INSERT_UPDATE_NON_IDEMPOTENT;
		}
	}

	/**
	 * @return <code>true</code> if {@link #entityManagerFactory} is connected
	 *         to a PostgreSQL database, <code>false</code> if it is not
	 */
	private boolean isDatabasePostgreSql() {
		AtomicBoolean result = new AtomicBoolean(false);

		EntityManager entityManager = null;
		try {
			entityManager = entityManagerFactory.createEntityManager();
			Session session = entityManager.unwrap(Session.class);
			session.doWork(new Work() {
				/**
				 * @see org.hibernate.jdbc.Work#execute(java.sql.Connection)
				 */
				@Override
				public void execute(Connection connection) throws SQLException {
					String databaseName = connection.getMetaData().getDatabaseProductName();
					if (databaseName.equals("PostgreSQL"))
						result.set(true);
				}
			});
		} finally {
			if (entityManager != null)
				entityManager.close();
		}

		return result.get();
	}

	/**
	 * <p>
	 * Consumes the input {@link Stream} of {@link RifRecordEvent}s, pushing
	 * each {@link RifRecordEvent}'s record to the database, and passing the
	 * result for each of those bundles to the specified error handler and
	 * result handler, as appropriate.
	 * </p>
	 * <p>
	 * This is a <a href=
	 * "https://docs.oracle.com/javase/8/docs/api/java/util/stream/package-summary.html#StreamOps">
	 * terminal operation</a>.
	 * </p>
	 * 
	 * @param dataToLoad
	 *            the FHIR {@link RifRecordEvent}s to be loaded
	 * @param errorHandler
	 *            the {@link Consumer} to pass each error that occurs to
	 *            (possibly one error per {@link RifRecordEvent}, if every input
	 *            element fails to load), which will be run on the caller's
	 *            thread
	 * @param resultHandler
	 *            the {@link Consumer} to pass each the
	 *            {@link RifRecordLoadResult} for each of the
	 *            successfully-processed input {@link RifRecordEvent}s, which
	 *            will be run on the caller's thread
	 */
	public void process(RifFileRecords dataToLoad, Consumer<Throwable> errorHandler,
			Consumer<RifRecordLoadResult> resultHandler) {
		BlockingThreadPoolExecutor loadExecutor = createLoadExecutor(options);

		MetricRegistry fileEventMetrics = dataToLoad.getSourceEvent().getEventMetrics();
		Timer.Context timerDataSetFile = appMetrics
				.timer(MetricRegistry.name(getClass().getSimpleName(), "dataSet", "file", "processed")).time();
		LOGGER.info("Processing '{}'...", dataToLoad);

		dataToLoad.getSourceEvent().getEventMetrics().register(
				MetricRegistry.name(getClass().getSimpleName(), "loadExecutorService", "queueSize"),
				new Gauge<Integer>() {
					/**
					 * @see com.codahale.metrics.Gauge#getValue()
					 */
					@Override
					public Integer getValue() {
						return loadExecutor.getQueue().size();
					}
				});
		dataToLoad.getSourceEvent().getEventMetrics().register(
				MetricRegistry.name(getClass().getSimpleName(), "loadExecutorService", "activeBatches"),
				new Gauge<Integer>() {
					/**
					 * @see com.codahale.metrics.Gauge#getValue()
					 */
					@Override
					public Integer getValue() {
						return loadExecutor.getActiveCount();
					}
				});

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

		try (PostgreSqlCopyInserter postgresBatch = new PostgreSqlCopyInserter(entityManagerFactory,
				fileEventMetrics)) {
			// Define the Consumer that will handle each batch.
			Consumer<List<RifRecordEvent<?>>> batchProcessor = recordsBatch -> {
				/*
				 * Submit the RifRecordEvent for asynchronous processing. Note
				 * that, due to the ExecutorService's configuration (see in
				 * constructor), this will block if too many tasks are already
				 * pending. That's desirable behavior, as it prevents
				 * OutOfMemoryErrors.
				 */
				processAsync(loadExecutor, recordsBatch, postgresBatch, resultHandler, errorHandler);
			};

			// Collect records into batches and submit each to batchProcessor.
			if (RECORD_BATCH_SIZE > 1)
				BatchSpliterator.batches(dataToLoad.getRecords(), RECORD_BATCH_SIZE).forEach(batchProcessor);
			else
				dataToLoad.getRecords().map(record -> {
					List<RifRecordEvent<?>> ittyBittyBatch = new LinkedList<>();
					ittyBittyBatch.add(record);
					return ittyBittyBatch;
				}).forEach(batchProcessor);

			// Wait for all submitted batches to complete.
			try {
				loadExecutor.shutdown();
				boolean terminatedSuccessfully = loadExecutor.awaitTermination(72, TimeUnit.HOURS);
				if (!terminatedSuccessfully)
					throw new IllegalStateException(
							String.format("%s failed to complete processing the records in time: '%s'.",
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
	 * @param loadExecutor
	 *            the {@link BlockingThreadPoolExecutor} to use for asynchronous
	 *            load tasks
	 * @param recordsBatch
	 *            the {@link RifRecordEvent}s to process
	 * @param postgresBatch
	 *            the {@link PostgreSqlCopyInserter} for the current set of
	 *            {@link RifFilesEvent}s being processed
	 * @param resultHandler
	 *            the {@link Consumer} to notify when the batch completes
	 *            successfully
	 * @param errorHandler
	 *            the {@link Consumer} to notify when the batch fails for any
	 *            reason
	 */
	private void processAsync(BlockingThreadPoolExecutor loadExecutor, List<RifRecordEvent<?>> recordsBatch,
			PostgreSqlCopyInserter postgresBatch,
			Consumer<RifRecordLoadResult> resultHandler, Consumer<Throwable> errorHandler) {
		loadExecutor.submit(() -> {
			try {
				List<RifRecordLoadResult> processResults = process(recordsBatch, postgresBatch);
				processResults.forEach(resultHandler::accept);
			} catch (Throwable e) {
				errorHandler.accept(e);
			}
		});
	}

	/**
	 * @param recordsBatch
	 *            the {@link RifRecordEvent}s to process
	 * @param postgresBatch
	 *            the {@link PostgreSqlCopyInserter} for the current set of
	 *            {@link RifFilesEvent}s being processed
	 * @return the {@link RifRecordLoadResult}s that model the results of the
	 *         operation
	 */
	private List<RifRecordLoadResult> process(List<RifRecordEvent<?>> recordsBatch,
			PostgreSqlCopyInserter postgresBatch) {
		RifFileEvent fileEvent = recordsBatch.get(0).getFileEvent();
		MetricRegistry fileEventMetrics = fileEvent.getEventMetrics();

		RifFileType rifFileType = fileEvent.getFile().getFileType();

		// If these are Beneficiary records, first hash their HICNs.
		if (rifFileType == RifFileType.BENEFICIARY) {
			for (RifRecordEvent<?> rifRecordEvent : recordsBatch)
				hashBeneficiaryHicn(fileEventMetrics, rifRecordEvent);
		} else if (rifFileType == RifFileType.BENEFICIARY_HISTORY) {
			for (RifRecordEvent<?> rifRecordEvent : recordsBatch)
				hashBeneficiaryHistoryHicn(fileEventMetrics, rifRecordEvent);
		}

		// Only one of each failure/success Timer.Contexts will be applied.
		Timer.Context timerBatchSuccess = appMetrics
				.timer(MetricRegistry.name(getClass().getSimpleName(), "recordBatches")).time();
		Timer.Context timerBatchTypeSuccess = fileEventMetrics
				.timer(MetricRegistry.name(getClass().getSimpleName(), "recordBatches", rifFileType.name())).time();
		Timer.Context timerBundleFailure = appMetrics
				.timer(MetricRegistry.name(getClass().getSimpleName(), "recordBatches", "failed")).time();

		EntityManager entityManager = null;

		// TODO: refactor the following to be less of an indented mess
		try {
			entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();

			List<RifRecordLoadResult> loadResults = new ArrayList<>(recordsBatch.size());
			for (RifRecordEvent<?> rifRecordEvent : recordsBatch) {
				RecordAction recordAction = rifRecordEvent.getRecordAction();
				Object record = rifRecordEvent.getRecord();

				LOGGER.trace("Loading '{}' record.", rifFileType);
				LoadStrategy strategy = selectStrategy(recordAction);
				LoadAction loadAction;

				if (strategy == LoadStrategy.INSERT_IDEMPOTENT) {
					// Check to see if record already exists.
					Timer.Context timerIdempotencyQuery = fileEventMetrics
							.timer(MetricRegistry.name(getClass().getSimpleName(), "idempotencyQueries")).time();
					Object recordId = entityManagerFactory.getPersistenceUnitUtil().getIdentifier(record);
					Objects.requireNonNull(recordId);
					Object recordInDb = entityManager.find(record.getClass(), recordId);
					timerIdempotencyQuery.close();

					if (recordInDb == null) {
						loadAction = LoadAction.INSERTED;
						entityManager.persist(record);
						Object recordInDbAfterUpdate = entityManager.find(record.getClass(), recordId);
					} else {
						loadAction = LoadAction.DID_NOTHING;
					}
				} else if (strategy == LoadStrategy.INSERT_UPDATE_NON_IDEMPOTENT) {
					if (rifRecordEvent.getRecordAction().equals(RecordAction.INSERT)) {
						loadAction = LoadAction.INSERTED;
						entityManager.persist(record);
					} else if (rifRecordEvent.getRecordAction().equals(RecordAction.UPDATE)) {
						loadAction = LoadAction.UPDATED;

						/*
						 * When beneficiaries are updated, we need to be careful to capture their
						 * current/previous state as a BeneficiaryHistory record.
						 */
						if (record instanceof Beneficiary) {
							updateBeneficaryHistory(entityManager, (Beneficiary) record);
						}

						entityManager.merge(record);
					} else {
						throw new BadCodeMonkeyException(String.format("Unhandled %s: '%s'.", RecordAction.class,
								rifRecordEvent.getRecordAction()));
					}
				} else
					throw new BadCodeMonkeyException();

				LOGGER.trace("Loaded '{}' record.", rifFileType);

				fileEventMetrics.meter(MetricRegistry.name(getClass().getSimpleName(), "records", loadAction.name()))
						.mark(1);

				loadResults.add(new RifRecordLoadResult(rifRecordEvent, loadAction));
			}

			entityManager.getTransaction().commit();

			// Update the metrics now that things have been pushed.
			timerBatchSuccess.stop();
			timerBatchTypeSuccess.stop();

			return loadResults;
		} catch (Throwable t) {
			timerBundleFailure.stop();
			fileEventMetrics.meter(MetricRegistry.name(getClass().getSimpleName(), "recordBatches", "failed")).mark(1);
			LOGGER.warn("Failed to load '{}' record.", rifFileType, t);

			throw new RifLoadFailure(recordsBatch, t);
		} finally {
			/*
			 * Some errors (e.g. HSQL constraint violations) seem to cause the
			 * rollback to fail. Extra error handling is needed here, too, to
			 * ensure that the failing data is captured.
			 */
			try {
				if (entityManager != null && entityManager.getTransaction().isActive())
					entityManager.getTransaction().rollback();
			} catch (Throwable t) {
				timerBundleFailure.stop();
				fileEventMetrics.meter(MetricRegistry.name(getClass().getSimpleName(), "recordBatches", "failed"))
						.mark(1);
				LOGGER.warn("Failed to load '{}' record.", rifFileType, t);

				throw new RifLoadFailure(recordsBatch, t);
			}

			if (entityManager != null)
				entityManager.close();
		}
	}

	/**
	 * Ensures that a {@link BeneficiaryHistory} record is created for the specified
	 * {@link Beneficiary}, if that {@link Beneficiary} already exists and is just
	 * being updated.
	 *
	 * @param entityManager
	 *            the {@link EntityManager} to use
	 * @param newBeneficiaryRecord
	 *            the {@link Beneficiary} record being processed
	 */
	private static void updateBeneficaryHistory(EntityManager entityManager, Beneficiary newBeneficiaryRecord) {
		Beneficiary oldBeneficiaryRecord = entityManager.find(Beneficiary.class,
				newBeneficiaryRecord.getBeneficiaryId());

		if (oldBeneficiaryRecord != null) {
			BeneficiaryHistory oldBeneCopy = new BeneficiaryHistory();
			oldBeneCopy.setBeneficiaryId(oldBeneficiaryRecord.getBeneficiaryId());
			oldBeneCopy.setBirthDate(oldBeneficiaryRecord.getBirthDate());
			oldBeneCopy.setHicn(oldBeneficiaryRecord.getHicn());
			oldBeneCopy.setHicnUnhashed(oldBeneficiaryRecord.getHicnUnhashed());
			oldBeneCopy.setSex(oldBeneficiaryRecord.getSex());
			oldBeneCopy.setMedicareBeneficiaryId(oldBeneficiaryRecord.getMedicareBeneficiaryId());

			entityManager.persist(oldBeneCopy);
		}
	}

	/**
	 * Computes and logs a count for all record types.
	 */
	private void logRecordCounts() {
		if (!LOGGER_RECORD_COUNTS.isDebugEnabled())
			return;

		Timer.Context timerCounting = appMetrics
				.timer(MetricRegistry.name(getClass().getSimpleName(), "recordCounting")).time();
		LOGGER.debug("Counting records...");
		String entityTypeCounts = entityManagerFactory.getMetamodel().getManagedTypes().stream()
				.map(t -> t.getJavaType()).sorted(Comparator.comparing(Class::getName)).map(t -> {
					long entityTypeRecordCount = queryForEntityCount(t);
					return String.format("%s: %d", t.getSimpleName(), entityTypeRecordCount);
				}).collect(Collectors.joining(", "));
		LOGGER.debug("Record counts by entity type: '{}'.", entityTypeCounts);
		timerCounting.stop();
	}

	/**
	 * @param entityType
	 *            the JPA {@link Entity} type to count instances of
	 * @return a count of the number of instances of the specified JPA
	 *         {@link Entity} type that are currently in the database
	 */
	private long queryForEntityCount(Class<?> entityType) {
		EntityManager entityManager = null;
		try {
			entityManager = entityManagerFactory.createEntityManager();

			CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
			criteriaQuery.select(criteriaBuilder.count(criteriaQuery.from(entityType)));

			return entityManager.createQuery(criteriaQuery).getSingleResult();
		} finally {
			if (entityManager != null)
				entityManager.close();
		}
	}

	/**
	 * <p>
	 * For {@link RifRecordEvent}s where the {@link RifRecordEvent#getRecord()}
	 * is a {@link Beneficiary}, switches the {@link Beneficiary#getHicn()}
	 * property to a cryptographic hash of its current value. This is done for
	 * security purposes, and the Blue Button API frontend applications know how
	 * to compute the exact same hash, which allows the two halves of the system
	 * to interoperate.
	 * </p>
	 * <p>
	 * All other {@link RifRecordEvent}s are left unmodified.
	 * </p>
	 * 
	 * @param metrics
	 *            the {@link MetricRegistry} to use
	 * @param rifRecordEvent
	 *            the {@link RifRecordEvent} to (possibly) modify
	 */
	private void hashBeneficiaryHicn(MetricRegistry metrics, RifRecordEvent<?> rifRecordEvent) {
		if (rifRecordEvent.getFileEvent().getFile().getFileType() != RifFileType.BENEFICIARY)
			return;

		Timer.Context timerHashing = metrics.timer(MetricRegistry.name(getClass().getSimpleName(), "hicnsHashed"))
				.time();

		Beneficiary beneficiary = (Beneficiary) rifRecordEvent.getRecord();
		// set the unhashed Hicn
		beneficiary.setHicnUnhashed(Optional.of(beneficiary.getHicn()));
		// set the hashed Hicn
		beneficiary.setHicn(computeHicnHash(options, secretKeyFactory, beneficiary.getHicn()));

		timerHashing.stop();
	}

	/**
	 * <p>
	 * For {@link RifRecordEvent}s where the {@link RifRecordEvent#getRecord()} is a
	 * {@link BeneficiaryHistory}, switches the {@link BeneficiaryHistory#getHicn()}
	 * property to a cryptographic hash of its current value. This is done for
	 * security purposes, and the Blue Button API frontend applications know how to
	 * compute the exact same hash, which allows the two halves of the system to
	 * interoperate.
	 * </p>
	 * <p>
	 * All other {@link RifRecordEvent}s are left unmodified.
	 * </p>
	 *
	 * @param metrics
	 *            the {@link MetricRegistry} to use
	 * @param rifRecordEvent
	 *            the {@link RifRecordEvent} to (possibly) modify
	 */
	private void hashBeneficiaryHistoryHicn(MetricRegistry metrics, RifRecordEvent<?> rifRecordEvent) {
		if (rifRecordEvent.getFileEvent().getFile().getFileType() != RifFileType.BENEFICIARY_HISTORY)
			return;

		Timer.Context timerHashing = metrics.timer(MetricRegistry.name(getClass().getSimpleName(), "hicnsHashed"))
				.time();

		BeneficiaryHistory beneficiaryHistory = (BeneficiaryHistory) rifRecordEvent.getRecord();

		// set the unhashed Hicn
		beneficiaryHistory.setHicnUnhashed(Optional.of(beneficiaryHistory.getHicn()));

		// set the hashed Hicn
		beneficiaryHistory.setHicn(computeHicnHash(options, secretKeyFactory, beneficiaryHistory.getHicn()));

		timerHashing.stop();
	}

	/**
	 * @return a new {@link SecretKeyFactory} for the
	 *         <code>PBKDF2WithHmacSHA256</code> algorithm
	 */
	static SecretKeyFactory createSecretKeyFactory() {
		try {
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Computes a one-way cryptographic hash of the specified HICN value. This
	 * is used as a secure means of identifying Medicare beneficiaries between
	 * the Blue Button API frontend and backend systems: the HICN is the only
	 * unique beneficiary identifier shared between those two systems.
	 * 
	 * @param options
	 *            the {@link LoadAppOptions} to use
	 * @param secretKeyFactory
	 *            the {@link SecretKeyFactory} to use
	 * @param hicn
	 *            the Medicare beneficiary HICN to be hashed
	 * @return a one-way cryptographic hash of the specified HICN value, exactly
	 *         64 characters long
	 */
	static String computeHicnHash(LoadAppOptions options, SecretKeyFactory secretKeyFactory, String hicn) {
		try {
			/*
			 * Our approach here is NOT using a salt, as salts must be randomly
			 * generated for each value to be hashed and then included in
			 * plaintext with the hash results. Random salts would prevent the
			 * Blue Button API frontend systems from being able to produce equal
			 * hashes for the same HICNs. Instead, we use a secret "pepper" that
			 * is shared out-of-band with the frontend. This value MUST be kept
			 * secret.
			 */
			byte[] salt = options.getHicnHashPepper();

			/*
			 * Bigger is better here as it reduces chances of collisions, but
			 * the equivalent Python Django hashing functions used by the
			 * frontend default to this value, so we'll go with it.
			 */
			int derivedKeyLength = 256;

			PBEKeySpec hicnKeySpec = new PBEKeySpec(hicn.toCharArray(), salt, options.getHicnHashIterations(),
					derivedKeyLength);
			SecretKey hicnSecret = secretKeyFactory.generateSecret(hicnKeySpec);
			String hexEncodedHash = Hex.encodeHexString(hicnSecret.getEncoded());

			return hexEncodedHash;
		} catch (InvalidKeySpecException e) {
			throw new BadCodeMonkeyException(e);
		}
	}

	/**
	 * <p>
	 * Provides the state tracking and logic needed for {@link RifLoader} to
	 * handle PostgreSQL {@link RecordAction#INSERT}s via the use of
	 * PostgreSQL's non-standard {@link CopyManager} APIs.
	 * </p>
	 * <p>
	 * In <a href="https://www.postgresql.org/docs/9.6/static/populate.html">
	 * PostgreSQL 9.6 Manual: Populating a Database</a>, this is recommended as
	 * the fastest way to insert large amounts of data. However, real-world
	 * testing with Blue Button data has shown that to be not be exactly true:
	 * highly parallelized <code>INSERT</code>s (e.g. hundreds of simultaneous
	 * connections) can actually be about 18% faster. Even still, this code may
	 * eventually be useful for some situations, so we'll keep it around.
	 * </p>
	 */
	private static final class PostgreSqlCopyInserter implements AutoCloseable {
		private final EntityManagerFactory entityManagerFactory;
		private final MetricRegistry metrics;
		private final List<CsvPrinterBundle> csvPrinterBundles;

		/**
		 * Constructs a new {@link PostgreSqlCopyInserter} instance.
		 * 
		 * @param entityManagerFactory
		 *            the {@link EntityManagerFactory} to use
		 * @param metrics
		 *            the {@link MetricRegistry} to use
		 */
		public PostgreSqlCopyInserter(EntityManagerFactory entityManagerFactory, MetricRegistry metrics) {
			this.entityManagerFactory = entityManagerFactory;
			this.metrics = metrics;

			List<CsvPrinterBundle> csvPrinters = new LinkedList<>();
			csvPrinters.add(createCsvPrinter(Beneficiary.class));
			csvPrinters.add(createCsvPrinter(CarrierClaim.class));
			csvPrinters.add(createCsvPrinter(CarrierClaimLine.class));
			this.csvPrinterBundles = csvPrinters;
		}

		/**
		 * @param entityType
		 *            the JPA {@link Entity} to create a {@link CSVPrinter} for
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
				csvPrinterBundle.csvPrinter = new CSVPrinter(new FileWriter(csvPrinterBundle.backingTempFile),
						baseCsvFormat);
				return csvPrinterBundle;
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} finally {
			}
		}

		/**
		 * Queues the specified {@link RifRecordEvent#getRecord()} top-level
		 * entity instance (e.g. a {@link Beneficiary}, {@link CarrierClaim},
		 * etc.) for insertion when {@link #submit()} is called.
		 * 
		 * @param record
		 *            the {@link RifRecordEvent#getRecord()} top-level entity
		 *            instance (e.g. a {@link Beneficiary}, {@link CarrierClaim}
		 *            , etc.) to queue for insertion
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
			} else
				throw new BadCodeMonkeyException();

			/*
			 * Hand off the *CsvWriter results to the appropriate
			 * CsvPrinterBundle entries.
			 */
			for (Entry<String, Object[][]> tableRecordsEntry : csvRecordsByTable.entrySet()) {
				String tableName = tableRecordsEntry.getKey();
				CsvPrinterBundle tablePrinterBundle = csvPrinterBundles.stream()
						.filter(b -> tableName.equals(b.tableName)).findAny().get();

				// Set the column header if it hasn't been yet.
				if (tablePrinterBundle.columnNames == null) {
					Object[] columnNamesAsObjects = tableRecordsEntry.getValue()[0];
					String[] columnNames = Arrays.copyOf(columnNamesAsObjects, columnNamesAsObjects.length,
							String[].class);
					tablePrinterBundle.columnNames = columnNames;
				}

				/*
				 * Write out each Object[] entity row to the temp CSV for the
				 * appropriate SQL table. These HAVE to be written out now, as
				 * there isn't enough RAM to store all of them in memory until
				 * submit() gets called.
				 */
				for (int recordIndex = 1; recordIndex < tableRecordsEntry.getValue().length; recordIndex++) {
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
		 * @return <code>true</code> if {@link #add(Object)} hasn't been called
		 *         yet, <code>false</code> if it has
		 */
		public boolean isEmpty() {
			return !csvPrinterBundles.stream().filter(b -> b.recordsPrinted.get() > 0).findAny().isPresent();
		}

		/**
		 * <p>
		 * Uses PostgreSQL's {@link CopyManager} API to bulk-insert all of the
		 * JPA entities that have been queued via {@link #add(Object)}.
		 * </p>
		 * <p>
		 * Note: this is an <em>efficient</em> operation, but still not
		 * necessarily a <em>fast</em> one: it can take hours to run for large
		 * amounts of data.
		 * </p>
		 */
		public void submit() {
			Timer.Context submitTimer = metrics
					.timer(MetricRegistry.name(getClass().getSimpleName(), "postgresSqlBatches", "submitted")).time();

			EntityManager entityManager = null;
			try {
				entityManager = entityManagerFactory.createEntityManager();

				/*
				 * PostgreSQL's CopyManager needs a raw PostgreSQL
				 * BaseConnection. So here we unwrap one from the EntityManager.
				 */
				Session session = entityManager.unwrap(Session.class);
				session.doWork(new Work() {
					/**
					 * @see org.hibernate.jdbc.Work#execute(java.sql.Connection)
					 */
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
						csvPrinterBundles.stream().filter(b -> b.recordsPrinted.get() > 0).forEach(b -> {
							try {
								LOGGER.debug("Flushing PostgreSQL COPY queue: '{}'.", b.backingTempFile);
								/*
								 * First, flush the CSVPrinter to ensure that
								 * all queued records are available on disk.
								 */
								b.csvPrinter.flush();

								/*
								 * Crack open the queued CSV records and feed
								 * them into the CopyManager.
								 */
								Timer.Context postgresCopyTimer = metrics.timer(
										MetricRegistry.name(getClass().getSimpleName(), "postgresCopy", "completed"))
										.time();
								LOGGER.info("Submitting PostgreSQL COPY queue: '{}'...", b.tableName);
								FileReader reader = new FileReader(b.backingTempFile);
								String columnsList = Arrays.stream(b.columnNames).map(c -> "\"" + c + "\"")
										.collect(Collectors.joining(", "));
								copyManager.copyIn(
										String.format("COPY \"%s\" (%s) FROM STDIN DELIMITERS ',' CSV ENCODING 'UTF8'",
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
				if (entityManager != null)
					entityManager.close();
			}

			submitTimer.stop();
		}

		/**
		 * @see java.lang.AutoCloseable#close()
		 */
		@Override
		public void close() {
			csvPrinterBundles.stream().forEach(b -> {
				try {
					b.csvPrinter.close();
					b.backingTempFile.delete();
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
		}

		/**
		 * A simple struct for storing all of the state and tracking information
		 * for each SQL table's {@link CSVPrinter}.
		 */
		private static final class CsvPrinterBundle {
			String tableName = null;
			CSVPrinter csvPrinter = null;
			File backingTempFile = null;
			String[] columnNames = null;
			AtomicInteger recordsPrinted = new AtomicInteger(0);
		}
	}

	/**
	 * Enumerates the {@link RifLoader} record handling strategies.
	 */
	private static enum LoadStrategy {
		INSERT_IDEMPOTENT,

		INSERT_UPDATE_NON_IDEMPOTENT;
	}

	/**
	 * Encapsulates the {@link RifLoader} record handling preferences.
	 */
	private static final class LoadFeatures {
		private final boolean idempotencyRequired;
		private final boolean copyDesired;

		/**
		 * Constructs a new {@link LoadFeatures} instance.
		 * 
		 * @param idempotencyRequired
		 *            the value to use for {@link #isIdempotencyRequired()}
		 * @param copyDesired
		 *            the value to use for {@link #isCopyDesired()}
		 */
		public LoadFeatures(boolean idempotencyRequired, boolean copyDesired) {
			this.idempotencyRequired = idempotencyRequired;
			this.copyDesired = copyDesired;
		}

		/**
		 * @return <code>true</code> if record inserts must be idempotent,
		 *         <code>false</code> if that's not required
		 */
		public boolean isIdempotencyRequired() {
			return idempotencyRequired;
		}

		/**
		 * @return <code>true</code> if PostgreSQL's {@link CopyManager} APIs
		 *         should be used to load data when possible, <code>false</code>
		 *         if not
		 */
		public boolean isCopyDesired() {
			return copyDesired;
		}
	}
}
