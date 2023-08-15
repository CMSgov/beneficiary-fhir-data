package gov.cms.bfd.migrator.app;

import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
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
   * @param messageGroupId identifies a specific sequence of messages within a FIFO queue
   * @param messageId identifies a specific message within a sequence
   * @param messageBody text of the message to send
   * @throws QueueDoesNotExistException if queue does not exist
   * @throws SqsException if the operation cannot be completed
   */
  public void sendMessage(
      String queueUrl, String messageGroupId, String messageId, String messageBody) {
    final var request =
        SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody).build();
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
    final var createQueueRequest = CreateQueueRequest.builder().queueName(queueName).build();
    final var response = sqsClient.createQueue(createQueueRequest);
    return response.queueUrl();
  }

  /**
   * Try to pull a message from the queue and return it. If none exists returns an empty optional,
   * otherwise returns the message's body.
   *
   * @param queueUrl identifies the queue to read from
   * @return empty if no message, otherwise the message body
   * @throws QueueDoesNotExistException if queue does not exist
   * @throws SqsException if the operation cannot be completed
   */
  public Optional<String> nextMessage(String queueUrl) {
    final var request =
        ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(1).build();
    List<Message> messages = sqsClient.receiveMessage(request).messages();
    return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0).body());
  }
}
