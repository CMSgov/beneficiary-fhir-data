package gov.hhs.cms.bluebutton.datapipeline.rif.extract.s3;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.migesok.jaxb.adapter.javatime.InstantXmlAdapter;

import gov.hhs.cms.bluebutton.data.model.rif.RifFileType;

/**
 * Represents the <code>manifest.xml</code> files that detail which specific
 * files are included in a transfer from the CMS Chronic Conditions Warehouse to
 * the Blue Button API backend.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class DataSetManifest implements Comparable<DataSetManifest> {
	@XmlAttribute
	@XmlSchemaType(name = "dateTime")
	@XmlJavaTypeAdapter(TweakedInstantXmlAdapter.class)
	private final Instant timestamp;

	@XmlAttribute
	private int sequenceId;

	@XmlElement(name = "entry")
	private final List<DataSetManifestEntry> entries;

	/**
	 * Constructs a new {@link DataSetManifest} instance.
	 * 
	 * @param timestamp
	 *            the value to use for {@link #getTimestamp()}
	 * @param sequenceId
	 *            the value to use for {@link #getSequenceId()}
	 * @param entries
	 *            the value to use for {@link #getEntries()}
	 */
	public DataSetManifest(Instant timestamp, int sequenceId, List<DataSetManifestEntry> entries) {
		this.timestamp = timestamp;
		this.sequenceId = sequenceId;
		this.entries = entries;
	}

	/**
	 * Constructs a new {@link DataSetManifest} instance.
	 * 
	 * @param timestamp
	 *            the value to use for {@link #getTimestamp()}
	 * @param sequenceId
	 *            the value to use for {@link #getSequenceId()}
	 * @param entries
	 *            the value to use for {@link #getEntries()}
	 */
	public DataSetManifest(Instant timestamp, int sequenceId, DataSetManifestEntry... entries) {
		this.timestamp = timestamp;
		this.sequenceId = sequenceId;
		this.entries = Arrays.asList(entries);
		this.entries.forEach(entry -> entry.parentManifest = this);
	}

	/**
	 * This default constructor is required by JAX-B, and should not otherwise
	 * be used.
	 */
	@SuppressWarnings("unused")
	private DataSetManifest() {
		this.timestamp = null;
		this.entries = null;
		this.sequenceId = 0;
	}

	/**
	 * @return the date and time that the represented data set was
	 *         created/prepared at
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the {@link int} sequence number of the file represented by this
	 *         {@link DataSetManifest}
	 */
	public int getSequenceId() {
		return sequenceId;
	}

	/**
	 * @return a {@link DataSetManifestId} that models this
	 *         {@link DataSetManifest}'s identity and ordering
	 */
	public DataSetManifestId getId() {
		return new DataSetManifestId(this);
	}

	/**
	 * @return the list of {@link DataSetManifestEntry} included in this
	 *         {@link DataSetManifest}
	 */
	public List<DataSetManifestEntry> getEntries() {
		return entries;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(DataSetManifest o) {
		if (o == null)
			return 1;
		return getId().compareTo(o.getId());
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DataSetManifest [timestamp=");
		builder.append(timestamp);
		builder.append(", sequenceId=");
		builder.append(sequenceId);
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
		@XmlTransient
		private DataSetManifest parentManifest;

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
			this.parentManifest = null;
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
		 * @return the {@link DataSetManifest} that this
		 *         {@link DataSetManifestEntry} is a part of
		 */
		public DataSetManifest getParentManifest() {
			return parentManifest;
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
		 * Per the {@link Unmarshaller} JavaDocs, when unmarshalling
		 * {@link DataSetManifestEntry} instances from XML via JAX-B, this
		 * method is called after all the properties (except IDREF) are
		 * unmarshalled for this object, but before this object is set to the
		 * parent object.
		 * 
		 * @param unmarshaller
		 *            the {@link Unmarshaller} that created this instance
		 * @param parent
		 *            the value to use for {@link #getParentManifest()}
		 */
		void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
			this.parentManifest = (DataSetManifest) parent;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("DataSetManifestEntry [parentManifest.getTimestamp()=");
			builder.append(parentManifest.getTimestamp());
			builder.append(", parentManifest.getSequenceId()=");
			builder.append(parentManifest.getSequenceId());
			builder.append(", name=");
			builder.append(name);
			builder.append(", type=");
			builder.append(type);
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * Represents the unique identity of a {@link DataSetManifest}, allowing for
	 * equality comparisons. Also implements {@link Comparable} such that the
	 * processing order of {@link DataSetManifest}s can be determined.
	 */
	public static final class DataSetManifestId implements Comparable<DataSetManifestId> {
		private final Instant timestamp;
		private final int sequenceId;

		/**
		 * Constructs a new {@link DataSetManifestId}.
		 * 
		 * @param timestamp
		 *            the {@link DataSetManifest#getTimestamp()} value
		 * @param sequenceId
		 *            the {@link DataSetManifest#getSequenceId()} value
		 */
		private DataSetManifestId(Instant timestamp, int sequenceId) {
			this.timestamp = timestamp;
			this.sequenceId = sequenceId;
		}

		/**
		 * Constructs a new {@link DataSetManifestId}.
		 * 
		 * @param manifest
		 *            the {@link DataSetManifest} to extract the
		 *            {@link DataSetManifestId} from
		 */
		public DataSetManifestId(DataSetManifest manifest) {
			this(manifest.getTimestamp(), manifest.getSequenceId());
		}

		/**
		 * Parses a {@link DataSetManifestId} from the S3 key of a
		 * {@link DataSetManifest}.
		 * 
		 * @param s3ManifestKey
		 *            the S3 key of the {@link DataSetManifest} to extract a
		 *            {@link DataSetManifestId} from
		 * @return the {@link DataSetManifestId} represented by the specified S3
		 *         key, or <code>null</code> if the key doesn't seem to point to
		 *         a {@link DataSetManifest} ready for processing
		 */
		public static DataSetManifestId parseManifestIdFromS3Key(String s3ManifestKey) {
			Matcher manifestKeyMatcher = DataSetMonitorWorker.REGEX_PENDING_MANIFEST.matcher(s3ManifestKey);
			boolean keyMatchesRegex = manifestKeyMatcher.matches();

			if (!keyMatchesRegex)
				return null;

			String dataSetId = manifestKeyMatcher.group(1);
			Instant dataSetTimestamp;
			try {
				dataSetTimestamp = Instant.parse(dataSetId);
			} catch (DateTimeParseException e) {
				return null;
			}

			int dataSetSequenceId = Integer.parseInt(manifestKeyMatcher.group(2));

			return new DataSetManifestId(dataSetTimestamp, dataSetSequenceId);
		}

		/**
		 * @param s3Prefix
		 *            the S3 key prefix that should be prepended to the
		 *            calculated S3 key, e.g. "<code>Incoming</code>"
		 * @return the S3 key for this {@link DataSetManifestId}, under the
		 *         specified prefix
		 */
		public String computeS3Key(String s3Prefix) {
			return String.format("%s/%s/%d_manifest.xml", s3Prefix, timestamp.toString(), sequenceId);
		}

		/**
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(DataSetManifestId o) {
			if (o == null)
				throw new IllegalArgumentException();

			/*
			 * This is a two-level sort: always sort first by timestamp. Within
			 * equal timestamps, sort by sequenceId. This ensures that data sets
			 * are processed in the correct order.
			 */

			int timestampComparison = timestamp.compareTo(o.timestamp);
			if (timestampComparison != 0)
				return timestampComparison;
			return Integer.compare(sequenceId, o.sequenceId);
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + sequenceId;
			result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
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
			DataSetManifestId other = (DataSetManifestId) obj;
			if (sequenceId != other.sequenceId)
				return false;
			if (timestamp == null) {
				if (other.timestamp != null)
					return false;
			} else if (!timestamp.equals(other.timestamp))
				return false;
			return true;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("DataSetManifestId [timestamp=");
			builder.append(timestamp);
			builder.append(", sequenceId=");
			builder.append(sequenceId);
			builder.append("]");
			return builder.toString();
		}
	}

	/**
	 * This is a slightly tweaked version of {@link InstantXmlAdapter}. The
	 * specific tweak here allows leading whitespace in unmarshalled values, to
	 * workaround the issue encountered in
	 * <a href="http://issues.hhsdevcloud.us/browse/CBBD-207">CBBD-207: Invalid
	 * manifest timestamp causes ETL service to fail</a>.
	 */
	private static final class TweakedInstantXmlAdapter extends XmlAdapter<String, Instant> {
		private static final XmlAdapter<String, Instant> WRAPPED_ADAPTER = new InstantXmlAdapter();

		/**
		 * @see javax.xml.bind.annotation.adapters.XmlAdapter#unmarshal(java.lang.Object)
		 */
		@Override
		public Instant unmarshal(String v) throws Exception {
			return WRAPPED_ADAPTER.unmarshal(v != null ? v.trim() : null);
		}

		/**
		 * @see javax.xml.bind.annotation.adapters.XmlAdapter#marshal(java.lang.Object)
		 */
		@Override
		public String marshal(Instant v) throws Exception {
			return WRAPPED_ADAPTER.marshal(v);
		}
	}
}
