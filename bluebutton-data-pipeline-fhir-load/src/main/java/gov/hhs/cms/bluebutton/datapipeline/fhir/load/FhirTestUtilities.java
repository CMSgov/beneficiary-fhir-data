package gov.hhs.cms.bluebutton.datapipeline.fhir.load;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.dstu21.model.Bundle;
import org.hl7.fhir.dstu21.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu21.model.Conformance;
import org.hl7.fhir.dstu21.model.Conformance.ConditionalDeleteStatus;
import org.hl7.fhir.dstu21.model.Conformance.RestfulConformanceMode;

import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;
import com.justdavis.karl.misc.exceptions.unchecked.UncheckedUriSyntaxException;

import ca.uhn.fhir.rest.client.IGenericClient;
import gov.hhs.cms.bluebutton.datapipeline.fhir.LoadAppOptions;

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
	 * The address of the FHIR server to run tests against. See the parent
	 * project's <code>pom.xml</code> for details on how it's stood up.
	 */
	public static final String FHIR_API = "https://localhost:9094/baseDstu2";

	/**
	 * The password for {@link #getClientKeyStorePath()} and the key inside of
	 * it.
	 */
	public static final char[] CLIENT_KEY_STORE_PASSWORD = "changeit".toCharArray();

	/**
	 * The password for {@link #getClientTrustStorePath()}.
	 */
	public static final char[] CLIENT_TRUST_STORE_PASSWORD = "changeit".toCharArray();

	/**
	 * <strong>Serious Business:</strong> deletes all resources from the FHIR
	 * server used in tests.
	 */
	public static void cleanFhirServer() {
		// Before disabling this check, please go and update your resume.
		if (!FHIR_API.contains("localhost"))
			throw new BadCodeMonkeyException("Saving you from a career-changing event.");

		IGenericClient fhirClient = createFhirClient();
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
	 * @return the local {@link Path} to the key store that FHIR clients should
	 *         use
	 */
	public static Path getClientKeyStorePath() {
		/*
		 * The working directory for tests will either be the module directory
		 * or their parent directory. With that knowledge, we're searching for
		 * the ssl-stores directory.
		 */
		Path sslStoresDir = Paths.get("..", "dev", "ssl-stores");
		if (!Files.isDirectory(sslStoresDir))
			sslStoresDir = Paths.get("dev", "ssl-stores");
		if (!Files.isDirectory(sslStoresDir))
			throw new IllegalStateException();

		Path keyStorePath = sslStoresDir.resolve("client.keystore");
		return keyStorePath;
	}

	/**
	 * @return the local {@link Path} to the trust store that FHIR clients
	 *         should use
	 */
	public static Path getClientTrustStorePath() {
		Path trustStorePath = getClientKeyStorePath().getParent().resolve("client.truststore");
		return trustStorePath;
	}

	/**
	 * @return the {@link LoadAppOptions} that should be used in tests, which
	 *         specifies how to connect to the FHIR server that tests should be
	 *         run against
	 */
	public static LoadAppOptions getLoadOptions() {
		try {
			return new LoadAppOptions(new URI(FHIR_API), getClientKeyStorePath(), CLIENT_KEY_STORE_PASSWORD,
					getClientTrustStorePath(), CLIENT_TRUST_STORE_PASSWORD);
		} catch (URISyntaxException e) {
			throw new UncheckedUriSyntaxException(e);
		}
	}

	/**
	 * @return a FHIR {@link IGenericClient} that can be used to query the FHIR
	 *         server used in tests
	 */
	public static IGenericClient createFhirClient() {
		IGenericClient client = FhirLoader.createFhirClient(getLoadOptions());

		// Client logging can be enabled here, when needed.
		// LoggingInterceptor clientLogger = new LoggingInterceptor();
		// clientLogger.setLogRequestBody(false);
		// clientLogger.setLogResponseBody(false);
		// client.registerInterceptor(clientLogger);

		return client;
	}
}
