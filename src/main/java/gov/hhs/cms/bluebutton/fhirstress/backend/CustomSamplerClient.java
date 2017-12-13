package gov.hhs.cms.bluebutton.fhirstress.backend;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import gov.hhs.cms.bluebutton.fhirclient.FhirClient;
//import gov.hhs.cms.bluebutton.rifparser.RifParser;
//import gov.hhs.cms.bluebutton.rifparser.RifEntry;

//import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.IGenericClient;

public abstract class CustomSamplerClient extends AbstractJavaSamplerClient {
	protected static final String PARAM_SERVER = "fhir_server";
	protected static final String KEYSTORE_DIR = "keystore_dir";
	protected static final String RIFFILE = "riffile";
	protected static final String DELIMITER = "delimiter";
	protected static final String LOOPS = "thread_loops";
	protected static final String HOSTNAME_UNKNOWN = "unknown-host";

	protected String hostName;
	protected IGenericClient client;
  //protected RifParser parser = null;

	/**
	 * @see org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient#getDefaultParameters()
	 */
	@Override
	public Arguments getDefaultParameters() {
		Arguments defaultParameters = new Arguments();
		defaultParameters.addArgument(PARAM_SERVER, "http://localhost:8080/hapi-fhir/baseDstu2");
		defaultParameters.addArgument(KEYSTORE_DIR, "/opt/fhir_stress/dev/ssl-stores");
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
		this.client = FhirClient.create(
      context.getParameter(PARAM_SERVER),
      context.getParameter(KEYSTORE_DIR)
    );
    //this.parser = new RifParser(context.getParameter(RIFFILE), context.getParameter(DELIMITER));
    //System.out.println("Thread loops = " + context.getParameter(LOOPS));

		// Find the IDs that can be queried in each sample run.
		//this.patientIds = findPatientIds();
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

