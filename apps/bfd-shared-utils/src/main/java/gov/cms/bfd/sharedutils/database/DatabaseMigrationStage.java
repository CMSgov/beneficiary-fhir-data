package gov.cms.bfd.sharedutils.database;

import lombok.Data;

/** Embodies migration progress. Sent to a callback during flyway migration. */
@Data
public class DatabaseMigrationStage {
  /** Current stage of processing. */
  public enum Stage {
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
    Completed
  }

  /** Stage of processing. */
  private final Stage stage;
  /** Any detail or empty string. Currently just the current migration file name. */
  private final String detail;
}
