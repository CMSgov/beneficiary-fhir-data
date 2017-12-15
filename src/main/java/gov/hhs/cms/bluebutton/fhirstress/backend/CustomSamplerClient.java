package gov.hhs.cms.bluebutton.fhirstress.backend;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;

import gov.hhs.cms.bluebutton.fhirstress.utils.FhirClient;
//import gov.hhs.cms.bluebutton.fhirstress.utils.RifParser;
//import gov.hhs.cms.bluebutton.fhirstress.utils.RifEntry;

//import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.IGenericClient;

public abstract class CustomSamplerClient extends AbstractJavaSamplerClient {
	protected static final String PARAM_SERVER = "fhir_server";
	protected static final String KEYSTORE_DIR = "keystore_dir";
	protected static final String PROXY_HOST = "proxy_host";
	protected static final String PROXY_PORT = "proxy_port";
	protected static final String RIFFILE = "riffile";
	protected static final String DELIMITER = "delimiter";
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
		defaultParameters.addArgument(PARAM_SERVER, "https://fhir.backend.bluebutton.hhsdevcloud.us/baseDstu3");
		defaultParameters.addArgument(KEYSTORE_DIR, "/opt/fhir_stress/dev/ssl-stores");
		defaultParameters.addArgument(PROXY_HOST, "null");
		defaultParameters.addArgument(PROXY_PORT, "0");
		defaultParameters.addArgument(RIFFILE, "beneficiary_test.rif");
		defaultParameters.addArgument(DELIMITER, "|");
		return defaultParameters;
	}

	/**
	 * @see org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient#setupTest(org.apache.jmeter.protocol.java.sampler.JavaSamplerContext)
	 */
	@Override
	public void setupTest(JavaSamplerContext context) {
		super.setupTest(context);
		this.hostName = getHostname();
    int proxyPort = Integer.parseInt(context.getParameter(PROXY_PORT));

    if(context.getParameter(PROXY_HOST).equals("null")) { // no proxy
		  this.client = FhirClient.create(
        context.getParameter(PARAM_SERVER),
        context.getParameter(KEYSTORE_DIR)
      );
    }
    else { // use proxy
		  this.client = FhirClient.create(
        context.getParameter(PARAM_SERVER),
        context.getParameter(KEYSTORE_DIR),
        context.getParameter(PROXY_HOST),
        Integer.parseInt(context.getParameter(PROXY_PORT))
      );
    }
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

