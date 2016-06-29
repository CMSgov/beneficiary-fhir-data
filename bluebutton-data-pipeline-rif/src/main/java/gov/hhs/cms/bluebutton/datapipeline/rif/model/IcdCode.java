package gov.hhs.cms.bluebutton.datapipeline.rif.model;

/**
 * A simple struct for modeling ICD codes, as stored in the CCW.
 */
public final class IcdCode {
	private final IcdVersion version;
	private final String code;

	/**
	 * Constructs a new {@link IcdCode} instance.
	 * 
	 * @param version
	 *            the value to use for {@link #getVersion()}
	 * @param code
	 *            the value to use for {@link #getCode()}
	 */
	public IcdCode(IcdVersion version, String code) {
		this.version = version;
		this.code = code;
	}

	/**
	 * @return the {@link IcdVersion} of this {@link IcdCode}
	 */
	public IcdVersion getVersion() {
		return version;
	}

	/**
	 * @return the ICD code textual value
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Enumerates the ICD versions that are used by {@link IcdCode}.
	 */
	public static enum IcdVersion {
		ICD_9,

		ICD_10;
	}
}
