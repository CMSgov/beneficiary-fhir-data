package gov.cms.bfd.migrator.app;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.events.EventPublisher;
import gov.cms.bfd.migrator.app.MigratorProgressReporter.SqsProgressMessage;
import gov.cms.bfd.sharedutils.database.DatabaseMigrationProgress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link MigratorProgressReporterTest}. */
@ExtendWith(MockitoExtension.class)
class MigratorProgressReporterTest {
  /** Used to verify correct events are published. */
  @Mock private EventPublisher publisher;

  /** Reports progress and verifies correct events are published. */
  @Test
  void reportProgressAndVerifyPublishedEvents() {
    final var appProgress = new MigratorProgress(MigratorProgress.Stage.Started, null);
    final var migratorProgress =
        new MigratorProgress(
            MigratorProgress.Stage.Migrating,
            new DatabaseMigrationProgress(
                DatabaseMigrationProgress.Stage.Completed, "1", "detail"));
    final var reporter = spy(new MigratorProgressReporter(publisher));
    doReturn(5046L).when(reporter).getPid();
    reporter.reportMigratorProgress(appProgress);
    reporter.reportMigratorProgress(migratorProgress);
    verify(publisher)
        .publishEvent(new SqsProgressMessage(5046L, 1L, MigratorProgress.Stage.Started, null));
    verify(publisher)
        .publishEvent(
            new SqsProgressMessage(
                5046L,
                2L,
                MigratorProgress.Stage.Migrating,
                migratorProgress.getMigrationProgress()));
  }
}
