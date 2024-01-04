package gov.cms.bfd.pipeline.ccw.rif;

import gov.cms.bfd.events.EventPublisher;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.CcwRifLoadJobStatusEventBuilder;
import static gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.JobStage.AwaitingManifestDataFiles;
import static gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.JobStage.CheckingBucketForManifest;
import static gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.JobStage.Idle;
import static gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.JobStage.ProcessingManifestDataFiles;
import static java.time.ZoneOffset.UTC;
import lombok.RequiredArgsConstructor;

import java.time.Clock;
import java.time.ZonedDateTime;

@RequiredArgsConstructor
public class CcwRifLoadJobStatusReporter {
  private final EventPublisher publisher;
  private final Clock clock;
  private String lastCompletedManifestKey;
  private ZonedDateTime lastCompletedTimestamp;

  public void reportCheckingBucketForManifest() {
    final var event = createEvent().jobStage(CheckingBucketForManifest).build();
    publisher.publishEvent(event);
  }

  public void reportAwaitingManifestData(String manifestKey) {
    final var event =
        createEvent().jobStage(AwaitingManifestDataFiles).currentManifestKey(manifestKey).build();
    publisher.publishEvent(event);
  }

  public void reportProcessingManifestData(String manifestKey) {
    final var event =
        createEvent().jobStage(ProcessingManifestDataFiles).currentManifestKey(manifestKey).build();
    publisher.publishEvent(event);
  }

  public void reportCompletedManifest(String manifestKey) {
    lastCompletedManifestKey = manifestKey;
    lastCompletedTimestamp = currentTimestamp();
    final var event =
        createEvent().jobStage(AwaitingManifestDataFiles).currentManifestKey(manifestKey).build();
    publisher.publishEvent(event);
  }

  public void reportIdle() {
    final var event = createEvent().jobStage(Idle).build();
    publisher.publishEvent(event);
  }

  private ZonedDateTime currentTimestamp() {
    return clock.instant().atZone(UTC);
  }

  private CcwRifLoadJobStatusEventBuilder createEvent() {
    return CcwRifLoadJobStatusEvent.builder()
        .currentTimestamp(currentTimestamp())
        .lastCompletedManifestKey(lastCompletedManifestKey)
        .lastCompletedTimestamp(lastCompletedTimestamp);
  }
}
