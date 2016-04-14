package gov.hhs.cms.bluebutton.fhirstress.backend;

import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.hl7.fhir.dstu21.model.Bundle;
import org.hl7.fhir.dstu21.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu21.model.DomainResource;
import org.hl7.fhir.dstu21.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu21.model.Patient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.IGenericClient;

/**
 * This JMeter sampler will run a search for a random FHIR {@link Patient} and
 * then retrieve that {@link Patient} and all of their
 * {@link ExplanationOfBenefit}s.
 */
public final class RetrievePatientAndAllEobs extends AbstractJavaSamplerClient {
	private static final String PARAM_SERVER = "fhir_server";
	private static final String HOSTNAME_UNKNOWN = "unknown-host";

	private Random rng = new Random();
	private String hostName;
	private IGenericClient client;
	private List<String> patientIds;

	/**
	 * @see org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient#getDefaultParameters()
	 */
	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultParameters = new Arguments();
		defaultParameters.addArgument(PARAM_SERVER, "http://localhost:8080/hapi-fhir/baseDstu2");
		return defaultParameters;
	}

	/**
	 * @see org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient#setupTest(org.apache.jmeter.protocol.java.sampler.JavaSamplerContext)
	 */
	@Override
	public void setupTest(JavaSamplerContext context) {
		super.setupTest(context);

		this.hostName = getHostname();

		this.client = createFhirClient(context.getParameter(PARAM_SERVER));

		// Find the IDs that can be queried in each sample run.
		this.patientIds = findPatientIds();
	}

	/**
	 * @return the hostname of the system this code is running on, or
	 *         {@link #HOSTNAME_UNKNOWN} is it can't be determined
	 */
	private static String getHostname() {
		String hostname = null;
		if (hostname == null)
			hostname = System.getenv().get("HOSTNAME");
		if (hostname == null)
			hostname = System.getenv().get("COMPUTERNAME");
		try {
			if (hostname == null)
				hostname = Inet4Address.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = null;
		}

		return hostname != null ? hostname : HOSTNAME_UNKNOWN;
	}

	/**
	 * @return the {@link Patient#getId()}s that should be queried during sample
	 *         executions
	 */
	private List<String> findPatientIds() {
		List<String> patientIds = new ArrayList<>(100);

		Bundle bundleOfResults = client.search().forResource(Patient.class).returnBundle(Bundle.class)
				.elementsSubset(DomainResource.RES_ID.getParamName()).execute();
		while (bundleOfResults != null && patientIds.size() < 100) {
			// Add in all of the patient IDs from this page.
			for (BundleEntryComponent bundleEntry : bundleOfResults.getEntry()) {
				patientIds.add(bundleEntry.getResource().getId());
			}

			// Grab the next page, if there is one.
			if (bundleOfResults.getLink(Bundle.LINK_NEXT) != null) {
				bundleOfResults = client.loadPage().next(bundleOfResults).execute();
			} else {
				bundleOfResults = null;
			}
		}

		return patientIds;
	}

	/**
	 * @param fhirServerUrlText
	 *            a {@link String} for the URL of the FHIR server to create a
	 *            client for
	 * @return a new FHIR {@link IGenericClient} instance
	 */
	private IGenericClient createFhirClient(String fhirServerUrlText) {
		FhirContext ctx = FhirContext.forDstu2_1();
		ctx.getRestfulClientFactory().setSocketTimeout(600 * 1000);
		IGenericClient client = ctx.newRestfulGenericClient(fhirServerUrlText);
		return client;
	}

	/**
	 * @see org.apache.jmeter.protocol.java.sampler.JavaSamplerClient#runTest(org.apache.jmeter.protocol.java.sampler.JavaSamplerContext)
	 */
	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		SampleResult sample = new SampleResult();
		sample.sampleStart();
		try {
			runTest();

			sample.sampleEnd();
			sample.setSuccessful(true);
			sample.setResponseMessage("Sample succeeded on host: " + hostName);
			sample.setResponseCodeOK();
		} catch (IllegalArgumentException e) {
			// Mark this sample iteration as failed.
			sample.sampleEnd();
			sample.setSuccessful(false);
			sample.setResponseMessage("Exception: " + e);

			/*
			 * Serialize the stack trace to a String and attach it to the sample
			 * results.
			 */
			StringWriter stringWriter = new StringWriter();
			e.printStackTrace(new java.io.PrintWriter(stringWriter));
			sample.setResponseData(stringWriter.toString(), StandardCharsets.UTF_8.name());
			sample.setDataType(org.apache.jmeter.samplers.SampleResult.TEXT);
			sample.setResponseCode("500");
		}

		return sample;
	}

	/**
	 * Actually run the test.
	 */
	private void runTest() {
		String patientId = patientIds.get(rng.nextInt(patientIds.size()));
		Bundle currentResultsPage = client.search().forResource(Patient.class)
				.where(DomainResource.RES_ID.matchesExactly().value(patientId))
				.revInclude(ExplanationOfBenefit.INCLUDE_PATIENT).returnBundle(Bundle.class).execute();
		while (currentResultsPage != null) {
			// Keep grabbing the next page, until there isn't one.
			if (currentResultsPage.getLink(Bundle.LINK_NEXT) != null) {
				currentResultsPage = client.loadPage().next(currentResultsPage).execute();
			} else {
				currentResultsPage = null;
			}
		}
	}
}
