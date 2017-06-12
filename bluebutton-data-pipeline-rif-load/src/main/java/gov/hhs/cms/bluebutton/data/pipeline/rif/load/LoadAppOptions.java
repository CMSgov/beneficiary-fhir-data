package gov.hhs.cms.bluebutton.data.pipeline.rif.load;

import java.io.Serializable;

/**
 * Models the user-configurable application options.
 */
public final class LoadAppOptions implements Serializable {
	/*
	 * This class is marked Serializable purely to help keep
	 * AppConfigurationTest simple. Unfortunately, Path implementations aren't
	 * also Serializable, so we have to store Strings here, instead.
	 */

	private static final long serialVersionUID = 2884121140016566847L;

	/**
	 * A reasonable (though not terribly performant) suggested default value for
	 * {@link #getLoaderThreads()}.
	 */
	public static final int DEFAULT_LOADER_THREADS = Math.max(1, (Runtime.getRuntime().availableProcessors() - 1)) * 2;

	private final int hicnHashIterations;
	private final byte[] hicnHashPepper;
	private final String databaseUrl;
	private final String databaseUsername;
	private final char[] databasePassword;
	private final int loaderThreads;

	/**
	 * Constructs a new {@link LoadAppOptions} instance.
	 * 
	 * @param hicnHashIterations
	 *            the value to use for {@link #getHicnHashIterations()}
	 * @param hicnHashPepper
	 *            the value to use for {@link #getHicnHashPepper()}
	 * @param databaseUrl
	 *            the value to use for {@link #getDatabaseUrl()}
	 * @param databaseUsername
	 *            the value to use for {@link #getDatabaseUsername()}
	 * @param databasePassword
	 *            the value to use for {@link #getDatabasePassword()}
	 * @param loaderThreads
	 *            the value to use for {@link #getLoaderThreads()}
	 */
	public LoadAppOptions(int hicnHashIterations, byte[] hicnHashPepper, String databaseUrl, String databaseUsername,
			char[] databasePassword, int loaderThreads) {
		if (loaderThreads < 1)
			throw new IllegalArgumentException();

		this.hicnHashIterations = hicnHashIterations;
		this.hicnHashPepper = hicnHashPepper;
		this.databaseUrl = databaseUrl;
		this.databaseUsername = databaseUsername;
		this.databasePassword = databasePassword;
		this.loaderThreads = loaderThreads;
	}

	/**
	 * @return the number of <code>PBKDF2WithHmacSHA256</code> iterations to use
	 *         when hashing beneficiary HICNs
	 */
	public int getHicnHashIterations() {
		return hicnHashIterations;
	}

	/**
	 * @return the shared secret pepper to use (in lieu of a salt) with
	 *         <code>PBKDF2WithHmacSHA256</code> when hashing beneficiary HICNs
	 */
	public byte[] getHicnHashPepper() {
		return hicnHashPepper;
	}

	/**
	 * @return the JDBC URL of the database to load into
	 */
	public String getDatabaseUrl() {
		return databaseUrl;
	}

	/**
	 * @return the database username to connect as when loading data
	 */
	public String getDatabaseUsername() {
		return databaseUsername;
	}

	/**
	 * @return the database password to connect with when loading data
	 */
	public char[] getDatabasePassword() {
		return databasePassword;
	}

	/**
	 * @return the number of threads that will be used to simultaneously process
	 *         {@link RifLoader} operations
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
		builder.append("LoadAppOptions [hicnHashIterations=");
		builder.append(hicnHashIterations);
		builder.append(", hicnHashPepper=");
		builder.append("***");
		builder.append(", databaseUrl=");
		builder.append(databaseUrl);
		builder.append(", databaseUsername=");
		builder.append("***");
		builder.append(", databasePassword=");
		builder.append("***");
		builder.append(", loaderThreads=");
		builder.append(loaderThreads);
		builder.append("]");
		return builder.toString();
	}
}
