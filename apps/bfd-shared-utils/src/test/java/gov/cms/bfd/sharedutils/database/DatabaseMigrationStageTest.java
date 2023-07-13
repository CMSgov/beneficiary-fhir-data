package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

/** Unit test for {@link DatabaseMigrationStage}. */
class DatabaseMigrationStageTest {
  /** Tests constructor that accepts a {@link MigrationInfo}. */
  @Test
  void testConstructorForMigrationInfo() {
    MigrationInfo info = mock(MigrationInfo.class);
    MigrationVersion version = mock(MigrationVersion.class);
    var stage = DatabaseMigrationStage.Stage.BeforeFile;

    // with no info just set fields to null
    assertEquals(
        new DatabaseMigrationStage(stage, null, null), new DatabaseMigrationStage(stage, null));

    // with no script or version sets fields to null
    assertEquals(
        new DatabaseMigrationStage(stage, null, null), new DatabaseMigrationStage(stage, info));

    // with script and null version just sets filename
    doReturn(version).when(info).getVersion();
    doReturn("filename").when(info).getScript();
    assertEquals(
        new DatabaseMigrationStage(stage, null, "filename"),
        new DatabaseMigrationStage(stage, info));

    // with script and version just sets filename and version
    doReturn(version).when(info).getVersion();
    doReturn("100").when(version).getVersion();
    assertEquals(
        new DatabaseMigrationStage(stage, "100", "filename"),
        new DatabaseMigrationStage(stage, info));
  }
}
