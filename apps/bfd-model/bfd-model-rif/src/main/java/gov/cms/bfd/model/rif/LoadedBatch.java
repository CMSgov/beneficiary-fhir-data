package gov.cms.bfd.model.rif;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** JPA class for the loaded_batches table. */
@Entity
@Table(name = "loaded_batches")
public class LoadedBatch {
  /** Separator for joining and splitting data. */
  public static final String SEPARATOR = ",";

  /** The batch identifier. */
  @Id
  @Column(name = "loaded_batch_id", nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loadedbatches_loadedbatchid_seq")
  @SequenceGenerator(
      name = "loadedbatches_loadedbatchid_seq",
      sequenceName = "loadedbatches_loadedbatchid_seq",
      allocationSize = 20)
  private long loadedBatchId;

  /** The loaded file identifier. */
  @Column(name = "loaded_file_id", nullable = false)
  private long loadedFileId;

  /** The beneficiaries in this batch. */
  @Column(name = "beneficiaries", columnDefinition = "varchar", nullable = false)
  private String beneficiaries;

  /** The batch creation timestamp. */
  @Column(name = "created", nullable = false)
  private Instant created;

  /** Default constructor. */
  public LoadedBatch() {}

  /**
   * Creates a new LoadedBatch with known values.
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
    this.created = created;
  }

  /**
   * Creates a new LoadedBatch with known values.
   *
   * @param loadedBatchId unique sequence id
   * @param loadedFileId associated file
   * @param beneficiaries to associate
   * @param created batch creation date
   */
  public LoadedBatch(
      long loadedBatchId, long loadedFileId, List<Long> beneficiaries, Instant created) {
    this();
    this.loadedBatchId = loadedBatchId;
    this.loadedFileId = loadedFileId;
    this.beneficiaries = convertToString(beneficiaries);
    this.created = created;
  }

  /**
   * Gets the {@link #loadedBatchId}.
   *
   * @return the loadedBatchId
   */
  public long getLoadedBatchId() {
    return loadedBatchId;
  }

  /**
   * Sets the {@link #loadedBatchId}.
   *
   * @param loadedBatchId the identifier to set
   */
  public void setLoadedBatchId(long loadedBatchId) {
    this.loadedBatchId = loadedBatchId;
  }

  /**
   * Gets the {@link #loadedFileId}.
   *
   * @return the loadedFileId
   */
  public long getLoadedFileId() {
    return loadedFileId;
  }

  /**
   * Sets the {@link #loadedFileId}.
   *
   * @param loadedFileId the identifier to set
   */
  public void setLoadedFileId(long loadedFileId) {
    this.loadedFileId = loadedFileId;
  }

  /**
   * Gets the {@link #beneficiaries}.
   *
   * @return the beneficiaries
   */
  public String getBeneficiaries() {
    return beneficiaries;
  }

  /**
   * Sets the {@link #beneficiaries}.
   *
   * @param beneficiaries the beneficiaryId to set
   */
  public void setBeneficiaries(String beneficiaries) {
    this.beneficiaries = beneficiaries;
  }

  /**
   * Gets the {@link #created}.
   *
   * @return the creation time stamp
   */
  public Instant getCreated() {
    return created;
  }

  /**
   * Sets the {@link #created}.
   *
   * @param created time stamp to set
   */
  public void setCreated(Instant created) {
    this.created = created;
  }

  /**
   * Set the {@link #beneficiaries} from a list.
   *
   * @param beneficiaries list to convert
   */
  public void setBeneficiaries(List<Long> beneficiaries) {
    this.beneficiaries = convertToString(beneficiaries);
  }

  /**
   * Get the {@link #beneficiaries} as a list.
   *
   * @return beneficiaries as list
   */
  public List<Long> getBeneficiariesAsList() {
    return convertToList(this.beneficiaries);
  }

  /**
   * Utility function to combine to batch into a larger batch. Useful for small number of batches.
   *
   * @param a batch to combine
   * @param b batch to combine
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
    sum.created = (a.created.isAfter(b.created)) ? a.created : b.created;
    return sum;
  }

  /*
   * Dev Note: A JPA AttributeConverter could be created instead of these static methods. This is
   * slightly simpler and, since conversion is done once, just as efficient.
   */

  /**
   * Converts a string list to a single string, delimited by {@link #SEPARATOR}.
   *
   * @param list the list to convert
   * @return the string containing the values of the string list delimited by {@link #SEPARATOR}
   */
  private static String convertToString(List<Long> list) {
    if (list == null || list.isEmpty()) {
      return "";
    }
    return list.stream().map(String::valueOf).collect(Collectors.joining(SEPARATOR));
  }

  /**
   * Converts a {@link #SEPARATOR} delimited string to a list of strings.
   *
   * @param commaSeparated the {@link #SEPARATOR} separated string
   * @return the list of string values
   */
  private static List<Long> convertToList(String commaSeparated) {
    if (commaSeparated == null || commaSeparated.isEmpty()) {
      return new ArrayList<Long>();
    }
    return Stream.of(commaSeparated.split(SEPARATOR))
        .map(Long::parseLong)
        .collect(Collectors.toList());
  }
}
