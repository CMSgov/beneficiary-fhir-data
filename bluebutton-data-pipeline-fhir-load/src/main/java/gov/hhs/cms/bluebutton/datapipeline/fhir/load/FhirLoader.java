package gov.hhs.cms.bluebutton.datapipeline.fhir.load;

import java.util.List;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.hl7.fhir.dstu21.model.Bundle;
import org.hl7.fhir.dstu21.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu21.model.Bundle.HTTPVerb;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu21.model.IdType;
import org.hl7.fhir.dstu21.model.Resource;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import gov.hhs.cms.bluebutton.datapipeline.fhir.LoadAppOptions;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.BeneficiaryBundle;
import gov.hhs.cms.bluebutton.datapipeline.fhir.transform.TransformedBundle;
import rx.Observable;

/**
 * Pushes already-transformed CCW data ({@link ExplanationOfBenefit} records)
 * into a FHIR server.
 */
public final class FhirLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(FhirLoader.class);

	private final MetricRegistry metrics;
	private final IGenericClient client;

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

		FhirContext ctx = FhirContext.forDstu2_1();
		/*
		 * The default timeout is 10s, which was failing for batches of 100. A
		 * 300s timeout was failing for batches of 100 once Part B claims were
		 * mostly mapped, so batches were cut to 10, which ran at 12s or so,
		 * each.
		 */
		ctx.getRestfulClientFactory().setSocketTimeout(300 * 1000);
		IGenericClient client = ctx.newRestfulGenericClient(options.getFhirServer().toString());
		LoggingInterceptor fhirClientLogging = new LoggingInterceptor();
		fhirClientLogging.setLogRequestBody(LOGGER.isTraceEnabled());
		fhirClientLogging.setLogResponseBody(false);
		if (LOGGER.isInfoEnabled())
			client.registerInterceptor(fhirClientLogging);

		this.client = client;
	}

	/**
	 * @param dataToLoad
	 *            the FHIR {@link BeneficiaryBundle}s to be pushed to a FHIR
	 *            server
	 * @return the {@link FhirResult}s that record the results of each batch
	 *         FHIR operation
	 */
	public List<FhirResult> insertFhirRecords(Stream<BeneficiaryBundle> dataToLoad) {
		/*
		 * Fun trivia: this one line of code literally took three hours to write
		 * (took ages to decide that RxJava was the best choice, and then figure
		 * out how to actually use it. For background: the only reason it's
		 * needed at all is that there's no reasonably simple way to
		 * batch/buffer items from plain Java 8 streams.
		 */
		Timer.Context timerContextStream = metrics.timer(MetricRegistry.name(getClass(), "stream")).time();
		List<FhirResult> batchResults = Observable.from(dataToLoad::iterator).buffer(10).map(batch -> process(batch))
				.toList().toBlocking().single();
		timerContextStream.stop();

		return batchResults;
	}

	/**
	 * @param batch
	 *            the batch of {@link BeneficiaryBundle}s to push to the FHIR
	 *            server
	 * @return a {@link FhirResult} that summarizes the results of the operation
	 *         without keeping all of the pushed objects in memory
	 */
	private FhirResult process(List<BeneficiaryBundle> batch) {
		Timer.Context timerContextBatch = metrics.timer(MetricRegistry.name(getClass(), "stream", "batch")).time();

		// Create the bundle to push.
		Bundle bundle = new Bundle();
		for (BeneficiaryBundle beneficiaryBundle : batch) {
			for (IBaseResource resource : beneficiaryBundle.getFhirResources()) {
				Resource typedResource = (Resource) resource;
				bundle.addEntry().setFullUrl(typedResource.getId()).setResource(typedResource).getRequest()
						.setMethod(HTTPVerb.POST);
			}
		}

		// Push the bundle.
		LOGGER.trace("Loading bundle with {} beneficiaries and {} resources.", batch.size(), bundle.getEntry().size());
		Bundle resultBundle = client.transaction().withBundle(bundle).execute();

		// Update the metrics now that things have been pushed.
		timerContextBatch.stop();
		metrics.meter(MetricRegistry.name(getClass(), "stream", "processed-beneficiaries")).mark(batch.size());
		metrics.meter(MetricRegistry.name(getClass(), "stream", "processed-resources"))
				.mark(resultBundle.getEntry().size());

		/*
		 * FIXME this count is inaccurate: need to verify status of each
		 * response entry
		 */
		return new FhirResult(resultBundle.getEntry().size());
	}

	/**
	 * @param dataToLoad
	 *            the FHIR {@link TransformedBundle}s to be loaded to a FHIR
	 *            server
	 * @return the {@link FhirResult}s that record the results of each batch
	 *         FHIR operation
	 */
	public Stream<FhirBundleResult> process(Stream<TransformedBundle> dataToLoad) {
		Timer.Context timerContextStream = metrics.timer(MetricRegistry.name(getClass(), "stream")).time();
		Stream<FhirBundleResult> resultsStream = dataToLoad.map(bundle -> process(bundle));
		timerContextStream.stop();

		return resultsStream;
	}

	/**
	 * @param inputBundle
	 *            the input {@link TransformedBundle} to process
	 * @return a {@link FhirBundleResult} that models the results of the
	 *         operation
	 */
	private FhirBundleResult process(TransformedBundle inputBundle) {
		Timer.Context timerContextBatch = metrics.timer(MetricRegistry.name(getClass(), "stream", "bundle")).time();

		/*
		 * Set the bundle entries' very very required `fullUrl` field. This has
		 * to be set here, as the Transformer doesn't (and shouldn't) know the
		 * FHIR server's base URL.
		 */
		for (BundleEntryComponent entry : inputBundle.getResult().getEntry()) {
			if (entry.getResource().hasId() && !entry.getResource().getId().startsWith("urn:"))
				entry.setFullUrl(String.format("%s/%s", client.getServerBase(), entry.getResource().getId()));
			else
				entry.setFullUrl(IdType.newRandomUuid().asStringValue());
		}

		// Push the input bundle.
		int inputBundleCount = inputBundle.getResult().getEntry().size();
		LOGGER.trace("Loading bundle with {} resources", inputBundleCount);
		Bundle resultBundle = client.transaction().withBundle(inputBundle.getResult()).execute();

		// Update the metrics now that things have been pushed.
		timerContextBatch.stop();
		metrics.meter(MetricRegistry.name(getClass(), "stream", "processed-bundles")).mark(1);
		metrics.meter(MetricRegistry.name(getClass(), "stream", "processed-resources")).mark(inputBundleCount);

		return new FhirBundleResult(inputBundle, resultBundle);
	}
}
