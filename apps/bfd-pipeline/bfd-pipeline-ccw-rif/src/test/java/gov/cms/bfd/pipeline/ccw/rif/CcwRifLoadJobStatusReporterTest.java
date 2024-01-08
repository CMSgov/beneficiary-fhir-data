package gov.cms.bfd.pipeline.ccw.rif;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import gov.cms.bfd.json.JsonConverter;
import gov.cms.bfd.pipeline.ccw.rif.CcwRifLoadJobStatusEvent.JobStage;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CcwRifLoadJobStatusReporter}. */
public class CcwRifLoadJobStatusReporterTest {
  /** Only these stages are allowed to have a current time stamp. */
  private static final Set<JobStage> STAGES_WITH_MANIFEST_KEY =
      Set.of(
          JobStage.AwaitingManifestDataFiles,
          JobStage.ProcessingManifestDataFiles,
          JobStage.CompletedManifest);

  /** List of events published during the test. */
  private List<CcwRifLoadJobStatusEvent> publishedEvents;

  /** The {@link CcwRifLoadJobStatusReporter} being tested. */
  private CcwRifLoadJobStatusReporter reporter;

  /** Current epoch millisecond value used for timestamps in tests. */
  private long currentMillis;

  /** Sets up the simulated clock, event list, and the reporter we are testing. */
  @BeforeEach
  void setUp() {
    // allow the test to set the time to any millisecond by setting currentMillis
    currentMillis = 0;
    var clock = mock(Clock.class);
    doAnswer(i -> Instant.ofEpochMilli(currentMillis)).when(clock).instant();

    publishedEvents = new ArrayList<>();
    reporter = new CcwRifLoadJobStatusReporter(this::publishEvent, clock);
  }

  /** Publish a {@link JobStage#CheckingBucketForManifest} event and check its invariants. */
  @Test
  void testCheckingBucketForManifestOnly() {
    reporter.reportCheckingBucketForManifest();
    checkLatestEvent(JobStage.CheckingBucketForManifest, null);
    checkLatestEventInvariants();
  }

  /** Publish a {@link JobStage#AwaitingManifestDataFiles} event and check its invariants. */
  @Test
  void testAwaitingManifestDataOnly() {
    reporter.reportAwaitingManifestData("1");
    checkLatestEvent(JobStage.AwaitingManifestDataFiles, "1");
    checkLatestEventInvariants();
  }

  /** Publish a {@link JobStage#ProcessingManifestDataFiles} event and check its invariants. */
  @Test
  void testProcessingManifestDataOnly() {
    reporter.reportProcessingManifestData("1");
    checkLatestEvent(JobStage.ProcessingManifestDataFiles, "1");
    checkLatestEventInvariants();
  }

  /** Publish a {@link JobStage#CompletedManifest} event and check its invariants. */
  @Test
  void testCompletedManifestOnly() {
    reporter.reportCompletedManifest("1");
    checkLatestEvent(JobStage.CompletedManifest, "1");
    checkLatestEventInvariants();
  }

  /** Publish a {@link JobStage#NothingToDo} event and check its invariants. */
  @Test
  void testNothingToDoOnly() {
    reporter.reportNothingToDo();
    checkLatestEvent(JobStage.NothingToDo, null);
    checkLatestEventInvariants();
  }

  /**
   * Go through a realistic set of events and verify the invariants hold for every published event.
   * The events are nothing to do, the complete set of events for two different manifests, and then
   * two more nothing to do events.
   */
  @Test
  void testIdleThenBusyThenIdleAgain() {
    currentMillis = 30_000;

    reporter.reportNothingToDo();
    checkLatestEvent(JobStage.NothingToDo, null);
    checkLatestEventInvariants();

    final var baseKey = "Incoming/2022-02-11T01:03:33Z";

    for (int sequenceNumber : List.of(0, 1, 2)) {
      final var manifestKey = String.format("%s/%d_manifest.xml", baseKey, sequenceNumber);

      incrementTheClock();
      reporter.reportCheckingBucketForManifest();
      checkLatestEvent(JobStage.CheckingBucketForManifest, null);
      checkLatestEventInvariants();

      incrementTheClock();
      reporter.reportAwaitingManifestData(manifestKey);
      checkLatestEvent(JobStage.AwaitingManifestDataFiles, manifestKey);
      checkLatestEventInvariants();

      incrementTheClock();
      reporter.reportProcessingManifestData(manifestKey);
      checkLatestEvent(JobStage.ProcessingManifestDataFiles, manifestKey);
      checkLatestEventInvariants();

      incrementTheClock();
      reporter.reportCompletedManifest(manifestKey);
      checkLatestEvent(JobStage.CompletedManifest, manifestKey);
      checkLatestEventInvariants();
    }

    incrementTheClock();
    reporter.reportNothingToDo();
    checkLatestEvent(JobStage.NothingToDo, null);
    checkLatestEventInvariants();

    incrementTheClock();
    reporter.reportNothingToDo();
    checkLatestEvent(JobStage.NothingToDo, null);
    checkLatestEventInvariants();

    printEvents();
  }

  /**
   * During development this can be used to print all events to verify the JSON looks correct.
   */
  private void printEvents() {
    final var converter = JsonConverter.prettyInstance();
    for (CcwRifLoadJobStatusEvent event : publishedEvents) {
      System.out.println(converter.objectToJson(event));
    }
  }

  /** Bumps the simulated clock by one second. */
  private void incrementTheClock() {
    currentMillis += 1000;
  }

  /**
   * Publishes events by validating they are instances of {@link CcwRifLoadJobStatusEvent} and then
   * adding them to the end our {@link #publishedEvents}.
   *
   * @param event event to publish
   */
  private void publishEvent(Object event) {
    assertThat(event).isInstanceOf(CcwRifLoadJobStatusEvent.class);
    publishedEvents.add((CcwRifLoadJobStatusEvent) event);
  }

  /**
   * Checks the stage, manifest key, and current time stamp of the most recently published event.
   *
   * @param expectedStage expected job stage
   * @param expectedManifestKey expected current manifest key
   */
  private void checkLatestEvent(JobStage expectedStage, String expectedManifestKey) {
    assertThat(publishedEvents).isNotEmpty();
    final var latestEvent = publishedEvents.getLast();
    assertEquals(expectedStage, latestEvent.getJobStage());
    assertEquals(expectedManifestKey, latestEvent.getCurrentManifestKey());
    assertEquals(currentMillis, latestEvent.getCurrentTimestamp().toEpochMilli());
  }

  /** Checks the invariants for the most recently published event. */
  private void checkLatestEventInvariants() {
    assertThat(publishedEvents).isNotEmpty();
    final int lastIndex = publishedEvents.size() - 1;
    final var previousEvent = (lastIndex > 0) ? publishedEvents.get(lastIndex - 1) : null;
    final var latestEvent = publishedEvents.get(lastIndex);
    checkEventInvariants(previousEvent, latestEvent);
  }

  /**
   * Checks all of the class invariants hold for the provided {@link CcwRifLoadJobStatusEvent}s.
   *
   * @param previous the previous event or null if current is the first event
   * @param current the event to check invariants for
   */
  private void checkEventInvariants(
      CcwRifLoadJobStatusEvent previous, CcwRifLoadJobStatusEvent current) {
    // Required fields must always be present
    assertNotNull(current.getJobStage());
    assertNotNull(current.getCurrentTimestamp());

    // Only certain stages can have a current manifest key
    if (STAGES_WITH_MANIFEST_KEY.contains(current.getJobStage())) {
      assertThat(current.getCurrentManifestKey())
          .describedAs("stage requires a manifest key: %s", current.getJobStage())
          .isNotEmpty();
    } else {
      assertThat(current.getCurrentManifestKey())
          .describedAs("stage must not have a manifest key: %s", current.getJobStage())
          .isNull();
    }

    // LastCompleted fields must always be present or absent at same time.
    boolean hasCompletedManifest = current.getLastCompletedManifestKey() != null;
    boolean hasCompletedTimestamp = current.getLastCompletedTimestamp() != null;
    assertEquals(hasCompletedManifest, hasCompletedTimestamp, "lastCompleted fields do not match");

    // LastCompleted fields carry forward from previous event unless this event is a new completion.
    if (hasCompletedManifest) {
      if (current.getJobStage() == JobStage.CompletedManifest) {
        assertEquals(current.getLastCompletedManifestKey(), current.getCurrentManifestKey());
        assertEquals(current.getLastCompletedTimestamp(), current.getCurrentTimestamp());
      } else {
        assertNotNull(previous, "lastCompleted fields carrying forward without previous event");
        assertEquals(current.getLastCompletedManifestKey(), previous.getLastCompletedManifestKey());
        assertEquals(current.getLastCompletedTimestamp(), previous.getLastCompletedTimestamp());
      }
    }

    // If previous had a completion then current must have one too.
    if (previous != null && previous.getLastCompletedTimestamp() != null) {
      assertTrue(
          hasCompletedTimestamp, "since previous has a completed timestamp this event should too");
    }

    // NothingToDo fields must always be present or absent at same time.
    boolean statusIsNothingToDo = current.getJobStage() == JobStage.NothingToDo;
    boolean hasNothingToDoSince = current.getNothingToDoSinceTimestamp() != null;
    assertEquals(statusIsNothingToDo, hasNothingToDoSince, "NothingToDo fields do not match");
  }
}
