package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.migesok.jaxb.adapter.javatime.InstantXmlAdapter;

import gov.hhs.cms.bluebutton.datapipeline.rif.model.RifFileType;

/**
 * Represents the <code>manifest.xml</code> files that detail which specific
 * files are included in a transfer from the CMS Chronic Conditions Warehouse to
 * the Blue Button API backend.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class DataSetManifest {
	@XmlAttribute
	@XmlSchemaType(name = "dateTime")
	@XmlJavaTypeAdapter(InstantXmlAdapter.class)
	private final Instant timestamp;

	@XmlElement(name = "entry")
	private final List<DataSetManifestEntry> entries;

	/**
	 * Constructs a new {@link DataSetManifest} instance.
	 * 
	 * @param timestamp
	 *            the value to use for {@link #getTimestamp()}
	 * @param entries
	 *            the value to use for {@link #getEntries()}
	 */
	public DataSetManifest(Instant timestamp, List<DataSetManifestEntry> entries) {
		this.timestamp = timestamp;
		this.entries = entries;
	}

	/**
	 * Constructs a new {@link DataSetManifest} instance.
	 * 
	 * @param timestamp
	 *            the value to use for {@link #getTimestamp()}
	 * @param entries
	 *            the value to use for {@link #getEntries()}
	 */
	public DataSetManifest(Instant timestamp, DataSetManifestEntry... entries) {
		this.timestamp = timestamp;
		this.entries = Arrays.asList(entries);
	}

	/**
	 * This default constructor is required by JAX-B, and should not otherwise
	 * be used.
	 */
	@SuppressWarnings("unused")
	private DataSetManifest() {
		this.timestamp = null;
		this.entries = null;
	}

	/**
	 * @return the date and time that the represented data set was
	 *         created/prepared at
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the list of {@link DataSetManifestEntry} included in this
	 *         {@link DataSetManifest}
	 */
	public List<DataSetManifestEntry> getEntries() {
		return entries;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DataSetManifest [timestamp=");
		builder.append(timestamp);
		builder.append(", entries=");
		builder.append(entries);
		builder.append("]");
		return builder.toString();
	}

	/**
	 * Each {@link DataSetManifestEntry} instance represents a single file
	 * included in a {@link DataSetManifest}.
	 */
	@XmlAccessorType(XmlAccessType.FIELD)
	public static final class DataSetManifestEntry {
		@XmlAttribute
		private final String name;

		@XmlAttribute
		private final RifFileType type;

		/**
		 * Constructs a new {@link DataSetManifestEntry} instance.
		 * 
		 * @param name
		 *            the value to use for {@link #getName()}
		 * @param type
		 *            the value to use for {@link #getType()}
		 */
		public DataSetManifestEntry(String name, RifFileType type) {
			this.name = name;
			this.type = type;
		}

		/**
		 * This default constructor is required by JAX-B, and should not
		 * otherwise be used.
		 */
		@SuppressWarnings("unused")
		private DataSetManifestEntry() {
			this.name = null;
			this.type = null;
		}

		/**
		 * @return the name of the S3 object/file that is represented by this
		 *         {@link DataSetManifestEntry}, which is effectively a relative
		 *         S3 key (relative to this <code>manifest.xml</code> object's
		 *         key, that is)
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the {@link RifFileType} of the file represented by this
		 *         {@link DataSetManifestEntry}
		 */
		public RifFileType getType() {
			return type;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("DataSetManifestEntry [name=");
			builder.append(name);
			builder.append(", type=");
			builder.append(type);
			builder.append("]");
			return builder.toString();
		}
	}
}
