package gov.cms.bfd.migrator.app;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

/** Data access object that encapsulate interactions with SQS. */
@AllArgsConstructor
public class SqsDao {
  /** Used to communicate with SQS. */
  private final SqsClient sqsClient;

  /**
   * Looks up the URL associated with a given queue name.
   *
   * @param queueName name to look up
   * @return the URL
   * @throws QueueDoesNotExistException if queue does not exist
   * @throws SqsException if the operation cannot be completed
   */
  public String lookupQueueUrl(String queueName) {
    final var request = GetQueueUrlRequest.builder().queueName(queueName).build();
    final var response = sqsClient.getQueueUrl(request);
    return response.queueUrl();
  }

  /**
   * Sends a message to an SQS queue.
   *
   * @param queueUrl identifies the queue to post the message to
   * @param messageBody text of the message to send
   * @param messageGroupId the message group id
   * @throws QueueDoesNotExistException if queue does not exist
   * @throws SqsException if the operation cannot be completed
   */
  public void sendMessage(String queueUrl, String messageBody, String messageGroupId) {
    final var request =
        SendMessageRequest.builder()
            .queueUrl(queueUrl)
            .messageBody(messageBody)
            .messageGroupId(messageGroupId)
            .build();
    sqsClient.sendMessage(request);
  }

  /**
   * Create a FIFO queue with the given name and return its URL. This is very basic and intended
   * only for use in tests.
   *
   * @param queueName name of queue to create
   * @return URL of created queue
   * @throws SqsException if the operation cannot be completed
   */
  public String createQueue(String queueName) {
    Map<QueueAttributeName, String> attributes = new HashMap<>();
    attributes.put(QueueAttributeName.FIFO_QUEUE, Boolean.TRUE.toString());
    attributes.put(QueueAttributeName.CONTENT_BASED_DEDUPLICATION, Boolean.TRUE.toString());
    final var createQueueRequest =
        CreateQueueRequest.builder().queueName(queueName).attributes(attributes).build();
    final var response = sqsClient.createQueue(createQueueRequest);
    return response.queueUrl();
  }

  /**
   * Try to pull a message from the queue and return it. If none exists returns an empty optional,
   * otherwise returns the message's body. Deletes the message after consumption.
   *
   * @param queueUrl identifies the queue to read from
   * @return empty if no message, otherwise the {@link SqsMessage} message
   * @throws QueueDoesNotExistException if queue does not exist
   * @throws SqsException if the operation cannot be completed
   */
  public Optional<SqsMessage> nextMessage(String queueUrl) {
    final var request =
        ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(1).build();
    List<Message> messages = sqsClient.receiveMessage(request).messages();
    if (messages.isEmpty()) return Optional.empty();
    final var message = messages.get(0);
    return Optional.of(
        SqsMessage.builder().body(message.body()).receiptHandle(message.receiptHandle()).build());
  }

  /**
   * Read all currently available messages and pass them to the provided function.
   *
   * @param queueUrl identifies the queue to read from
   * @param consumer a function to receive each message
   * @throws QueueDoesNotExistException if queue does not exist
   * @throws SqsException if the operation cannot be completed
   */
  public void processAllMessages(String queueUrl, Consumer<String> consumer) {
    for (Optional<SqsMessage> message = nextMessage(queueUrl);
        message.isPresent();
        message = nextMessage(queueUrl)) {
      consumer.accept(message.get().body());
      deleteMessage(queueUrl, message.get());
    }
  }

  /**
   * Deletes the specified message from the queue.
   *
   * @param queueUrl identifies the queue to read from
   * @param message the {@link SqsMessage} message to delete
   * @throws SqsException if the operation cannot be completed
   */
  public void deleteMessage(String queueUrl, SqsMessage message) {
    final var request =
        DeleteMessageRequest.builder()
            .queueUrl(queueUrl)
            .receiptHandle(message.receiptHandle())
            .build();
    sqsClient.deleteMessage(request);
  }
}

/**
 * Immutable {@link Record} holding the relevant information required to process SQS messages.
 *
 * @param body the body of the message
 * @param receiptHandle the receipt handle of the message; identifies messages during deletion
 */
@Builder
record SqsMessage(String body, String receiptHandle) {}
