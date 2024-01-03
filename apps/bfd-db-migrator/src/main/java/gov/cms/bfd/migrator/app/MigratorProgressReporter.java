package gov.cms.bfd.migrator.app;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.events.EventPublisher;
import gov.cms.bfd.sharedutils.database.DatabaseMigrationProgress;
import gov.cms.bfd.sharedutils.exceptions.UncheckedIOException;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.sqs.model.SqsException;

/**
 * Uses an {@link SqsDao} to post JSON messages to an SQS queue for each {@link MigratorProgress}.
 */
@RequiredArgsConstructor
public class MigratorProgressReporter {
  /** Used to publish the progress messages. */
  private final EventPublisher eventPublisher;

  /**
   * Applied to every message to uniquely identify it within a specific sequence. Explicit
   * deduplication ids prevent SQS assuming two messages with the same hash are redundant.
   */
  private final AtomicInteger nextMessageId;

  /**
   * Initialize a new instance.
   *
   * @param eventPublisher used to publish progress messages
   */
  public MigratorProgressReporter(EventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
    this.nextMessageId = new AtomicInteger(1);
  }

  /**
   * Converts the progress object into JSON and sends it to SQS. Method signature allows it to be
   * used as a {@link Consumer} argument when creating a {@link MigratorProgressTracker}.
   *
   * @param progress the progress to report
   * @throws SqsException if SQS message send failed
   * @throws UncheckedIOException if conversion to JSON fails
   */
  public void reportMigratorProgress(MigratorProgress progress) {
    final long pid = getPid();
    final var message =
        new SqsProgressMessage(
            pid,
            nextMessageId.getAndIncrement(),
            progress.getStage(),
            progress.getMigrationProgress());
    eventPublisher.publishEvent(message);
  }

  /**
   * Looks up the process id. Visible here to allow test to mock the call.
   *
   * @return process id
   */
  @VisibleForTesting
  long getPid() {
    return ProcessHandle.current().pid();
  }

  /** Java object from which the JSON message is constructed. */
  @Data
  public static class SqsProgressMessage {
    /** Used to sort messages in original send order when testing. */
    static final Comparator<SqsProgressMessage> SORT_BY_IDS =
        Comparator.comparingLong(SqsProgressMessage::getPid)
            .thenComparingLong(SqsProgressMessage::getMessageId);

    /** Our process id. */
    private final long pid;

    /** Unique id for this message (relative to pid). Can be used for sorting messages. */
    private final long messageId;

    /** Stage of app processing. */
    private final MigratorProgress.Stage appStage;

    /** Migration stage if appropriate. */
    @Nullable private final DatabaseMigrationProgress migrationStage;

    /**
     * Initializes an instance. Has approperiate Jackson annotations to allow deserialization of
     * JSON into an instance.
     *
     * @param pid the {@link #pid}
     * @param messageId the {@link #messageId}
     * @param appStage the {@link #appStage}
     * @param migrationStage the {@link #migrationStage}
     */
    @JsonCreator
    public SqsProgressMessage(
        @JsonProperty("pid") long pid,
        @JsonProperty("messageId") long messageId,
        @JsonProperty("appStage") MigratorProgress.Stage appStage,
        @JsonProperty("migrationStage") @Nullable DatabaseMigrationProgress migrationStage) {
      this.pid = pid;
      this.messageId = messageId;
      this.appStage = appStage;
      this.migrationStage = migrationStage;
    }
  }
}
