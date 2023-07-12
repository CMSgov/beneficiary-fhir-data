package gov.cms.bfd.migrator.app;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.sharedutils.database.DatabaseMigrationStage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link SqsProgressReporterTest}. */
@ExtendWith(MockitoExtension.class)
class SqsProgressReporterTest {
  /** Used to simulate the SQS calls. */
  @Mock private SqsDao sqsDao;

  /** Reports progress and verifies correct JSON is transmitted to the queue. */
  @Test
  void reportProgressAndVerifyMessageText() {
    final var appProgress = new MigratorProgress(MigratorProgress.Stage.Started, null);
    final var migratorProgress =
        new MigratorProgress(
            MigratorProgress.Stage.Migrating,
            new DatabaseMigrationStage(DatabaseMigrationStage.Stage.Finished, "detail"));
    final var queueUrl = "queue-url";
    final var reporter = spy(new SqsProgressReporter(sqsDao, queueUrl));
    doReturn(5046L).when(reporter).getPid();
    reporter.reportMigratorProgress(appProgress);
    reporter.reportMigratorProgress(migratorProgress);
    verify(sqsDao).sendMessage(queueUrl, "{\"appStage\":\"Started\",\"pid\":5046}");
    verify(sqsDao)
        .sendMessage(
            queueUrl,
            "{\"appStage\":\"Migrating\",\"migrationStage\":{\"detail\":\"detail\",\"stage\":\"Finished\"},\"pid\":5046}");
  }
}
