package gov.cms.bfd.pipeline.ccw.rif;

import java.time.Instant;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.With;

/**
 * Java bean published using an {@link gov.cms.bfd.events.EventPublisher} to inform external systems
 * of the pipeline's status.
 */
@Data
@Builder
@With
public class CcwRifLoadJobStatusEvent {
  /** Current stage of processing. */
  private final JobStage jobStage;

  /**
   * Optional S3 key of most recently completed manifest. Once present it maintains the same value
   * until a different manifest has been completed.
   */
  @Nullable private final String lastCompletedManifestKey;

  /**
   * Optional timestamp for when most recently completed manifest was finished processing. Once
   * present it maintains the same value until a different manifest has been completed.
   */
  @Nullable private final Instant lastCompletedTimestamp;

  /** Optional S3 key of manifest currently being processed. */
  @Nullable private final String currentManifestKey;

  /**
   * Optional timestamp for when the job first found it had nothing to do. Only present if {@link
   * #jobStage} is {@link JobStage#NothingToDo}.
   */
  @Nullable private final Instant nothingToDoSinceTimestamp;

  /** Timestamp for when this event was created. */
  private final Instant currentTimestamp;

  /**
   * Represents milestones in the job's control flow. Granular enough to inform external systems
   * without flooding the event publisher with minutia.
   */
  public enum JobStage {
    /** Scanning bucket looking for a manifest to process. */
    CheckingBucketForManifest,
    /** Awaiting arrival of all data files for current manifest in the S3 bucket. */
    AwaitingManifestDataFiles,
    /** Processing the data files for a manifest. */
    ProcessingManifestDataFiles,
    /** Processing of a manifest and its data files is now complete. */
    CompletedManifest,
    /** Nothing available to process at the moment. */
    NothingToDo
  }
}
