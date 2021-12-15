package gov.cms.bfd.model.rif;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

@Entity
@Table(name = "loaded_files")
public class LoadedFile {
  @Id
  @Column(name = "loaded_fileid", nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loadedfiles_loadedfileid_seq")
  @SequenceGenerator(
      name = "loadedFiles_loadedFileId_seq",
      sequenceName = "loadedfiles_loadedfileid_seq",
      allocationSize = 1)
  private long loaded_fileid;

  @Column(name = "rif_type", nullable = false)
  private String rif_type;

  @Column(name = "created", nullable = false)
  private Instant created;

  @OneToMany(
      mappedBy = "loaded_fileid",
      orphanRemoval = false,
      fetch = FetchType.LAZY,
      cascade = CascadeType.ALL)
  private Set<LoadedBatch> batches = new HashSet<>();

  public LoadedFile() {}

  /**
   * Create a LoadedFile
   *
   * @param loadedFileId id
   * @param rifType RifFileType
   * @param created time stamp
   */
  public LoadedFile(long loadedFileId, String rifType, Instant created) {
    this();
    this.loaded_fileid = loadedFileId;
    this.rif_type = rifType;
    this.created = created;
  }

  /**
   * Create a LoadedFile
   *
   * @param rifType RifFileType
   */
  public LoadedFile(String rifType) {
    this();
    this.rif_type = rifType;
  }

  /** @return the identifier */
  public long getLoadedFileId() {
    return loaded_fileid;
  }

  /** @param loadedFileId the identifier to set */
  public void setLoadedFileId(long loadedFileId) {
    this.loaded_fileid = loadedFileId;
  }

  /** @return the rifType */
  public String getRifType() {
    return rif_type;
  }

  /** @param rifType the rifType to set */
  public void setRifType(String rifType) {
    this.rif_type = rifType;
  }

  /** @return the creation time stamp */
  public Instant getCreated() {
    return created;
  }

  /** @param created time stamp to set */
  public void setCreated(Instant created) {
    this.created = created;
  }

  /** @return the batches associated with this file */
  public Set<LoadedBatch> getBatches() {
    return batches;
  }

  /** @param batches associated with this file */
  public void setBatches(Set<LoadedBatch> batches) {
    this.batches = batches;
  }
}
