package gov.cms.bfd.pipeline.ccw.rif;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.sharedutils.json.JsonConverter;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link CcwRifLoadJobStatusEvent}. */
public class CcwRifLoadJobStatusEventTest {
  /** Converter used during testing. */
  private final JsonConverter jsonConverter = JsonConverter.minimalInstance();

  /** Use a fixed time for all tests. */
  private final Instant currentTime =
      java.time.LocalDateTime.of(2024, 1, 9, 16, 46, 0).toInstant(ZoneOffset.UTC);

  /** Verify round trip for an event with only the required fields present. */
  @Test
  void testMinimalEvent() {
    final var event =
        CcwRifLoadJobStatusEvent.builder()
            .jobStage(CcwRifLoadJobStatusEvent.JobStage.NothingToDo)
            .currentTimestamp(currentTime)
            .build();
    verifyRoundTripJsonConversion(event);
  }

  /** Verify round trip for an event with all fields present. */
  @Test
  void testFullEvent() {
    final var event =
        CcwRifLoadJobStatusEvent.builder()
            .jobStage(CcwRifLoadJobStatusEvent.JobStage.CompletedManifest)
            .currentTimestamp(currentTime)
            .nothingToDoSinceTimestamp(currentTime.minus(10, ChronoUnit.SECONDS))
            .currentManifestKey("manifest")
            .lastCompletedManifestKey("completed")
            .lastCompletedTimestamp(currentTime.minus(5, ChronoUnit.SECONDS))
            .build();
    verifyRoundTripJsonConversion(event);
  }

  /**
   * Verifies that an event can be converted to JSON and back to an equivalent object instance.
   *
   * @param original object to verify
   */
  void verifyRoundTripJsonConversion(CcwRifLoadJobStatusEvent original) {
    final var json = jsonConverter.objectToJson(original);
    final var converted = jsonConverter.jsonToObject(json, CcwRifLoadJobStatusEvent.class);
    assertEquals(original, converted);
  }
}
