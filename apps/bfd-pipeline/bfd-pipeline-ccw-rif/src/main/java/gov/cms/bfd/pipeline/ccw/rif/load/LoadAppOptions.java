package gov.cms.bfd.pipeline.ccw.rif.load;

import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifRecordEvent;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import lombok.Data;
import lombok.Getter;

/** Models the user-configurable application options. */
public final class LoadAppOptions {

  /**
   * A reasonable (though not terribly performant) suggested default value for {@link
   * PerformanceSettings#loaderThreads}.
   */
  public static final int DEFAULT_LOADER_THREADS =
      Math.max(1, (Runtime.getRuntime().availableProcessors() - 1)) * 2;

  /** The config for the id hasher. */
  @Getter private final IdHasher.Config idHasherConfig;

  /** If idempotency mode should be used. */
  @Getter private final boolean idempotencyRequired;

  /** Settings used for loading beneficiary data. */
  @Getter private final PerformanceSettings beneficiaryPerformanceSettings;

  /** Settings used for loading claims data. */
  @Getter private final PerformanceSettings claimPerformanceSettings;

  /**
   * Initializes an instance.
   *
   * @param idHasherConfig the value to use for {@link #idHasherConfig}
   * @param idempotencyRequired the value to use for {@link #idempotencyRequired}
   * @param beneficiaryPerformanceSettings performance settings used for beneficiary records
   * @param claimPerformanceSettings performance settings used for claim records
   */
  public LoadAppOptions(
      IdHasher.Config idHasherConfig,
      boolean idempotencyRequired,
      PerformanceSettings beneficiaryPerformanceSettings,
      PerformanceSettings claimPerformanceSettings) {

    this.idHasherConfig = idHasherConfig;
    this.idempotencyRequired = idempotencyRequired;
    this.beneficiaryPerformanceSettings = beneficiaryPerformanceSettings;
    this.claimPerformanceSettings = claimPerformanceSettings;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("LoadAppOptions [hicnHashIterations=");
    builder.append(idHasherConfig.getHashIterations());
    builder.append(", hicnHashPepper=");
    builder.append("***");
    builder.append(", idempotencyRequired=");
    builder.append(idempotencyRequired);
    builder.append(", beneficiaryPerformanceSettings=");
    builder.append(beneficiaryPerformanceSettings);
    builder.append(", claimPerformanceSettings=");
    builder.append(claimPerformanceSettings);
    builder.append("]");
    return builder.toString();
  }

  /**
   * Select the appropriate {@link PerformanceSettings} to use for the given {@link RifFileType}.
   *
   * @param fileType type of rif data being loaded
   * @return appropriate performance settings
   */
  public PerformanceSettings selectPerformanceSettingsForFileType(RifFileType fileType) {
    return switch (fileType) {
      case BENEFICIARY, BENEFICIARY_HISTORY -> beneficiaryPerformanceSettings;
      default -> claimPerformanceSettings;
    };
  }

  /** Settings used for performance tuning of the {@link RifLoader}. */
  @Data
  public static class PerformanceSettings {
    /** The number of loader threads. */
    private final int loaderThreads;

    /**
     * The number of {@link RifRecordEvent}s that will be included in each processing batch. Note
     * that larger batch sizes mean that more {@link RifRecordEvent}s will be held in memory
     * simultaneously.
     */
    private final int recordBatchSize;

    /** The maximum size (per thread) of the task queue used to process batches. */
    private final int taskQueueSizeMultiple;
  }
}
