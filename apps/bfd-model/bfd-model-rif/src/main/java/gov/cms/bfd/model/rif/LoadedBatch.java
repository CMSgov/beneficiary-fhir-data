package gov.cms.bfd.model.rif;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.*;

/** JPA class for the loaded_batches table */
@Entity
@Table(name = "loaded_batches")
public class LoadedBatch {
  public static final String SEPARATOR = ",";

  @Id
  @Column(name = "loaded_batchid", nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loadedBatches_loadedBatchId_seq")
  @SequenceGenerator(
      name = "loadedBatches_loadedBatchId_seq",
      sequenceName = "loadedBatches_loadedBatchId_seq",
      allocationSize = 20)
  private long loaded_batchid;

  @Column(name = "loaded_fileid", nullable = false)
  private long loaded_fileid;

  @Column(name = "beneficiaries", columnDefinition = "varchar", nullable = false)
  private String beneficiaries;

  @Column(name = "created", nullable = false)
  private Instant created;

  /** default constructor */
  public LoadedBatch() {}

  /**
   * Create with known values
   *
   * @param loaded_batchid unique sequence id
   * @param loaded_fileid associated file
   * @param beneficiaries to associate
   * @param created batch creation date
   */
  public LoadedBatch(long loadedBatchId, long loadedFileId, String beneficiaries, Instant created) {
    this();
    this.loaded_batchid = loadedBatchId;
    this.loaded_fileid = loadedFileId;
    this.beneficiaries = beneficiaries;
    this.created = created;
  }

  /**
   * Create with known values
   *
   * @param loaded_batchid unique sequence id
   * @param loaded_fileid associated file
   * @param beneficiaries to associate
   * @param created batch creation date
   */
  public LoadedBatch(
      long loadedBatchId, long loadedFileId, List<String> beneficiaries, Instant created) {
    this();
    this.loaded_batchid = loadedBatchId;
    this.loaded_fileid = loadedFileId;
    this.beneficiaries = convertToString(beneficiaries);
    this.created = created;
  }

  /** @return the loadedBatchId */
  public long getLoadedBatchId() {
    return loaded_fileid;
  }

  /** @param loadedBatchId the identifier to set */
  public void setLoadedBatchId(long loadedBatchId) {
    this.loaded_batchid = loadedBatchId;
  }

  /** @return the loadedFileId */
  public long getLoadedFileId() {
    return loaded_fileid;
  }

  /** @param loadedFileId the identifier to set */
  public void setLoadedFileId(long loadedFileId) {
    this.loaded_fileid = loadedFileId;
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
    return created;
  }

  /** @param created time stamp to set */
  public void setCreated(Instant created) {
    this.created = created;
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
    sum.loaded_batchid = a.loaded_batchid;
    sum.loaded_fileid = a.loaded_fileid;
    sum.beneficiaries =
        a.beneficiaries.isEmpty()
            ? b.beneficiaries
            : b.beneficiaries.isEmpty()
                ? a.beneficiaries
                : a.beneficiaries + SEPARATOR + b.beneficiaries;
    sum.created = (a.created.isAfter(b.created)) ? a.created : b.created;
    return sum;
  }

  /*
   * Dev Note: A JPA AttributeConverter could be created instead of these static methods. This is
   * slightly simpler and, since conversion is done once, just as efficient.
   */
  private static String convertToString(List<String> list) {
    return (list == null || list.isEmpty())
        ? ""
        : list.stream().collect(Collectors.joining(SEPARATOR));
  }

  private static List<String> convertToList(String commaSeparated) {
    return (commaSeparated == null || commaSeparated.isEmpty())
        ? new ArrayList<>()
        : Arrays.asList(commaSeparated.split(SEPARATOR, -1));
  }
}
