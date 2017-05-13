package gov.hhs.cms.bluebutton.datapipeline.fhir;

import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

import gov.hhs.cms.bluebutton.datapipeline.fhir.load.FhirLoader;

/**
 * Models the user-configurable application options.
 */
public final class LoadAppOptions implements Serializable {
	/*
	 * This class is marked Serializable purely to help keep
	 * AppConfigurationTest simple. Unfortunately, Path implementations aren't
	 * also Serializable, so we have to store Strings here, instead.
	 */

	private static final long serialVersionUID = 8178859162754788432L;

	/**
	 * A reasonable (though not terribly performant) suggested default value for
	 * {@link #getLoaderThreads()}.
	 */
	public static final int DEFAULT_LOADER_THREADS = Math.max(1, (Runtime.getRuntime().availableProcessors() - 1)) * 2;

	private final URI fhirServer;
	private final String keyStorePath;
	private final char[] keyStorePassword;
	private final String trustStorePath;
	private final char[] trustStorePassword;
	private final int loaderThreads;

	/**
	 * Constructs a new {@link LoadAppOptions} instance.
	 * 
	 * @param fhirServer
	 *            the value to use for {@link #getFhirServer()}
	 * @param keyStorePath
	 *            the value to use for {@link #getKeyStorePath()}
	 * @param keyStorePassword
	 *            the value to use for {@link #getKeyStorePassword()}
	 * @param trustStorePath
	 *            the value to use for {@link #getTrustStorePath()}
	 * @param trustStorePassword
	 *            the value to use for {@link #getTrustStorePassword()}
	 * @param loaderThreads
	 *            the value to use for {@link #getLoaderThreads()}
	 */
	public LoadAppOptions(URI fhirServer, Path keyStorePath, char[] keyStorePassword, Path trustStorePath,
			char[] trustStorePassword, int loaderThreads) {
		if (loaderThreads < 1)
			throw new IllegalArgumentException();

		this.fhirServer = fhirServer;
		this.keyStorePath = keyStorePath.toString();
		this.keyStorePassword = keyStorePassword;
		this.trustStorePath = trustStorePath.toString();
		this.trustStorePassword = trustStorePassword;
		this.loaderThreads = loaderThreads;
	}

	/**
	 * @return the {@link URI} for the FHIR server that the application should
	 *         push data to
	 */
	public URI getFhirServer() {
		return fhirServer;
	}

	/**
	 * @return the local {@link Path} to the keystore that the application
	 *         should use for all its connections to the server
	 */
	public Path getKeyStorePath() {
		return Paths.get(keyStorePath);
	}

	/**
	 * @return the password for {@link #getKeyStorePath()} and the certificate
	 *         in it
	 */
	public char[] getKeyStorePassword() {
		return keyStorePassword;
	}

	/**
	 * @return the local {@link Path} to the truststore that the application
	 *         should use for all its connections to the server
	 */
	public Path getTrustStorePath() {
		return Paths.get(trustStorePath);
	}

	/**
	 * @return the password for {@link #getTrustStorePath()}
	 */
	public char[] getTrustStorePassword() {
		return trustStorePassword;
	}

	/**
	 * @return the number of threads that will be used to simultaneously process
	 *         {@link FhirLoader} operations
	 */
	public int getLoaderThreads() {
		return loaderThreads;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("LoadAppOptions [fhirServer=");
		builder.append(fhirServer);
		builder.append(", keyStorePath=");
		builder.append(keyStorePath);
		builder.append(", keyStorePassword=");
		builder.append("***");
		builder.append(", trustStorePath=");
		builder.append(trustStorePath);
		builder.append(", trustStorePassword=");
		builder.append("***");
		builder.append(", loaderThreads=");
		builder.append(loaderThreads);
		builder.append("]");
		return builder.toString();
	}
}
