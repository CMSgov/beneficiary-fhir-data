package gov.cms.bfd.model.rif;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA class for the loaded_batches table. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "loaded_batches", schema = "ccw")
public class LoadedBatch {
  /** Separator for joining and splitting data. */
  public static final String SEPARATOR = ",";

  /** The batch identifier. */
  @Id
  @Column(name = "loaded_batch_id", nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loadedbatches_loadedbatchid_seq")
  @SequenceGenerator(
      name = "loadedbatches_loadedbatchid_seq",
      sequenceName = "ccw.loadedbatches_loadedbatchid_seq",
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

  /**
   * The beneficiaries as an actual {@code List<String>}.
   *
   * @implNote We store this as a field on this object to avoid unnecessary string splitting/joining
   *     operations each time we want to use this object during the creation of Bloom Filters within
   *     the Server. At most, this list will be 100 elements large, so the extra memory consumption
   *     is worth the tradeoff in reduced computation time.
   */
  @Transient private List<Long> beneficiariesList;

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
    this.beneficiariesList = beneficiaries;
    this.created = created;
  }

  /**
   * Creates a new LoadedBatch with known values where {@code beneficiaries} has not yet been
   * transformed into a {@link List} of {@link Long}s.
   *
   * @param loadedBatchId unique sequence id
   * @param loadedFileId associated file
   * @param beneficiariesCsv to associate with this
   * @param created batch creation date
   * @implNote This constructor avoids an unnecessary, immediate conversion from an actual {@link
   *     List} back to a {@link String} for {@code beneficiaries} when building this object from raw
   *     {@link java.sql.ResultSet}s in the V1/V2 Server Bloom Filter manager. Essentially, we
   *     _have_ to do some work to convert the CSV into a {@link List}, but we do not need to do any
   *     work to just store the CSV as-is from the database.
   */
  public LoadedBatch(
      long loadedBatchId, long loadedFileId, String beneficiariesCsv, Instant created) {
    this();
    this.loadedBatchId = loadedBatchId;
    this.loadedFileId = loadedFileId;
    this.beneficiaries = beneficiariesCsv;
    this.beneficiariesList =
        Arrays.stream(beneficiariesCsv.split(",")).map(Long::parseLong).toList();
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
    return beneficiariesList;
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

    return new LoadedBatch(
        a.loadedBatchId,
        a.loadedFileId,
        Stream.concat(a.beneficiariesList.stream(), b.beneficiariesList.stream()).toList(),
        (a.created.isAfter(b.created)) ? a.created : b.created);
  }

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
}
