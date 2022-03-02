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
@Table(name = "skipped_rif_records")
public class SkippedRifRecord {
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

  @Column(name = "rif_file_timestamp", nullable = false)
  private Instant rifFileTimestamp;

  @Column(name = "rif_file_type", nullable = false)
  private String rifFileType;

  @Column(name = "dml_ind", nullable = false)
  private String dmlInd;

  @Column(name = "bene_id", nullable = false)
  private long beneId;

  @Column(name = "rif_data", nullable = false)
  private String rifData;

  /** This default constructor is required, per the JPA spec. */
  public SkippedRifRecord() {}

  /**
   * Constructs a new {@link SkippedRifRecord} instance.
   *
   * @param rifFileTimestamp the value to use for {@link #getRifFileTimestamp()}
   * @param rifFileType the value to use for {@link #getRifFileType()}
   * @param dmlInd the value to use for {@link #getDmlInd()}
   * @param beneId the value to use for {@link #getBeneId()}
   * @param rifData the value to use for {@link #getRifData()}
   */
  public SkippedRifRecord(
      Instant rifFileTimestamp,
      String rifFileType,
      RecordAction dmlInd,
      long beneId,
      String rifData) {
    this.rifFileTimestamp = rifFileTimestamp;
    this.rifFileType = rifFileType;
    this.dmlInd = dmlInd.name();
    this.beneId = beneId;
    this.rifData = rifData;
  }

  /**
   * Constructs a new {@link SkippedRifRecord} instance.
   *
   * @param rifFileTimestamp the value to use for {@link #getRifFileTimestamp()}
   * @param rifFileType the value to use for {@link #getRifFileType()}
   * @param dmlInd the value to use for {@link #getDmlInd()}
   * @param beneId the value to use for {@link #getBeneId()}
   * @param rifData the value to use for {@link #getRifData()}
   */
  public SkippedRifRecord(
      Instant rifFileTimestamp,
      String rifFileType,
      RecordAction dmlInd,
      String beneId,
      String rifData) {
    this(rifFileTimestamp, rifFileType, dmlInd, Long.parseLong(beneId), rifData);
  }

  /** @return the unique (sequence-generated) ID for this {@link SkippedRifRecord} instance */
  public long getRecordId() {
    return recordId;
  }

  /** @return the timestamp associated with the CCW data set manifest that this record is from */
  public Instant getRifFileTimestamp() {
    return rifFileTimestamp;
  }

  /** @return the {@link RifFileType} of the RIF file that this record is from */
  public String getRifFileType() {
    return rifFileType;
  }

  /** @return the {@link RecordAction} of the RIF record(s) */
  public RecordAction getDmlInd() {
    return RecordAction.match(dmlInd);
  }

  /**
   * @return the {@link Beneficiary}{@link #getBeneId()} of the {@link Beneficiary} that this record
   *     is of / associated with
   */
  public long getBeneId() {
    return beneId;
  }

  /**
   * @return the RIF/CSV row or rows representing the record (i.e. beneficiary or claim) that was
   *     skipped
   */
  public String getRifData() {
    return rifData;
  }
}
