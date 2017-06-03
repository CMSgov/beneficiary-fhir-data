package gov.hhs.cms.bluebutton.datapipeline.app;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;

import gov.hhs.cms.bluebutton.datapipeline.fhir.LoadAppOptions;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.ExtractionOptions;
import gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3.DataSetManifest;
import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;

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
	 * {@link ExtractionOptions#getS3BucketName()} value.
	 */
	public static final String ENV_VAR_KEY_BUCKET = "S3_BUCKET_NAME";

	/**
	 * <p>
	 * The name of the environment variable that should be used to provide the
	 * {@link ExtractionOptions#getDataSetFilter()} value: This environment
	 * variable specifies the {@link RifFileType} that will be processed. Any
	 * {@link DataSetManifest}s that contain other {@link RifFileType}s will be
	 * skipped entirely (even if they <em>also</em> contain the allowed
	 * {@link RifFileType}. For example, specifying "BENEFICIARY" will configure
	 * the application to only process data sets that <strong>only</strong>
	 * contain {@link RifFileType#BENEFICIARY}s.
	 * </p>
	 */
	public static final String ENV_VAR_KEY_ALLOWED_RIF_TYPE = "DATA_SET_TYPE_ALLOWED";

	/**
	 * The name of the environment variable that should be used to provide the
	 * {@link #getLoadOptions()} {@link LoadAppOptions#getHicnHashIterations()}
	 * value.
	 */
	public static final String ENV_VAR_KEY_HICN_HASH_ITERATIONS = "HICN_HASH_ITERATIONS";

	/**
	 * The name of the environment variable that should be used to provide a hex
	 * encoded representation of the {@link #getLoadOptions()}
	 * {@link LoadAppOptions#getHicnHashPepper()} value.
	 */
	public static final String ENV_VAR_KEY_HICN_HASH_PEPPER = "HICN_HASH_PEPPER";

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

	/**
	 * The name of the environment variable that should be used to provide the
	 * {@link #getLoadOptions()} {@link LoadAppOptions#getLoaderThreads()}
	 * value.
	 */
	public static final String ENV_VAR_KEY_LOADER_THREADS = "LOADER_THREADS";

	private final ExtractionOptions extractionOptions;
	private final LoadAppOptions loadOptions;

	/**
	 * Constructs a new {@link AppConfiguration} instance.
	 * 
	 * @param extractionOptions
	 *            the value to use for {@link #getExtractionOptions()}
	 * @param loadOptions
	 *            the value to use for {@link #getLoadOptions()}
	 */
	public AppConfiguration(ExtractionOptions extractionOptions, LoadAppOptions loadOptions) {
		this.extractionOptions = extractionOptions;
		this.loadOptions = loadOptions;
	}

	/**
	 * @return the {@link ExtractionOptions} that the application will use
	 */
	public ExtractionOptions getExtractionOptions() {
		return extractionOptions;
	}

	/**
	 * @return the {@link LoadAppOptions} that the application will use
	 */
	public LoadAppOptions getLoadOptions() {
		return loadOptions;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("AppConfiguration [extractionOptions=");
		builder.append(extractionOptions);
		builder.append(", loadOptions=");
		builder.append(loadOptions);
		builder.append("]");
		return builder.toString();
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

		String rifFilterText = System.getenv(ENV_VAR_KEY_ALLOWED_RIF_TYPE);
		RifFileType allowedRifFileType;
		if (rifFilterText != null && !rifFilterText.isEmpty()) {
			try {
				allowedRifFileType = RifFileType.valueOf(rifFilterText);
			} catch (IllegalArgumentException e) {
				throw new AppConfigurationException(
						String.format("Invalid value for configuration environment variable '%s': '%s'",
								ENV_VAR_KEY_ALLOWED_RIF_TYPE, rifFilterText),
						e);
			}
		} else {
			allowedRifFileType = null;
		}

		String hicnHashIterationsText = System.getenv(ENV_VAR_KEY_HICN_HASH_ITERATIONS);
		if (hicnHashIterationsText == null || hicnHashIterationsText.isEmpty())
			throw new AppConfigurationException(String.format(
					"Missing value for configuration environment variable '%s'.", ENV_VAR_KEY_HICN_HASH_ITERATIONS));
		int hicnHashIterations;
		try {
			hicnHashIterations = Integer.parseInt(hicnHashIterationsText);
		} catch (NumberFormatException e) {
			hicnHashIterations = -1;
		}
		if (hicnHashIterations < 1)
			throw new AppConfigurationException(
					String.format("Invalid value for configuration environment variable '%s': '%s'",
							ENV_VAR_KEY_HICN_HASH_ITERATIONS, hicnHashIterationsText));

		String hicnHashPepperText = System.getenv(ENV_VAR_KEY_HICN_HASH_PEPPER);
		if (hicnHashPepperText == null || hicnHashPepperText.isEmpty())
			throw new AppConfigurationException(String.format(
					"Missing value for configuration environment variable '%s'.", ENV_VAR_KEY_HICN_HASH_PEPPER));
		byte[] hicnHashPepper;
		try {
			hicnHashPepper = Hex.decodeHex(hicnHashPepperText.toCharArray());
		} catch (DecoderException e) {
			hicnHashPepper = new byte[] {};
		}
		if (hicnHashPepperText.length() < 1)
			throw new AppConfigurationException(
					String.format("Invalid value for configuration environment variable '%s': '%s'",
							ENV_VAR_KEY_HICN_HASH_PEPPER, hicnHashPepperText));

		String fhirServerUrlText = System.getenv(ENV_VAR_KEY_FHIR);
		if (fhirServerUrlText == null || fhirServerUrlText.isEmpty())
			throw new AppConfigurationException(
					String.format("Missing value for configuration environment variable '%s'.", ENV_VAR_KEY_FHIR));
		URI fhirServerUri;
		try {
			fhirServerUri = new URI(fhirServerUrlText);
		} catch (URISyntaxException e) {
			throw new AppConfigurationException(
					String.format("Invalid value for configuration environment variable '%s': '%s'", ENV_VAR_KEY_FHIR,
							fhirServerUrlText),
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

		String loaderThreadsText = System.getenv(ENV_VAR_KEY_LOADER_THREADS);
		if (loaderThreadsText == null || loaderThreadsText.isEmpty())
			throw new AppConfigurationException(String
					.format("Missing value for configuration environment variable '%s'.", ENV_VAR_KEY_LOADER_THREADS));
		int loaderThreads;
		try {
			loaderThreads = Integer.parseInt(loaderThreadsText);
		} catch (NumberFormatException e) {
			loaderThreads = -1;
		}
		if (loaderThreads < 1)
			throw new AppConfigurationException(
					String.format("Invalid value for configuration environment variable '%s': '%s'",
							ENV_VAR_KEY_LOADER_THREADS, loaderThreadsText));

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

		return new AppConfiguration(new ExtractionOptions(s3BucketName, allowedRifFileType),
				new LoadAppOptions(hicnHashIterations, hicnHashPepper, fhirServerUri, Paths.get(keyStorePath),
						keyStorePassword.toCharArray(), Paths.get(trustStorePath), trustStorePassword.toCharArray(),
						loaderThreads));
	}
}
