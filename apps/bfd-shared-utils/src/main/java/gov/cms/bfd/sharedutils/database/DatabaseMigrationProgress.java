package gov.cms.bfd.sharedutils.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;

/** Embodies migration progress. Sent to a callback during flyway migration. */
@Data
@AllArgsConstructor
public class DatabaseMigrationProgress {
  /** Current stage of processing. */
  public enum Stage {
    /** Before any migration has been executed. */
    BeforeMigration,
    /** Preparing to start. */
    Preparing,
    /** About to process a file. */
    BeforeFile,
    /** Finished processing a file. */
    AfterFile,
    /** About to validate schema. */
    BeforeValidate,
    /** Finished validating schema. */
    AfterValidate,
    /** Migration complete. */
    Completed,
    /** After all migrations (if any) have been executed. */
    AfterMigration
  }

  /** Stage of processing. */
  private final Stage stage;

  /** Current version string reported by Flyway, if available. */
  @Nullable private final String version;

  /** Migration file name, if relevant to the stage. */
  @Nullable private final String migrationFile;

  /**
   * This constructor initializes an instance using the {@link MigrationInfo} from either a {@link
   * Context} passed to a Flyway {@link Callback} or a {@link MigrationInfoService} returned by
   * {@link Flyway#info}.
   *
   * @param stage value for {@link #stage}
   * @param migrationInfo used to extract values for {@link #version} and {@link #migrationFile}
   */
  @JsonCreator
  public DatabaseMigrationProgress(
      @JsonProperty("stage") DatabaseMigrationProgress.Stage stage,
      @JsonProperty("migrationInfo") @Nullable MigrationInfo migrationInfo) {
    this.stage = stage;
    version =
        (migrationInfo == null || migrationInfo.getVersion() == null)
            ? null
            : migrationInfo.getVersion().getVersion();
    migrationFile =
        (migrationInfo == null || migrationInfo.getScript() == null)
            ? null
            : migrationInfo.getScript();
  }
}
