package gov.cms.bfd.migrator.app;

import gov.cms.bfd.sharedutils.database.DatabaseMigrationStage;
import javax.annotation.Nullable;
import lombok.Data;

@Data
public class MigratorProgress {
  enum Stage {
    Started,
    Connected,
    Migrating,
    Finished,
    Failed
  }

  private final Stage stage;
  @Nullable private final DatabaseMigrationStage migrationProgress;
}
