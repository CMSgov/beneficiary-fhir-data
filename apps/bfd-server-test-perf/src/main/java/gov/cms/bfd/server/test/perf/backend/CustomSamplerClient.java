package gov.cms.bfd.server.test.perf.backend;

import java.io.StringWriter;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import gov.cms.bfd.server.test.perf.utils.FhirClient;
import gov.cms.bfd.server.test.perf.utils.RifParser;

/**
 * {@link CustomSamplerClient} encapsulates the common variables and methods
 * that are shared among all tests in this package. Simply derive from this
 * class and implement {@link CustomSamplerClient#executeTest()} at a minimum to
 * create new test functionality.
 */
public abstract class CustomSamplerClient extends AbstractJavaSamplerClient {
	protected static final String PARAM_SERVER = "fhir_server";
	protected static final String KEYSTORE_DIR = "keystore_dir";
	protected static final String PROXY_HOST = "proxy_host";
	protected static final String PROXY_PORT = "proxy_port";
	protected static final String RIFFILE = "riffile";
	protected static final String RIFTYPE = "riftype";
	protected static final String HOSTNAME_UNKNOWN = "unknown-host";
	private static final Logger LOGGER = LoggerFactory.getLogger(FhirClient.class);

	/**
	 * Abstract method that should be implemented by derived classes to implement
	 * test functionality specific to the desired test case.
	 */
	protected abstract void executeTest();

	protected String hostName;
	protected IGenericClient client;
	protected RifParser parser = null;

	/**
	 * @see org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient#getDefaultParameters()
	 */
	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultParameters = new Arguments();
		defaultParameters.addArgument(PARAM_SERVER, "https://fhir.backend.bluebutton.hhsdevcloud.us/v1/fhir");
		defaultParameters.addArgument(KEYSTORE_DIR, "/usr/local/bluebutton-jmeter-service/dev/ssl-stores");
		defaultParameters.addArgument(PROXY_HOST, "null");
		defaultParameters.addArgument(PROXY_PORT, "0");
		defaultParameters.addArgument(RIFFILE, "beneficiary_test.rif");
		defaultParameters.addArgument(RIFTYPE, "BENEFICIARY");
		return defaultParameters;
	}

	/**
	 * @see org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient#setupTest(org.apache.jmeter.protocol.java.sampler.JavaSamplerContext)
	 */
	@Override
	public void setupTest(JavaSamplerContext context) {
		super.setupTest(context);
		this.hostName = getHostname();

		if (context.getParameter(PROXY_HOST).equals("null")) { // no proxy
			this.client = FhirClient.create(context.getParameter(PARAM_SERVER), context.getParameter(KEYSTORE_DIR));
		} else { // use proxy
			this.client = FhirClient.create(context.getParameter(PARAM_SERVER), context.getParameter(KEYSTORE_DIR),
					context.getParameter(PROXY_HOST), Integer.parseInt(context.getParameter(PROXY_PORT)));
		}

//		try {
//			this.parser = new RifParser(context.getParameter(RIFFILE), RifFileType.valueOf(context.getParameter(RIFTYPE).toUpperCase()));
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
//		}
	}

	/**
	 * @see org.apache.jmeter.protocol.java.sampler.JavaSamplerClient#runTest(org.apache.jmeter.protocol.java.sampler.JavaSamplerContext)
	 */
	@Override
	public SampleResult runTest(JavaSamplerContext context) {
		SampleResult sample = new SampleResult();
		sample.sampleStart();
		try {
			executeTest();

			sample.sampleEnd();
			sample.setSuccessful(true);
			sample.setResponseMessage("Sample succeeded on host: " + hostName);
			sample.setResponseCodeOK();
		} catch (Throwable e) {
			// Mark this sample iteration as failed.
			sample.sampleEnd();
			sample.setSuccessful(false);
			sample.setResponseMessage(ExceptionUtils.getStackTrace(e));

			/*
			 * Serialize the stack trace to a String and attach it to the sample results.
			 */
			StringWriter stringWriter = new StringWriter();
			e.printStackTrace(new java.io.PrintWriter(stringWriter));
			sample.setResponseData(stringWriter.toString(), StandardCharsets.UTF_8.name());
			sample.setDataType(org.apache.jmeter.samplers.SampleResult.TEXT);
			if (e instanceof FhirClientConnectionException) {
				sample.setResponseCode("504");
			} else {
				sample.setResponseCode("500");
			}
		}

		return sample;
	}

	/**
	 * @return the hostname of the system this code is running on, or
	 *         {@link #HOSTNAME_UNKNOWN} is it can't be determined
	 */
	protected static String getHostname() {
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

}
