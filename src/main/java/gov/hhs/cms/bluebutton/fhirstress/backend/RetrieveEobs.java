package gov.hhs.cms.bluebutton.fhirstress.backend;

import gov.hhs.cms.bluebutton.fhirclient.FhirClient;
import gov.hhs.cms.bluebutton.rifparser.RifParser;
import gov.hhs.cms.bluebutton.rifparser.RifEntry;

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
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.dstu3.model.DomainResource;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Provenance;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Coverage;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.TransformerUtils;
import gov.hhs.cms.bluebutton.server.app.stu3.providers.MedicareSegment;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

/**
 * This JMeter sampler will run a search for a random FHIR {@link Patient} and
 * then retrieve that {@link Patient} and all of their
 * {@link ExplanationOfBenefit}s.
 */
public final class RetrieveEobs extends AbstractJavaSamplerClient {
	private static final String PARAM_SERVER = "fhir_server";
	private static final String RIFFILE = "riffile";
	private static final String DELIMITER = "delimiter";
	private static final String LOOPS = "thread_loops";
	private static final String HOSTNAME_UNKNOWN = "unknown-host";

	private Random rng = new Random();
	private String hostName;
	private IGenericClient client;
	private List<String> patientIds;
  private RifParser parser = null;
  private int bene_id = 1;

	/**
	 * @see org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient#getDefaultParameters()
	 */
	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultParameters = new Arguments();
		defaultParameters.addArgument(PARAM_SERVER, "http://localhost:8080/hapi-fhir/baseDstu2");
		defaultParameters.addArgument(RIFFILE, "beneficiary_test.rif");
		defaultParameters.addArgument(DELIMITER, "|");
		defaultParameters.addArgument(LOOPS, "300");
		return defaultParameters;
	}

	/**
	 * @see org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient#setupTest(org.apache.jmeter.protocol.java.sampler.JavaSamplerContext)
	 */
	@Override
	public void setupTest(JavaSamplerContext context) {
		super.setupTest(context);
		this.hostName = getHostname();
		this.client = FhirClient.create(context.getParameter(PARAM_SERVER));
    this.parser = new RifParser(context.getParameter(RIFFILE), context.getParameter(DELIMITER));
    //System.out.println("Thread loops = " + context.getParameter(LOOPS));

		// Find the IDs that can be queried in each sample run.
		//this.patientIds = findPatientIds();
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
		} catch (IllegalArgumentException|InvalidRequestException e) {
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
    // Removed the Rif parser to speed things up
    //if (this.parser.hasNext()) 
    //{  
      //RifEntry entry = this.parser.next();

      // query all EOBs for a patient
		  Bundle searchResults = client.search()
        .forResource(ExplanationOfBenefit.class)
				.where(ExplanationOfBenefit.PATIENT.hasId(TransformerUtils.buildPatientId(String.valueOf(this.bene_id++))))
				.returnBundle(Bundle.class)
        .execute();

      // TODO - query EOBs for a patient filtering by date
    //}
	}
}
