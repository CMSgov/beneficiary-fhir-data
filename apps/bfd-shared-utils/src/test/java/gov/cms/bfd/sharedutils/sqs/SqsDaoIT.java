package gov.cms.bfd.sharedutils.sqs;

import static gov.cms.bfd.SqsTestUtils.createSqsClientForLocalStack;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

import gov.cms.bfd.AbstractLocalStackTest;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

/** Integration tests for {@link SqsDao}. */
class SqsDaoIT extends AbstractLocalStackTest {
  /** Will be connected to the localstack SQS service. */
  private SqsDao dao;

  /** Create the {@link SqsDao} connected to our localstack SQS service. */
  @BeforeEach
  void setUp() {
    SqsClient client = createSqsClientForLocalStack(localstack);
    dao = new SqsDao(client);
  }

  /** Test creating a queue. */
  @Test
  void createQueue() {
    String queueName = "my-created-queue";
    String createdQueueUri = dao.createQueue(queueName);
    String lookupQueueUri = dao.lookupQueueUrl(queueName);
    assertEquals(createdQueueUri, lookupQueueUri);
  }

  /** Test sending and receiving. */
  @Test
  void sendAndReceiveMessages() {
    String queueName = "my-test-queue";
    String queueUri = dao.createQueue(queueName);
    String message1 = "this is a first message";
    String message2 = "this is a second message";
    dao.sendMessage(queueUri, message1);
    dao.sendMessage(queueUri, message2);

    // SQS does not guarantee messages will be received in order so we just collect all
    // of them and compare to all that we sent.
    Set<String> receivedMessages = new HashSet<>();
    dao.processAllMessages(queueUri, receivedMessages::add);
    assertEquals(Set.of(message1, message2), receivedMessages);
    assertEquals(Optional.empty(), dao.nextMessage(queueUri));
  }

  /** Test conditions that can throw {@link QueueDoesNotExistException}. */
  @Test
  void variousNonExistentQueueScenarios() {
    assertThatThrownBy(() -> dao.lookupQueueUrl("no-such-queue-exists"))
        .isInstanceOf(QueueDoesNotExistException.class);
    assertThatThrownBy(() -> dao.sendMessage("no-such-queue-exists", "not gonna make it there"))
        .isInstanceOf(QueueDoesNotExistException.class);
    assertThatThrownBy(() -> dao.nextMessage("no-such-queue-exists"))
        .isInstanceOf(QueueDoesNotExistException.class);
  }
}
