package gov.hhs.cms.bluebutton.datapipeline.app;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirLoader;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetMonitor;

/**
 * <p>
 * Models the configuration options for the application.
 * </p>
 * <p>
 * Note that, in addition to the configuration specified here, the application
 * must also be provided with credentials that can be used to access the
 * specified S3 bucket. For that, the application supports all of the mechanisms
 * that are supported by {@link DefaultAWSCredentialsProviderChain}, which
 * include environment variables, EC2 instance profiles, etc.
 * </p>
 */
public final class AppConfiguration implements Serializable {
	private static final long serialVersionUID = -6845504165285244536L;

	/**
	 * The name of the environment variable that should be used to provide the
	 * {@link #getS3BucketName()} value.
	 */
	public static final String ENV_VAR_KEY_BUCKET = "S3_BUCKET_NAME";

	/**
	 * The name of the environment variable that should be used to provide the
	 * {@link #getFhirServer()} value.
	 */
	public static final String ENV_VAR_KEY_FHIR = "FHIR_SERVER_URL";

	private final String s3BucketName;
	private final URL fhirServer;

	/**
	 * Constructs a new {@link AppConfiguration} instance.
	 * 
	 * @param s3BucketName
	 *            the value to use for {@link #getS3BucketName()}
	 * @param fhirServer
	 *            the value to use for {@link #getFhirServer()}
	 */
	public AppConfiguration(String s3BucketName, URL fhirServer) {
		this.s3BucketName = s3BucketName;
		this.fhirServer = fhirServer;
	}

	/**
	 * @return the name of the S3 bucket that the application's
	 *         {@link DataSetMonitor} will be configured to pull CCW RIF data
	 *         from
	 */
	public String getS3BucketName() {
		return s3BucketName;
	}

	/**
	 * @return the {@link URL} of the FHIR server that the application's
	 *         {@link FhirLoader} will be configured to push to
	 */
	public URL getFhirServer() {
		return fhirServer;
	}

	/**
	 * <p>
	 * Per <code>/dev/design-decisions-readme.md</code>, this application
	 * accepts its configuration via environment variables. Read those in, and
	 * build an {@link AppConfiguration} instance from them.
	 * </p>
	 * <p>
	 * As a convenience, this method will also verify that AWS credentials were
	 * provided, such that {@link DefaultAWSCredentialsProviderChain} can load
	 * them. If not, an {@link AppConfigurationException} will be thrown.
	 * </p>
	 * 
	 * @return the {@link AppConfiguration} instance represented by the
	 *         configuration provided to this application via the environment
	 *         variables
	 * @throws AppConfigurationException
	 *             An {@link AppConfigurationException} will be thrown if the
	 *             configuration passed to the application are incomplete or
	 *             incorrect.
	 */
	static AppConfiguration readConfigFromEnvironmentVariables() {
		String s3BucketName = System.getenv(ENV_VAR_KEY_BUCKET);
		if (s3BucketName == null || s3BucketName.isEmpty())
			throw new AppConfigurationException(
					String.format("Missing value for configuration environment variable '%s'.", ENV_VAR_KEY_BUCKET));

		String fhirServerUrlText = System.getenv(ENV_VAR_KEY_FHIR);
		if (fhirServerUrlText == null || fhirServerUrlText.isEmpty())
			throw new AppConfigurationException(
					String.format("Missing value for configuration environment variable '%s'.", ENV_VAR_KEY_FHIR));
		URL fhirServerUrl;
		try {
			fhirServerUrl = new URL(fhirServerUrlText);
		} catch (MalformedURLException e) {
			throw new AppConfigurationException(
					String.format("Invalid value for configuration environment variable '%s': '%s'", fhirServerUrlText),
					e);
		}

		/*
		 * Just for convenience: make sure DefaultAWSCredentialsProviderChain
		 * has whatever it needs.
		 */
		try {
			DefaultAWSCredentialsProviderChain awsCredentialsProvider = new DefaultAWSCredentialsProviderChain();
			awsCredentialsProvider.getCredentials();
		} catch (AmazonClientException e) {
			/*
			 * The credentials provider should throw this if it can't find what
			 * it needs.
			 */
			throw new AppConfigurationException(String.format("Missing configuration for AWS credentials (for %s).",
					DefaultAWSCredentialsProviderChain.class.getName()), e);
		}

		return new AppConfiguration(s3BucketName, fhirServerUrl);
	}
}
