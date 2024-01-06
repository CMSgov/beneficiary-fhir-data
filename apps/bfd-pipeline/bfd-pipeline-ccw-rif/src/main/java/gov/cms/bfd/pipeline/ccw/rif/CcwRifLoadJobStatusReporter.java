package gov.cms.bfd.pipeline.ccw.rif;

import static gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.JobStage.AwaitingManifestDataFiles;
import static gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.JobStage.CheckingBucketForManifest;
import static gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.JobStage.CompletedManifest;
import static gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.JobStage.NothingToDo;
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
  @Nullable private Instant lastCompletedTime;

  /** Null or the timestamp when job first reported that it was idle. */
  @Nullable private Instant nothingToDoSinceTime;

  /** Send a status event when bucket is about to be scanned. */
  public void reportCheckingBucketForManifest() {
    nothingToDoSinceTime = null;
    final var event = createEvent(CheckingBucketForManifest, clock.instant());
    publisher.publishEvent(event);
  }

  /**
   * Send a status event when waiting for data files for a manifest to become available in S3.
   *
   * @param manifestKey S3 key of the manifest
   */
  public void reportAwaitingManifestData(String manifestKey) {
    nothingToDoSinceTime = null;
    final var event =
        createEvent(AwaitingManifestDataFiles, clock.instant()).withCurrentManifestKey(manifestKey);
    publisher.publishEvent(event);
  }

  /**
   * Send a status event when processing of a manifest's data files starts.
   *
   * @param manifestKey S3 key of the manifest
   */
  public void reportProcessingManifestData(String manifestKey) {
    nothingToDoSinceTime = null;
    final var event =
        createEvent(ProcessingManifestDataFiles, clock.instant())
            .withCurrentManifestKey(manifestKey);
    publisher.publishEvent(event);
  }

  /**
   * Send a status event when processing of a manifest's data files has been completed successfully.
   *
   * @param manifestKey S3 key of the manifest
   */
  public void reportCompletedManifest(String manifestKey) {
    final Instant currentTime = clock.instant();
    lastCompletedManifestKey = manifestKey;
    lastCompletedTime = currentTime;
    nothingToDoSinceTime = null;
    final var event =
        createEvent(CompletedManifest, currentTime).withCurrentManifestKey(manifestKey);
    publisher.publishEvent(event);
  }

  /** Send a status event when no manifest is available to be processed. */
  public void reportNothingToDo() {
    final Instant currentTime = clock.instant();
    if (nothingToDoSinceTime == null) {
      nothingToDoSinceTime = currentTime;
    }
    final var event = createEvent(NothingToDo, currentTime);
    publisher.publishEvent(event);
  }

  /**
   * Creates a bare event based on the given stage and our current field values.
   *
   * @param jobStage stage to include in the event
   * @param currentTime value for current timestamp
   * @return the event
   */
  private CcwRifLoadJobStatusEvent createEvent(
      CcwRifLoadJobStatusEvent.JobStage jobStage, Instant currentTime) {
    return CcwRifLoadJobStatusEvent.builder()
        .jobStage(jobStage)
        .currentTimestamp(currentTime)
        .lastCompletedManifestKey(lastCompletedManifestKey)
        .lastCompletedTimestamp(lastCompletedTime)
        .nothingToDoSinceTimestamp(nothingToDoSinceTime)
        .build();
  }
}
