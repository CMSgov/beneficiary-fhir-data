package gov.cms.bfd.model.rif;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/** JPA class for the LoadedBatches table */
@Entity
@Table(name = "`LoadedBatches`")
public class LoadedBatch {
  @Id
  @Column(name = "`loadedBatchId`", nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "loadedBatches_loadedBatchId_seq")
  @SequenceGenerator(
      name = "loadedBatches_loadedBatchId_seq",
      sequenceName = "loadedBatches_loadedBatchId_seq",
      allocationSize = 100)
  private long loadedBatchId;

  @Column(name = "`loadedFileId`", nullable = false)
  private long loadedFileId;

  @Column(name = "`beneficiaries`", columnDefinition = "varchar", nullable = false)
  @Convert(converter = LoadedBenficiaryConverter.class)
  private List<String> beneficiaries;

  @Column(name = "`created`", nullable = false)
  private Date created;

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
  public LoadedBatch(
      long loadedBatchId, long loadedFileId, List<String> beneficiaries, Date created) {
    this();
    this.loadedBatchId = loadedBatchId;
    this.loadedFileId = loadedFileId;
    this.beneficiaries = beneficiaries;
    this.created = created;
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
  public List<String> getBeneficiaries() {
    return beneficiaries;
  }

  /** @param beneficiaries the beneficiaryId to set */
  public void setBeneficiaries(List<String> beneficiaries) {
    this.beneficiaries = beneficiaries;
  }

  /** @return the creation time stamp */
  public Date getCreated() {
    return created;
  }

  /** @param created time stamp to set */
  public void setCreated(Date created) {
    this.created = created;
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
    ArrayList<String> beneficiaries =
        new ArrayList<>(a.beneficiaries.size() + b.beneficiaries.size());
    beneficiaries.addAll(a.beneficiaries);
    beneficiaries.addAll(b.beneficiaries);
    LoadedBatch sum = new LoadedBatch();
    sum.loadedBatchId = a.loadedBatchId;
    sum.loadedFileId = a.loadedFileId;
    sum.beneficiaries = beneficiaries;
    sum.created = (a.created.after(b.created)) ? a.created : b.created;
    return sum;
  }
}
