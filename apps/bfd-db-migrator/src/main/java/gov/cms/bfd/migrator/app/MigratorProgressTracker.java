package gov.cms.bfd.migrator.app;

import gov.cms.bfd.sharedutils.database.DatabaseMigrationStage;
import java.util.function.Consumer;
import lombok.Data;

@Data
public class MigratorProgressTracker {
  private final Consumer<MigratorProgress> progressReporter;

  public void appStarted() {
    var progress = new MigratorProgress(MigratorProgress.Stage.Started, null);
    progressReporter.accept(progress);
  }

  public void appConnected() {
    var progress = new MigratorProgress(MigratorProgress.Stage.Connected, null);
    progressReporter.accept(progress);
  }

  public void appFinished() {
    var progress = new MigratorProgress(MigratorProgress.Stage.Finished, null);
    progressReporter.accept(progress);
  }

  public void appFailed() {
    var progress = new MigratorProgress(MigratorProgress.Stage.Failed, null);
    progressReporter.accept(progress);
  }

  public void migrating(DatabaseMigrationStage migrationStage) {
    var progress = new MigratorProgress(MigratorProgress.Stage.Migrating, migrationStage);
    progressReporter.accept(progress);
  }
}
