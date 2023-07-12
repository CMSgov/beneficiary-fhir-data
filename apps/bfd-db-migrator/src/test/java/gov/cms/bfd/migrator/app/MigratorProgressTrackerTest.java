package gov.cms.bfd.migrator.app;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.sharedutils.database.DatabaseMigrationStage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MigratorProgressTracker}. */
class MigratorProgressTrackerTest {
  /** Calls all possible methods and ensures they are passed on to the reporter as expected. */
  @Test
  void testAllMethods() {
    var progressList = new ArrayList<MigratorProgress>();
    var tracker = new MigratorProgressTracker(progressList::add);
    tracker.appStarted();
    tracker.appConnected();
    tracker.appFinished();
    tracker.appFailed();

    final var migration1 =
        new DatabaseMigrationStage(DatabaseMigrationStage.Stage.Preparing, "preparing");
    final var migration2 =
        new DatabaseMigrationStage(DatabaseMigrationStage.Stage.Finished, "finished");

    tracker.migrating(migration1);
    tracker.migrating(migration2);

    assertEquals(
        List.of(
            new MigratorProgress(MigratorProgress.Stage.Started, null),
            new MigratorProgress(MigratorProgress.Stage.Connected, null),
            new MigratorProgress(MigratorProgress.Stage.Connected, null),
            new MigratorProgress(MigratorProgress.Stage.Failed, null),
            new MigratorProgress(MigratorProgress.Stage.Migrating, migration1),
            new MigratorProgress(MigratorProgress.Stage.Migrating, migration2)),
        progressList);
  }
}
