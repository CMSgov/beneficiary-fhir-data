package gov.cms.bfd.model.rif;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/** Class to build a LoadedBatch. Thread safe. */
public class LoadedBatchBuilder {
  /** The beneficiaries in this batch. */
  private final List<Long> beneficiaries;

  /** The loaded file's identifier. */
  private final long loadedFileId;

  /** The batch creation timestamp. -- GETTER -- Gets the timestamp of the batch */
  @Getter private final Instant timestamp;

  /**
   * Create a builder from a particular file event.
   *
   * @param loadedFileId to start building
   * @param capacityIncrement to use for this batch
   */
  public LoadedBatchBuilder(long loadedFileId, int capacityIncrement) {
    this.loadedFileId = loadedFileId;
    this.beneficiaries = new ArrayList<>(capacityIncrement);
    this.timestamp = Instant.now();
  }

  /**
   * Associate a beneficiaryId with this LoadedFile.
   *
   * @param beneficiaryId to put in the filter
   */
  public synchronized void associateBeneficiary(Long beneficiaryId) {
    if (beneficiaryId == null) {
      throw new IllegalArgumentException("Null or empty beneficiary");
    }
    beneficiaries.add(beneficiaryId);
  }

  /**
   * Create a LoadedBatch from the data in the builder.
   *
   * @return a new LoadedBatch
   */
  public synchronized LoadedBatch build() {
    final LoadedBatch loadedBatch = new LoadedBatch();
    loadedBatch.setLoadedFileId(loadedFileId);
    loadedBatch.setBeneficiaries(beneficiaries);
    loadedBatch.setCreated(timestamp);
    return loadedBatch;
  }
}
