package gov.cms.bfd.sharedutils.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

/** Unit test for {@link DatabaseMigrationProgress}. */
class DatabaseMigrationProgressTest {
  /** Tests constructor that accepts a {@link MigrationInfo}. */
  @Test
  void testConstructorForMigrationInfo() {
    MigrationInfo info = mock(MigrationInfo.class);
    MigrationVersion version = mock(MigrationVersion.class);
    var stage = DatabaseMigrationProgress.Stage.BeforeFile;

    // with no info just set fields to null
    assertEquals(
        new DatabaseMigrationProgress(stage, null, null),
        new DatabaseMigrationProgress(stage, null));

    // info with no script or version sets fields to null
    assertEquals(
        new DatabaseMigrationProgress(stage, null, null),
        new DatabaseMigrationProgress(stage, info));

    // info with script and null version just sets filename
    doReturn(version).when(info).getVersion();
    doReturn("filename").when(info).getScript();
    assertEquals(
        new DatabaseMigrationProgress(stage, null, "filename"),
        new DatabaseMigrationProgress(stage, info));

    // info with script and version just sets filename and version
    doReturn(version).when(info).getVersion();
    doReturn("100").when(version).getVersion();
    assertEquals(
        new DatabaseMigrationProgress(stage, "100", "filename"),
        new DatabaseMigrationProgress(stage, info));
  }
}
