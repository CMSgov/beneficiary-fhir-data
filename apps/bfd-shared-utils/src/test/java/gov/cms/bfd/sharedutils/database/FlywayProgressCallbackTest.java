package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.junit.jupiter.api.Test;

/** Unit test for {@link FlywayProgressCallback}. */
class FlywayProgressCallbackTest {
  /** Try all supported event types and ensure they pass the expected result to the callback. */
  @Test
  void verifySupportedEventsPassThroughToFunction() {
    // Create mocks for the arguments that would be passed to our callback normally.
    MigrationInfo info = mock(MigrationInfo.class);
    doReturn("filename").when(info).getScript();

    MigrationVersion version = mock(MigrationVersion.class);
    doReturn(version).when(info).getVersion();
    doReturn("100").when(version).getVersion();

    Context context = mock(Context.class);
    doReturn(info).when(context).getMigrationInfo();

    // Build up a set of expected mappings.
    Set<DatabaseMigrationProgress> expectedMappings =
        FlywayProgressCallback.SUPPORTED_EVENTS_MAPPING.values().stream()
            .map(stage -> new DatabaseMigrationProgress(stage, info))
            .collect(Collectors.toSet());

    // Invoke the callback to get a set of actual mappings that it produces.
    Set<DatabaseMigrationProgress> actualMappings = new HashSet<>();
    FlywayProgressCallback callback = new FlywayProgressCallback(actualMappings::add);
    Stream.of(Event.values())
        .filter(FlywayProgressCallback.SUPPORTED_EVENTS_MAPPING::containsKey)
        .forEach(event -> callback.handle(event, context));

    // They better match!
    assertEquals(expectedMappings, actualMappings);
  }

  /** Try all unsupported event types and verify that they throw an exception. */
  @Test
  void verifyUnsupportedEventsThrowAnException() {
    // Create mocks for the arguments that would be passed to our callback normally.
    MigrationInfo info = mock(MigrationInfo.class);
    doReturn("filename").when(info).getScript();

    MigrationVersion version = mock(MigrationVersion.class);
    doReturn(version).when(info).getVersion();
    doReturn("100").when(version).getVersion();

    Context context = mock(Context.class);
    doReturn(info).when(context).getMigrationInfo();

    // Invoke the callback only for unsupported events and ensure they all throw.
    Set<DatabaseMigrationProgress> actualMappings = new HashSet<>();
    FlywayProgressCallback callback = new FlywayProgressCallback(actualMappings::add);
    for (Event event : Event.values()) {
      if (!callback.supports(event, context)) {
        assertThrows(BadCodeMonkeyException.class, () -> callback.handle(event, context));
      }
    }

    // nothing should have been collected in the process
    assertEquals(0, actualMappings.size());
  }
}
