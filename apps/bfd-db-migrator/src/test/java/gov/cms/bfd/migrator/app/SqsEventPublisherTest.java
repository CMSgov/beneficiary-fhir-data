package gov.cms.bfd.migrator.app;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import gov.cms.bfd.sharedutils.database.DatabaseMigrationProgress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link SqsEventPublisher}. */
@ExtendWith(MockitoExtension.class)
public class SqsEventPublisherTest {
  /** Used to simulate the SQS calls. */
  @Mock private SqsDao sqsDao;

  /** Reports progress and verifies correct JSON is transmitted to the queue. */
  @Test
  void reportProgressAndVerifyMessageText() {
    final var appProgress = new MigratorProgress(MigratorProgress.Stage.Started, null);
    final var migratorProgress =
        new MigratorProgress(
            MigratorProgress.Stage.Migrating,
            new DatabaseMigrationProgress(
                DatabaseMigrationProgress.Stage.Completed, "1", "detail"));
    final var queueUrl = "queue-url";
    final var publisher = spy(new SqsEventPublisher(sqsDao, queueUrl));
    publisher.publishEvent(appProgress);
    publisher.publishEvent(migratorProgress);
    verify(sqsDao).sendMessage(queueUrl, "{\"stage\":\"Started\"}");
    verify(sqsDao)
        .sendMessage(
            queueUrl,
            "{\"migrationProgress\":{\"stage\":\"Completed\",\"migrationFile\":\"detail\",\"version\":\"1\"},\"stage\":\"Migrating\"}");
  }
}
