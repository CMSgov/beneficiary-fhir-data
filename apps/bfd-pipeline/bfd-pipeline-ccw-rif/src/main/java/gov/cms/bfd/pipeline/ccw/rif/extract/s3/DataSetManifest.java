package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJob;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.XmlType;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * Represents the <code>manifest.xml</code> files that detail which specific files are included in a
 * transfer from the CMS Chronic Conditions Warehouse to the Blue Button API backend.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "",
    propOrder = {"entries", "preValidationProperties"})
@XmlRootElement(name = "dataSetManifest")
public final class DataSetManifest implements Comparable<DataSetManifest> {
  /** A timestamp {@link String} that maps to an S3 bucket folder. */
  @XmlAttribute(name = "timestamp", required = true)
  private final String timestampText;

  /** A numeric sequence identifier as provided by CCW for this batch of data. */
  @XmlAttribute(name = "sequenceId", required = true)
  private int sequenceId;

  /** A boolean denoting if this is synthetic data (true) or not (false}. */
  @XmlAttribute(name = "syntheticData", required = false)
  private boolean syntheticData = false;

  /** A list of {@link DataSetManifestEntry} elements that identify a RIF file. */
  @XmlElement(name = "entry")
  private final List<DataSetManifestEntry> entries;

  /** A {@link PreValidationProperties} optional element that provides pre-validation meta-data. */
  @XmlElement(name = "preValidationProperties", required = false)
  protected PreValidationProperties preValidationProperties;

  /** Denotes the s3 key where the manifest was located when it was first read. */
  @XmlTransient private String manifestKeyIncomingLocation;

  /**
   * Denotes the s3 key where the manifest and its files should be placed when it's processing is
   * complete.
   */
  @XmlTransient private String manifestKeyDoneLocation;

  /**
   * Denotes the s3 key where the manifest and its files should be placed when it's processing is
   * complete.
   */
  @XmlTransient private String manifestKeyFailLocation;

  /**
   * Constructs a new {@link DataSetManifest} instance.
   *
   * @param timestampText the value to use for {@link #getTimestampText()}
   * @param sequenceId the value to use for {@link #getSequenceId()}
   * @param syntheticData the value to use for {@link #isSyntheticData()}
   * @param manifestKeyIncomingLocation the value to use for {@link #manifestKeyIncomingLocation}
   * @param manifestKeyDoneLocation the value to use for {@link #manifestKeyDoneLocation}
   * @param entries the value to use for {@link #getEntries()}
   */
  public DataSetManifest(
      String timestampText,
      int sequenceId,
      boolean syntheticData,
      String manifestKeyIncomingLocation,
      String manifestKeyDoneLocation,
      List<DataSetManifestEntry> entries) {
    this.timestampText = timestampText;
    this.sequenceId = sequenceId;
    this.syntheticData = syntheticData;
    this.entries = entries;
    this.entries.forEach(entry -> entry.parentManifest = this);
    this.manifestKeyIncomingLocation = manifestKeyIncomingLocation;
    this.manifestKeyDoneLocation = manifestKeyDoneLocation;
  }

  /**
   * Constructs a new {@link DataSetManifest} instance.
   *
   * @param timestamp the value to use for {@link #getTimestampText()}
   * @param sequenceId the value to use for {@link #getSequenceId()}
   * @param syntheticData the value to use for {@link #isSyntheticData()}
   * @param entries the value to use for {@link #getEntries()}
   */
  public DataSetManifest(
      Instant timestamp,
      int sequenceId,
      boolean syntheticData,
      List<DataSetManifestEntry> entries) {
    // This appears to only be used in dead test code, so hardcoding the input/output
    // locations to the old locations unless we need otherwise
    this(
        DateTimeFormatter.ISO_INSTANT.format(timestamp),
        sequenceId,
        syntheticData,
        CcwRifLoadJob.S3_PREFIX_PENDING_DATA_SETS,
        CcwRifLoadJob.S3_PREFIX_COMPLETED_DATA_SETS,
        entries);
  }

  /**
   * Constructs a new {@link DataSetManifest} instance.
   *
   * @param timestampText the value to use for {@link #getTimestampText()}
   * @param sequenceId the value to use for {@link #getSequenceId()}
   * @param syntheticData the value to use for {@link #isSyntheticData()}
   * @param manifestKeyIncomingLocation the value to use for {@link #manifestKeyIncomingLocation}
   * @param manifestKeyDoneLocation the value to use for {@link #manifestKeyDoneLocation}
   * @param entries the value to use for {@link #getEntries()}
   */
  public DataSetManifest(
      String timestampText,
      int sequenceId,
      boolean syntheticData,
      String manifestKeyIncomingLocation,
      String manifestKeyDoneLocation,
      DataSetManifestEntry... entries) {
    this(
        timestampText,
        sequenceId,
        syntheticData,
        manifestKeyIncomingLocation,
        manifestKeyDoneLocation,
        Arrays.asList(entries));
  }

  /**
   * Constructs a new {@link DataSetManifest} instance.
   *
   * @param timestamp the value to use for {@link #getTimestampText()}
   * @param sequenceId the value to use for {@link #getSequenceId()}
   * @param syntheticData the value to use for {@link #isSyntheticData()}
   * @param manifestKeyIncomingLocation the value to use for {@link #manifestKeyIncomingLocation}
   * @param manifestKeyDoneLocation the value to use for {@link #manifestKeyDoneLocation}
   * @param entries the value to use for {@link #getEntries()}
   */
  public DataSetManifest(
      Instant timestamp,
      int sequenceId,
      boolean syntheticData,
      String manifestKeyIncomingLocation,
      String manifestKeyDoneLocation,
      DataSetManifestEntry... entries) {
    this(
        DateTimeFormatter.ISO_INSTANT.format(timestamp),
        sequenceId,
        syntheticData,
        manifestKeyIncomingLocation,
        manifestKeyDoneLocation,
        Arrays.asList(entries));
  }

  /** This default constructor is required by JAX-B, and should not otherwise be used. */
  @SuppressWarnings("unused")
  private DataSetManifest() {
    this.timestampText = null;
    this.entries = null;
    this.sequenceId = 0;
  }

  /**
   * Returns the original S3 key for this manifest.
   *
   * @return original S3 key
   */
  public String getIncomingS3Key() {
    return getId().computeS3Key(manifestKeyIncomingLocation);
  }

  /**
   * Gets the timestamp text.
   *
   * @return the {@link String} representation of {@link #getTimestamp()} used to identify this
   *     {@link DataSetManifest} in S3 and elsewhere
   */
  public String getTimestampText() {
    /*
     * Design note: As discovered in CBBD-298, Java's DateTimeFormatter and
     * Instant classes don't always preserve the precision of parsed
     * timestamps. Accordingly, we need to store this value as a String to
     * ensure that the S3 key of the manifest isn't mangled.
     */

    return timestampText;
  }

  /**
   * Gets the timestamp as an {@link Instant}.
   *
   * @return the date and time that the represented data set was created/prepared at
   */
  public Instant getTimestamp() {
    return Instant.parse(timestampText.trim());
  }

  /**
   * Gets the {@link #sequenceId}.
   *
   * @return the {@link int} sequence number of the file represented by this {@link DataSetManifest}
   */
  public int getSequenceId() {
    return sequenceId;
  }

  /**
   * Determines if this is synthetic data.
   *
   * @return the {@link boolean} denoting if the data is synthetic based on the {@link
   *     DataSetManifest}
   */
  public boolean isSyntheticData() {
    return syntheticData;
  }

  /**
   * Gets the {@link DataSetManifestId}.
   *
   * @return a {@link DataSetManifestId} that models this {@link DataSetManifest}'s identity and
   *     ordering
   */
  public DataSetManifestId getId() {
    return new DataSetManifestId(this);
  }

  /**
   * Gets the {@link #entries}.
   *
   * @return the list of {@link DataSetManifestEntry} included in this {@link DataSetManifest}
   */
  public List<DataSetManifestEntry> getEntries() {
    return entries;
  }

  /**
   * Sets the {@link #manifestKeyIncomingLocation}.
   *
   * @param location the location
   */
  public void setManifestKeyIncomingLocation(String location) {
    this.manifestKeyIncomingLocation = location;
  }

  /**
   * Sets the {@link #manifestKeyDoneLocation}.
   *
   * @param location the location
   */
  public void setManifestKeyDoneLocation(String location) {
    this.manifestKeyDoneLocation = location;
  }

  /**
   * Gets the {@link #manifestKeyIncomingLocation}.
   *
   * @return the incoming location key
   */
  public String getManifestKeyIncomingLocation() {
    return manifestKeyIncomingLocation;
  }

  /**
   * Gets the {@link #manifestKeyDoneLocation}.
   *
   * @return the done location key
   */
  public String getManifestKeyDoneLocation() {
    return manifestKeyDoneLocation;
  }

  /**
   * Gets the value of the syntheaEndStateProperties property.
   *
   * @return possible object is {@link PreValidationProperties }
   */
  public Optional<PreValidationProperties> getPreValidationProperties() {
    return preValidationProperties != null
        ? Optional.of(preValidationProperties)
        : Optional.empty();
  }

  /**
   * Sets the {@link #preValidationProperties}.
   *
   * @param value allowed object is {@link PreValidationProperties }
   */
  public void setPreValidationProperties(PreValidationProperties value) {
    this.preValidationProperties = value;
  }

  /** {@inheritDoc} */
  @Override
  public int compareTo(DataSetManifest o) {
    if (o == null) return 1;
    return getId().compareTo(o.getId());
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("DataSetManifest [timestamp=");
    builder.append(timestampText);
    builder.append(", sequenceId=");
    builder.append(sequenceId);
    builder.append(", syntheticData=");
    builder.append(syntheticData);
    builder.append(", entries=");
    builder.append(entries);
    if (preValidationProperties != null) {
      builder.append(preValidationProperties.toString());
    }
    builder.append("]");
    return builder.toString();
  }

  /**
   * Each {@link DataSetManifestEntry} instance represents a single file included in a {@link
   * DataSetManifest}.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static final class DataSetManifestEntry {
    /** The parent {@link DataSetManifest} of this element {@link DataSetManifestEntry}. */
    @XmlTransient private DataSetManifest parentManifest;

    /** The element name {@link String}. */
    @XmlAttribute private final String name;

    /** The file type {@link RifFileType} of this element. */
    @XmlAttribute private final RifFileType type;

    /** The file export type {@link String} of this element. */
    @XmlTransient private final String exportType;

    /**
     * Constructs a new {@link DataSetManifestEntry} instance.
     *
     * @param name the value to use for {@link #getName()}
     * @param type the value to use for {@link #getType()}
     */
    public DataSetManifestEntry(String name, RifFileType type) {
      this.parentManifest = null;
      this.name = name;
      this.type = type;
      this.exportType = null;
    }

    /** This default constructor is required by JAX-B, and should not otherwise be used. */
    @SuppressWarnings("unused")
    private DataSetManifestEntry() {
      this.name = null;
      this.type = null;
      this.exportType = null;
    }

    /**
     * Gets the {@link #parentManifest}.
     *
     * @return the {@link DataSetManifest} that this {@link DataSetManifestEntry} is a part of
     */
    public DataSetManifest getParentManifest() {
      return parentManifest;
    }

    /**
     * Gets the {@link #name}.
     *
     * @return the name of the S3 object/file that is represented by this {@link
     *     DataSetManifestEntry}, which is effectively a relative S3 key (relative to this <code>
     *      manifest.xml</code> object's key, that is)
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the {@link #type}.
     *
     * @return the {@link RifFileType} of the file represented by this {@link DataSetManifestEntry}
     */
    public RifFileType getType() {
      return type;
    }

    /**
     * Per the {@link Unmarshaller} JavaDocs, when unmarshalling {@link DataSetManifestEntry}
     * instances from XML via JAX-B, this method is called after all the properties (except IDREF)
     * are unmarshalled for this object, but before this object is set to the parent object.
     *
     * @param unmarshaller the {@link Unmarshaller} that created this instance
     * @param parent the value to use for {@link #getParentManifest()}
     */
    void afterUnmarshal(Unmarshaller unmarshaller, Object parent) {
      this.parentManifest = (DataSetManifest) parent;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("DataSetManifestEntry [parentManifest.getTimestamp()=");
      builder.append(parentManifest.getTimestampText());
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
   * Represents the unique identity of a {@link DataSetManifest}, allowing for equality comparisons.
   * Also implements {@link Comparable} such that the processing order of {@link DataSetManifest}s
   * can be determined.
   */
  public static final class DataSetManifestId implements Comparable<DataSetManifestId> {
    /** a {@link String} derived {@link DataSetManifest#getTimestamp()} value. */
    private final String timestampText;

    /** an {@link Instant} object derived timestampText {@link String} value. */
    private final Instant timestamp;

    /** an integer value derived from {@link DataSetManifest#getSequenceId()} value. */
    private final int sequenceId;

    /**
     * Constructs a new {@link DataSetManifestId}.
     *
     * @param timestampText a {@link String} representation of the {@link
     *     DataSetManifest#getTimestamp()} value
     * @param sequenceId the {@link DataSetManifest#getSequenceId()} value
     */
    private DataSetManifestId(String timestampText, int sequenceId) {
      this.timestampText = timestampText;
      this.timestamp = Instant.parse(timestampText);
      this.sequenceId = sequenceId;
    }

    /**
     * Constructs a new {@link DataSetManifestId}.
     *
     * @param manifest the {@link DataSetManifest} to extract the {@link DataSetManifestId} from
     */
    public DataSetManifestId(DataSetManifest manifest) {
      this(manifest.getTimestampText(), manifest.getSequenceId());
    }

    /**
     * Parses a {@link DataSetManifestId} from the S3 key of a {@link DataSetManifest}.
     *
     * @param s3ManifestKey the S3 key of the {@link DataSetManifest} to extract a {@link
     *     DataSetManifestId} from
     * @return the {@link DataSetManifestId} represented by the specified S3 key, or <code>null
     *     </code> if the key doesn't seem to point to a {@link DataSetManifest} ready for
     *     processing
     */
    public static DataSetManifestId parseManifestIdFromS3Key(String s3ManifestKey) {
      Matcher manifestKeyMatcher = CcwRifLoadJob.REGEX_PENDING_MANIFEST.matcher(s3ManifestKey);
      boolean keyMatchesRegex = manifestKeyMatcher.matches();

      if (!keyMatchesRegex) return null;

      String dataSetTimestampText = manifestKeyMatcher.group(2);
      try {
        Instant.parse(dataSetTimestampText);
      } catch (DateTimeParseException e) {
        return null;
      }

      int dataSetSequenceId = Integer.parseInt(manifestKeyMatcher.group(3));

      return new DataSetManifestId(dataSetTimestampText, dataSetSequenceId);
    }

    /**
     * Computes an s3 key string.
     *
     * @param s3Prefix the S3 key prefix that should be prepended to the calculated S3 key, e.g. "
     *     <code>Incoming</code>"
     * @return the S3 key for this {@link DataSetManifestId}, under the specified prefix
     */
    public String computeS3Key(String s3Prefix) {
      return String.format("%s/%s/%d_manifest.xml", s3Prefix, timestampText, sequenceId);
    }

    /**
     * Checks if the parsed manifest has a date in the future, compared to the current instant.
     *
     * @return {@code true} if the manifest has a future date
     */
    public boolean isFutureManifest() {
      return Instant.now().compareTo(timestamp) <= 0;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(DataSetManifestId o) {
      if (o == null) throw new IllegalArgumentException();

      /*
       * This is a two-level sort: always sort first by timestamp. Within
       * equal timestamps, sort by sequenceId. This ensures that data sets
       * are processed in the correct order.
       */

      int timestampComparison = timestamp.compareTo(o.timestamp);
      if (timestampComparison != 0) return timestampComparison;
      return Integer.compare(sequenceId, o.sequenceId);
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + sequenceId;
      result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
      return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      DataSetManifestId other = (DataSetManifestId) obj;
      if (sequenceId != other.sequenceId) return false;
      if (timestamp == null) {
        if (other.timestamp != null) return false;
      } else if (!timestamp.equals(other.timestamp)) return false;
      return true;
    }

    /** {@inheritDoc} */
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

  /** Optional object that can be used to perform pre-validation during ETL pipeline processing. */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class PreValidationProperties {
    /**
     * a {@link long} value denoting the lower-bound of claim group identifiers for the RIF dataset.
     */
    @XmlElement(name = "clm_grp_id_start", required = true)
    protected long clmGrpIdStart;

    /**
     * a {@link long} value denoting the lower-bound of Part D Event identifiers for the RIF
     * dataset.
     */
    @XmlElement(name = "pde_id_start", required = true)
    protected long pdeIdStart;

    /**
     * a {@link long} value denoting the lower-bound of Carrier Claim control number for the RIF
     * dataset.
     */
    @XmlElement(name = "carr_clm_cntl_num_start", required = true)
    protected long carrClmCntlNumStart;

    /**
     * a {@link long} value denoting the lower-bound of FI document control number for the RIF
     * dataset.
     */
    @XmlElement(name = "fi_doc_cntl_num_start", required = true)
    protected String fiDocCntlNumStart;

    /** a {@link String} denoting the starting point for the HICN hash value for the RIF dataset. */
    @XmlElement(name = "hicn_start", required = true)
    protected String hicnStart;

    /**
     * a {@link long} value denoting the lower-bound of the set of beneficiary identifiers for the
     * RIF dataset.
     */
    @XmlElement(name = "bene_id_start", required = true)
    protected long beneIdStart;

    /** a {@link long} value denoting the lower-bound of Claim ID(s) for the RIF dataset. */
    @XmlElement(name = "clm_id_start", required = true)
    protected long clmIdStart;

    /** a {@link String} denoting the starting point for the MBI hash value for the RIF dataset. */
    @XmlElement(name = "mbi_start", required = true)
    protected String mbiStart;

    /**
     * a {@link long} value denoting the upper-bound of the set of beneficiary identifiers for the
     * RIF dataset.
     */
    @XmlElement(name = "bene_id_end", required = true)
    protected long beneIdEnd;

    /**
     * a {@link long} value denoting the upper-bound of the set of Claim identifiers for the RIF
     * claims dataset.
     */
    @XmlElement(name = "clm_id_end", required = true)
    protected long clmIdEnd;

    /**
     * a {@link long} value denoting the upper-bound of the set of Part D events identifiers for the
     * RIF claims dataset.
     */
    @XmlElement(name = "pde_id_end", required = true)
    protected long pdeIdEnd;

    /** a {@link String} timestamp denoting when Synthea dataset was generated . */
    @XmlElement(name = "generated", required = false)
    protected String generated;

    /** Create an instance of {@link PreValidationProperties}. */
    public PreValidationProperties() {}

    /**
     * Constructs a new {@link PreValidationProperties}.
     *
     * @param clmGrpIdStart lower-bound bene_id range value to verify
     * @param pdeIdStart lower-bound bene_id range value to verify
     * @param carrClmCntlNumStart lower-bound bene_id range value to verify
     * @param fiDocCntlNumStart lower-bound bene_id range value to verify
     * @param hicnStart string denoting where genertated HICN hash range values begin
     * @param beneIdStart lower-bound bene_id range value to verify
     * @param clmIdStart lower-bound bene_id range value to verify
     * @param mbiStart string denoting where genertated HICN hash range values begin
     * @param beneIdEnd upper-bound bene_id range value to verify
     * @param clmIdEnd upper-bound bene_id range value to verify
     * @param pdeIdEnd upper-bound bene_id range value to verify
     * @param generated string denoting when the end state meta-data was generated
     */
    public PreValidationProperties(
        long clmGrpIdStart,
        long pdeIdStart,
        long carrClmCntlNumStart,
        String fiDocCntlNumStart,
        String hicnStart,
        long beneIdStart,
        long clmIdStart,
        String mbiStart,
        long beneIdEnd,
        long clmIdEnd,
        long pdeIdEnd,
        String generated) {
      this.clmGrpIdStart = clmGrpIdStart;
      this.pdeIdStart = pdeIdStart;
      this.carrClmCntlNumStart = carrClmCntlNumStart;
      this.fiDocCntlNumStart = fiDocCntlNumStart;
      this.hicnStart = hicnStart;
      this.beneIdStart = beneIdStart;
      this.clmIdStart = clmIdStart;
      this.mbiStart = mbiStart;
      this.beneIdEnd = beneIdEnd;
      this.clmIdEnd = clmIdEnd;
      this.pdeIdEnd = pdeIdEnd;
      this.generated = generated;
    }

    /**
     * Gets the value of the {@link #clmGrpIdStart} property.
     *
     * @return value {@link long }
     */
    public long getClmGrpIdStart() {
      return clmGrpIdStart;
    }

    /**
     * Sets the {@link #clmGrpIdStart}.
     *
     * @param value {@link long} to set
     */
    public void setClmGrpIdStart(long value) {
      this.clmGrpIdStart = value;
    }

    /**
     * Gets the value of the {@link #pdeIdStart} property.
     *
     * @return value {@link long }
     */
    public long getPdeIdStart() {
      return pdeIdStart;
    }

    /**
     * Sets the {@link #pdeIdStart}.
     *
     * @param value {@link long} to set
     */
    public void setPdeIdStart(long value) {
      this.pdeIdStart = value;
    }

    /**
     * Gets the value of the {@link #carrClmCntlNumStart} property.
     *
     * @return value {@link long }
     */
    public long getCarrClmCntlNumStart() {
      return carrClmCntlNumStart;
    }

    /**
     * Sets the {@link #carrClmCntlNumStart}.
     *
     * @param value {@link long} to set
     */
    public void setCarrClmCntlNumStart(long value) {
      this.carrClmCntlNumStart = value;
    }

    /**
     * Gets the value of the {@link #fiDocCntlNumStart} property.
     *
     * @return value {@link String}
     */
    public String getFiDocCntlNumStart() {
      return fiDocCntlNumStart;
    }

    /**
     * Sets the {@link #fiDocCntlNumStart}.
     *
     * @param value {@link String} to set
     */
    public void setFiDocCntlNumStart(String value) {
      this.fiDocCntlNumStart = value;
    }

    /**
     * Gets the value of the {@link #hicnStart} property.
     *
     * @return possible object is {@link String }
     */
    public String getHicnStart() {
      return hicnStart;
    }

    /**
     * Sets the {@link #hicnStart}.
     *
     * @param value allowed object is {@link String }
     */
    public void setHicnStart(String value) {
      this.hicnStart = value;
    }

    /**
     * Gets the value of the {@link #beneIdStart} property.
     *
     * @return value {@link long }
     */
    public long getBeneIdStart() {
      return beneIdStart;
    }

    /**
     * Sets the {@link #beneIdStart}.
     *
     * @param value {@link long} to set
     */
    public void setBeneIdStart(long value) {
      this.beneIdStart = value;
    }

    /**
     * Gets the value of the {@link #clmIdStart} property.
     *
     * @return value {@link long }
     */
    public long getClmIdStart() {
      return clmIdStart;
    }

    /**
     * Sets the {@link #clmIdStart}.
     *
     * @param value {@link long} to set
     */
    public void setClmIdStart(long value) {
      this.clmIdStart = value;
    }

    /**
     * Gets the value of the {@link #mbiStart} property.
     *
     * @return possible object is {@link String }
     */
    public String getMbiStart() {
      return mbiStart;
    }

    /**
     * Sets the {@link #mbiStart}.
     *
     * @param value allowed object is {@link String }
     */
    public void setMbiStart(String value) {
      this.mbiStart = value;
    }

    /**
     * Gets the value of the {@link #beneIdEnd} property.
     *
     * @return value {@link long }
     */
    public long getBeneIdEnd() {
      return beneIdEnd;
    }

    /**
     * Sets the {@link #beneIdEnd}.
     *
     * @param value {@link long} to set
     */
    public void setBeneIdEnd(long value) {
      this.beneIdEnd = value;
    }

    /**
     * Gets the value of the {@link #clmIdEnd} property.
     *
     * @return value {@link long }
     */
    public long getClmIdEnd() {
      return clmIdEnd;
    }

    /**
     * Sets the {@link #clmIdEnd}.
     *
     * @param value {@link long} to set
     */
    public void setClmIdEnd(long value) {
      this.clmIdEnd = value;
    }

    /**
     * Gets the value of the {@link #pdeIdEnd} property.
     *
     * @return value {@link long }
     */
    public long getPdeIdEnd() {
      return pdeIdEnd;
    }

    /**
     * Sets the {@link #pdeIdEnd}.
     *
     * @param value {@link long} to set
     */
    public void setPdeIdEnd(long value) {
      this.pdeIdEnd = value;
    }

    /**
     * Gets the value of the {@link #generated} property.
     *
     * @return possible object is {@link String }
     */
    public String getGenerated() {
      return generated;
    }

    /**
     * Sets the {@link #generated}.
     *
     * @param value allowed object is {@link String }
     */
    public void setGenerated(String value) {
      this.generated = value;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("; PreValidationProperties [clmGrpIdStart=");
      sb.append(clmGrpIdStart);
      sb.append(", pdeIdStart=");
      sb.append(pdeIdStart);
      sb.append(", carrClmCntlNumStart=");
      sb.append(carrClmCntlNumStart);
      sb.append(", fiDocCntlNumStart=");
      sb.append(fiDocCntlNumStart);
      sb.append(", hicnStart=");
      sb.append(hicnStart);
      sb.append(", beneIdStart=");
      sb.append(beneIdStart);
      sb.append(", clmIdStart=");
      sb.append(clmIdStart);
      sb.append(", mbiStart=");
      sb.append(mbiStart);
      sb.append(", beneIdEnd=");
      sb.append(beneIdEnd);
      sb.append(", clmIdEnd=");
      sb.append(clmIdEnd);
      sb.append(", pdeIdEnd=");
      sb.append(pdeIdEnd);
      sb.append(", generated=");
      sb.append(generated);
      sb.append("]");
      return sb.toString();
    }
  }
}
