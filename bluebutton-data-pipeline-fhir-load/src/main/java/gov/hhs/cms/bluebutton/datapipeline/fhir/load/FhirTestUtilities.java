package gov.hhs.cms.bluebutton.datapipeline.fhir.load;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu21.model.Bundle;
import org.hl7.fhir.dstu21.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu21.model.Conformance;
import org.hl7.fhir.dstu21.model.Conformance.ConditionalDeleteStatus;
import org.hl7.fhir.dstu21.model.Conformance.RestfulConformanceMode;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.IGenericClient;

/**
 * <p>
 * Contains utilities that are useful when running against the FHIR server.
 * </p>
 * <p>
 * This is being left in <code>src/main</code> so that it can be used from other
 * modules' tests, without having to delve into classpath dark arts.
 * </p>
 */
public final class FhirTestUtilities {
	/**
	 * <strong>Serious Business:</strong> deletes all resources from the
	 * specified FHIR server.
	 * 
	 * @param fhirApiUrl
	 *            the "base" FHIR API endpoint of the server to create a client
	 *            for, e.g. <code>http://localhost:9093/baseDstu2</code>
	 */
	public static void cleanFhirServer(String fhirApiUrl) {
		// Before disabling this check, please go and update your resume.
		if (!fhirApiUrl.contains("localhost"))
			throw new BadCodeMonkeyException("Saving you from a career-changing event.");

		IGenericClient fhirClient = createFhirClient(fhirApiUrl);
		Conformance conformance = fhirClient.fetchConformance().ofType(Conformance.class).execute();

		/*
		 * This is ugly code, but not worth making more readable. Here's what it
		 * does: grabs the server's conformance statement, looks at the
		 * supported resources, and then grabs all of the resource type names
		 * that support bulk conditional delete operations.
		 */
		List<String> resourcesToDelete = conformance.getRest().stream()
				.filter(r -> r.getMode() == RestfulConformanceMode.SERVER).flatMap(r -> r.getResource().stream())
				.filter(r -> r.getConditionalDelete() != null)
				.filter(r -> r.getConditionalDelete() == ConditionalDeleteStatus.MULTIPLE).map(r -> r.getType())
				.collect(Collectors.toList());

		// Loop over each resource that can be deleted, and delete all of them.
		/*
		 * TODO This commented-out version should work, given HAPI 1.4's
		 * conformance statement, but doesn't. Try again in a later version? The
		 * not-commented-out version below does work, but is slower.
		 */
		// for (String resourceTypeName : resourcesToDelete)
		// fhirClient.delete().resourceConditionalByUrl(resourceTypeName).execute();
		for (String resourceTypeName : resourcesToDelete) {
			Bundle results = fhirClient.search().forResource(resourceTypeName).returnBundle(Bundle.class).execute();
			while (true) {
				for (BundleEntryComponent resourceEntry : results.getEntry())
					fhirClient.delete()
							.resourceById(resourceTypeName, resourceEntry.getResource().getIdElement().getIdPart())
							.execute();

				// Get next page of results (if there is one), or exit loop.
				if (results.getLink(Bundle.LINK_NEXT) != null)
					results = fhirClient.loadPage().next(results).execute();
				else
					break;
			}
		}
	}

	/**
	 * @param fhirApiUrl
	 *            the "base" FHIR API endpoint of the server to create a client
	 *            for, e.g. <code>http://example.com:9093/baseDstu2</code>
	 * @return a FHIR {@link IGenericClient} that can be used to query the
	 *         specified FHIR server
	 */
	public static IGenericClient createFhirClient(String fhirApiUrl) {
		FhirContext ctx = FhirContext.forDstu2_1();
		IGenericClient client = ctx.newRestfulGenericClient(fhirApiUrl);

		// Client logging can be enabled here, when needed.
		// LoggingInterceptor clientLogger = new LoggingInterceptor();
		// clientLogger.setLogRequestBody(false);
		// clientLogger.setLogResponseBody(false);
		// client.registerInterceptor(clientLogger);

		return client;
	}
}
