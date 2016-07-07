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
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((code == null) ? 0 : code.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IcdCode other = (IcdCode) obj;
		if (code == null) {
			if (other.code != null)
				return false;
		} else if (!code.equals(other.code))
			return false;
		if (version != other.version)
			return false;
		return true;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("IcdCode [version=");
		builder.append(version);
		builder.append(", code=");
		builder.append(code);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Enumerates the ICD versions that are used by {@link IcdCode}.
	 */
	public static enum IcdVersion {
		ICD_9("9", "http://hl7.org/fhir/sid/icd-9-cm"),

		// TODO confirm that we're using intl ICD-10 codes
		ICD_10("0", "http://hl7.org/fhir/sid/icd-10");

		private final String ccwCoding;
		private final String fhirCodingSystem;

		/**
		 * Enum constant constructor.
		 * 
		 * @param ccwCoding
		 *            the serialization/coding used for this {@link IcdVersion}
		 *            constant in the CCW, per <a href=
		 *            "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prncpal_dgns_vrsn_cd.txt">
		 *            CCW Data Dictionary: PRNCPAL_DGNS_VRSN_CD</a> and other
		 *            similar fields
		 * @param fhirCodingSystem the value to use for {@link #getFhirSystem()}
		 */
		private IcdVersion(String ccwCoding, String fhirCodingSystem) {
			this.ccwCoding = ccwCoding;this.fhirCodingSystem = fhirCodingSystem;
		}

		/**
		 * @return the <a href=
		 *         "https://www.hl7.org/fhir/terminologies-systems.html">FHIR
		 *         Coding system</a> value for this ICD version
		 */
		public String getFhirSystem() {
			return fhirCodingSystem;
		}

		/**
		 * @param ccwCoding
		 *            the CCW ICD version coding value to be parsed, which must
		 *            conform to the coding used in <a href=
		 *            "https://www.ccwdata.org/cs/groups/public/documents/datadictionary/prncpal_dgns_vrsn_cd.txt">
		 *            CCW Data Dictionary: PRNCPAL_DGNS_VRSN_CD</a> and other
		 *            similar fields
		 * @return the parsed {@link IcdVersion} represented by the specified
		 *         value
		 */
		public static IcdVersion parse(String ccwCoding) {
			for (IcdVersion icdVersion : IcdVersion.values())
				if (icdVersion.ccwCoding.equals(ccwCoding))
					return icdVersion;

			throw new IllegalArgumentException("Unknown value: " + ccwCoding);
		}
	}
}
