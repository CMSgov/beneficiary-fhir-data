package gov.cms.bfd.migrator.app;

import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@AllArgsConstructor
public class SqsDao {
  private final SqsClient sqsClient;

  public String lookupQueueUrl(String queueName) {
    final var request = GetQueueUrlRequest.builder().queueName(queueName).build();
    final var response = sqsClient.getQueueUrl(request);
    return response.queueUrl();
  }

  public void sendMessage(String queueUrl, String messageBody) {
    SendMessageRequest sendMessageRequest =
        SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody).build();
    sqsClient.sendMessage(sendMessageRequest);
  }
}
