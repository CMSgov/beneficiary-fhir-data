package gov.cms.bfd.model.rif;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.*;

/** JPA class for the LoadedBatches table */
@Entity
@Table(name = "`LoadedBatches`")
public class LoadedBatch {
  public static final String SEPARATOR = ",";

  @Id
  @Column(name = "`loadedBatchId`", nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loadedBatches_loadedBatchId_seq")
  @SequenceGenerator(
      name = "loadedBatches_loadedBatchId_seq",
      sequenceName = "loadedBatches_loadedBatchId_seq",
      allocationSize = 20)
  private long loadedBatchId;

  @Column(name = "`loadedFileId`", nullable = false)
  private long loadedFileId;

  @Column(name = "`beneficiaries`", columnDefinition = "varchar", nullable = false)
  private String beneficiaries;

  @Column(name = "`created`", nullable = false)
  // @Temporal(TemporalType.TIMESTAMP)
  private java.sql.Timestamp created;

  /** default constructor */
  public LoadedBatch() {}

  /**
   * Create with known values
   *
   * @param loadedBatchId unique sequence id
   * @param loadedFileId associated file
   * @param beneficiaries to associate
   * @param created batch creation date
   */
  public LoadedBatch(long loadedBatchId, long loadedFileId, String beneficiaries, Instant created) {
    this();
    this.loadedBatchId = loadedBatchId;
    this.loadedFileId = loadedFileId;
    this.beneficiaries = beneficiaries;
    this.created = java.sql.Timestamp.from(created);
  }

  /**
   * Create with known values
   *
   * @param loadedBatchId unique sequence id
   * @param loadedFileId associated file
   * @param beneficiaries to associate
   * @param created batch creation date
   */
  public LoadedBatch(
      long loadedBatchId, long loadedFileId, List<String> beneficiaries, Instant created) {
    this();
    this.loadedBatchId = loadedBatchId;
    this.loadedFileId = loadedFileId;
    this.beneficiaries = convertToString(beneficiaries);
    this.created = java.sql.Timestamp.from(created);
  }

  /** @return the loadedBatchId */
  public long getLoadedBatchId() {
    return loadedFileId;
  }

  /** @param loadedBatchId the identifier to set */
  public void setLoadedBatchId(long loadedBatchId) {
    this.loadedBatchId = loadedBatchId;
  }

  /** @return the loadedFileId */
  public long getLoadedFileId() {
    return loadedFileId;
  }

  /** @param loadedFileId the identifier to set */
  public void setLoadedFileId(long loadedFileId) {
    this.loadedFileId = loadedFileId;
  }

  /** @return the beneficiaries */
  public String getBeneficiaries() {
    return beneficiaries;
  }

  /** @param beneficiaries the beneficiaryId to set */
  public void setBeneficiaries(String beneficiaries) {
    this.beneficiaries = beneficiaries;
  }

  /** @return the creation time stamp */
  public Instant getCreated() {
    return created.toInstant();
  }

  /** @param created time stamp to set */
  public void setCreated(Instant created) {
    this.created = java.sql.Timestamp.from(created);
  }

  /**
   * Set the beneficiaries from a list
   *
   * @param beneficiaries list to convert
   */
  public void setBeneficiaries(List<String> beneficiaries) {
    this.beneficiaries = convertToString(beneficiaries);
  }

  /**
   * Get the beneficiaries as a list
   *
   * @return beneficiaries as list
   */
  public List<String> getBeneficiariesAsList() {
    return convertToList(this.beneficiaries);
  }

  /**
   * Utility function to combine to batch into a larger batch. Useful for small number of batches.
   *
   * @param a batch
   * @param b batch
   * @return batch which has id of a, beneficiaries of both, and the latest created
   */
  public static LoadedBatch combine(LoadedBatch a, LoadedBatch b) {
    if (a == null) return b;
    if (b == null) return a;
    LoadedBatch sum = new LoadedBatch();
    sum.loadedBatchId = a.loadedBatchId;
    sum.loadedFileId = a.loadedFileId;
    sum.beneficiaries =
        a.beneficiaries.isEmpty()
            ? b.beneficiaries
            : b.beneficiaries.isEmpty()
                ? a.beneficiaries
                : a.beneficiaries + SEPARATOR + b.beneficiaries;
    sum.created = (a.created.after(b.created)) ? a.created : b.created;
    return sum;
  }

  /*
   * Dev Note: A JPA AttributeConverter could be created instead of these static methods. This is
   * slightly simpler and, since conversion is done once, just as efficient.
   */
  private static String convertToString(List<String> list) {
    if (list == null || list.isEmpty()) {
      return "";
    }
    return list.stream().collect(Collectors.joining(SEPARATOR));
  }

  private static List<String> convertToList(String commaSeparated) {
    if (commaSeparated == null || commaSeparated.isEmpty()) {
      return new ArrayList<>();
    }
    return Arrays.asList(commaSeparated.split(SEPARATOR, -1));
  }
}
