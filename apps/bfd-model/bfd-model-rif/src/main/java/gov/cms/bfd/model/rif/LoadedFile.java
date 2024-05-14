package gov.cms.bfd.model.rif;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Represents a loaded RIF file. */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "loaded_files", schema = "ccw")
public class LoadedFile {
  /** The file identifier. */
  @Id
  @Column(name = "loaded_file_id", nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loadedfiles_loadedfileid_seq")
  @SequenceGenerator(
      name = "loadedfiles_loadedfileid_seq",
      sequenceName = "ccw.loadedfiles_loadedfileid_seq",
      allocationSize = 1)
  private long loadedFileId;

  /** The rif type of this file. */
  @Column(name = "rif_type", nullable = false)
  private String rifType;

  /** The creation timestamp. */
  @Column(name = "created", nullable = false)
  private Instant created;

  /**
   * Optional {@link gov.cms.bfd.model.rif.entities.S3ManifestFile#manifestId}. Can be used to join
   * with s3_manifest_files and s3_data_files.
   */
  @Column(name = "s3_manifest_id")
  private Long s3ManifestId;

  /**
   * Optional {@link gov.cms.bfd.model.rif.entities.S3DataFile#index}. Can be used to join with
   * s3_data_files.
   */
  @Column(name = "s3_file_index")
  private Short s3FileIndex;

  /** The batches associated with this file. */
  @OneToMany(
      mappedBy = "loadedFileId",
      orphanRemoval = false,
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL)
  private Set<LoadedBatch> batches = new HashSet<>();

  /**
   * Creates a LoadedFile.
   *
   * @param loadedFileId id
   * @param rifType RifFileType
   * @param created time stamp
   */
  public LoadedFile(long loadedFileId, String rifType, Instant created) {
    this();
    this.loadedFileId = loadedFileId;
    this.rifType = rifType;
    this.created = created;
  }
}
