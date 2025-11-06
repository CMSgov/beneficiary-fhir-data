package gov.cms.bfd.model.rif;

import jakarta.persistence.*;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
  @Convert(converter = BeneficiariesConverter.class)
  private List<Long> beneficiaries;

  /** The batch creation timestamp. */
  @Column(name = "created", nullable = false)
  private Instant created;

  /**
   * Creates a new LoadedBatch with known values where {@code beneficiaries} has not yet been
   * transformed into a {@link List} of {@link Long}s; e.g. when loading {@link LoadedBatch}s from
   * the database using raw {@link ResultSet}s
   *
   * @param loadedBatchId unique sequence id
   * @param loadedFileId associated {@link LoadedFile} ID
   * @param beneficiariesCsv comma-delimited {@link String} list of beneficiary IDs
   * @param created batch creation date
   */
  public LoadedBatch(
      long loadedBatchId, long loadedFileId, String beneficiariesCsv, Instant created) {
    this();
    this.loadedBatchId = loadedBatchId;
    this.loadedFileId = loadedFileId;
    this.beneficiaries = BeneficiariesConverter.toBeneficiariesList(beneficiariesCsv);
    this.created = created;
  }

  @Converter
  private static class BeneficiariesConverter implements AttributeConverter<List<Long>, String> {
    public static final String SEPARATOR = ",";

    public static String toBenesCsv(List<Long> benes) {
      if (benes == null) {
        return "";
      }

      return benes.stream().map(String::valueOf).collect(Collectors.joining(SEPARATOR));
    }

    public static List<Long> toBeneficiariesList(String benesCsv) {
      if (benesCsv == null) {
        return List.of();
      }

      return Arrays.stream(benesCsv.split(",")).map(Long::parseLong).toList();
    }

    @Override
    public String convertToDatabaseColumn(List<Long> benes) {
      return toBenesCsv(benes);
    }

    @Override
    public List<Long> convertToEntityAttribute(String benesCsv) {
      return toBeneficiariesList(benesCsv);
    }
  }
}
