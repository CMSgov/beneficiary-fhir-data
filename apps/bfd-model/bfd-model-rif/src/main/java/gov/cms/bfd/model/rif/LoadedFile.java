package gov.cms.bfd.model.rif;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
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
@Table(name = "loaded_files")
public class LoadedFile {
  /** The file identifier. */
  @Id
  @Column(name = "loaded_file_id", nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loadedfiles_loadedfileid_seq")
  @SequenceGenerator(
      name = "loadedfiles_loadedfileid_seq",
      sequenceName = "loadedfiles_loadedfileid_seq",
      allocationSize = 1)
  private long loadedFileId;

  /** The rif type of this file. */
  @Column(name = "rif_type", nullable = false)
  private String rifType;

  /** The creation timestamp. */
  @Column(name = "created", nullable = false)
  private Instant created;

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

  /**
   * Create a LoadedFile.
   *
   * @param rifType RifFileType
   */
  public LoadedFile(String rifType) {
    this();
    this.rifType = rifType;
  }
}
