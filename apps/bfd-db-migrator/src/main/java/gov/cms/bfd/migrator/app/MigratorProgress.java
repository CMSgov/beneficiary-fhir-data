package gov.cms.bfd.migrator.app;

import gov.cms.bfd.sharedutils.database.DatabaseMigrationProgress;
import jakarta.annotation.Nullable;
import lombok.Data;

/** Embodies facts related to completion of some milestone or stage of processing by the app. */
@Data
public class MigratorProgress {
  /** Stage of processing in the app itself. */
  enum Stage {
    /** App has started but hasn't done anything yet. */
    Started,
    /** App has successfully connected to the database. */
    Connected,
    /** App has completed a migration step. */
    Migrating,
    /** App has finished processing with no errors. */
    Finished,
    /** App has terminated processing due to some error. */
    Failed
  }

  /** Stage being reported. */
  private final Stage stage;

  /** Optional details when {@link #stage} is {@link Stage#Migrating}. */
  @Nullable private final DatabaseMigrationProgress migrationProgress;
}
