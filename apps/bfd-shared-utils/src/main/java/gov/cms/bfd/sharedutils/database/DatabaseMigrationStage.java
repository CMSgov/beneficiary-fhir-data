package gov.cms.bfd.sharedutils.database;

import lombok.Data;

@Data
public class DatabaseMigrationStage {
  public enum Stage {
    Preparing,
    BeforeFile,
    AfterFile,
    BeforeValidate,
    AfterValidate,
    Finished
  }

  private final Stage stage;
  private final String detail;
}
