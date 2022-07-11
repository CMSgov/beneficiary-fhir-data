package gov.cms.bfd.model.rif;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Represents a table for storing CCW RIF records that have been temporarily skipped.
 *
 * <p>Please note that every record in this table is a piece of as-yet-unaddressed technical debt,
 * with an exorbitantly high "interest rate": the older these records get, the harder it will be to
 * figure out what to do with them, and the harder it will be to implement any such process.
 * Ideally, this table is only ever populated for VERY brief periods of time.
 *
 * <p>Added as part of <a href="https://jira.cms.gov/browse/BFD-1566">BFD-1566</a>.
 */
@Entity
@Table(name = "skipped_rif_records_new")
public class SkippedRifRecord {
  /** The unique (sequence-generated) ID for this {@link SkippedRifRecord} instance. */
  @Id
  @Column(name = "record_id", nullable = false)
  @GeneratedValue(
      strategy = GenerationType.SEQUENCE,
      generator = "skipped_rif_records_record_id_seq")
  @SequenceGenerator(
      name = "skipped_rif_records_record_id_seq",
      sequenceName = "skipped_rif_records_record_id_seq",
      allocationSize = 1)
  private long recordId;

  /** The timestamp associated with the CCW data set manifest that this record is from. */
  @Column(name = "rif_file_timestamp", nullable = false)
  private Instant rifFileTimestamp;

  /**
   * The {@link SkipReasonCode} that identifies why this {@link SkippedRifRecord} was skipped in the
   * first place.
   */
  @Column(name = "skip_reason", nullable = false)
  private String skipReason;

  /** The {@link RifFileType} of the RIF file that this record is from. */
  @Column(name = "rif_file_type", nullable = false)
  private String rifFileType;

  /** The {@link RecordAction} of the RIF record(s). */
  @Column(name = "dml_ind", nullable = false)
  private String dmlInd;

  /**
   * The {@link Beneficiary}{@link #getBeneId()} of the {@link Beneficiary} that this record is of /
   * associated with.
   */
  @Column(name = "bene_id", nullable = false)
  private long beneId;

  /**
   * The RIF/CSV row or rows representing the record (i.e. beneficiary or claim) that was skipped.
   */
  @Column(name = "rif_data", nullable = false)
  private String rifData;

  /** This default constructor is required, per the JPA spec. */
  public SkippedRifRecord() {}

  /**
   * Constructs a new {@link SkippedRifRecord} instance.
   *
   * @param rifFileTimestamp the value to use for {@link #getRifFileTimestamp()}
   * @param skipReason the value to use for {@link #getSkipReason()}
   * @param rifFileType the value to use for {@link #getRifFileType()}
   * @param dmlInd the value to use for {@link #getDmlInd()}
   * @param beneId the value to use for {@link #getBeneId()}
   * @param rifData the value to use for {@link #getRifData()}
   */
  public SkippedRifRecord(
      Instant rifFileTimestamp,
      SkipReasonCode skipReason,
      String rifFileType,
      RecordAction dmlInd,
      long beneId,
      String rifData) {
    this.rifFileTimestamp = rifFileTimestamp;
    this.skipReason = skipReason.name();
    this.rifFileType = rifFileType;
    this.dmlInd = dmlInd.name();
    this.beneId = beneId;
    this.rifData = rifData;
  }

  /**
   * Gets the {@link #recordId}.
   *
   * @return the unique (sequence-generated) ID for this {@link SkippedRifRecord} instance
   */
  public long getRecordId() {
    return recordId;
  }

  /**
   * Gets the {@link #skipReason}.
   *
   * @return the {@link SkipReasonCode} that identifies why this {@link SkippedRifRecord} was
   *     skipped in the first place
   */
  public SkipReasonCode getSkipReason() {
    return SkipReasonCode.valueOf(skipReason);
  }

  /**
   * Gets the {@link #rifFileTimestamp}.
   *
   * @return the timestamp associated with the CCW data set manifest that this record is from
   */
  public Instant getRifFileTimestamp() {
    return rifFileTimestamp;
  }

  /**
   * Gets the {@link #rifFileType}.
   *
   * @return the {@link RifFileType} of the RIF file that this record is from
   */
  public String getRifFileType() {
    return rifFileType;
  }

  /**
   * Gets the {@link #dmlInd}.
   *
   * @return the {@link RecordAction} of the RIF record(s)
   */
  public RecordAction getDmlInd() {
    return RecordAction.match(dmlInd);
  }

  /**
   * Gets the {@link #beneId}.
   *
   * @return the {@link Beneficiary}{@link #getBeneId()} of the {@link Beneficiary} that this record
   *     is of / associated with
   */
  public long getBeneId() {
    return beneId;
  }

  /**
   * Gets the {@link #rifData}.
   *
   * @return the RIF/CSV row or rows representing the record (i.e. beneficiary or claim) that was
   *     skipped
   */
  public String getRifData() {
    return rifData;
  }

  /** Represents the allowed/known values for {@link SkippedRifRecord#getSkipReason()}. */
  public static enum SkipReasonCode {
    /**
     * The code that represents the filtering for non-null and 2022 benes, implemented as part of <a
     * href="https://jira.cms.gov/browse/BFD-1566">BFD-1566</a>.
     */
    DELAYED_BACKDATED_ENROLLMENT_BFD_1566;
  }
}
