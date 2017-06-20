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
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariProxyConnection;

import gov.hhs.cms.bluebutton.data.model.rif.Beneficiary;
import gov.hhs.cms.bluebutton.data.model.rif.BeneficiaryCsvWriter;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaim;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimCsvWriter;
import gov.hhs.cms.bluebutton.data.model.rif.CarrierClaimLine;
import gov.hhs.cms.bluebutton.data.model.rif.RecordAction;
import gov.hhs.cms.bluebutton.data.model.rif.RifFileType;
import gov.hhs.cms.bluebutton.data.model.rif.RifFilesEvent;
import gov.hhs.cms.bluebutton.data.model.rif.RifRecordEvent;
import gov.hhs.cms.bluebutton.data.pipeline.rif.load.RifRecordLoadResult.LoadAction;

/**
 * Pushes CCW beneficiary and claims data from {@link RifRecordEvent}s into the
 * Blue Button API's database.
 */
public final class RifLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(RifLoader.class);

	private final LoadAppOptions options;
	private final MetricRegistry metrics;
	private final EntityManagerFactory entityManagerFactory;
	private final boolean isPostgreSql;
	private final SecretKeyFactory secretKeyFactory;
	private final ExecutorService loadExecutorService;

	/**
	 * Constructs a new {@link RifLoader} instance.
	 * 
	 * @param options
	 *            the {@link LoadAppOptions} to use
	 * @param metrics
	 *            the {@link MetricRegistry} to use
	 */
	public RifLoader(LoadAppOptions options, MetricRegistry metrics) {
		this.options = options;
		this.metrics = metrics;

		this.entityManagerFactory = createEntityManagerFactory(options, metrics);
		this.isPostgreSql = isDatabasePostgreSql();

		this.secretKeyFactory = createSecretKeyFactory();

		/*
		 * A bit of a trick, here: our thread pool will only accept an
		 * (arbitrary) fixed number of pending tasks, before it starts rejecting
		 * new submissions. But because the "rejection policy" is
		 * CallerRunsPolicy, any rejected tasks will instead be run on the
		 * submitting thread. This effectively ensures that all submitted tasks
		 * get run, but that the number of submitted tasks is kept in check via
		 * this backpressure, preventing OutOfMemoryErrors. This trick and a
		 * similar one were found here:
		 * https://stackoverflow.com/a/9510713/1851299, and here:
		 * http://www.nurkiewicz.com/2014/11/executorservice-10-tips-and-tricks.
		 * html.
		 */
		BlockingQueue<Runnable> loadExecutorQueue = new ArrayBlockingQueue<>(options.getLoaderThreads() * 100);
		this.loadExecutorService = new ThreadPoolExecutor(options.getLoaderThreads(), options.getLoaderThreads(), 0L,
				TimeUnit.MILLISECONDS, loadExecutorQueue, new ThreadPoolExecutor.CallerRunsPolicy());

		LOGGER.info("Configured to load with '{}' threads.", options.getLoaderThreads());
	}

	/**
	 * @param options
	 *            the {@link LoadAppOptions} to use
	 * @param metrics
	 *            the {@link MetricRegistry} to use
	 * @return a JPA {@link EntityManagerFactory} for the Blue Button API's
	 *         database
	 */
	static EntityManagerFactory createEntityManagerFactory(LoadAppOptions options, MetricRegistry metrics) {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setMaximumPoolSize(options.getLoaderThreads());
		dataSource.setJdbcUrl(options.getDatabaseUrl());
		dataSource.setUsername(options.getDatabaseUsername());
		dataSource.setPassword(String.valueOf(options.getDatabasePassword()));
		dataSource.setRegisterMbeans(true);
		dataSource.setMetricRegistry(metrics);

		Map<String, Object> hibernateProperties = new HashMap<>();
		hibernateProperties.put(org.hibernate.cfg.AvailableSettings.DATASOURCE, dataSource);
		hibernateProperties.put(org.hibernate.cfg.AvailableSettings.HBM2DDL_AUTO, Action.UPDATE);

		EntityManagerFactory entityManagerFactory = Persistence
				.createEntityManagerFactory("gov.hhs.cms.bluebutton.data", hibernateProperties);
		return entityManagerFactory;
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
	public void process(Stream<RifRecordEvent<?>> dataToLoad, Consumer<Throwable> errorHandler,
			Consumer<RifRecordLoadResult> resultHandler) {
		LOGGER.trace("Started process(...)...");

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

		Function<Throwable, ? extends RifRecordLoadResult> errorHandlerWrapper = error -> {
			errorHandler.accept(error);

			/*
			 * If we eventually want to retry failed records (in some or all
			 * cases), this is the place to do/arrange that. Right now, though:
			 * we don't retry any errors.
			 */

			return null;
		};

		try (PostgreSqlCopyInserter postgresBatch = new PostgreSqlCopyInserter(entityManagerFactory, metrics)) {
			/*
			 * We need a way to wait for all of the asynchronous jobs to
			 * complete, without keeping all of their Futures in memory (which
			 * would cause OutOfMemoryErrors). This calls for something like the
			 * JDK's CountDownLatch, but that also supports incrementing. The
			 * Phaser class supports this use case, with its register() method
			 * being equivalent to a countUp() and its arrive*() methods being
			 * equivalent to countDown(). See its JavaDoc for an example of
			 * exactly this use case.
			 */
			Phaser phaser = new Phaser(1);

			// Submit every record in the stream for loading.
			dataToLoad.forEach(rifRecordEvent -> {
				/*
				 * Increment the Phaser to mark that a new task is pending.
				 * Then, create the decrementer function that we will register
				 * with the task's CompletableFuture.
				 */
				phaser.register();
				BiFunction<RifRecordLoadResult, Throwable, RifRecordLoadResult> phaserDecrementer = (r, t) -> {
					phaser.arriveAndDeregister();
					return r != null ? r : null;
				};

				/*
				 * Submit the RifRecordEvent for asynchronous processing. Note
				 * that, due to the ExecutorService's configuration (see in
				 * constructor), this will block if too many tasks are already
				 * pending. That's desirable behavior, as it prevents
				 * OutOfMemoryErrors.
				 */
				CompletableFuture<RifRecordLoadResult> recordResultFuture = processAsync(rifRecordEvent, postgresBatch);

				/*
				 * Wire the up the Future to notify the Phaser decrementer and
				 * error/result handler, as appropriate, when it completes.
				 */
				recordResultFuture.handle(phaserDecrementer).exceptionally(errorHandlerWrapper)
						.thenAccept(resultHandler);
			});

			// Wait for all submitted tasks to complete.
			phaser.arriveAndAwaitAdvance();

			// Submit the queued PostgreSQL COPY operations, if any.
			if (!postgresBatch.isEmpty()) {
				postgresBatch.submit();
			}
		}

		LOGGER.trace("Completed process(...).");
	}

	/**
	 * @param rifRecordEvent
	 *            the {@link RifRecordEvent} to process
	 * @param postgresBatch
	 *            the {@link PostgreSqlCopyInserter} for the current set of
	 *            {@link RifFilesEvent}s being processed
	 * @return a {@link CompletableFuture} for the {@link RifRecordLoadResult}
	 *         that models the results of the operation
	 */
	private CompletableFuture<RifRecordLoadResult> processAsync(RifRecordEvent<?> rifRecordEvent,
			PostgreSqlCopyInserter postgresBatch) {
		return CompletableFuture.supplyAsync(() -> {
			return process(rifRecordEvent, postgresBatch);
		}, loadExecutorService);
	}

	/**
	 * @param rifRecordEvent
	 *            the {@link RifRecordEvent} to process
	 * @param postgresBatch
	 *            the {@link PostgreSqlCopyInserter} for the current set of
	 *            {@link RifFilesEvent}s being processed
	 * @return the {@link RifRecordLoadResult} that models the results of the
	 *         operation
	 */
	public RifRecordLoadResult process(RifRecordEvent<?> rifRecordEvent, PostgreSqlCopyInserter postgresBatch) {
		String rifFileType = rifRecordEvent.getFile().getFileType().name();

		// If this is a Beneficiary record, first hash its HICN.
		if (rifRecordEvent.getFile().getFileType() == RifFileType.BENEFICIARY) {
			hashBeneficiaryHicn(rifRecordEvent);
		}

		// Only one of each failure/success Timer.Contexts will be applied.
		Timer.Context timerBundleSuccess = metrics.timer(MetricRegistry.name(getClass(), "timer", "records", "loaded"))
				.time();
		Timer.Context timerBundleTypeSuccess = metrics
				.timer(MetricRegistry.name(getClass(), "timer", "records", rifFileType, "loaded")).time();
		Timer.Context timerBundleFailure = metrics.timer(MetricRegistry.name(getClass(), "timer", "records", "failed"))
				.time();
		Timer.Context timerBundleTypeFailure = metrics.timer(MetricRegistry.name(getClass(), "timer", "records",
				rifRecordEvent.getFile().getFileType().name(), "failed")).time();

		EntityManager entityManager = null;
		try {
			/*
			 * FIXME A batch size of 1 RIF record ain't great. But fixing that
			 * is an optimization for a later date.
			 */

			// FIXME remove this once everything is JPAified
			if (!Arrays.asList(RifFileType.BENEFICIARY, RifFileType.CARRIER)
					.contains(rifRecordEvent.getFile().getFileType()))
				return new RifRecordLoadResult(rifRecordEvent, LoadAction.LOADED);

			/*
			 * If we can, load the record using PostgreSQL's native copy APIs,
			 * which are ludicrously fast.
			 */
			if (rifRecordEvent.getRecordAction() == RecordAction.INSERT && isPostgreSql) {
				postgresBatch.add(rifRecordEvent.getRecord());
			} else {
				// Push the record, if it's not already in DB.
				LOGGER.trace("Loading '{}' record.", rifFileType);
				entityManager = entityManagerFactory.createEntityManager();
				entityManager.getTransaction().begin();

				Object record = rifRecordEvent.getRecord();
				Object recordId = entityManagerFactory.getPersistenceUnitUtil().getIdentifier(record);
				if (recordId == null)
					throw new BadCodeMonkeyException();
				if (entityManager.find(record.getClass(), recordId) == null)
					entityManager.persist(record);

				entityManager.getTransaction().commit();
				LOGGER.trace("Loaded '{}' record.", rifFileType);
			}

			// Update the metrics now that things have been pushed.
			timerBundleSuccess.stop();
			timerBundleTypeSuccess.stop();
			metrics.meter(MetricRegistry.name(getClass(), "meter", "records", "loaded")).mark(1);

			return new RifRecordLoadResult(rifRecordEvent, LoadAction.LOADED);
		} catch (Throwable t) {
			timerBundleFailure.stop();
			timerBundleTypeFailure.stop();
			metrics.meter(MetricRegistry.name(getClass(), "meter", "records", "failed")).mark(1);
			LOGGER.trace("Failed to load '{}' record.", rifFileType);

			throw new RifLoadFailure(rifRecordEvent, t);
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
	 * @param rifRecordEvent
	 *            the {@link RifRecordEvent} to (possibly) modify
	 */
	private void hashBeneficiaryHicn(RifRecordEvent<?> rifRecordEvent) {
		if (rifRecordEvent.getFile().getFileType() != RifFileType.BENEFICIARY)
			return;

		Timer.Context timerHashing = metrics.timer(MetricRegistry.name(getClass(), "timer", "hicnsHashed")).time();

		Beneficiary beneficiary = (Beneficiary) rifRecordEvent.getRecord();
		beneficiary.setHicn(computeHicnHash(beneficiary.getHicn()));

		timerHashing.stop();
	}

	/**
	 * @return a new {@link SecretKeyFactory} for the
	 *         <code>PBKDF2WithHmacSHA256</code> algorithm
	 */
	private static SecretKeyFactory createSecretKeyFactory() {
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
	 * @param hicn
	 *            the Medicare beneficiary HICN to be hashed
	 * @return a one-way cryptographic hash of the specified HICN value, exactly
	 *         64 characters long
	 */
	private String computeHicnHash(String hicn) {
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
	 * In <a href=" Populating a Database">PostgreSQL 9.6 Manual: Populating a
	 * Database</a>, this is recommended as the fastest way to insert large
	 * amounts of data. However, real-world testing with Blue Button data has
	 * shown that to be not be exactly true: highly parallelized
	 * <code>INSERT</code>s (e.g. hundreds of simultaneous connections) can
	 * actually be about 18% faster. Even still, this code may eventually be
	 * useful for some situations, so we'll keep it around.
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
					.timer(MetricRegistry.name(getClass(), "timer", "postgresSqlBatches", "submitted")).time();

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
								Timer.Context postgresCopyTimer = metrics
										.timer(MetricRegistry.name(getClass(), "timer", "postgresCopy", "completed"))
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
}
