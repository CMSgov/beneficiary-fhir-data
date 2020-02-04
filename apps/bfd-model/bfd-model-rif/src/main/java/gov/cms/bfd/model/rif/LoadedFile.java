package gov.cms.bfd.model.rif;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

@Entity
@Table(name = "`LoadedFiles`")
public class LoadedFile {
  @Id
  @Column(name = "`loadedFileId`", nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loadedFiles_loadedFileId_seq")
  @SequenceGenerator(
      name = "loadedFiles_loadedFileId_seq",
      sequenceName = "loadedFiles_loadedFileId_seq",
      allocationSize = 1)
  private long loadedFileId;

  @Column(name = "`rifType`", nullable = false)
  private String rifType;

  @Column(name = "`created`", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private Date created;

  @OneToMany(
      mappedBy = "loadedFileId",
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
  public LoadedFile(long loadedFileId, String rifType, Date created) {
    this();
    this.loadedFileId = loadedFileId;
    this.rifType = rifType;
    this.created = created;
  }

  /**
   * Create a LoadedFile
   *
   * @param rifType RifFileType
   */
  public LoadedFile(String rifType) {
    this();
    this.rifType = rifType;
  }

  /** @return the identifier */
  public long getLoadedFileId() {
    return loadedFileId;
  }

  /** @param loadedFileId the identifier to set */
  public void setLoadedFileId(long loadedFileId) {
    this.loadedFileId = loadedFileId;
  }

  /** @return the rifType */
  public String getRifType() {
    return rifType;
  }

  /** @param rifType the rifType to set */
  public void setRifType(String rifType) {
    this.rifType = rifType;
  }

  /** @return the creation time stamp */
  public Date getCreated() {
    return created;
  }

  /** @param created time stamp to set */
  public void setCreated(Date created) {
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
