package gov.cms.bfd.migrator.app;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.sharedutils.database.DatabaseMigrationProgress;
import gov.cms.bfd.sharedutils.exceptions.UncheckedIOException;
import java.io.IOException;
import java.util.UUID;
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
public class SqsProgressReporter {
  /**
   * Used to serialize the messages. Using Jackson for this is a little slower than building up the
   * JSON directly but is far less error prone. Null properties will be omitted from the JSON to
   * make it more compact. Sorting properties alphabetically can make tests more stable and make it
   * easier to find particular fields in test samples.
   */
  private final ObjectMapper objectMapper =
      JsonMapper.builder()
          .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .addModule(new Jdk8Module())
          .addModule(new JavaTimeModule())
          .serializationInclusion(JsonInclude.Include.NON_NULL)
          .build();

  /** Used to communicate with SQS. */
  private final SqsDao sqsDao;
  /** URL of the queue we post messages to. */
  private final String queueUrl;
  /** Applied to every message to identify a specific sequence for FIFO purposes. */
  private final String messageGroupId;
  /**
   * Applied to every message to uniquely identify it within a specific sequence. Explicit
   * deduplication ids prevent SQS assuming two messages with the same hash are redundant.
   */
  private final AtomicInteger nextMessageId;

  /**
   * Initialize a new instance using UUID for {@link #messageGroupId}.
   *
   * @param sqsDao used to communicate with SQS
   * @param queueUrl identifies the queue to which progress updates are sent
   */
  public SqsProgressReporter(SqsDao sqsDao, String queueUrl) {
    this(sqsDao, queueUrl, UUID.randomUUID().toString());
  }

  /**
   * Initialize a new instance.
   *
   * @param sqsDao used to communicate with SQS
   * @param queueUrl identifies the queue to which progress updates are sent
   * @param messageGroupId assigned to every message sent to queue to identify the sequence
   */
  public SqsProgressReporter(SqsDao sqsDao, String queueUrl, String messageGroupId) {
    this.sqsDao = sqsDao;
    this.queueUrl = queueUrl;
    this.messageGroupId = messageGroupId;
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
        new SqsProgressMessage(pid, progress.getStage(), progress.getMigrationProgress());
    final var messageId = String.valueOf(nextMessageId.getAndIncrement());
    final var messageText = convertMessageToJson(message);
    sqsDao.sendMessage(queueUrl, messageGroupId, messageId, messageText);
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

  /**
   * Does the conversion and wraps any checked exception in an unchecked one.
   *
   * @param message object to convert into JSON
   * @return converted JSON string
   */
  private String convertMessageToJson(SqsProgressMessage message) {
    try {
      return objectMapper.writeValueAsString(message);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  /** Java object from which the JSON message is constructed. */
  @Data
  public static class SqsProgressMessage {
    /** Our process id. */
    private final long pid;

    /** Stage of app processing. */
    private final MigratorProgress.Stage appStage;

    /** Migration stage if appropriate. */
    @Nullable private final DatabaseMigrationProgress migrationStage;
  }
}
