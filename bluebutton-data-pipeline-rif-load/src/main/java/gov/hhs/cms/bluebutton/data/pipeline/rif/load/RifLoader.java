package gov.hhs.cms.bluebutton.data.pipeline.rif.load;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.hibernate.tool.schema.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.zaxxer.hikari.HikariDataSource;

import gov.hhs.cms.bluebutton.data.model.rif.RifFileType;
import gov.hhs.cms.bluebutton.data.model.rif.RifRecordEvent;
import gov.hhs.cms.bluebutton.data.pipeline.rif.load.RifRecordLoadResult.LoadAction;

/**
 * Pushes CCW beneficiary and claims data from {@link RifRecordEvent}s into the
 * Blue Button API's database.
 */
public final class RifLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(RifLoader.class);

	/**
	 * The maximum number of {@link RifRecordEvent}s that will be collected
	 * before waiting for all submitted transactions to complete. This number is
	 * used to prevent memory overruns, but can (and should) be quite high:
	 * anything less than <code>1000</code> would be silly. (Setting it to
	 * <code>1</code> would effectively make processing serial, and anything
	 * less than {@link LoadAppOptions#getLoaderThreads()} would act as an
	 * unintended floor on concurrency.)
	 */
	private static final int WINDOW_SIZE = 100000;

	private final MetricRegistry metrics;
	private final EntityManagerFactory entityManagerFactory;
	private final ExecutorService loadExecutorService;

	/**
	 * Constructs a new {@link RifLoader} instance.
	 * @param options
	 *            the (injected) {@link LoadAppOptions} to use
	 * @param metrics
	 *            the {@link MetricRegistry} to use
	 */
	public RifLoader(LoadAppOptions options, MetricRegistry metrics) {
		this.metrics = metrics;

		this.entityManagerFactory = createEntityManagerFactory(options, metrics);
		this.loadExecutorService = Executors.newFixedThreadPool(options.getLoaderThreads());

		LOGGER.info("Configured to load in windows of '{}', with '{}' threads.", WINDOW_SIZE,
				options.getLoaderThreads());
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

		/*
		 * This is a super tricky chunk of code. Let's break it down (a bit out
		 * of reading order): 1) `.map(...)`: Each input bundle is mapped to a
		 * CompleteableFuture that will asynchronously send it off to the
		 * database server and return the result of that transaction. 2)
		 * `.collect(...)`: All of those futures are collected into groups
		 * called "windows". If this line weren't here (or with a window size of
		 * one, we'd actually be processing the bundles serially (and slowly!).
		 * An infinite window size would lead to memory overruns on large files,
		 * as we'd end up with a CompleteableFuture in memory for each record in
		 * the file. 3) `.forEach(...)` and `.join()`: Once everything's been
		 * submitted, we wait for each CompleteableFuture to complete and hand
		 * off each result to the handlers provided by the caller. If we didn't
		 * do that here, this method would return before processing had
		 * completed. This would lead to concurrently processing of multiple
		 * RifFiles, which would lead to data race errors.
		 */
		Consumer<List<CompletableFuture<RifRecordLoadResult>>> futureBatchProcessor = futures -> {
			LOGGER.trace("Processing window of {}. Peek at first element: {}", WINDOW_SIZE,
					futures.isEmpty() ? null : futures.get(0));
			futures.forEach(future -> {
				resultHandler.accept(future.exceptionally(errorHandlerWrapper).join());
			});
		};
		dataToLoad.map(bundle -> processAsync(bundle))
				.collect(BatchCollector.batchCollector(WINDOW_SIZE, futureBatchProcessor));

		LOGGER.trace("Completed process(...).");
	}

	/**
	 * @param rifRecordEvent
	 *            the {@link RifRecordEvent} to process
	 * @return a {@link CompletableFuture} for the {@link RifRecordLoadResult}
	 *         that models the results of the operation
	 */
	private CompletableFuture<RifRecordLoadResult> processAsync(RifRecordEvent<?> rifRecordEvent) {
		return CompletableFuture.supplyAsync(() -> {
			return process(rifRecordEvent);
		}, loadExecutorService);
	}

	/**
	 * @param rifRecordEvent
	 *            the {@link RifRecordEvent} to process
	 * @return the {@link RifRecordLoadResult} that models the results of the
	 *         operation
	 */
	public RifRecordLoadResult process(RifRecordEvent<?> rifRecordEvent) {
		String rifFileType = rifRecordEvent.getFile().getFileType().name();

		// Only one of each failure/success Timer.Contexts will be applied.
		Timer.Context timerBundleSuccess = metrics.timer(MetricRegistry.name(getClass(), "timer", "records", "loaded"))
				.time();
		Timer.Context timerBundleTypeSuccess = metrics
				.timer(MetricRegistry.name(getClass(), "timer", "records", rifFileType, "loaded")).time();
		Timer.Context timerBundleFailure = metrics.timer(MetricRegistry.name(getClass(), "timer", "records", "failed"))
				.time();
		Timer.Context timerBundleTypeFailure = metrics.timer(MetricRegistry.name(getClass(), "timer", "records",
				rifRecordEvent.getFile().getFileType().name(), "failed")).time();

		try {
			/*
			 * FIXME A batch size of 1 RIF record ain't great. But fixing that
			 * is an optimization for a later date.
			 */

			// FIXME remove this once everything is JPAified
			if (!Arrays.asList(RifFileType.BENEFICIARY, RifFileType.CARRIER)
					.contains(rifRecordEvent.getFile().getFileType()))
				return new RifRecordLoadResult(rifRecordEvent, LoadAction.LOADED);

			// Push the input bundle.
			LOGGER.trace("Loading '{}' record.", rifFileType);
			EntityManager entityManager = entityManagerFactory.createEntityManager();
			entityManager.getTransaction().begin();
			entityManager.persist(rifRecordEvent.getRecord());
			entityManager.getTransaction().commit();
			LOGGER.trace("Loaded '{}' record.", rifFileType);

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
		}
	}

	/**
	 * A somewhat hacky Java 8 {@link Stream} {@link Collector} that allows
	 * stream elements to be grouped/windowed/batched together. Rather than
	 * returning a single useful result, like other {@link Collector}s, this one
	 * actually just returns an empty {@link List} from
	 * {@link BatchCollector#finisher()}. However, it accepts a {@link Consumer}
	 * at construction that it will spit out accumulated {@link List}s of
	 * batches to as it goes along. It's definitely a non-standard usage of the
	 * API, but nonetheless very useful.
	 * 
	 * @param <T>
	 *            the type of input elements to the reduction operation
	 */
	private static final class BatchCollector<T> implements Collector<T, List<T>, List<T>> {

		private final int batchSize;
		private final Consumer<List<T>> batchProcessor;

		/**
		 * Creates a new batch collector
		 * 
		 * @param batchSize
		 *            the batch size after which the batchProcessor should be
		 *            called
		 * @param batchProcessor
		 *            the batch processor which accepts batches of records to
		 *            process
		 * @param <T>
		 *            the type of elements being processed
		 * @return a batch collector instance
		 */
		public static <T> Collector<T, List<T>, List<T>> batchCollector(int batchSize,
				Consumer<List<T>> batchProcessor) {
			return new BatchCollector<T>(batchSize, batchProcessor);
		}

		/**
		 * Constructs the batch collector
		 *
		 * @param batchSize
		 *            the batch size after which the batchProcessor should be
		 *            called
		 * @param batchProcessor
		 *            the batch processor which accepts batches of records to
		 *            process
		 */
		BatchCollector(int batchSize, Consumer<List<T>> batchProcessor) {
			batchProcessor = Objects.requireNonNull(batchProcessor);

			this.batchSize = batchSize;
			this.batchProcessor = batchProcessor;
		}

		/**
		 * @see java.util.stream.Collector#supplier()
		 */
		public Supplier<List<T>> supplier() {
			return ArrayList::new;
		}

		/**
		 * @see java.util.stream.Collector#accumulator()
		 */
		public BiConsumer<List<T>, T> accumulator() {
			return (ts, t) -> {
				ts.add(t);
				if (ts.size() >= batchSize) {
					batchProcessor.accept(ts);
					ts.clear();
				}
			};
		}

		/**
		 * @see java.util.stream.Collector#combiner()
		 */
		public BinaryOperator<List<T>> combiner() {
			return (ts, ots) -> {
				// process each parallel list without checking for batch size
				// avoids adding all elements of one to another
				// can be modified if a strict batching mode is required
				batchProcessor.accept(ts);
				batchProcessor.accept(ots);
				return Collections.emptyList();
			};
		}

		/**
		 * @see java.util.stream.Collector#finisher()
		 */
		public Function<List<T>, List<T>> finisher() {
			return ts -> {
				if (!ts.isEmpty())
					batchProcessor.accept(ts);
				return Collections.emptyList();
			};
		}

		/**
		 * @see java.util.stream.Collector#characteristics()
		 */
		public Set<Characteristics> characteristics() {
			return Collections.emptySet();
		}
	}
}
