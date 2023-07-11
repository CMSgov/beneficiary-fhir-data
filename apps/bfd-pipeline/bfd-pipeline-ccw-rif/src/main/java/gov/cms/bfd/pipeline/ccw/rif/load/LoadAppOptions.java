package gov.cms.bfd.pipeline.ccw.rif.load;

import gov.cms.bfd.model.rif.Beneficiary;
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
  /**
   * Special property used to filter non-2023 beneficiaries from loading, as sometimes our upstream
   * partners have historically sent us previous years mixed with the current year, which causes
   * issues with the database overwriting newer year data with older.
   *
   * <p>As part of <a href="https://jira.cms.gov/browse/BFD-1566">BFD-1566</a> and <a
   * href="https://jira.cms.gov/browse/BFD-2265">BFD-2265</a>, we want a filtering mechanism in our
   * loads such some {@link Beneficiary}s are temporarily skipped: only those with a {@link
   * Beneficiary#getBeneEnrollmentReferenceYear()} of "2023" or where the reference year is {@code
   * null} will be processed. As part of this filtering, we are implementing an assumption that no
   * non-2023 {@code INSERT} {@link Beneficiary} records will be received, as skipping those would
   * also require skipping their associated claims, which is additional complexity that we want to
   * avoid. If any such records are encountered, the load will go boom. This filtering is an
   * inelegant hack to workaround upstream data issues, and was ideally only in place very
   * temporarily, although it's now been in place for at least a year. See the code that uses this
   * field in {@link RifLoader} for details. This filtering is being made configurable so as not to
   * invalidate all of our existing test coverage.
   */
  @Getter private final boolean filteringNonNullAndNon2023Benes;

  /** Settings used for loading beneficiary data. */
  @Getter private final PerformanceSettings beneficiaryPerformanceSettings;

  /** Settings used for loading claims data. */
  @Getter private final PerformanceSettings claimPerformanceSettings;

  /**
   * Initializes an instance.
   *
   * @param idHasherConfig the value to use for {@link #idHasherConfig}
   * @param idempotencyRequired the value to use for {@link #idempotencyRequired}
   * @param filterNon2023Benes the filter non 2023 benes
   * @param beneficiaryPerformanceSettings performance settings used for beneficiary records
   * @param claimPerformanceSettings performance settings used for claim records
   */
  public LoadAppOptions(
      IdHasher.Config idHasherConfig,
      boolean idempotencyRequired,
      boolean filterNon2023Benes,
      PerformanceSettings beneficiaryPerformanceSettings,
      PerformanceSettings claimPerformanceSettings) {

    this.idHasherConfig = idHasherConfig;
    this.idempotencyRequired = idempotencyRequired;
    this.filteringNonNullAndNon2023Benes = filterNon2023Benes;
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
    builder.append(", filteringNonNullAndNon2023Benes=");
    builder.append(filteringNonNullAndNon2023Benes);
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

    /**
     * Initializes an instance.
     *
     * @param loaderThreads the value to use for {@link #loaderThreads}
     * @param recordBatchSize the load batch size
     * @param taskQueueSizeMultiple the task queue size multiple
     */
    public PerformanceSettings(int loaderThreads, int recordBatchSize, int taskQueueSizeMultiple) {
      if (loaderThreads < 1) throw new IllegalArgumentException();
      if (taskQueueSizeMultiple < 1) throw new IllegalArgumentException();
      this.loaderThreads = loaderThreads;
      this.recordBatchSize = recordBatchSize;
      this.taskQueueSizeMultiple = taskQueueSizeMultiple;
    }
  }
}
