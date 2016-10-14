package gov.hhs.cms.bluebutton.datapipeline.fhir.load;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.net.ssl.SSLContext;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContexts;
import org.hl7.fhir.dstu21.model.Bundle;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import gov.hhs.cms.bluebutton.datapipeline.fhir.LoadAppOptions;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.TransformedBundle;

/**
 * Pushes already-transformed CCW data ({@link ExplanationOfBenefit} records)
 * into a FHIR server.
 */
public final class FhirLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(FhirLoader.class);

	/**
	 * The number of threads that will be used to simultaneously process
	 * {@link #process(TransformedBundle)} operations.
	 */
	private static final int PARALLELISM = 10;

	/**
	 * The maximum number of FHIR transactions that will be collected before
	 * waiting for all submitted transactions to complete. This number is used
	 * to prevent memory overruns, but can (and should) be quite high: anything
	 * less than <code>1000</code> would be silly. (Setting it to <code>1</code>
	 * would effectively make processing serial, and anything less than
	 * {@link #PARALLELISM} would act as an unintended floor on concurrency.)
	 */
	private static final int WINDOW_SIZE = 1000;

	private final MetricRegistry metrics;
	private final IGenericClient client;
	private final ExecutorService loadExecutorService;

	/**
	 * A utility method that centralizes the creation of FHIR
	 * {@link IGenericClient}s.
	 * 
	 * @param options
	 *            the {@link LoadAppOptions} to use
	 * @return a new FHIR {@link IGenericClient} for use
	 */
	public static IGenericClient createFhirClient(LoadAppOptions options) {
		FhirContext ctx = FhirContext.forDstu2_1();

		/*
		 * The default timeout is 10s, which was failing for batches of 100. A
		 * 300s timeout was failing for batches of 100 once Part B claims were
		 * mostly mapped, so batches were cut to 10, which ran at 12s or so,
		 * each.
		 */
		ctx.getRestfulClientFactory().setSocketTimeout(300 * 1000);

		/*
		 * We need to override the FHIR client's SSLContext. Unfortunately, that
		 * requires overriding the entire HttpClient that it uses. Otherwise,
		 * the settings used here mirror those that the default FHIR HttpClient
		 * would use.
		 */
		try {
			SSLContext sslContext = SSLContexts.custom()
					.loadKeyMaterial(options.getKeyStorePath().toFile(), options.getKeyStorePassword(),
							options.getKeyStorePassword())
					.loadTrustMaterial(options.getTrustStorePath().toFile(), options.getTrustStorePassword()).build();
			PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
					RegistryBuilder.<ConnectionSocketFactory> create()
							.register("http", PlainConnectionSocketFactory.getSocketFactory())
							.register("https", new SSLConnectionSocketFactory(sslContext)).build(),
					null, null, null, 5000, TimeUnit.MILLISECONDS);
			connectionManager.setDefaultMaxPerRoute(PARALLELISM * 2);
			connectionManager.setMaxTotal(PARALLELISM * 2);
			@SuppressWarnings("deprecation")
			RequestConfig defaultRequestConfig = RequestConfig.custom()
					.setSocketTimeout(ctx.getRestfulClientFactory().getSocketTimeout())
					.setConnectTimeout(ctx.getRestfulClientFactory().getConnectTimeout())
					.setConnectionRequestTimeout(ctx.getRestfulClientFactory().getConnectionRequestTimeout())
					.setStaleConnectionCheckEnabled(true).build();
			HttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager)
					.setDefaultRequestConfig(defaultRequestConfig).disableCookieManagement().build();
			ctx.getRestfulClientFactory().setHttpClient(httpClient);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException
				| CertificateException e) {
			throw new IllegalStateException(e);
		}

		IGenericClient client = ctx.newRestfulGenericClient(options.getFhirServer().toString());

		LoggingInterceptor fhirClientLogging = new LoggingInterceptor();
		fhirClientLogging.setLogRequestBody(LOGGER.isTraceEnabled());
		fhirClientLogging.setLogResponseBody(LOGGER.isTraceEnabled());
		if (LOGGER.isDebugEnabled())
			client.registerInterceptor(fhirClientLogging);

		return client;
	}

	/**
	 * Constructs a new {@link FhirLoader} instance.
	 * 
	 * @param metrics
	 *            the {@link MetricRegistry} to use
	 * @param options
	 *            the (injected) {@link LoadAppOptions} to use
	 */
	@Inject
	public FhirLoader(MetricRegistry metrics, LoadAppOptions options) {
		this.metrics = metrics;

		IGenericClient client = createFhirClient(options);
		this.client = client;
		this.loadExecutorService = Executors.newFixedThreadPool(PARALLELISM);
	}

	/**
	 * <p>
	 * Consumes the input {@link Stream} of {@link TransformedBundle}s, pushing
	 * each transaction bundle to the configured FHIR server, and passing the
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
	 *            the FHIR {@link TransformedBundle}s to be loaded to a FHIR
	 *            server
	 * @param errorHandler
	 *            the {@link Consumer} to pass each error that occurs to
	 *            (possibly one error per {@link TransformedBundle}, if every
	 *            input element fails to load), which will be run on the
	 *            caller's thread
	 * @param resultHandler
	 *            the {@link Consumer} to pass each the {@link FhirBundleResult}
	 *            for each of the successfully-processed input
	 *            {@link TransformedBundle}s, which will be run on the caller's
	 *            thread
	 */
	public void process(Stream<TransformedBundle> dataToLoad, Consumer<Throwable> errorHandler,
			Consumer<FhirBundleResult> resultHandler) {
		LOGGER.trace("Started process(...)...");

		/*
		 * Design history note: Initially, this function just returned a stream
		 * of CompleteableFutures, which seems like the obvious choice.
		 * Unfortunately, that ends up being rather hard to use correctly. Had
		 * some tests that were misusing it and ended up unintentionally forcing
		 * the processing back to being serial. Also, it leads to a ton of
		 * copy-pasted code. Those, we just return void, and instead accept
		 * handlers that folks can do whatever they want with. It makes things
		 * harder for the tests to inspect, but also ensures that the loading is
		 * always run in a consistent manner.
		 */

		Function<Throwable, ? extends FhirBundleResult> errorHandlerWrapper = error -> {
			errorHandler.accept(error);

			/*
			 * If we eventually want to retry failed bundles (in some or all
			 * cases), this is the place to do/arrange that. Right now, though:
			 * we don't retry any errors.
			 */

			return null;
		};

		/*
		 * This is a super tricky and chunk of code. Let's break it down (a bit
		 * out of reading order): 1) `.map(...)`: Each input bundle is mapped to
		 * a CompleteableFuture that will asynchronously send it off to the FHIR
		 * server and return the result of that transaction. 2) `.collect(...)`:
		 * All of those futures are collected into batches/windows. If this line
		 * weren't here (or with a window size of one, we'd actually be
		 * processing the bundles serially (and slowly!). An infinite window
		 * size would lead to memory overruns on large files, as we'd end up
		 * with a CompleteableFuture in memory for each record in the file. 3)
		 * `.forEach(...)` and `.join()`: Once everything's been submitted, we
		 * wait for each CompleteableFuture to complete and hand off each result
		 * to the handlers provided by the caller. If we didn't do that here,
		 * this method would return before processing had completed. This would
		 * lead to concurrently process multiple RifFiles, which would lead to
		 * data race errors.
		 */
		Consumer<List<CompletableFuture<FhirBundleResult>>> futureBatchProcessor = futures -> {
			LOGGER.trace("Processing window of {}. Peek at first element: {}", WINDOW_SIZE,
					futures.isEmpty() ? null : futures.get(0));
			futures.forEach(future -> {
				resultHandler.accept(future.exceptionally(errorHandlerWrapper).join());
			});
		};
		dataToLoad.map(bundle -> process(bundle))
				.collect(BatchCollector.batchCollector(WINDOW_SIZE, futureBatchProcessor));

		LOGGER.trace("Completed process(...).");
	}

	/**
	 * @param inputBundle
	 *            the input {@link TransformedBundle} to process
	 * @return a {@link CompletableFuture} for the {@link FhirBundleResult} that
	 *         models the results of the operation
	 */
	private CompletableFuture<FhirBundleResult> process(TransformedBundle inputBundle) {
		return CompletableFuture.supplyAsync(() -> {
			// Only one of these two Timer.Contexts will be applied.
			Timer.Context timerBundleSuccess = metrics
					.timer(MetricRegistry.name(getClass(), "timer", "bundles", "loaded")).time();
			Timer.Context timerBundleFailure = metrics
					.timer(MetricRegistry.name(getClass(), "timer", "bundles", "failed")).time();

			int inputBundleCount = inputBundle.getResult().getEntry().size();
			try {
				// Push the input bundle.
				LOGGER.trace("Loading bundle with {} resources", inputBundleCount);
				Bundle resultBundle = client.transaction().withBundle(inputBundle.getResult()).execute();
				LOGGER.trace("Loaded bundle with {} resources", inputBundleCount);

				// Update the metrics now that things have been pushed.
				timerBundleSuccess.stop();
				metrics.meter(MetricRegistry.name(getClass(), "meter", "bundles", "loaded")).mark(1);
				metrics.meter(MetricRegistry.name(getClass(), "meter", "resources", "loaded")).mark(inputBundleCount);

				return new FhirBundleResult(inputBundle, resultBundle);
			} catch (Throwable t) {
				timerBundleFailure.stop();
				metrics.meter(MetricRegistry.name(getClass(), "meter", "bundles", "failed")).mark(1);
				metrics.meter(MetricRegistry.name(getClass(), "meter", "resources", "failed")).mark(inputBundleCount);
				LOGGER.trace("Failed to load bundle with {} resources", inputBundleCount);

				throw new FhirLoadFailure(inputBundle, t);
			}
		}, loadExecutorService);
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
