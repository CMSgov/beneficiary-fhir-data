package gov.cms.bfd.model.rif;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Class to build a LoadedBatch. Thread safe. */
public class LoadedBatchBuilder {
  private final List<String> beneficiaries;
  private final long loadedFileId;

  /**
   * Create a builder from a particular file event
   *
   * @param loadedFileId to start building
   * @param capacityIncrement to use for this batch
   */
  public LoadedBatchBuilder(long loadedFileId, int capacityIncrement) {
    this.loadedFileId = loadedFileId;
    this.beneficiaries = new ArrayList<>(capacityIncrement);
  }

  /**
   * Associate a beneficiaryId with this LoadedFile
   *
   * @param beneficiaryId to put in the filter
   */
  public synchronized void associateBeneficiary(String beneficiaryId) {
    if (beneficiaryId == null || beneficiaryId.isEmpty()) {
      throw new IllegalArgumentException("Null or empty beneficiary");
    }
    beneficiaries.add(beneficiaryId);
  }

  /**
   * Create a LoadedBatch from the data in the builder
   *
   * @return a new LoadedBatch
   */
  public synchronized LoadedBatch build() {
    final LoadedBatch loadedBatch = new LoadedBatch();
    loadedBatch.setLoadedFileId(loadedFileId);
    loadedBatch.setBeneficiaries(beneficiaries);
    loadedBatch.setCreated(new Date());
    return loadedBatch;
  }
}
