package gov.hhs.cms.bluebutton.datapipeline.app;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

import gov.hhs.cms.bluebutton.datapipeline.fhir.LoadAppOptions;
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
	 * {@link #getLoadOptions()} {@link LoadAppOptions#getFhirServer()} value.
	 */
	public static final String ENV_VAR_KEY_FHIR = "FHIR_SERVER_URL";

	/**
	 * The name of the environment variable that should be used to provide the
	 * {@link #getLoadOptions()} {@link LoadAppOptions#getKeyStorePath()} value.
	 */
	public static final String ENV_VAR_KEY_KEY_STORE_PATH = "KEY_STORE_PATH";

	/**
	 * The name of the environment variable that should be used to provide the
	 * {@link #getLoadOptions()} {@link LoadAppOptions#getKeyStorePassword()}
	 * value.
	 */
	public static final String ENV_VAR_KEY_KEY_STORE_PASSWORD = "KEY_STORE_PASSWORD";

	/**
	 * The name of the environment variable that should be used to provide the
	 * {@link #getLoadOptions()} {@link LoadAppOptions#getTrustStorePath()}
	 * value.
	 */
	public static final String ENV_VAR_KEY_TRUST_STORE_PATH = "TRUST_STORE_PATH";

	/**
	 * The name of the environment variable that should be used to provide the
	 * {@link #getLoadOptions()} {@link LoadAppOptions#getTrustStorePassword()}
	 * value.
	 */
	public static final String ENV_VAR_KEY_TRUST_STORE_PASSWORD = "TRUST_STORE_PASSWORD";

	private final String s3BucketName;
	private final LoadAppOptions loadOptions;

	/**
	 * Constructs a new {@link AppConfiguration} instance.
	 * 
	 * @param s3BucketName
	 *            the value to use for {@link #getS3BucketName()}
	 * @param loadOptions
	 *            the value to use for {@link #getLoadOptions()}
	 */
	public AppConfiguration(String s3BucketName, LoadAppOptions loadOptions) {
		this.s3BucketName = s3BucketName;
		this.loadOptions = loadOptions;
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
	 * @return the {@link LoadAppOptions} that the application will use
	 */
	public LoadAppOptions getLoadOptions() {
		return loadOptions;
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
		URI fhirServerUri;
		try {
			fhirServerUri = new URI(fhirServerUrlText);
		} catch (URISyntaxException e) {
			throw new AppConfigurationException(
					String.format("Invalid value for configuration environment variable '%s': '%s'", fhirServerUrlText),
					e);
		}

		String keyStorePath = System.getenv(ENV_VAR_KEY_KEY_STORE_PATH);
		if (keyStorePath == null || keyStorePath.isEmpty())
			throw new AppConfigurationException(String
					.format("Missing value for configuration environment variable '%s'.", ENV_VAR_KEY_KEY_STORE_PATH));

		String keyStorePassword = System.getenv(ENV_VAR_KEY_KEY_STORE_PASSWORD);
		if (keyStorePassword == null || keyStorePassword.isEmpty())
			throw new AppConfigurationException(String
					.format("Missing value for configuration environment variable '%s'.", ENV_VAR_KEY_KEY_STORE_PASSWORD));

		String trustStorePath = System.getenv(ENV_VAR_KEY_TRUST_STORE_PATH);
		if (trustStorePath == null || trustStorePath.isEmpty())
			throw new AppConfigurationException(String
					.format("Missing value for configuration environment variable '%s'.", ENV_VAR_KEY_TRUST_STORE_PATH));

		String trustStorePassword = System.getenv(ENV_VAR_KEY_TRUST_STORE_PASSWORD);
		if (trustStorePassword == null || trustStorePassword.isEmpty())
			throw new AppConfigurationException(String.format(
					"Missing value for configuration environment variable '%s'.", ENV_VAR_KEY_TRUST_STORE_PASSWORD));

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

		return new AppConfiguration(s3BucketName, new LoadAppOptions(fhirServerUri, Paths.get(keyStorePath),
				keyStorePassword.toCharArray(), Paths.get(trustStorePath), trustStorePassword.toCharArray()));
	}
}
