package gov.cms.bfd.model.rif;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/** Represents a loaded RIF file. */
@Entity
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

  /** Default constructor. */
  public LoadedFile() {}

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

  /**
   * Gets the {@link #loadedFileId}.
   *
   * @return the identifier
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
   * Gets the {@link #rifType}.
   *
   * @return the rifType
   */
  public String getRifType() {
    return rifType;
  }

  /**
   * Sets the {@link #rifType}.
   *
   * @param rifType the rifType to set
   */
  public void setRifType(String rifType) {
    this.rifType = rifType;
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
   * Gets the {@link #batches}.
   *
   * @return the batches associated with this file
   */
  public Set<LoadedBatch> getBatches() {
    return batches;
  }

  /**
   * Sets the {@link #batches}.
   *
   * @param batches associated with this file
   */
  public void setBatches(Set<LoadedBatch> batches) {
    this.batches = batches;
  }
}
