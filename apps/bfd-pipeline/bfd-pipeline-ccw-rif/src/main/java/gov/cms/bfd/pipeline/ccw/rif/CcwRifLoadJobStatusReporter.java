package gov.cms.bfd.pipeline.ccw.rif;

import static gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.JobStage.AwaitingManifestDataFiles;
import static gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.JobStage.CheckingBucketForManifest;
import static gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.JobStage.Idle;
import static gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.JobStage.ProcessingManifestDataFiles;

import gov.cms.bfd.events.EventPublisher;
import java.time.Clock;
import java.time.Instant;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;

/**
 * An object that publishes status updates to external systems about current stage of {@link
 * CcwRifLoadJob} processing.
 */
@RequiredArgsConstructor
public class CcwRifLoadJobStatusReporter {
  /** Uses to publish the status events. */
  private final EventPublisher publisher;

  /** Used to obtain current time. */
  private final Clock clock;

  /** Null or the S3 key of the most recently completed manifest. */
  @Nullable private String lastCompletedManifestKey;

  /** Null or the timestamp when most recently completed manifest was reported. */
  @Nullable private Instant lastCompletedTimestamp;

  /** Send a status event when bucket is about to be scanned. */
  public void reportCheckingBucketForManifest() {
    final var event = createEvent(CheckingBucketForManifest);
    publisher.publishEvent(event);
  }

  /**
   * Send a status event when waiting for data files for a manifest to become available in S3.
   *
   * @param manifestKey S3 key of the manifest
   */
  public void reportAwaitingManifestData(String manifestKey) {
    final var event = createEvent(AwaitingManifestDataFiles).withCurrentManifestKey(manifestKey);
    publisher.publishEvent(event);
  }

  /**
   * Send a status event when processing of a manifest's data files starts.
   *
   * @param manifestKey S3 key of the manifest
   */
  public void reportProcessingManifestData(String manifestKey) {
    final var event = createEvent(ProcessingManifestDataFiles).withCurrentManifestKey(manifestKey);
    publisher.publishEvent(event);
  }

  /**
   * Send a status event when processing of a manifest's data files has been completed successfully.
   *
   * @param manifestKey S3 key of the manifest
   */
  public void reportCompletedManifest(String manifestKey) {
    lastCompletedManifestKey = manifestKey;
    lastCompletedTimestamp = clock.instant();
    final var event = createEvent(AwaitingManifestDataFiles).withCurrentManifestKey(manifestKey);
    publisher.publishEvent(event);
  }

  /** Send a status event when no manifest is available to be processed. */
  public void reportIdle() {
    final var event = createEvent(Idle);
    publisher.publishEvent(event);
  }

  /**
   * Creates a bare event based on the given stage and our current field values.
   *
   * @param jobStage stage to include in the event
   * @return the event
   */
  private CcwRifLoadJobStatusEvent createEvent(CcwRifLoadJobStatusEvent.JobStage jobStage) {
    return CcwRifLoadJobStatusEvent.builder()
        .jobStage(jobStage)
        .currentTimestamp(clock.instant())
        .lastCompletedManifestKey(lastCompletedManifestKey)
        .lastCompletedTimestamp(lastCompletedTimestamp)
        .build();
  }
}
